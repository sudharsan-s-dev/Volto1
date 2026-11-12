# VOLTO ESP32 Energy Node Firmware

Firmware for the **VOLTO Smart Home Energy Monitoring & Management System**.
Reads real-time electrical parameters from Peacefair PZEM-004T v3.0, publishes JSON telemetry over MQTT TLS every 5 seconds, listens for remote relay ON/OFF commands, and enforces local cut-off limits.

---

## ⚡ Certified Electrician Installation & Safety Warning
> **DANGER: MAINS VOLTAGE (230V AC) CAN KILL.**
> Installation inside distribution boards or switchboards must be conducted exclusively by a licensed, certified electrician following national electrical wiring regulations (IS 732 in India / NEC).

1. **Relay & Contactor Sizing**:
   - For loads > 10 Amps (e.g. 1.5-ton split AC, 25-litre bathroom geyser, induction cooktops), **never wire the load directly to small 10A PCB relays**.
   - Use the ESP32 relay as a **pilot signal** to actuate an industrial IEC contactor rated for **>= 25A AC-1 / AC-7a** with an RC snubber across coil contacts.
2. **Current Transformer (CT)**:
   - The PZEM-004T CT sensor coil must clamp **only around the Phase/Live wire**. Clamping both Phase and Neutral results in cancelation and zero reading.
3. **Fusing**:
   - Place an inline 1A fast-blow ceramic fuse on the 230V voltage sense leads powering the PZEM module.

---

## 🔌 Text Wiring Diagram

```text
[ 230V AC Live (Phase) ] ----[ 1A Fuse ]------------------> [ PZEM-004T Terminal 1 (L) ]
                         ----[ Contactor Main NO (25A) ]---> [ Appliance (AC / Geyser) ]
                         (CT Coil around Live Conductor) --> [ PZEM-004T Terminals 3 & 4 ]

[ 230V AC Neutral ] --------------------------------------> [ PZEM-004T Terminal 2 (N) ]
                    --------------------------------------> [ Appliance Neutral ]

[ ESP32 5V & GND ] <--------------------------------------> [ PZEM-004T 5V & GND ]
[ ESP32 GPIO 16 (RX2) ] <--------------------------------- [ PZEM-004T TX ]
[ ESP32 GPIO 17 (TX2) ] ---------------------------------> [ PZEM-004T RX ]
[ ESP32 GPIO 25 (Output) ] ------------------------------> [ Relay IN / Pilot Contactor Coil ]
[ ESP32 GPIO 0 (BOOT) ] ---------------------------------> [ Hold 5s for Wi-Fi AP Provisioning ]
```

---

## 📡 MQTT Topic Architecture
- **Telemetry Topic**: `volto/{homeId}/{deviceId}/telemetry` (Published every 5s)
- **Status Topic**: `volto/{homeId}/{deviceId}/status` (Relay confirmation)
- **Command Topic**: `volto/{homeId}/{deviceId}/command` (Listens for `{"command":"ON"|"OFF"}`)
- **LWT (Last-Will)**: `volto/{homeId}/{deviceId}/lwt` (Auto triggers hardware offline alert on broker)
