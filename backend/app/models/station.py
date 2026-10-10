from datetime import datetime
import enum
from sqlalchemy import Column, String, Float, Boolean, DateTime, Integer, Text, Index
from sqlalchemy.orm import relationship
from app.core.database import Base

class Station(Base):
    __tablename__ = "stations"
    __table_args__ = (
        Index("ix_stations_lat_lon", "latitude", "longitude"),
        Index("ix_stations_active_operator", "is_active", "operator"),
    )

    id = Column(String, primary_key=True, index=True)
    name = Column(String, nullable=False, index=True)
    operator = Column(String, nullable=False, index=True)  # Tata Power, ChargeZone, Statiq, Jio-bp, etc.
    address = Column(String, nullable=False)
    city = Column(String, default="Indore", index=True)
    state = Column(String, default="Madhya Pradesh")
    latitude = Column(Float, nullable=False)
    longitude = Column(Float, nullable=False)
    opening_time = Column(String, default="00:00")
    closing_time = Column(String, default="23:59")
    open_hours = Column(String, default="24/7 Open")
    is_active = Column(Boolean, default=True)
    rating = Column(Float, default=4.5)
    reviews_count = Column(Integer, default=120)
    amenities = Column(String, default="Parking,Cafeteria,Restroom,Wi-Fi,24/7 Support")
    operator_verified = Column(Boolean, default=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    # Relationships
    chargers = relationship("Charger", back_populates="station", cascade="all, delete-orphan")

