from datetime import datetime
import enum
from sqlalchemy import Column, String, Float, Boolean, DateTime, Integer, ForeignKey, Enum
from sqlalchemy.orm import relationship
from app.core.database import Base

class ChargerStatus(str, enum.Enum):
    AVAILABLE = "AVAILABLE"
    OCCUPIED = "OCCUPIED"
    OFFLINE = "OFFLINE"
    MAINTENANCE = "MAINTENANCE"
    RESERVED = "RESERVED"

class ConnectorTypeEnum(str, enum.Enum):
    CCS_2 = "CCS 2"
    TYPE_2 = "Type 2"
    CHADEMO = "CHAdeMO"
    AC_TYPE_1 = "AC 7.4kW"
    BHARAT_DC_001 = "Bharat DC"

class Charger(Base):
    __tablename__ = "chargers"

    id = Column(String, primary_key=True, index=True)
    station_id = Column(String, ForeignKey("stations.id"), nullable=False, index=True)
    charger_code = Column(String, unique=True, index=True, nullable=False)  # EVSE-08
    connector_type = Column(String, default="CCS 2", nullable=False)
    power_kw = Column(Integer, default=60, nullable=False)
    charging_speed = Column(String, default="DC Fast")  # DC Fast, Ultra-Fast, AC Slow
    price_per_kwh = Column(Float, default=18.5, nullable=False)
    status = Column(Enum(ChargerStatus), default=ChargerStatus.AVAILABLE, nullable=False)
    qr_code = Column(String, unique=True, index=True, nullable=False)  # volt-elite://charge?...
    is_active = Column(Boolean, default=True)
    supports_remote_start = Column(Boolean, default=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    # Relationships
    station = relationship("Station", back_populates="chargers")
    charging_sessions = relationship("ChargingSession", back_populates="charger")
    bookings = relationship("Booking", back_populates="charger")
