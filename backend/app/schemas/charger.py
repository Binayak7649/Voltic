from typing import Optional
from datetime import datetime
from pydantic import BaseModel

class ChargerBase(BaseModel):
    charger_code: str
    connector_type: str
    power_kw: int
    charging_speed: str = "DC Fast"
    price_per_kwh: float
    status: str = "AVAILABLE"
    supports_remote_start: bool = True

class ChargerCreate(ChargerBase):
    station_id: str
    qr_code: Optional[str] = None

class ChargerUpdate(BaseModel):
    status: Optional[str] = None
    price_per_kwh: Optional[float] = None
    power_kw: Optional[int] = None
    is_active: Optional[bool] = None

class ChargerResponse(ChargerBase):
    id: str
    station_id: str
    qr_code: str
    is_active: bool
    created_at: datetime

    class Config:
        from_attributes = True
