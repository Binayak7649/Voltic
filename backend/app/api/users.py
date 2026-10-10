from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.api.deps import get_current_user
from app.models.user import User
from app.schemas.user import UserUpdate, PasswordChangeRequest
from app.core.security import verify_password, get_password_hash
from app.utils.helpers import standard_response

router = APIRouter(prefix="/users", tags=["Users"])

@router.get("/profile")
def get_profile(current_user: User = Depends(get_current_user)):
    return standard_response(True, "Profile retrieved", {
        "id": current_user.id,
        "full_name": current_user.full_name,
        "email": current_user.email,
        "phone": current_user.phone,
        "city": current_user.city,
        "profile_image": current_user.profile_image,
        "role": current_user.role.value if hasattr(current_user.role, 'value') else str(current_user.role),
        "is_verified": current_user.is_verified,
        "auth_provider": current_user.auth_provider
    })

@router.put("/profile")
def update_profile(req: UserUpdate, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    if req.full_name is not None:
        current_user.full_name = req.full_name
    if req.email is not None:
        current_user.email = req.email
    if req.phone is not None:
        current_user.phone = req.phone
    if req.city is not None:
        current_user.city = req.city
    if req.profile_image is not None:
        current_user.profile_image = req.profile_image

    db.commit()
    db.refresh(current_user)
    return standard_response(True, "Profile updated successfully.")

@router.put("/password")
def change_password(req: PasswordChangeRequest, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    if current_user.password_hash and not verify_password(req.old_password, current_user.password_hash):
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Current password incorrect.")

    current_user.password_hash = get_password_hash(req.new_password)
    db.commit()
    return standard_response(True, "Password changed successfully.")

import io
import csv
from fastapi.responses import StreamingResponse
from app.models.charging_session import ChargingSession
from app.models.charger import Charger
from sqlalchemy.orm import joinedload

@router.get("/export")
def export_user_data(
    format: str = "csv",
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    sessions = (
        db.query(ChargingSession)
        .options(joinedload(ChargingSession.charger).joinedload(Charger.station))
        .filter(ChargingSession.user_id == current_user.id)
        .order_by(ChargingSession.start_time.desc())
        .all()
    )

    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow([
        "Session ID", "Station Name", "Operator", "Connector",
        "Power (kW)", "Energy (kWh)", "Duration (min)", "Cost (INR)",
        "Status", "Start Time"
    ])
    for s in sessions:
        st_name = s.charger.station.name if s.charger and s.charger.station else "VoltElite"
        op_name = s.charger.station.operator if s.charger and s.charger.station else "Partner"
        conn_type = s.charger.connector_type if s.charger else "CCS 2"
        writer.writerow([
            s.id, st_name, op_name, conn_type,
            s.power_kw, s.energy_consumed_kwh,
            int(s.charging_duration / 60) if s.charging_duration else 0,
            s.final_cost if s.final_cost > 0 else s.estimated_cost,
            s.status.value, s.start_time.strftime("%Y-%m-%d %H:%M:%S") if s.start_time else ""
        ])

    output.seek(0)
    return StreamingResponse(
        iter([output.getvalue()]),
        media_type="text/csv",
        headers={"Content-Disposition": f"attachment; filename=voltelite_charging_export_{current_user.id}.csv"}
    )

