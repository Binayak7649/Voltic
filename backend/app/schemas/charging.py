from typing import Optional
from datetime import datetime
from pydantic import BaseModel

class QRVerifyRequest(BaseModel):
    qr_payload: str
    vehicle_make_model: Optional[str] = "Tata Nexon EV"
    vehicle_connector: Optional[str] = "CCS 2"

class VehicleCompatibilitySchema(BaseModel):
    is_compatible: bool
    user_vehicle_make_model: str
    user_vehicle_connector: str
    charger_connector: str
    message: str

class QRVerifyResponse(BaseModel):
    is_valid: bool
    station_id: str
    station_name: str
    operator: str
    evse_id: str
    charger_id: str
    connector_type: str
    power_kw: int
    tariff_per_kwh: float
    location: str
    status: str
    supports_remote_start: bool
    compatibility: VehicleCompatibilitySchema
    estimated_full_cost: int
    estimated_duration_min: int

class StartChargingRequest(BaseModel):
    charger_id: str
    start_percentage: Optional[float] = 20.0
    target_percentage: Optional[float] = 85.0
    payment_method: Optional[str] = "UPI"

class StopChargingRequest(BaseModel):
    pass

class ChargingSessionResponse(BaseModel):
    id: str
    user_id: str
    charger_id: str
    station_name: str
    operator: str
    evse_id: str
    connector_type: str
    power_kw: int
    status: str
    start_percentage: float
    target_percentage: float
    current_percentage: float
    energy_consumed_kwh: float
    charging_duration: int  # in seconds
    tariff_per_kwh: float
    estimated_cost: float
    final_cost: float
    payment_method: str
    start_time: datetime
    end_time: Optional[datetime] = None
    txn_id: Optional[str] = None

    class Config:
        from_attributes = True
