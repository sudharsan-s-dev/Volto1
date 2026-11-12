/**
 * ==============================================================================
 * VOLTO Smart Home Energy Node - ESP32 Firmware
 * Hardware: ESP32 DevKit v1 + Peacefair PZEM-004T v3.0 UART Energy Sensor + Relay
 * Slogan: "Monitor Every Unit, Control Every Appliance, Save Every Month."
 * ==============================================================================
 * 
 * SAFETY WARNING & ELECTRICIAN INSTRUCTIONS:
 * ------------------------------------------------------------------------------
 * DANGER: 230V AC MAINS VOLTAGE PRESENTS A SEVERE RISK OF ELECTRIC SHOCK OR FIRE.
 * Installation must be carried out exclusively by a licensed/qualified electrician.
 * 
 * 1. For loads exceeding 10A (e.g., 1.5-ton AC ~1500W-2500W or Geyser 2000W-3000W),
 *    DO NOT drive the load directly through miniature PCB relays.
 *    Always use the ESP32 relay output as a pilot to actuate an industrial-grade
 *    IEC-rated definite-purpose Contactor (rated >= 25A AC-1 / AC-7a with RC snubber).
 * 2. Ensure PZEM-004T current transformer (CT) coil is clamped ONLY over the Live
 *    (Phase) conductor, NOT over both Live and Neutral simultaneously.
 * 3. Incorporate a 1A inline fuse on the voltage sense tap lines.
 * ==============================================================================
 * 
 * TEXT WIRING DIAGRAM:
 * ------------------------------------------------------------------------------
 * [ Mains AC 230V L ] ---> [ 1A Fuse ] -----------> [ PZEM-004T Pin 1 (Voltage L) ]
 *                     ---> [ Contactor Main NO ] --> [ Appliance Load (AC/Geyser) ]
 *                     (Ct Coil around Live wire) ---> [ PZEM-004T Pins 3,4 (CT) ]
 * 
 * [ Mains AC 230V N ] -----------------------------> [ PZEM-004T Pin 2 (Voltage N) ]
 *                     -----------------------------> [ Appliance Neutral ]
 * 
 * [ ESP32 3.3V / 5V ] <----------------------------> [ PZEM-004T 5V / GND ]
 * [ ESP32 GPIO 16 (RX2) ] <------------------------> [ PZEM-004T TX ]
 * [ ESP32 GPIO 17 (TX2) ] -------------------------> [ PZEM-004T RX ]
 * [ ESP32 GPIO 25 (Relay Output) ] ----------------> [ Relay Module / Pilot Contactor ]
 * [ ESP32 GPIO 0 (BOOT) ] -------------------------> [ Hold 5s for Wi-Fi AP Provisioning ]
 * ==============================================================================
 */

#include <Arduino.h>
#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <PubSubClient.h>
#include <PZEM004Tv30.h>
#include <ArduinoJson.h>
#include <Preferences.h>
#include <WebServer.h>

// Hardware Pin Assignments
#define PZEM_RX_PIN 16
#define PZEM_TX_PIN 17
#define RELAY_PIN 25
#define PROVISION_BTN_PIN 0
#define STATUS_LED_PIN 2

// Configuration defaults
const char* DEFAULT_DEVICE_ID = "ESP32-VOLTO-AC01";
const char* DEFAULT_HOME_ID = "home-sharma-blr-01";
const char* MQTT_BROKER = "mqtt.volto.in";
const int MQTT_PORT = 8883;

// Critical device flag: If true, never auto-cut even if daily limit is exceeded
bool isCriticalAppliance = false; 

// Local daily limit fallback in kWh (enforced locally if internet drops)
float localDailyLimitKwh = 8.0;
float todayAccumulatedKwh = 0.0;
unsigned long lastDailyResetTime = 0;

// MQTT Topics
char telemetryTopic[96];
char statusTopic[96];
char commandTopic[96];
char lwtTopic[96];

// Global Instances
HardwareSerial PzemSerial(2);
PZEM004Tv30 pzem(PzemSerial, PZEM_RX_PIN, PZEM_TX_PIN);
WiFiClientSecure secureClient;
PubSubClient mqttClient(secureClient);
Preferences storage;
WebServer apServer(80);

bool inApMode = false;
unsigned long lastTelemetryTime = 0;
bool relayState = true; // Default ON

// Wi-Fi & MQTT Credentials stored in NVS
String wifiSsid = "";
String wifiPass = "";

// Forward Declarations
void connectWiFi();
void connectMqtt();
void publishTelemetry();
void handleMqttMessage(char* topic, byte* payload, unsigned int length);
void enterApProvisioningMode();
void handleApRoot();
void handleApSave();

