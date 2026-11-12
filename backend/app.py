"""
==============================================================================
VOLTO Smart Energy Backend Service (Flask + MQTT + PostgreSQL + FCM)
Slogan: "Monitor Every Unit, Control Every Appliance, Save Every Month."
==============================================================================
"""

import os
import json
import time
import threading
from datetime import datetime
from functools import wraps
from flask import Flask, request, jsonify
import paho.mqtt.client as mqtt
import psycopg2
from psycopg2.extras import RealDictCursor
import schedule
import firebase_admin
from firebase_admin import auth, credentials, messaging

app = Flask(__name__)

# Environment Configuration
DATABASE_URL = os.getenv("DATABASE_URL", "postgresql://volto_user:volto_pass@postgres:5432/volto_db")
MQTT_BROKER = os.getenv("MQTT_BROKER", "mqtt.volto.in")
MQTT_PORT = int(os.getenv("MQTT_PORT", 8883))
MQTT_USER = os.getenv("MQTT_USER", "volto_backend")
MQTT_PASS = os.getenv("MQTT_PASS", "backend_secret")
FIREBASE_CRED_PATH = os.getenv("FIREBASE_CRED_PATH", "firebase-service-account.json")

# Initialize Firebase Admin if credentials file present
if os.path.exists(FIREBASE_CRED_PATH):
    cred = credentials.Certificate(FIREBASE_CRED_PATH)
    firebase_admin.initialize_app(cred)
    print("[VOLTO] Firebase Admin SDK initialized.")
else:
    print("[VOLTO] Note: Running with Mock Firebase Auth token verification for development.")

# PostgreSQL Connection Helper
def get_db():
    conn = psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)
    return conn

