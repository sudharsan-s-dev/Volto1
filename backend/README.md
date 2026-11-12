# VOLTO Python Flask Backend Service

Backend service for **VOLTO Smart Energy Monitoring System**.
Subscribes to telemetry topics over MQTT TLS, stores readings in PostgreSQL, validates Firebase JWT tokens, executes scheduled hourly energy aggregations, and delivers FCM push alerts.

---

## 🚀 Quick Setup & Run with Docker Compose

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_USER: volto_user
      POSTGRES_PASSWORD: volto_pass
      POSTGRES_DB: volto_db
    ports:
      - "5432:5432"

  backend:
    build: .
    ports:
      - "5000:5000"
    environment:
      DATABASE_URL: postgresql://volto_user:volto_pass@postgres:5432/volto_db
      MQTT_BROKER: mqtt.volto.in
      MQTT_PORT: 8883
    depends_on:
      - postgres
```

### Run Locally:
```bash
pip install -r requirements.txt
python app.py
```

---

## 🔒 Firebase Members-Based Security Rules (`firestore.rules`)
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function isAuthenticated() { return request.auth != null; }
    function isMember(homeId) {
      return isAuthenticated() &&
        exists(/databases/$(database)/documents/homes/$(homeId)) &&
        get(/databases/$(database)/documents/homes/$(homeId)).data.members[request.auth.uid] == true;
    }
    function isBackendAdmin() {
      return isAuthenticated() && (request.auth.token.admin == true || request.auth.token.firebase.sign_in_provider == 'service_account');
    }

    match /homes/{homeId} {
      allow read: if isAuthenticated() && resource.data.members[request.auth.uid] == true;
      allow create: if isAuthenticated() && request.resource.data.members[request.auth.uid] == true;
      allow update, delete: if isMember(homeId);

      match /appliances/{applianceId} { allow read, write: if isMember(homeId); }
      match /limits/{limitId} { allow read, write: if isMember(homeId); }
      match /budget/{budgetId} { allow read, write: if isMember(homeId); }
      match /hourly_readings/{readingId} { allow read: if isMember(homeId); allow write: if isBackendAdmin(); }
      match /telemetry/{telemetryId} { allow read: if isMember(homeId); allow write: if isBackendAdmin(); }
    }
  }
}
```
