from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.models.charging_session import ChargingSession
from app.models.payment import Payment
from app.api.deps import get_current_user
from app.models.user import User
from app.utils.helpers import standard_response

router = APIRouter(prefix="/history", tags=["History"])

@router.get("/charging")
def get_charging_history(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    sessions = db.query(ChargingSession).filter(
        ChargingSession.user_id == current_user.id
    ).order_by(ChargingSession.start_time.desc()).all()

    return standard_response(True, "Charging history retrieved", [
        {
            "id": s.id,
            "session_id": s.id,
            "station_id": s.charger.station_id if s.charger else "",
            "station_name": s.charger.station.name if s.charger and s.charger.station else "VoltElite Station",
            "operator": s.charger.station.operator if s.charger and s.charger.station else "Tata Power",
            "evse_id": s.evse_id,
            "connector_type": s.charger.connector_type if s.charger else "CCS 2",
            "power_kw": s.power_kw,
            "energy_consumed_kwh": s.energy_consumed_kwh,
            "charging_duration_minutes": int(s.charging_duration / 60) if s.charging_duration else 30,
            "total_cost": s.final_cost if s.final_cost > 0 else s.estimated_cost,
            "tariff_per_kwh": s.tariff_per_kwh,
            "payment_method": s.payment_method,
            "status": s.status.value,
            "timestamp": s.start_time.strftime("%d %b %Y, %I:%M %p"),
            "txn_id": s.txn_id,
            "invoice_number": s.payment.invoice_number if s.payment else f"INV-{s.id[-6:]}"
        } for s in sessions
    ])

@router.get("/payments")
def get_payment_history(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    payments = db.query(Payment).filter(
        Payment.user_id == current_user.id
    ).order_by(Payment.created_at.desc()).all()

    return standard_response(True, "Payment transactions retrieved", [
        {
            "id": p.id,
            "amount": p.amount,
            "currency": p.currency,
            "status": p.status.value,
            "payment_method": p.payment_method,
            "transaction_id": p.transaction_id,
            "invoice_number": p.invoice_number,
            "timestamp": p.created_at.strftime("%d %b %Y, %I:%M %p")
        } for p in payments
    ])