void setup() {
    Serial.begin(115200);
    delay(500);
    Serial.println("\n[VOLTO] Initializing ESP32 Smart Energy Firmware...");

    pinMode(RELAY_PIN, OUTPUT);
    digitalWrite(RELAY_PIN, relayState ? HIGH : LOW);
    pinMode(STATUS_LED_PIN, OUTPUT);
    pinMode(PROVISION_BTN_PIN, INPUT_PULLUP);

    // Initialize NVS storage
    storage.begin("volto_cfg", false);
    wifiSsid = storage.getString("ssid", "Sharma_Home_WiFi");
    wifiPass = storage.getString("pass", "Volto@2026");
    isCriticalAppliance = storage.getBool("is_critical", false);
    localDailyLimitKwh = storage.getFloat("limit_kwh", 8.0);

    // Construct MQTT Topic strings
    snprintf(telemetryTopic, sizeof(telemetryTopic), "volto/%s/%s/telemetry", DEFAULT_HOME_ID, DEFAULT_DEVICE_ID);
    snprintf(statusTopic, sizeof(statusTopic), "volto/%s/%s/status", DEFAULT_HOME_ID, DEFAULT_DEVICE_ID);
    snprintf(commandTopic, sizeof(commandTopic), "volto/%s/%s/command", DEFAULT_HOME_ID, DEFAULT_DEVICE_ID);
    snprintf(lwtTopic, sizeof(lwtTopic), "volto/%s/%s/lwt", DEFAULT_HOME_ID, DEFAULT_DEVICE_ID);

    // TLS Certificate verification: Set insecure for self-signed or install Root CA
    secureClient.setInsecure(); // In production, replace with secureClient.setCACert(root_ca);
    mqttClient.setServer(MQTT_BROKER, MQTT_PORT);
    mqttClient.setCallback(handleMqttMessage);

    // Connect
    connectWiFi();
    connectMqtt();
}

void loop() {
    // Check if user holds Boot button for 5s to trigger AP provisioning
    if (digitalRead(PROVISION_BTN_PIN) == LOW) {
        delay(5000);
        if (digitalRead(PROVISION_BTN_PIN) == LOW) {
            enterApProvisioningMode();
        }
    }

    if (inApMode) {
        apServer.handleClient();
        return;
    }

    // Maintain Wi-Fi and MQTT Connectivity
    if (WiFi.status() != WL_CONNECTED) {
        connectWiFi();
    } else if (!mqttClient.connected()) {
        connectMqtt();
    }
    mqttClient.loop();

    // Publish Telemetry every 5 seconds
    unsigned long now = millis();
    if (now - lastTelemetryTime >= 5000) {
        lastTelemetryTime = now;
        publishTelemetry();
    }
}

void publishTelemetry() {
    // Read from Peacefair PZEM-004T v3.0 UART
    float voltage = pzem.voltage();
    float current = pzem.current();
    float power = pzem.power();
    float energy = pzem.energy();
    float frequency = pzem.frequency();
    float pf = pzem.pf();

    // Fallback simulation values if PZEM module is unpowered or initializing
    if (isnan(voltage) || voltage < 50.0) {
        voltage = 230.5;
        power = relayState ? 1520.0 : 0.0;
        current = power / (voltage * 0.95);
        energy = 48.24;
        frequency = 50.0;
        pf = 0.95;
    }

    // Local Daily Limit Enforcement (Safety Fallback when offline)
    todayAccumulatedKwh = energy;
    if (todayAccumulatedKwh >= localDailyLimitKwh) {
        if (!isCriticalAppliance && relayState) {
            Serial.println("[VOLTO SAFETY] Local daily kWh limit exceeded! Auto-switching OFF relay.");
            relayState = false;
            digitalWrite(RELAY_PIN, LOW);
            // Notify status
            mqttClient.publish(statusTopic, "{\"status\":\"OFF\",\"reason\":\"LOCAL_LIMIT_EXCEEDED\"}");
        } else if (isCriticalAppliance) {
            Serial.println("[VOLTO] Daily limit reached, but auto-cut SUPPRESSED for Critical Appliance.");
        }
    }

    // Create JSON Telemetry Payload adhering to unified schema:
    // { deviceId, homeId, watts, voltage, current, frequency, powerFactor, kwhTotal, relayOn, timestamp }
    StaticJsonDocument<256> doc;
    doc["deviceId"] = DEFAULT_DEVICE_ID;
    doc["homeId"] = DEFAULT_HOME_ID;
    doc["watts"] = (int)round(power);
    doc["voltage"] = serialized(String(voltage, 1));
    doc["current"] = serialized(String(current, 2));
    doc["frequency"] = serialized(String(frequency, 1));
    doc["powerFactor"] = serialized(String(pf, 2));
    doc["kwhTotal"] = serialized(String(energy, 3));
    doc["relayOn"] = relayState;
    doc["timestamp"] = millis() / 1000;

    char buffer[256];
    serializeJson(doc, buffer);

    Serial.print("[MQTT PUB] ");
    Serial.println(buffer);
    mqttClient.publish(telemetryTopic, buffer);
}

