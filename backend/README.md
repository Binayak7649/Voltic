# VoltElite — EV Charging Backend API

Production-ready backend architecture for **VoltElite**, India's unified EV charging super app. Built with **FastAPI**, **PostgreSQL / SQLAlchemy ORM**, **JWT Authentication**, and **Hardware Simulation Engine** (future-ready for OCPP 1.6J/2.0.1).

---

## 🚀 Tech Stack

- **Framework:** Python 3.11 + FastAPI + Uvicorn (ASGI)
- **Database:** PostgreSQL (with SQLite zero-config support for local testing) + SQLAlchemy 2.0
- **Security:** JWT (HMAC-SHA256) + bcrypt password hashing + Role-Based Access Control (RBAC)
- **Validation:** Pydantic v2
- **Real-time:** WebSockets (`/api/charging/ws/{session_id}`)
- **Testing:** Pytest + HTTPX
- **Containerization:** Docker & Docker Compose

---

## 📁 Project Structure

```text
backend/
├── app/
│   ├── main.py                  # FastAPI Application Entrypoint & Middleware
│   ├── core/
│   │   ├── config.py            # Settings & Environment Variables
│   │   ├── database.py          # SQLAlchemy Engine & Session
│   │   └── security.py          # JWT Generation, Password Hashing
│   ├── models/                  # SQLAlchemy Relational Models
│   │   ├── user.py              # Users & Roles (USER, ADMIN, OPERATOR)
│   │   ├── station.py           # EV Charging Stations across India
│   │   ├── charger.py           # Chargers & Connectors (CCS2, Type 2, etc.)
│   │   ├── charging_session.py  # Live Sessions & Telemetry
│   │   ├── payment.py           # Razorpay-ready Invoices & Receipts
│   │   ├── booking.py           # Charger Slot Reservations
│   │   ├── notification.py      # User Push/In-App Alerts
│   │   └── transaction.py       # Financial Ledger Entries
│   ├── schemas/                 # Pydantic Request/Response Models
│   ├── api/                     # REST Endpoints
│   │   ├── auth.py              # Register, Login, Refresh, Phone OTP, Google
│   │   ├── users.py             # Profile, Password
│   │   ├── stations.py          # Listing, Haversine Nearby Search
│   │   ├── chargers.py          # Port Status & Updates
│   │   ├── qr.py                # QR Verification & Vehicle Compatibility Check
│   │   ├── charging.py          # Start, Active, Stop, WebSocket Stream
│   │   ├── payments.py          # Order Creation, Simulation & Verification
│   │   ├── bookings.py          # Slot Bookings
│   │   ├── notifications.py     # Notification Management
│   │   ├── history.py           # Sessions & Payment History
│   │   └── analytics.py         # Carbon Savings & Dashboard Stats
│   ├── services/
│   │   ├── charging_provider.py # Hardware Abstraction (Simulation vs OCPP Real)
│   │   ├── auth_service.py
│   │   ├── station_service.py
│   │   ├── charging_service.py
│   │   ├── payment_service.py
│   │   └── analytics_service.py
│   └── utils/
│       ├── qr.py                # Multi-Provider QR Code Parser
│       └── helpers.py           # Uniform JSON Response Envelope
├── tests/                       # Pytest Suite
├── .env.example                 # Sample Configuration
├── Dockerfile                   # Production Containerfile
├── docker-compose.yml           # PostgreSQL + Backend Stack
├── requirements.txt             # Python Dependencies
├── seed.py                      # Multi-City Seed Data Generator
└── README.md
```

---

## ⚡ Quick Start (Local Development)

### 1. Prerequisites
- Python 3.10+
- PostgreSQL (or use the built-in SQLite for zero-config testing)

### 2. Environment Setup
```bash
cd backend
cp .env.example .env
python -m venv venv
source venv/bin/activate    # On Windows: venv\Scripts\activate
pip install -r requirements.txt
```

### 3. Seed Database
```bash
python seed.py
```
This populates:
- Demo EV Driver (`demo@voltelite.app` / `Password123!`)
- Admin Account (`admin@voltelite.app` / `AdminPassword123!`)
- Verified EV Stations across **Indore, Bhopal, Bengaluru, Mumbai, and Delhi NCR** (Tata Power, ChargeZone, Statiq, Jio-bp, Kazam, Zeon, Ather Grid, BPCL)
- Historical charging sessions and digital receipts

### 4. Run Server
```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```
- API Base URL: `http://localhost:8000`
- **Interactive Swagger UI:** `http://localhost:8000/docs`
- ReDoc Documentation: `http://localhost:8000/redoc`

---

## 🐳 Running with Docker Compose

To start PostgreSQL and the FastAPI backend in synchronized containers:
```bash
docker-compose up --build -d
```
Stop services:
```bash
docker-compose down
```

---

## 🧪 Running Automated Tests

```bash
pytest -v
```

---

## 📱 End-to-End College Demonstration Flow

1. **Authentication:**
   - Call `POST /api/auth/login` with `demo@voltelite.app` / `Password123!` or `POST /api/auth/verify-otp` with phone `+919876543210` & OTP `123456`.
2. **Find Nearby Stations:**
   - Call `GET /api/stations/nearby?latitude=22.7196&longitude=75.8577&radius=50` to receive nearby stations sorted by real distance.
3. **Scan QR Code:**
   - Call `POST /api/qr/verify` with payload `VOLT-DEMO-STN001-EVSE08-CCS2` and vehicle connector `CCS 2`.
   - The backend validates the EVSE ID, tests availability, and returns compatibility status (`is_compatible: true`).
4. **Start Remote Charging:**
   - Call `POST /api/charging/start` with `charger_id: chg_idr_01_a`. The charger status switches to `OCCUPIED`.
5. **Live Telemetry:**
   - Poll `GET /api/charging/active` or connect to WebSocket `ws://localhost:8000/api/charging/ws/{session_id}` for real-time battery % and kWh delivery.
6. **Stop & Invoicing:**
   - Call `POST /api/charging/{session_id}/stop`.
   - Charger becomes `AVAILABLE` again, digital receipt `INV-...` is generated, and charging history is updated.
