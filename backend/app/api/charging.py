import asyncio
from fastapi import APIRouter, Depends, HTTPException, WebSocket, WebSocketDisconnect, status
from sqlalchemy.orm import Session
from app.core.database import get_db, SessionLocal
from app.schemas.charging import StartChargingRequest
from app.services.charging_service import ChargingService
from app.models.charging_session import ChargingSession, SessionStatus
from app.api.deps import get_current_user
from app.models.user import User
from app.utils.helpers import standard_response

from app.services.charging_provider import get_all_providers_status

router = APIRouter(prefix="/charging", tags=["Charging Management"])

@router.get("/providers/status")
def list_providers_status():
    """Returns connectivity and required credentials for all charging provider adapters."""
    return standard_response(True, "Charging provider integrations retrieved", get_all_providers_status())

@router.post("/start")
def start_charging(
    req: StartChargingRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    session, err = ChargingService.start_session(
        db=db,
        user_id=current_user.id,
        charger_id=req.charger_id,
        start_percentage=req.start_percentage or 20.0,
        target_percentage=req.target_percentage or 85.0,
        payment_method=req.payment_method or "UPI",
        is_demo=req.is_demo or False,
        idempotency_key=req.idempotency_key
    )
    if err:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=err)

    return standard_response(True, "Charging session initiated successfully.", {
        "session_id": session.id,
        "charger_id": session.charger_id,
        "status": session.status.value,
        "current_percentage": session.current_percentage,
        "energy_consumed_kwh": session.energy_consumed_kwh,
        "power_kw": session.power_kw,
        "estimated_cost": session.estimated_cost,
        "txn_id": session.txn_id,
        "evse_id": session.evse_id,
        "is_demo": session.is_demo,
        "provider_name": session.provider_name
    })

@router.get("/active")
def get_active_session(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    session = db.query(ChargingSession).filter(
        ChargingSession.user_id == current_user.id,
        ChargingSession.status == SessionStatus.CHARGING
    ).first()

    if not session:
        return standard_response(True, "No active charging session.", None)

    updated = ChargingService.get_live_session(db, session.id)
    return standard_response(True, "Active charging session retrieved.", {
        "session_id": updated.id,
        "station_name": updated.charger.station.name if updated.charger and updated.charger.station else "VoltElite Hub",
        "evse_id": updated.evse_id,
        "status": updated.status.value,
        "current_percentage": updated.current_percentage,
        "target_percentage": updated.target_percentage,
        "energy_consumed_kwh": updated.energy_consumed_kwh,
        "charging_duration": updated.charging_duration,
        "power_kw": updated.power_kw,
        "tariff_per_kwh": updated.tariff_per_kwh,
        "estimated_cost": updated.estimated_cost,
        "txn_id": updated.txn_id,
        "payment_method": updated.payment_method,
        "is_demo": updated.is_demo,
        "provider_name": updated.provider_name
    })

@router.get("/{session_id}")
def get_charging_session(session_id: str, db: Session = Depends(get_db)):
    session = ChargingService.get_live_session(db, session_id)
    if not session:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Session not found.")

    return standard_response(True, "Charging session details retrieved.", {
        "session_id": session.id,
        "station_name": session.charger.station.name if session.charger and session.charger.station else "VoltElite Hub",
        "evse_id": session.evse_id,
        "status": session.status.value,
        "current_percentage": session.current_percentage,
        "target_percentage": session.target_percentage,
        "energy_consumed_kwh": session.energy_consumed_kwh,
        "charging_duration": session.charging_duration,
        "power_kw": session.power_kw,
        "tariff_per_kwh": session.tariff_per_kwh,
        "estimated_cost": session.estimated_cost,
        "final_cost": session.final_cost,
        "txn_id": session.txn_id,
        "payment_method": session.payment_method,
        "is_demo": session.is_demo,
        "provider_name": session.provider_name,
        "start_time": session.start_time.isoformat(),
        "end_time": session.end_time.isoformat() if session.end_time else None
    })

@router.post("/{session_id}/stop")
def stop_charging(
    session_id: str,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    session, err = ChargingService.stop_session(db, session_id, current_user.id)
    if err:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=err)

    return standard_response(True, "Charging session stopped. Digital bill generated.", {
        "session_id": session.id,
        "status": session.status.value,
        "final_cost": session.final_cost,
        "energy_consumed_kwh": session.energy_consumed_kwh,
        "charging_duration_seconds": session.charging_duration,
        "station_name": session.charger.station.name if session.charger and session.charger.station else "VoltElite Hub",
        "evse_id": session.evse_id,
        "payment_status": "SUCCESS",
        "txn_id": session.txn_id,
        "is_demo": session.is_demo,
        "provider_name": session.provider_name
    })

@router.post("/{session_id}/cancel")
def cancel_charging(
    session_id: str,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    session = db.query(ChargingSession).filter(
        ChargingSession.id == session_id,
        ChargingSession.user_id == current_user.id
    ).first()
    if not session:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Session not found.")

    session.status = SessionStatus.CANCELLED
    if session.charger:
        session.charger.status = "AVAILABLE"
    db.commit()
    return standard_response(True, "Charging session cancelled.")


# WebSocket live telemetry updates
@router.websocket("/ws/{session_id}")
async def websocket_charging_telemetry(websocket: WebSocket, session_id: str):
    await websocket.accept()
    db = SessionLocal()
    try:
        while True:
            session = ChargingService.get_live_session(db, session_id)
            if not session:
                await websocket.send_json({"error": "Session not found", "status": "ERROR"})
                break

            payload = {
                "session_id": session.id,
                "status": session.status.value,
                "battery_percentage": session.current_percentage,
                "energy_kwh": session.energy_consumed_kwh,
                "power_kw": session.power_kw,
                "duration_seconds": session.charging_duration,
                "estimated_cost": session.estimated_cost
            }
            await websocket.send_json(payload)

            if session.status in [SessionStatus.COMPLETED, SessionStatus.CANCELLED, SessionStatus.FAILED]:
                break

            await asyncio.sleep(2)
    except WebSocketDisconnect:
        pass
    finally:
        db.close()