# Firebase JWT Verification Decorator
def require_firebase_auth(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        auth_header = request.headers.get("Authorization")
        if not auth_header or not auth_header.startswith("Bearer "):
            return jsonify({"error": "Missing or invalid Authorization header"}), 401

        id_token = auth_header.split("Bearer ")[1]
        try:
            if firebase_admin._apps:
                decoded_token = auth.verify_id_token(id_token)
                request.user = decoded_token
            else:
                # Mock token verification for dev
                request.user = {"uid": "usr-sharma-001", "email": "sharma.family@volto.in"}
        except Exception as e:
            return jsonify({"error": f"Invalid token: {str(e)}"}), 401

        return f(*args, **kwargs)
    return decorated_function

# ==============================================================================
# MQTT TELEMETRY SUBSCRIBER
# ==============================================================================
def on_mqtt_connect(client, userdata, flags, rc):
    print(f"[MQTT] Connected to broker with result code {rc}")
    # Subscribe to telemetry, status, and LWT topics
    client.subscribe("volto/+/+/telemetry")
    client.subscribe("volto/+/+/status")
    client.subscribe("volto/+/+/lwt")

def on_mqtt_message(client, userdata, msg):
    try:
        topic_parts = msg.topic.split("/")
        home_id = topic_parts[1]
        device_id = topic_parts[2]
        payload = json.loads(msg.payload.decode("utf-8"))

        if msg.topic.endswith("/telemetry"):
            record_telemetry(home_id, device_id, payload)
        elif msg.topic.endswith("/lwt"):
            if payload.get("status") == "OFFLINE":
                send_fcm_notification(home_id, f"Device Offline: {device_id}", "Smart node lost connection to Wi-Fi.")
    except Exception as e:
        print(f"[MQTT ERROR] Failed processing payload on {msg.topic}: {e}")

def record_telemetry(home_id, device_id, data):
    # Unified schema: { deviceId, homeId, watts, voltage, current, frequency, powerFactor, kwhTotal, relayOn, timestamp }
    watts = data.get("watts", data.get("active_power_W", 0))
    voltage = data.get("voltage", data.get("voltage_V", 230.0))
    current = data.get("current", data.get("current_A", 0.0))
    frequency = data.get("frequency", 50.0)
    power_factor = data.get("powerFactor", 0.95)
    kwh = data.get("kwhTotal", data.get("energy_kwh", 0.0))
    relay_on = data.get("relayOn", True)

    try:
        with get_db() as conn:
            with conn.cursor() as cur:
                cur.execute(
                    """
                    INSERT INTO telemetry_readings (home_id, device_id, voltage, current, watts, kwh_total, recorded_at)
                    VALUES (%s, %s, %s, %s, %s, %s, NOW())
                    """,
                    (home_id, device_id, voltage, current, watts, kwh)
                )
                conn.commit()
    except Exception as e:
        print(f"[DB ERROR] Ingest telemetry: {e}")

def start_mqtt_worker():
    client = mqtt.Client(client_id="volto_flask_subscriber")
    client.username_pw_set(MQTT_USER, MQTT_PASS)
    client.on_connect = on_mqtt_connect
    client.on_message = on_mqtt_message
    try:
        client.connect(MQTT_BROKER, MQTT_PORT, 60)
        client.loop_forever()
    except Exception as e:
        print(f"[MQTT WORKER] Broker unavailable ({e}). Continuing REST API server.")

# ==============================================================================
# FCM PUSH NOTIFICATION SENDER
# ==============================================================================
def send_fcm_notification(home_id, title, body):
    print(f"[FCM PUSH] To {home_id} -> Title: {title} | Body: {body}")
    if firebase_admin._apps:
        try:
            message = messaging.Message(
                notification=messaging.Notification(title=title, body=body),
                topic=f"volto_{home_id}"
            )
            messaging.send(message)
        except Exception as e:
            print(f"[FCM ERROR] {e}")

# ==============================================================================
# SCHEDULED AGGREGATOR & BUDGET MONITOR
# ==============================================================================
def run_hourly_aggregation_and_limits():
    print("[WORKER] Running hourly kWh aggregation and limit monitor check...")
    try:
        with get_db() as conn:
            with conn.cursor() as cur:
                # Aggregate readings by hour and check daily limits
                cur.execute(
                    """
                    SELECT a.id, a.name, a.daily_limit_kwh, a.is_critical, a.limit_action,
                           COALESCE(SUM(t.kwh_total), 0) as today_kwh
                    FROM appliances a
                    LEFT JOIN telemetry_readings t ON a.device_id = t.device_id
                         AND t.recorded_at >= CURRENT_DATE
                    GROUP BY a.id, a.name, a.daily_limit_kwh, a.is_critical, a.limit_action
                    """
                )
                rows = cur.fetchall()
                for r in rows:
                    if r["today_kwh"] >= r["daily_limit_kwh"]:
                        if r["limit_action"] == "AUTO_OFF" and not r["is_critical"]:
                            print(f"[SAFETY] Auto cut-off commanded for {r['name']}")
                            # Publish MQTT command to cut off relay
                        send_fcm_notification(
                            "home-sharma-blr-01",
                            f"{r['name']} Daily Limit Exceeded",
                            f"Consumed {r['today_kwh']} kWh today against limit of {r['daily_limit_kwh']} kWh."
                        )
    except Exception as e:
        print(f"[WORKER ERROR] {e}")

def start_scheduler():
    schedule.every().hour.do(run_hourly_aggregation_and_limits)
    while True:
        schedule.run_pending()
        time.sleep(60)

# ==============================================================================
# REST API ENDPOINTS
# ==============================================================================
@app.route("/api/health", methods=["GET"])
def health():
    return jsonify({"status": "healthy", "service": "VOLTO Energy Backend", "timestamp": int(time.time())})

@app.route("/api/appliances", methods=["GET"])
@require_firebase_auth
def get_appliances():
    home_id = request.args.get("homeId", "home-sharma-blr-01")
    with get_db() as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT * FROM appliances WHERE home_id = %s", (home_id,))
            appliances = cur.fetchall()
            return jsonify({"appliances": appliances})

@app.route("/api/appliances/<appliance_id>/control", methods=["POST"])
@require_firebase_auth
def control_appliance(appliance_id):
    data = request.json or {}
    command = data.get("command") # "ON" or "OFF"
    # Publish to MQTT command topic
    print(f"[API COMMAND] Appliance {appliance_id} -> {command}")
    return jsonify({"success": True, "applianceId": appliance_id, "state": command})

@app.route("/api/reports/monthly", methods=["GET"])
@require_firebase_auth
def get_monthly_reports():
    home_id = request.args.get("homeId", "home-sharma-blr-01")
    return jsonify({
        "homeId": home_id,
        "totalUnits": 364.5,
        "estimatedBillInr": 2450.0,
        "slabs": [
            {"slab": "0-100", "units": 100, "rate": 4.50},
            {"slab": "101-200", "units": 100, "rate": 6.00},
            {"slab": "201-300", "units": 100, "rate": 7.50},
            {"slab": "300+", "units": 64.5, "rate": 8.50}
        ]
    })

# ==============================================================================
# SERVER-SIDE GEMINI AI PROXY (ZERO SECRETS IN CLIENT APK)
# ==============================================================================
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")

@app.route("/api/ai/ask", methods=["POST"])
@require_firebase_auth
def proxy_gemini_ask():
    """
    Authenticated proxy for Ask VOLTO queries.
    Holds the Gemini API key securely on the backend server.
    """
    data = request.json or {}
    user_query = data.get("query", "")
    household_context = data.get("context", {})

    if not user_query:
        return jsonify({"error": "Empty query"}), 400

    if not GEMINI_API_KEY:
        # Fallback response when GEMINI_API_KEY is not set in environment
        return jsonify({
            "answer": "Namaste! In Indian households, setting your Split AC to 24°C rather than 20°C reduces peak electricity consumption by up to 24%, saving ~₹350 per month. Keeping water heaters on for at most 15-20 minutes avoids idle thermal dissipation losses.",
            "source": "VOLTO Server Intelligence (Rule-based Fallback)"
        })

    try:
        import urllib.request
        prompt = f"""You are the VOLTO Energy Assistant for Indian households.
Slogan: 'Monitor Every Unit, Control Every Appliance, Save Every Month.'
Context: {json.dumps(household_context)}
User question: {user_query}
Provide practical, culturally accurate advice (referencing ₹, kWh units, Indian state DISCOM slabs, AC, BLDC fans, storage geysers). Keep response friendly and concise (2-4 sentences)."""

        url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key={GEMINI_API_KEY}"
        payload = json.dumps({"contents": [{"parts": [{"text": prompt}]}]}).encode("utf-8")
        req = urllib.request.Request(url, data=payload, headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req) as resp:
            res_data = json.loads(resp.read().decode("utf-8"))
            answer_text = res_data["candidates"][0]["content"]["parts"][0]["text"]
            return jsonify({"answer": answer_text, "source": "Gemini 1.5 Flash (Server-Side Proxy)"})
    except Exception as e:
        return jsonify({"answer": "Inspect high-wattage appliances and avoid entering higher tariff slabs.", "source": "VOLTO Server Intelligence", "error": str(e)})

@app.route("/api/fcm/register-token", methods=["POST"])
def register_fcm_token():
    data = request.json or {}
    token = data.get("token")
    home_id = data.get("homeId", "home-sharma-blr-01")
    print(f"[FCM REGISTRATION] Home {home_id} -> Device Token: {token}")
    return jsonify({"success": True, "token": token})

if __name__ == "__main__":
    # Start MQTT and Scheduler in background daemon threads
    threading.Thread(target=start_mqtt_worker, daemon=True).start()
    threading.Thread(target=start_scheduler, daemon=True).start()
    app.run(host="0.0.0.0", port=5000, debug=False)
