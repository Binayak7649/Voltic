import math
from typing import List, Optional
from sqlalchemy.orm import Session
from app.models.station import Station
from app.models.charger import Charger, ChargerStatus

class StationService:
    @staticmethod
    def calculate_distance(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
        """Haversine formula to compute distance in km between two lat/lon coordinates."""
        R = 6371.0  # Earth radius in km
        d_lat = math.radians(lat2 - lat1)
        d_lon = math.radians(lon2 - lon1)
        a = (math.sin(d_lat / 2) ** 2 +
             math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) *
             math.sin(d_lon / 2) ** 2)
        c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
        return round(R * c, 2)

    @staticmethod
    def get_stations(
        db: Session,
        city: Optional[str] = None,
        operator: Optional[str] = None,
        search: Optional[str] = None
    ) -> List[Station]:
        query = db.query(Station).filter(Station.is_active == True)
        if city:
            query = query.filter(Station.city.ilike(f"%{city}%"))
        if operator:
            query = query.filter(Station.operator.ilike(f"%{operator}%"))
        if search:
            query = query.filter(
                (Station.name.ilike(f"%{search}%")) |
                (Station.address.ilike(f"%{search}%")) |
                (Station.operator.ilike(f"%{search}%"))
            )
        return query.all()

    @staticmethod
    def get_nearby_stations(
        db: Session,
        user_lat: float,
        user_lon: float,
        radius_km: float = 50.0,
        operator: Optional[str] = None
    ) -> List[dict]:
        stations = db.query(Station).filter(Station.is_active == True).all()
        results = []

        for st in stations:
            if operator and st.operator.lower() != operator.lower():
                continue
            dist = StationService.calculate_distance(user_lat, user_lon, st.latitude, st.longitude)
            if dist <= radius_km:
                chargers = db.query(Charger).filter(Charger.station_id == st.id, Charger.is_active == True).all()
                total_ports = len(chargers)
                total_available = sum(1 for c in chargers if c.status == ChargerStatus.AVAILABLE)
                max_power = max((c.power_kw for c in chargers), default=60)
                min_price = min((c.price_per_kwh for c in chargers), default=18.5)
                eta = max(5, int(dist * 2.2))

                results.append({
                    "id": st.id,
                    "name": st.name,
                    "operator": st.operator,
                    "address": st.address,
                    "city": st.city,
                    "state": st.state,
                    "latitude": st.latitude,
                    "longitude": st.longitude,
                    "distance_km": dist,
                    "eta_minutes": eta,
                    "rating": st.rating,
                    "reviews_count": st.reviews_count,
                    "total_available": total_available,
                    "total_ports": total_ports,
                    "max_power_kw": max_power,
                    "price_per_kwh": min_price,
                    "status": "AVAILABLE" if total_available > 0 else "BUSY",
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
                        } for c in chargers
                    ]
                })

        # Sort by distance
        results.sort(key=lambda x: x["distance_km"])
        return results

    @staticmethod
    def get_station_by_id(db: Session, station_id: str) -> Optional[Station]:
        return db.query(Station).filter(Station.id == station_id).first()
