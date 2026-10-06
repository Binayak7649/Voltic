import uuid
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.schemas.booking import BookingCreate
from app.models.booking import Booking, BookingStatus
from app.models.charger import Charger, ChargerStatus
from app.api.deps import get_current_user
from app.models.user import User
from app.utils.helpers import standard_response

router = APIRouter(prefix="/bookings", tags=["Bookings"])

@router.post("")
def create_booking(
    req: BookingCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    charger = db.query(Charger).filter(Charger.id == req.charger_id).first()
    if not charger:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Charger not found")

    booking = Booking(
        id=f"book_{uuid.uuid4().hex[:12]}",
        user_id=current_user.id,
        charger_id=req.charger_id,
        booking_start=req.booking_start,
        booking_end=req.booking_end,
        status=BookingStatus.CONFIRMED
    )
    charger.status = ChargerStatus.RESERVED
    db.add(booking)
    db.commit()
    db.refresh(booking)
    return standard_response(True, "Charger slot reserved successfully", {"booking_id": booking.id})

@router.get("")
def list_bookings(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    bookings = db.query(Booking).filter(Booking.user_id == current_user.id).order_by(Booking.created_at.desc()).all()
    return standard_response(True, "Bookings retrieved", [
        {
            "id": b.id,
            "charger_id": b.charger_id,
            "station_name": b.charger.station.name if b.charger and b.charger.station else "",
            "booking_start": b.booking_start.isoformat(),
            "booking_end": b.booking_end.isoformat(),
            "status": b.status.value
        } for b in bookings
    ])

@router.delete("/{booking_id}")
def cancel_booking(booking_id: str, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    booking = db.query(Booking).filter(Booking.id == booking_id, Booking.user_id == current_user.id).first()
    if not booking:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Booking not found")

    booking.status = BookingStatus.CANCELLED
    if booking.charger:
        booking.charger.status = ChargerStatus.AVAILABLE
    db.commit()
    return standard_response(True, "Booking cancelled successfully")
