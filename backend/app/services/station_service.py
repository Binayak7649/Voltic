import math
import time
from typing import List, Optional, Dict, Tuple
from sqlalchemy.orm import Session, joinedload
from app.models.station import Station
from app.models.charger import Charger, ChargerStatus
from app.services.mappls_service import MapplsService

class StationService:
    # Short-lived in-memory spatial cache: key -> (timestamp, List[dict])
    _nearby_cache: Dict[Tuple[float, float, float, Optional[str], bool], Tuple[float, List[dict]]] = {}
    CACHE_TTL_SECONDS = 60  # 60s cache for high responsiveness without stale data

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
        search: Optional[str] = None,
        limit: int = 50,
        offset: int = 0
    ) -> List[Station]:
        query = db.query(Station).options(joinedload(Station.chargers)).filter(Station.is_active == True)
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
        return query.offset(offset).limit(limit).all()

    @staticmethod
    def get_nearby_stations(
        db: Session,
        user_lat: float,
        user_lon: float,
        radius_km: float = 50.0,
        operator: Optional[str] = None,
        cars_only: bool = True,
        limit: Optional[int] = 100,
        offset: int = 0
    ) -> List[dict]:
        # 1. Validation & normalization
        user_lat = max(-90.0, min(90.0, float(user_lat)))
        user_lon = max(-180.0, min(180.0, float(user_lon)))
        radius_km = max(0.5, min(500.0, float(radius_km)))

        # 2. Cache lookup using spatial grid (~100m buckets)
        cache_key = (round(user_lat, 3), round(user_lon, 3), radius_km, operator.lower() if operator else None, cars_only)
        now = time.time()
        if cache_key in StationService._nearby_cache:
            ts, cached_results = StationService._nearby_cache[cache_key]
            if now - ts < StationService.CACHE_TTL_SECONDS:
                paged = cached_results[offset : offset + limit] if limit else cached_results[offset:]
                return paged

        # 3. Calculate bounding box for indexed database scan
        lat_delta = radius_km / 111.0
        cos_lat = math.cos(math.radians(user_lat))
        lon_delta = radius_km / (111.0 * max(0.01, cos_lat))

        min_lat = user_lat - lat_delta
        max_lat = user_lat + lat_delta
        min_lon = user_lon - lon_delta
        max_lon = user_lon + lon_delta

        # 4. Single optimized query with bounding box and eager loading of chargers (eliminates N+1)
        query = db.query(Station).options(joinedload(Station.chargers)).filter(
            Station.is_active == True,
            Station.latitude.between(min_lat, max_lat),
            Station.longitude.between(min_lon, max_lon)
        )
        if operator:
            query = query.filter(Station.operator.ilike(f"%{operator}%"))

        stations = query.all()
        results = []

        CAR_CONNECTOR_KEYWORDS = ["CCS", "TYPE 2", "TYPE2", "CHADEMO", "BHARAT DC", "DC FAST", "DC ULTRA"]

        for st in stations:
            dist = StationService.calculate_distance(user_lat, user_lon, st.latitude, st.longitude)
            if dist <= radius_km:
                # Use preloaded chargers without issuing additional SQL queries
                chargers = [c for c in st.chargers if c.is_active]

                # Filter specifically for 4-wheeler EV Car compatible chargers
                car_chargers = [
                    c for c in chargers
                    if any(kw in c.connector_type.upper() for kw in CAR_CONNECTOR_KEYWORDS) or c.power_kw >= 7.4
                ]
                if cars_only and not car_chargers:
                    continue

                active_chargers = car_chargers if cars_only else chargers
                total_ports = len(active_chargers)
                total_available = sum(1 for c in active_chargers if c.status == ChargerStatus.AVAILABLE)
                max_power = max((c.power_kw for c in active_chargers), default=60)
                min_price = min((c.price_per_kwh for c in active_chargers), default=18.5)
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
                    "is_car_compatible": True,
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
                            "supports_remote_start": c.supports_remote_start,
                            "is_car_connector": True
                        } for c in active_chargers
                    ]
                })

        # Fetch from Mappls REST API if key is available
        try:
            mappls_stations = MapplsService.fetch_nearby_chargers(
                user_lat=user_lat,
                user_lon=user_lon,
                radius_km=radius_km,
                cars_only=cars_only
            )
            for mst in mappls_stations:
                is_dup = any(
                    StationService.calculate_distance(mst["latitude"], mst["longitude"], r["latitude"], r["longitude"]) < 0.1
                    or mst["name"].strip().lower() == r["name"].strip().lower()
                    for r in results
                )
                if not is_dup:
                    results.append(mst)
        except Exception:
            pass

        # Sort by distance from user's current location
        results.sort(key=lambda x: x["distance_km"])

        # Cache unpaged results
        StationService._nearby_cache[cache_key] = (now, results)

        # Apply pagination
        if limit is not None:
            return results[offset : offset + limit]
        return results[offset:]

    @staticmethod
    def get_station_by_id(db: Session, station_id: str) -> Optional[Station]:
        return db.query(Station).options(joinedload(Station.chargers)).filter(Station.id == station_id).first()
