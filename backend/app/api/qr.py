from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.schemas.charging import QRVerifyRequest
from app.models.charger import Charger, ChargerStatus
from app.models.station import Station
from app.utils.qr import QRParser
from app.utils.helpers import standard_response

router = APIRouter(prefix="/qr", tags=["QR Verification"])

@router.post("/verify")
def verify_qr(req: QRVerifyRequest, db: Session = Depends(get_db)):
    parsed = QRParser.parse_payload(req.qr_payload)
    if not parsed.get("is_valid", True):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=parsed.get("error", "Invalid or unsupported EV charger QR code. Please retry scan.")
        )

    evse_id = parsed.get("evse_id")

    # Match scanned charger safely with the correct station and connector in backend
    charger = None
    if evse_id:
        charger = db.query(Charger).filter(
            (Charger.charger_code == evse_id) |
            (Charger.qr_code == req.qr_payload) |
            (Charger.id == evse_id)
        ).first()

    if not charger:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"No charging connector found matching '{evse_id or req.qr_payload[:20]}'. Please scan an authorized charger QR code or select a preset."
        )

    if not charger.is_active:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Charger connector {charger.charger_code} is currently inactive or under maintenance."
        )

    station = db.query(Station).filter(Station.id == charger.station_id).first()
    if not station or not station.is_active:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Parent charging station is offline."
        )

    # Check vehicle compatibility
    user_connector = (req.vehicle_connector or "CCS 2").upper().replace(" ", "").replace("_", "")
    charger_connector = charger.connector_type.upper().replace(" ", "").replace("_", "")

    # Handle Type 2 simulated incompatible QR codes
    if "TYPE2" in parsed.get("connector_type", "").upper() or "TYPE2" in req.qr_payload:
        is_compatible = False
        message = f"Connector mismatch: Charger is Type 2 AC (11 kW), but vehicle requires {req.vehicle_connector or 'CCS 2'}."
    else:
        is_compatible = (user_connector in charger_connector) or (charger_connector in user_connector)
        message = f"Compatible with your {req.vehicle_make_model or 'EV'}" if is_compatible else f"Connector mismatch: Charger has {charger.connector_type}, vehicle requires {req.vehicle_connector}."

    # Estimated cost to 80% (assuming 30 kWh needed)
    est_kwh = 30.0
    est_cost = int(est_kwh * charger.price_per_kwh)
    est_duration = int((est_kwh / charger.power_kw) * 60)

    return standard_response(True, "Charger verified successfully", {
        "is_valid": True,
        "station_id": station.id,
        "station_name": station.name,
        "operator": station.operator,
        "evse_id": charger.charger_code,
        "charger_id": charger.id,
        "connector_type": charger.connector_type,
        "power_kw": charger.power_kw,
        "tariff_per_kwh": charger.price_per_kwh,
        "location": station.address,
        "status": charger.status.value if hasattr(charger.status, 'value') else str(charger.status),
        "supports_remote_start": charger.supports_remote_start,
        "compatibility": {
            "is_compatible": is_compatible,
            "user_vehicle_make_model": req.vehicle_make_model or "Tata Nexon EV",
            "user_vehicle_connector": req.vehicle_connector or "CCS 2",
            "charger_connector": charger.connector_type,
            "message": message
        },
        "estimated_full_cost": est_cost,
        "estimated_duration_min": max(20, est_duration)
    })
