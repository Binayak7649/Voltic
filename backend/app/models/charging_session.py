from datetime import datetime
import enum
from sqlalchemy import Column, String, Float, DateTime, Integer, ForeignKey, Enum
from sqlalchemy.orm import relationship
from app.core.database import Base

class SessionStatus(str, enum.Enum):
    PENDING = "PENDING"
    CHARGING = "CHARGING"
    COMPLETED = "COMPLETED"
    CANCELLED = "CANCELLED"
    FAILED = "FAILED"

class ChargingSession(Base):
    __tablename__ = "charging_sessions"

    id = Column(String, primary_key=True, index=True)
    user_id = Column(String, ForeignKey("users.id"), nullable=False, index=True)
    charger_id = Column(String, ForeignKey("chargers.id"), nullable=False, index=True)
    start_time = Column(DateTime, default=datetime.utcnow, nullable=False)
    end_time = Column(DateTime, nullable=True)
    status = Column(Enum(SessionStatus), default=SessionStatus.CHARGING, nullable=False)
    
    # Telemetry
    energy_consumed_kwh = Column(Float, default=0.0)
    charging_duration = Column(Integer, default=0)  # in seconds
    start_percentage = Column(Float, default=20.0)
    end_percentage = Column(Float, default=80.0)
    current_percentage = Column(Float, default=20.0)
    target_percentage = Column(Float, default=85.0)
    power_kw = Column(Integer, default=60)
    
    # Financials
    tariff_per_kwh = Column(Float, default=18.5)
    estimated_cost = Column(Float, default=0.0)
    final_cost = Column(Float, default=0.0)
    evse_id = Column(String, default="EVSE-08")
    txn_id = Column(String, nullable=True)
    payment_method = Column(String, default="UPI")

    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    # Relationships
    user = relationship("User", back_populates="charging_sessions")
    charger = relationship("Charger", back_populates="charging_sessions")
    payment = relationship("Payment", back_populates="session", uselist=False)
