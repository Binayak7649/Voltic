import uuid
from typing import Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.services.station_service import StationService
from app.models.station import Station
from app.schemas.station import StationCreate, StationUpdate
from app.api.deps import get_current_user, get_current_admin
from app.utils.helpers import standard_response

router = APIRouter(prefix="/stations", tags=["Stations"])

@router.get("")
def list_stations(
    city: Optional[str] = None,
    operator: Optional[str] = None,
    search: Optional[str] = None,
    db: Session = Depends(get_db)
):
    stations = StationService.get_stations(db, city=city, operator=operator, search=search)
    return standard_response(True, "Stations fetched successfully", [
        {
            "id": s.id,
            "name": s.name,
            "operator": s.operator,
            "address": s.address,
            "city": s.city,
            "state": s.state,
            "latitude": s.latitude,
            "longitude": s.longitude,
            "rating": s.rating,
            "reviews_count": s.reviews_count,
            "open_hours": s.open_hours,
            "amenities": s.amenities.split(",") if s.amenities else [],
            "chargers_count": len(s.chargers),
            "status": "AVAILABLE" if any(c.status == "AVAILABLE" for c in s.chargers) else "BUSY"
        } for s in stations
    ])

@router.get("/nearby")
def nearby_stations(
    latitude: float = Query(22.7196, description="User latitude"),
    longitude: float = Query(75.8577, description="User longitude"),
    radius: float = Query(50.0, description="Radius in km"),
    operator: Optional[str] = None,
    db: Session = Depends(get_db)
):
    results = StationService.get_nearby_stations(db, user_lat=latitude, user_lon=longitude, radius_km=radius, operator=operator)
    return standard_response(True, f"Found {len(results)} stations within {radius} km", results)

@router.get("/{station_id}")
def get_station_details(station_id: str, db: Session = Depends(get_db)):
    st = StationService.get_station_by_id(db, station_id)
    if not st:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Station not found")

    return standard_response(True, "Station details retrieved", {
        "id": st.id,
        "name": st.name,
        "operator": st.operator,
        "address": st.address,
        "city": st.city,
        "state": st.state,
        "latitude": st.latitude,
        "longitude": st.longitude,
        "rating": st.rating,
        "reviews_count": st.reviews_count,
        "open_hours": st.open_hours,
        "amenities": st.amenities.split(",") if st.amenities else [],
        "operator_verified": st.operator_verified,
        "chargers": [
            {
                "id": c.id,
                "charger_code": c.charger_code,
                "connector_type": c.connector_type,
                "power_kw": c.power_kw,
                "charging_speed": c.charging_speed,
                "price_per_kwh": c.price_per_kwh,
                "status": c.status.value if hasattr(c.status, 'value') else str(c.status),
                "qr_code": c.qr_code,
                "supports_remote_start": c.supports_remote_start
            } for c in st.chargers if c.is_active
        ]
    })

@router.post("")
def create_station(req: StationCreate, db: Session = Depends(get_db), admin=Depends(get_current_admin)):
    st_id = f"stn_{uuid.uuid4().hex[:8]}"
    station = Station(
        id=st_id,
        name=req.name,
        operator=req.operator,
        address=req.address,
        city=req.city,
        state=req.state,
        latitude=req.latitude,
        longitude=req.longitude,
        opening_time=req.opening_time,
        closing_time=req.closing_time,
        open_hours=req.open_hours,
        rating=req.rating,
        reviews_count=req.reviews_count,
        amenities=req.amenities,
        operator_verified=req.operator_verified
    )
    db.add(station)
    db.commit()
    db.refresh(station)
    return standard_response(True, "Station created successfully", {"id": station.id})

@router.put("/{station_id}")
def update_station(station_id: str, req: StationUpdate, db: Session = Depends(get_db), admin=Depends(get_current_admin)):
    st = StationService.get_station_by_id(db, station_id)
    if not st:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Station not found")
    for key, val in req.dict(exclude_unset=True).items():
        setattr(st, key, val)
    db.commit()
    return standard_response(True, "Station updated successfully")

@router.delete("/{station_id}")
def delete_station(station_id: str, db: Session = Depends(get_db), admin=Depends(get_current_admin)):
    st = StationService.get_station_by_id(db, station_id)
    if not st:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Station not found")
    st.is_active = False
    db.commit()
    return standard_response(True, "Station archived successfully")
