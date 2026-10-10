from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session, joinedload
from app.core.database import get_db
from app.models.charging_session import ChargingSession
from app.models.charger import Charger
from app.models.payment import Payment
from app.api.deps import get_current_user
from app.models.user import User
from app.utils.helpers import standard_response

router = APIRouter(prefix="/history", tags=["History"])

@router.get("/charging")
def get_charging_history(
    limit: int = Query(50, ge=1, le=100),
    offset: int = Query(0, ge=0),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    sessions = (
        db.query(ChargingSession)
        .options(
            joinedload(ChargingSession.charger).joinedload(Charger.station),
            joinedload(ChargingSession.payment)
        )
        .filter(ChargingSession.user_id == current_user.id)
        .order_by(ChargingSession.start_time.desc())
        .offset(offset)
        .limit(limit)
        .all()
    )

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
            "timestamp": s.start_time.strftime("%d %b %Y, %I:%M %p") if s.start_time else "",
            "txn_id": s.txn_id,
            "invoice_number": s.payment.invoice_number if s.payment else f"INV-{s.id[-6:]}"
        } for s in sessions
    ])

@router.get("/payments")
def get_payment_history(
    limit: int = Query(50, ge=1, le=100),
    offset: int = Query(0, ge=0),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    payments = (
        db.query(Payment)
        .filter(Payment.user_id == current_user.id)
        .order_by(Payment.created_at.desc())
        .offset(offset)
        .limit(limit)
        .all()
    )

    return standard_response(True, "Payment transactions retrieved", [
        {
            "id": p.id,
            "amount": p.amount,
            "currency": p.currency,
            "status": p.status.value,
            "payment_method": p.payment_method,
            "transaction_id": p.transaction_id,
            "invoice_number": p.invoice_number,
            "timestamp": p.created_at.strftime("%d %b %Y, %I:%M %p") if p.created_at else ""
        } for p in payments
    ])
