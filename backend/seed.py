import sys
import os
from datetime import datetime, timedelta

# Add backend directory to sys.path
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from app.core.database import SessionLocal, engine, Base
from app.core.security import get_password_hash
from app.models import (
    User, UserRole,
    Station,
    Charger, ChargerStatus,
    ChargingSession, SessionStatus,
    Payment, PaymentStatus,
    Booking, BookingStatus,
    Transaction,
    Notification
)

def seed():
    print("🌱 Initializing VoltElite Database schema...")
    Base.metadata.create_all(bind=engine)
    db = SessionLocal()

    try:
        # 1. Seed Users
        if not db.query(User).filter(User.email == "demo@voltelite.app").first():
            print("👤 Seeding demo users...")
            demo_user = User(
                id="usr_demo101",
                full_name="Binayak Tiwari",
                email="demo@voltelite.app",
                phone="+919876543210",
                password_hash=get_password_hash("Password123!"),
                role=UserRole.USER,
                city="Indore, Madhya Pradesh",
                is_verified=True,
                auth_provider="local"
            )
            admin_user = User(
                id="usr_admin001",
                full_name="VoltElite Admin",
                email="admin@voltelite.app",
                phone="+919876500000",
                password_hash=get_password_hash("AdminPassword123!"),
                role=UserRole.ADMIN,
                city="Bengaluru, Karnataka",
                is_verified=True,
                auth_provider="local"
            )
            db.add(demo_user)
            db.add(admin_user)
            db.commit()

        # 2. Seed Stations & Chargers across India
        if db.query(Station).count() == 0:
            print("⚡ Seeding Indian EV Charging Stations across Indore, Bhopal, Bengaluru, Mumbai, Delhi...")
            stations_data = [
                # Indore
                {
                    "id": "stn_idr_01",
                    "name": "Tata Power Charging Station",
                    "operator": "Tata Power",
                    "address": "Near Vijay Nagar Square, AB Road",
                    "city": "Indore",
                    "state": "Madhya Pradesh",
                    "latitude": 22.7533,
                    "longitude": 75.8937,
                    "rating": 4.8,
                    "reviews_count": 240,
                    "chargers": [
                        {"id": "chg_idr_01_a", "code": "EVSE-08", "type": "CCS 2", "kw": 60, "price": 18.5, "speed": "DC Fast", "status": ChargerStatus.AVAILABLE},
                        {"id": "chg_idr_01_b", "code": "EVSE-09", "type": "CCS 2", "kw": 60, "price": 18.5, "speed": "DC Fast", "status": ChargerStatus.AVAILABLE},
                        {"id": "chg_idr_01_c", "code": "EVSE-10", "type": "Type 2", "kw": 22, "price": 15.0, "speed": "AC Fast", "status": ChargerStatus.AVAILABLE}
                    ]
                },
                {
                    "id": "stn_idr_02",
                    "name": "ChargeZone — Phoenix Citadel Mall",
                    "operator": "ChargeZone",
                    "address": "Basement B2 Parking, MR 10 Junction",
                    "city": "Indore",
                    "state": "Madhya Pradesh",
                    "latitude": 22.7788,
                    "longitude": 75.9247,
                    "rating": 4.9,
                    "reviews_count": 310,
                    "chargers": [
                        {"id": "chg_idr_02_a", "code": "EVSE-CZ-120A", "type": "CCS 2", "kw": 120, "price": 21.0, "speed": "Ultra-Fast DC", "status": ChargerStatus.AVAILABLE},
                        {"id": "chg_idr_02_b", "code": "EVSE-CZ-120B", "type": "CCS 2", "kw": 120, "price": 21.0, "speed": "Ultra-Fast DC", "status": ChargerStatus.AVAILABLE}
                    ]
                },
                {
                    "id": "stn_idr_03",
                    "name": "Statiq EV Station — Palasia",
                    "operator": "Statiq",
                    "address": "Old Palasia, Near Industry House, AB Road",
                    "city": "Indore",
                    "state": "Madhya Pradesh",
                    "latitude": 22.7230,
                    "longitude": 75.8821,
                    "rating": 4.6,
                    "reviews_count": 180,
                    "chargers": [
                        {"id": "chg_idr_03_a", "code": "EVSE-STQ-50A", "type": "CCS 2", "kw": 50, "price": 17.5, "speed": "DC Fast", "status": ChargerStatus.AVAILABLE},
                        {"id": "chg_idr_03_b", "code": "EVSE-STQ-22A", "type": "Type 2", "kw": 22, "price": 14.5, "speed": "AC Fast", "status": ChargerStatus.AVAILABLE}
                    ]
                },
                {
                    "id": "stn_idr_04",
                    "name": "Jio-bp pulse Station — Super Corridor",
                    "operator": "Jio-bp",
                    "address": "Super Corridor Airport Rd, TCS Square",
                    "city": "Indore",
                    "state": "Madhya Pradesh",
                    "latitude": 22.7712,
                    "longitude": 75.8289,
                    "rating": 4.7,
                    "reviews_count": 145,
                    "chargers": [
                        {"id": "chg_idr_04_a", "code": "EVSE-JBP-60A", "type": "CCS 2", "kw": 60, "price": 19.0, "speed": "DC Fast", "status": ChargerStatus.AVAILABLE}
                    ]
                },
                {
                    "id": "stn_idr_05",
                    "name": "Kazam EV Hub — Bhawarkuan",
                    "operator": "Kazam",
                    "address": "Bhawarkuan Square, Ring Road",
                    "city": "Indore",
                    "state": "Madhya Pradesh",
                    "latitude": 22.6908,
                    "longitude": 75.8652,
                    "rating": 4.3,
                    "reviews_count": 92,
                    "chargers": [
                        {"id": "chg_idr_05_a", "code": "EVSE-KZ-02", "type": "Type 2", "kw": 11, "price": 16.0, "speed": "AC Slow", "status": ChargerStatus.AVAILABLE}
                    ]
                },
                # Bhopal
                {
                    "id": "stn_bho_01",
                    "name": "Tata Power EZ Charge — MP Nagar",
                    "operator": "Tata Power",
                    "address": "Zone-I, MP Nagar, Bhopal",
                    "city": "Bhopal",
                    "state": "Madhya Pradesh",
                    "latitude": 23.2332,
                    "longitude": 77.4344,
                    "rating": 4.7,
                    "reviews_count": 165,
                    "chargers": [
                        {"id": "chg_bho_01_a", "code": "EVSE-BHO-60", "type": "CCS 2", "kw": 60, "price": 18.5, "speed": "DC Fast", "status": ChargerStatus.AVAILABLE}
                    ]
                },
                # Bengaluru
                {
                    "id": "stn_blr_01",
                    "name": "Zeon Charging Hub — Koramangala",
                    "operator": "Zeon Charging",
                    "address": "80 Feet Road, 4th Block, Koramangala",
                    "city": "Bengaluru",
                    "state": "Karnataka",
                    "latitude": 12.9352,
                    "longitude": 77.6245,
                    "rating": 4.9,
                    "reviews_count": 420,
                    "chargers": [
                        {"id": "chg_blr_01_a", "code": "EVSE-ZEON-150A", "type": "CCS 2", "kw": 150, "price": 22.5, "speed": "Ultra-Fast DC", "status": ChargerStatus.AVAILABLE}
                    ]
                },
                {
                    "id": "stn_blr_02",
                    "name": "Ather Grid Fast Station — Indiranagar",
                    "operator": "Ather Grid",
                    "address": "100 Feet Road, Indiranagar",
                    "city": "Bengaluru",
                    "state": "Karnataka",
                    "latitude": 12.9784,
                    "longitude": 77.6408,
                    "rating": 4.8,
                    "reviews_count": 390,
                    "chargers": [
                        {"id": "chg_blr_02_a", "code": "EVSE-ATH-7", "type": "Bharat DC", "kw": 25, "price": 16.0, "speed": "DC Fast", "status": ChargerStatus.AVAILABLE}
                    ]
                },
                # Mumbai
                {
                    "id": "stn_bom_01",
                    "name": "Tata Power Fast Hub — BKC",
                    "operator": "Tata Power",
                    "address": "G Block, Bandra Kurla Complex",
                    "city": "Mumbai",
                    "state": "Maharashtra",
                    "latitude": 19.0657,
                    "longitude": 72.8687,
                    "rating": 4.8,
                    "reviews_count": 510,
                    "chargers": [
                        {"id": "chg_bom_01_a", "code": "EVSE-BKC-120", "type": "CCS 2", "kw": 120, "price": 21.5, "speed": "Ultra-Fast DC", "status": ChargerStatus.AVAILABLE}
                    ]
                },
                # Delhi NCR
                {
                    "id": "stn_del_01",
                    "name": "Statiq Charging Hub — Cyber City",
                    "operator": "Statiq",
                    "address": "DLF Cyber City, Building 10, Gurugram",
                    "city": "Delhi NCR",
                    "state": "Haryana",
                    "latitude": 28.4950,
                    "longitude": 77.0895,
                    "rating": 4.7,
                    "reviews_count": 480,
                    "chargers": [
                        {"id": "chg_del_01_a", "code": "EVSE-CYB-60", "type": "CCS 2", "kw": 60, "price": 19.5, "speed": "DC Fast", "status": ChargerStatus.AVAILABLE}
                    ]
                }
            ]

            for s_data in stations_data:
                st = Station(
                    id=s_data["id"],
                    name=s_data["name"],
                    operator=s_data["operator"],
                    address=s_data["address"],
                    city=s_data["city"],
                    state=s_data["state"],
                    latitude=s_data["latitude"],
                    longitude=s_data["longitude"],
                    rating=s_data["rating"],
                    reviews_count=s_data["reviews_count"],
                    open_hours="24/7 Open",
                    amenities="Parking,Cafeteria,Restroom,Wi-Fi,24/7 Support",
                    operator_verified=True
                )
                db.add(st)

                for chg in s_data["chargers"]:
                    qr_payload = f"volt-elite://charge?station={s_data['id']}&evse={chg['code']}&connector={chg['type'].lower().replace(' ', '')}"
                    c = Charger(
                        id=chg["id"],
                        station_id=s_data["id"],
                        charger_code=chg["code"],
                        connector_type=chg["type"],
                        power_kw=chg["kw"],
                        charging_speed=chg["speed"],
                        price_per_kwh=chg["price"],
                        status=chg["status"],
                        qr_code=qr_payload,
                        is_active=True,
                        supports_remote_start=True
                    )
                    db.add(c)

            db.commit()

        # 3. Seed Past Charging Sessions & Payments for Demo User
        demo_user = db.query(User).filter(User.email == "demo@voltelite.app").first()
        if demo_user and db.query(ChargingSession).filter(ChargingSession.user_id == demo_user.id).count() == 0:
            print("💳 Seeding past charging sessions & digital invoices...")
            sample_sessions = [
                {
                    "id": "sess_past_01",
                    "charger_id": "chg_idr_01_a",
                    "kwh": 31.6,
                    "duration": 1920,
                    "cost": 584.6,
                    "evse": "EVSE-08",
                    "txn": "TXN-839201",
                    "invoice": "INV-839201",
                    "days_ago": 1
                },
                {
                    "id": "sess_past_02",
                    "charger_id": "chg_idr_02_a",
                    "kwh": 42.0,
                    "duration": 1440,
                    "cost": 882.0,
                    "evse": "EVSE-CZ-120A",
                    "txn": "TXN-729104",
                    "invoice": "INV-729104",
                    "days_ago": 3
                },
                {
                    "id": "sess_past_03",
                    "charger_id": "chg_idr_03_a",
                    "kwh": 24.5,
                    "duration": 1800,
                    "cost": 428.75,
                    "evse": "EVSE-STQ-50A",
                    "txn": "TXN-618293",
                    "invoice": "INV-618293",
                    "days_ago": 6
                }
            ]

            for ss in sample_sessions:
                st_time = datetime.utcnow() - timedelta(days=ss["days_ago"], hours=2)
                end_time = st_time + timedelta(seconds=ss["duration"])
                sess = ChargingSession(
                    id=ss["id"],
                    user_id=demo_user.id,
                    charger_id=ss["charger_id"],
                    start_time=st_time,
                    end_time=end_time,
                    status=SessionStatus.COMPLETED,
                    energy_consumed_kwh=ss["kwh"],
                    charging_duration=ss["duration"],
                    start_percentage=22.0,
                    current_percentage=85.0,
                    target_percentage=85.0,
                    power_kw=60,
                    tariff_per_kwh=18.5,
                    estimated_cost=ss["cost"],
                    final_cost=ss["cost"],
                    evse_id=ss["evse"],
                    txn_id=ss["txn"],
                    payment_method="UPI (Google Pay)"
                )
                db.add(sess)

                pmt = Payment(
                    id=f"pay_{ss['id']}",
                    user_id=demo_user.id,
                    session_id=ss["id"],
                    amount=ss["cost"],
                    currency="INR",
                    payment_method="UPI (Google Pay)",
                    transaction_id=ss["txn"],
                    status=PaymentStatus.SUCCESS,
                    invoice_number=ss["invoice"]
                )
                db.add(pmt)

                txn = Transaction(
                    id=f"txn_{ss['id']}",
                    user_id=demo_user.id,
                    payment_id=pmt.id,
                    amount=ss["cost"],
                    type="DEBIT",
                    description=f"EV Fast Charge at {ss['evse']}",
                    reference_id=ss["invoice"],
                    status="SUCCESS"
                )
                db.add(txn)

            # Notifications
            db.add(Notification(
                id="notif_01",
                user_id=demo_user.id,
                title="Charging Completed",
                message="Your Nexon EV reached 85% at Tata Power Vijay Nagar. 31.6 kWh added.",
                type="charging",
                is_read=True
            ))
            db.add(Notification(
                id="notif_02",
                user_id=demo_user.id,
                title="Invoice Generated",
                message="Invoice INV-839201 for ₹585 paid successfully via UPI.",
                type="payment",
                is_read=False
            ))
            db.commit()

        print("✅ Database seeding completed successfully!")
    finally:
        db.close()

if __name__ == "__main__":
    seed()