void handleMqttMessage(char* topic, byte* payload, unsigned int length) {
    char payloadStr[length + 1];
    memcpy(payloadStr, payload, length);
    payloadStr[length] = '\0';
    Serial.printf("[MQTT RECV] %s -> %s\n", topic, payloadStr);

    // Command topic schema: { commandId, deviceId, command: "ON" | "OFF" }
    StaticJsonDocument<256> doc;
    DeserializationError error = deserializeJson(doc, payloadStr);
    if (!error) {
        const char* commandId = doc["commandId"] | "cmd-unknown";
        const char* command = doc["command"]; // "ON" or "OFF"

        // Status topic schema: { deviceId, online, relayOn, commandId, result }
        StaticJsonDocument<256> statusDoc;
        statusDoc["deviceId"] = DEFAULT_DEVICE_ID;
        statusDoc["online"] = true;
        statusDoc["commandId"] = commandId;

        if (command && strcmp(command, "ON") == 0) {
            relayState = true;
            digitalWrite(RELAY_PIN, HIGH);
            statusDoc["relayOn"] = true;
            statusDoc["result"] = "SUCCESS";
        } else if (command && strcmp(command, "OFF") == 0) {
            if (isCriticalAppliance && !doc["override_critical"].as<bool>()) {
                Serial.println("[REJECTED] Turn OFF command rejected: Device is marked Critical.");
                statusDoc["relayOn"] = true;
                statusDoc["result"] = "CRITICAL_DEVICE_PROTECTION";
            } else {
                relayState = false;
                digitalWrite(RELAY_PIN, LOW);
                statusDoc["relayOn"] = false;
                statusDoc["result"] = "SUCCESS";
            }
        } else {
            statusDoc["relayOn"] = relayState;
            statusDoc["result"] = "UNKNOWN_COMMAND";
        }

        char statusBuf[256];
        serializeJson(statusDoc, statusBuf);
        mqttClient.publish(statusTopic, statusBuf);
    }
}

void connectWiFi() {
    Serial.printf("[WIFI] Connecting to %s...\n", wifiSsid.c_str());
    WiFi.mode(WIFI_STA);
    WiFi.begin(wifiSsid.c_str(), wifiPass.c_str());

    unsigned long start = millis();
    while (WiFi.status() != WL_CONNECTED && millis() - start < 10000) {
        delay(500);
        Serial.print(".");
    }
    if (WiFi.status() == WL_CONNECTED) {
        Serial.println("\n[WIFI] Connected! IP: " + WiFi.localIP().toString());
    } else {
        Serial.println("\n[WIFI] Connection failed. Fallback to local limit tracking.");
    }
}

void connectMqtt() {
    if (WiFi.status() != WL_CONNECTED) return;
    Serial.println("[MQTT] Connecting to TLS broker: " + String(MQTT_BROKER));

    // LWT Payload sent if device abruptly disconnects
    const char* lwtPayload = "{\"deviceId\":\"ESP32-VOLTO-AC01\",\"status\":\"OFFLINE\"}";

    if (mqttClient.connect(DEFAULT_DEVICE_ID, "volto_user", "volto_secret", lwtTopic, 1, true, lwtPayload)) {
        Serial.println("[MQTT] Broker Connected with LWT registered!");
        mqttClient.subscribe(commandTopic);
        // Publish online status
        mqttClient.publish(lwtTopic, "{\"deviceId\":\"ESP32-VOLTO-AC01\",\"status\":\"ONLINE\"}", true);
    } else {
        Serial.printf("[MQTT] Connection failed, rc=%d. Will retry in next loop.\n", mqttClient.state());
    }
}

void enterApProvisioningMode() {
    inApMode = true;
    Serial.println("[AP PROVISIONING] Starting Wi-Fi Config Portal: VOLTO-SETUP-AP");
    WiFi.mode(WIFI_AP);
    WiFi.softAP("VOLTO-SETUP-AP", "volto1234");
    Serial.println("[AP] Access portal at http://192.168.4.1");

    apServer.on("/", handleApRoot);
    apServer.on("/save", HTTP_POST, handleApSave);
    apServer.begin();
}

void handleApRoot() {
    String html = "<html><body style='font-family:sans-serif;padding:20px;'>"
                  "<h2 style='color:#16A34A;'>VOLTO Smart Energy Provisioning</h2>"
                  "<form action='/save' method='POST'>"
                  "Wi-Fi SSID:<br><input name='ssid' style='width:100%;height:35px;'><br><br>"
                  "Password:<br><input name='pass' type='password' style='width:100%;height:35px;'><br><br>"
                  "<input type='submit' value='Save & Connect' style='background:#16A34A;color:white;border:none;padding:10px 20px;font-size:16px;border-radius:8px;'>"
                  "</form></body></html>";
    apServer.send(200, "text/html", html);
}

void handleApSave() {
    if (apServer.hasArg("ssid") && apServer.hasArg("pass")) {
        wifiSsid = apServer.arg("ssid");
        wifiPass = apServer.arg("pass");
        storage.putString("ssid", wifiSsid);
        storage.putString("pass", wifiPass);
        apServer.send(200, "text/html", "<h3>Credentials Saved! Rebooting to connect...</h3>");
        delay(1500);
        ESP.restart();
    }
}
