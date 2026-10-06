from typing import List, Optional
from datetime import datetime
from pydantic import BaseModel
from app.schemas.charger import ChargerResponse

class StationBase(BaseModel):
    name: str
    operator: str
    address: str
    city: str = "Indore"
    state: str = "Madhya Pradesh"
    latitude: float
    longitude: float
    opening_time: str = "00:00"
    closing_time: str = "23:59"
    open_hours: str = "24/7 Open"
    rating: float = 4.5
    reviews_count: int = 120
    amenities: str = "Parking,Cafeteria,Restroom,Wi-Fi,24/7 Support"
    operator_verified: bool = True

class StationCreate(StationBase):
    pass

class StationUpdate(BaseModel):
    name: Optional[str] = None
    operator: Optional[str] = None
    address: Optional[str] = None
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    open_hours: Optional[str] = None
    is_active: Optional[bool] = None

class StationResponse(StationBase):
    id: str
    is_active: bool
    created_at: datetime
    chargers: List[ChargerResponse] = []
    
    # Computed fields for frontend compatibility
    distance_km: Optional[float] = 0.0
    eta_minutes: Optional[int] = 0
    total_available: Optional[int] = 0
    total_ports: Optional[int] = 0
    max_power_kw: Optional[int] = 60
    price_per_kwh: Optional[float] = 18.5
    status: Optional[str] = "AVAILABLE"

    class Config:
        from_attributes = True
