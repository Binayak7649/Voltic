from datetime import datetime
from pydantic import BaseModel

class BookingCreate(BaseModel):
    charger_id: str
    booking_start: datetime
    booking_end: datetime

class BookingResponse(BaseModel):
    id: str
    user_id: str
    charger_id: str
    booking_start: datetime
    booking_end: datetime
    status: str
    created_at: datetime

    class Config:
        from_attributes = True
