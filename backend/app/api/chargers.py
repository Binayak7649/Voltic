from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.services.charger_service import ChargerService
from app.models.charger import Charger, ChargerStatus
from app.schemas.charger import ChargerUpdate
from app.utils.helpers import standard_response

router = APIRouter(tags=["Chargers"])

@router.get("/stations/{station_id}/chargers")
def get_station_chargers(station_id: str, db: Session = Depends(get_db)):
    chargers = ChargerService.get_chargers_for_station(db, station_id)
    return standard_response(True, "Chargers retrieved", [
        {
            "id": c.id,
            "charger_code": c.charger_code,
            "connector_type": c.connector_type,
            "power_kw": c.power_kw,
            "charging_speed": c.charging_speed,
            "price_per_kwh": c.price_per_kwh,
            "status": c.status.value if hasattr(c.status, 'value') else str(c.status),
            "qr_code": c.qr_code,
            "supports_remote_start": c.supports_remote_start
        } for c in chargers
    ])

@router.get("/chargers/{charger_id}")
def get_charger(charger_id: str, db: Session = Depends(get_db)):
    c = ChargerService.get_charger_by_id(db, charger_id)
    if not c:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Charger not found")
    return standard_response(True, "Charger retrieved", {
        "id": c.id,
        "station_id": c.station_id,
        "station_name": c.station.name if c.station else "",
        "charger_code": c.charger_code,
        "connector_type": c.connector_type,
        "power_kw": c.power_kw,
        "charging_speed": c.charging_speed,
        "price_per_kwh": c.price_per_kwh,
        "status": c.status.value if hasattr(c.status, 'value') else str(c.status),
        "qr_code": c.qr_code,
        "supports_remote_start": c.supports_remote_start
    })

@router.get("/chargers/{charger_id}/status")
def get_charger_status(charger_id: str, db: Session = Depends(get_db)):
    c = ChargerService.get_charger_by_id(db, charger_id)
    if not c:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Charger not found")
    return standard_response(True, "Status retrieved", {
        "charger_id": c.id,
        "status": c.status.value if hasattr(c.status, 'value') else str(c.status),
        "is_available": c.status == ChargerStatus.AVAILABLE
    })

@router.post("/chargers/{charger_id}/status")
def update_charger_status(charger_id: str, req: ChargerUpdate, db: Session = Depends(get_db)):
    c = ChargerService.get_charger_by_id(db, charger_id)
    if not c:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Charger not found")
    if req.status:
        c.status = ChargerStatus(req.status.upper())
    db.commit()
    return standard_response(True, "Charger status updated successfully")
