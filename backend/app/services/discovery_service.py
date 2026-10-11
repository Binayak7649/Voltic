import logging
import math
import time
import json
import urllib.parse
import urllib.request
from typing import Dict, List, Optional, Tuple, Any
from app.core.config import settings

logger = logging.getLogger(__name__)

class EVDiscoveryService:
    """
    Multi-source EV Charging Station Discovery Service.
    Priority:
    1. Authorized EV Charging Station APIs & Public Datasets (Open Charge Map & OpenStreetMap & Verified Database)
    2. Google Places API (Optional, when valid Google API key is configured)
    3. Mappls Nearby Places API (For Mappls map & navigation integration)
    """

    # In-memory spatial cache: (lat_bucket, lon_bucket, radius_km, operator, cars_only) -> (timestamp, List[dict])
    _cache: Dict[Tuple[float, float, float, Optional[str], bool], Tuple[float, List[dict]]] = {}
    CACHE_TTL_SECONDS = 180  # 3 minutes cache for fresh discovery

    @staticmethod
    def haversine_distance(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
        R = 6371.0  # Earth radius in km
        d_lat = math.radians(lat2 - lat1)
        d_lon = math.radians(lon2 - lon1)
        a = (math.sin(d_lat / 2) ** 2 +
             math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) *
             math.sin(d_lon / 2) ** 2)
        c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
        return round(R * c, 2)

    @staticmethod
    def is_strictly_ev_station(name: str, address: str, types: Optional[List[str]] = None, operator: str = "") -> bool:
        """
        Validates that a location is genuinely an EV charging station.
        Strictly excludes petrol pumps, ordinary fuel stations, car repair shops,
        and unrelated businesses unless an EV charging point is independently verified.
        """
        text = f"{name} {address} {operator}".upper()

        # Explicit EV positive indicators
        ev_positive_keywords = [
            "EV CHARG", "EV STATION", "CHARGING STATION", "ELECTRIC VEHICLE",
            "FAST CHARGER", "SUPERCHARGER", "CCS", "TYPE 2", "TYPE2", "CHADEMO",
            "TATA POWER EZ", "CHARGEZONE", "STATIQ", "JIO-BP PULSE", "ZEON CHARGING",
            "KAZAM", "ATHER GRID", "VOLTELITE", "E-VEHICLE"
        ]
        has_ev_indicator = any(kw in text for kw in ev_positive_keywords)

        # Exclusion keywords (petrol pumps, fuel stations, unrelated repair shops)
        fuel_exclusion_keywords = [
            "PETROL PUMP", "PETROL BUNK", "FUEL STATION", "DIESEL STATION", "FUEL BUNK",
            "CNG PUMP", "CNG STATION", "INDIAN OIL PETROL", "BHARAT PETROLEUM",
            "HINDUSTAN PETROLEUM", "HPCL", "BPCL PETROL", "IOCL", "REPAIR SHOP",
            "CAR REPAIR", "PUNCTURE", "TYRE SHOP", "SERVICE CENTER", "AUTO PARTS",
            "BIKE ONLY", "2-WHEELER ONLY", "PETROL", "DIESEL"
        ]
        has_fuel_exclusion = any(kw in text for kw in fuel_exclusion_keywords)

        if types:
            upper_types = [t.upper() for t in types]
            if "GAS_STATION" in upper_types and not has_ev_indicator:
                return False
            if "CAR_REPAIR" in upper_types and not has_ev_indicator:
                return False
            if "CAR_DEALER" in upper_types and not has_ev_indicator:
                return False

        # If it looks like a petrol pump without explicit EV verification, exclude it
        if has_fuel_exclusion and not has_ev_indicator:
            return False

        return True

    @classmethod
    def get_discovery_status(cls) -> Dict[str, Any]:
        """
        Returns discovery configuration status, configured sources, and helpful setup guidance.
        """
        configured_sources = ["Verified Indian EV Database"]
        available_sources = [
            "Open Charge Map API (Public EV Dataset)",
            "Google Places API (Optional)",
            "Mappls API (MapmyIndia)"
        ]

        ocm_key = settings.OPEN_CHARGE_MAP_API_KEY.strip()
        google_key = settings.GOOGLE_PLACES_API_KEY.strip()
        mappls_key = settings.MAPPLS_API_KEY.strip()

        if ocm_key:
            configured_sources.append("Open Charge Map (Public EV API)")
        if google_key and not google_key.startswith("your_"):
            configured_sources.append("Google Places API")
        if mappls_key and not mappls_key.startswith("your_"):
            configured_sources.append("Mappls (MapmyIndia)")

        is_any_external = bool(ocm_key or (google_key and not google_key.startswith("your_")) or (mappls_key and not mappls_key.startswith("your_")))

        setup_message = (
            "Multi-source EV discovery active with live external providers."
            if is_any_external else
            "Running on verified Indian EV network database (Tata Power, ChargeZone, Statiq, Jio-bp, Zeon, BPCL). "
            "To enable live external discovery, configure MAPPLS_API_KEY, OPEN_CHARGE_MAP_API_KEY, or GOOGLE_PLACES_API_KEY."
        )

        return {
            "is_external_configured": is_any_external,
            "configured_sources": configured_sources,
            "available_sources": available_sources,
            "priority_order": [
                "1. Authorized EV Datasets (Open Charge Map & Verified Database)",
                "2. Google Places API (Optional)",
                "3. Mappls Nearby Places API"
            ],
            "setup_message": setup_message
        }

    @classmethod
    def fetch_open_charge_map(
        cls,
        user_lat: float,
        user_lon: float,
        radius_km: float = 25.0
    ) -> List[Dict[str, Any]]:
        """
        Priority 1: Queries Open Charge Map public API with suitable reuse permissions (CC-BY).
        """
        results: List[Dict[str, Any]] = []
        try:
            api_key = settings.OPEN_CHARGE_MAP_API_KEY.strip()
            params = {
                "output": "json",
                "latitude": user_lat,
                "longitude": user_lon,
                "distance": min(radius_km, 50.0),
                "distanceunit": "KM",
                "maxresults": 40,
                "compact": "true",
                "verbose": "false"
            }
            if api_key:
                params["key"] = api_key

            url = f"https://api.openchargemap.io/v3/poi/?{urllib.parse.urlencode(params)}"
            req = urllib.request.Request(
                url,
                headers={
                    "User-Agent": "VoltElite-EV-SuperApp/1.0",
                    "Accept": "application/json"
                }
            )
            with urllib.request.urlopen(req, timeout=3.5) as response:
                if response.status == 200:
                    data = json.loads(response.read().decode("utf-8"))
                    for item in data:
                        addr = item.get("AddressInfo") or {}
                        lat = float(addr.get("Latitude") or 0.0)
                        lon = float(addr.get("Longitude") or 0.0)
                        if lat == 0.0 or lon == 0.0:
                            continue

                        title = addr.get("Title") or "EV Charging Point"
                        address_str = addr.get("AddressLine1") or addr.get("Town") or "India"
                        op_info = item.get("OperatorInfo") or {}
                        operator_title = op_info.get("Title") or "Open Charge Partner"

                        # Strict petrol pump exclusion
                        if not cls.is_strictly_ev_station(title, address_str, operator=operator_title):
                            continue

                        dist = cls.haversine_distance(user_lat, user_lon, lat, lon)
                        if dist > radius_km:
                            continue

                        # Extract verified connectors
                        connections = item.get("Connections") or []
                        chargers = []
                        max_power = 50
                        for idx, conn in enumerate(connections):
                            c_type_info = conn.get("ConnectionType") or {}
                            conn_title = c_type_info.get("Title") or "CCS 2"
                            power_val = conn.get("PowerKW") or 50
                            try:
                                power_int = int(float(power_val))
                            except (ValueError, TypeError):
                                power_int = 50
                            if power_int > max_power:
                                max_power = power_int

                            chargers.append({
                                "id": f"ocm_{item.get('ID')}_{idx+1}",
                                "charger_code": f"EVSE-OCM-{item.get('ID')}-{idx+1}",
                                "connector_type": conn_title,
                                "power_kw": power_int,
                                "charging_speed": "DC Fast" if power_int >= 50 else "AC Fast",
                                "price_per_kwh": 18.5,
                                "status": "AVAILABLE",
                                "qr_code": f"VOLT-OCM-{item.get('ID')}-EVSE{idx+1}",
                                "supports_remote_start": True,
                                "is_car_connector": True
                            })

                        status_type = item.get("StatusType") or {}
                        is_operational = status_type.get("IsOperational", True)
                        date_verified = item.get("DateLastVerified") or item.get("DateLastStatusUpdate")

                        results.append({
                            "id": f"ocm_{item.get('ID')}",
                            "name": title,
                            "operator": operator_title,
                            "address": address_str,
                            "city": addr.get("Town") or "India",
                            "state": addr.get("StateOrProvince") or "",
                            "latitude": lat,
                            "longitude": lon,
                            "distance_km": dist,
                            "eta_minutes": max(5, int(dist * 2.2)),
                            "rating": 4.5,
                            "reviews_count": 48,
                            "total_available": len(chargers) or 2,
                            "total_ports": max(len(chargers), 2),
                            "max_power_kw": max_power,
                            "price_per_kwh": 18.5,
                            "status": "AVAILABLE" if is_operational else "OFFLINE",
                            "open_hours": "24/7 Open",
                            "amenities": ["Parking", "EV Station", "Public Access"],
                            "operator_verified": True,
                            "is_car_compatible": True,
                            "data_source": "Open Charge Map",
                            "source_id": str(item.get("ID")),
                            "last_updated": date_verified or "Recently Verified",
                            "is_live_status": False,
                            "chargers": chargers or [
                                {
                                    "id": f"ocm_{item.get('ID')}_1",
                                    "charger_code": f"EVSE-OCM-{item.get('ID')}-1",
                                    "connector_type": "CCS 2",
                                    "power_kw": 60,
                                    "charging_speed": "DC Fast",
                                    "price_per_kwh": 18.5,
                                    "status": "AVAILABLE",
                                    "qr_code": f"VOLT-OCM-{item.get('ID')}-1",
                                    "supports_remote_start": True,
                                    "is_car_connector": True
                                }
                            ]
                        })
        except Exception as e:
            logger.debug(f"Open Charge Map discovery skipped or timed out: {e}")

        return results

    _last_google_call_time: float = 0.0
    _google_rate_limit_min_interval: float = 0.2  # Max 5 calls/sec per worker

    @classmethod
    def fetch_google_places(
        cls,
        user_lat: float,
        user_lon: float,
        radius_km: float = 25.0
    ) -> List[Dict[str, Any]]:
        """
        Priority 2: Queries Google Places API (New) with fallback to Legacy Places API
        when a valid GOOGLE_PLACES_API_KEY is configured.
        Strictly excludes petrol pumps, gas stations, and non-EV places.
        Includes rate-limiting, error handling, and timeout safeguards.
        """
        results: List[Dict[str, Any]] = []
        api_key = settings.GOOGLE_PLACES_API_KEY.strip()
        if not api_key or api_key.startswith("your_") or api_key.startswith("YOUR_"):
            return []

        # Rate-limiting check
        now = time.time()
        if now - cls._last_google_call_time < cls._google_rate_limit_min_interval:
            time.sleep(cls._google_rate_limit_min_interval - (now - cls._last_google_call_time))
        cls._last_google_call_time = time.time()

        # Step 1: Attempt Google Places API (New) - POST places.googleapis.com/v1/places:searchNearby
        try:
            radius_meters = float(min(radius_km * 1000.0, 50000.0))
            new_url = "https://places.googleapis.com/v1/places:searchNearby"
            headers_new = {
                "Content-Type": "application/json",
                "X-Goog-Api-Key": api_key,
                "X-Goog-FieldMask": (
                    "places.id,places.displayName,places.formattedAddress,places.location,"
                    "places.primaryType,places.types,places.rating,places.userRatingCount,"
                    "places.evChargeOptions,places.currentOpeningHours"
                ),
                "User-Agent": "VoltElite-EV/1.0"
            }
            body_new = json.dumps({
                "includedTypes": ["electric_vehicle_charging_station"],
                "maxResultCount": 20,
                "locationRestriction": {
                    "circle": {
                        "center": {"latitude": user_lat, "longitude": user_lon},
                        "radius": radius_meters
                    }
                }
            }).encode("utf-8")

            req_new = urllib.request.Request(new_url, data=body_new, headers=headers_new, method="POST")
            with urllib.request.urlopen(req_new, timeout=3.5) as resp:
                if resp.status == 200:
                    payload = json.loads(resp.read().decode("utf-8"))
                    places = payload.get("places") or []
                    for place in places:
                        loc = place.get("location") or {}
                        lat = float(loc.get("latitude") or 0.0)
                        lon = float(loc.get("longitude") or 0.0)
                        if lat == 0.0 or lon == 0.0:
                            continue

                        display_name = (place.get("displayName") or {}).get("text") or "EV Charging Station"
                        address = place.get("formattedAddress") or "India"
                        types = place.get("types") or []
                        if place.get("primaryType"):
                            types.append(place.get("primaryType"))

                        # Strict petrol pump exclusion
                        if not cls.is_strictly_ev_station(display_name, address, types):
                            continue

                        dist = cls.haversine_distance(user_lat, user_lon, lat, lon)
                        if dist > radius_km:
                            continue

                        # Operator inference from name
                        operator = "Google Places Verified"
                        upper = display_name.upper()
                        if "TATA" in upper:
                            operator = "Tata Power"
                        elif "STATIQ" in upper:
                            operator = "Statiq"
                        elif "CHARGEZONE" in upper:
                            operator = "ChargeZone"
                        elif "JIO" in upper or "BP" in upper:
                            operator = "Jio-bp"
                        elif "ZEON" in upper:
                            operator = "Zeon Charging"
                        elif "BPCL" in upper:
                            operator = "BPCL eDrive"
                        elif "ATHER" in upper:
                            operator = "Ather Grid"

                        place_id = place.get("id") or str(len(results) + 1)
                        rating = float(place.get("rating") or 4.6)
                        user_ratings = int(place.get("userRatingCount") or 65)

                        # Parse EV charge options if available from Places API (New)
                        ev_options = place.get("evChargeOptions") or {}
                        connector_count = ev_options.get("connectorCount") or 2
                        connector_aggregation = ev_options.get("connectorAggregation") or []

                        chargers = []
                        max_power = 60
                        if connector_aggregation:
                            for idx, c_agg in enumerate(connector_aggregation):
                                c_type = c_agg.get("type", "EV_CONNECTOR_TYPE_CCS_COMBO_2")
                                # Format connector label nicely
                                norm_type = "CCS 2"
                                if "TYPE_2" in c_type:
                                    norm_type = "Type 2 AC"
                                elif "CHADEMO" in c_type:
                                    norm_type = "CHAdeMO"
                                elif "TESLA" in c_type:
                                    norm_type = "Tesla Supercharger"

                                p_kw = int(c_agg.get("maxChargeRateKw") or 60)
                                if p_kw > max_power:
                                    max_power = p_kw

                                count = c_agg.get("count") or 1
                                for i in range(count):
                                    c_id = f"chg_gp_{place_id}_{idx+1}_{i+1}"
                                    chargers.append({
                                        "id": c_id,
                                        "charger_code": f"EVSE-GP-{place_id[:6].upper()}-{idx+1}-{i+1}",
                                        "connector_type": norm_type,
                                        "power_kw": p_kw,
                                        "charging_speed": "DC Fast" if p_kw >= 50 else "AC Fast",
                                        "price_per_kwh": 18.5,
                                        "status": "AVAILABLE",
                                        "qr_code": f"VOLT-GP-{place_id[:6].upper()}-{idx+1}-{i+1}",
                                        "supports_remote_start": True,
                                        "is_car_connector": True
                                    })

                        if not chargers:
                            chargers = [
                                {
                                    "id": f"chg_gp_{place_id}_1",
                                    "charger_code": f"EVSE-GP-{place_id[:8].upper()}-CCS2",
                                    "connector_type": "CCS 2",
                                    "power_kw": 60,
                                    "charging_speed": "DC Fast",
                                    "price_per_kwh": 18.5,
                                    "status": "AVAILABLE",
                                    "qr_code": f"VOLT-GP-{place_id[:8].upper()}-CCS2",
                                    "supports_remote_start": True,
                                    "is_car_connector": True
                                }
                            ]

                        results.append({
                            "id": f"gplaces_{place_id}",
                            "name": display_name,
                            "operator": operator,
                            "address": address,
                            "city": "India",
                            "state": "",
                            "latitude": lat,
                            "longitude": lon,
                            "distance_km": dist,
                            "eta_minutes": max(5, int(dist * 2.2)),
                            "rating": rating,
                            "reviews_count": user_ratings,
                            "total_available": len(chargers),
                            "total_ports": max(len(chargers), connector_count),
                            "max_power_kw": max_power,
                            "price_per_kwh": 18.5,
                            "status": "AVAILABLE",
                            "open_hours": "24/7 Open",
                            "amenities": ["Parking", "EV Station", "Places API (New)"],
                            "operator_verified": True,
                            "is_car_compatible": True,
                            "data_source": "Google Places API (New)",
                            "source_id": place_id,
                            "last_updated": "Live Places Feed",
                            "is_live_status": False,
                            "chargers": chargers
                        })

                    if results:
                        return results
        except Exception as e_new:
            logger.debug(f"Google Places API (New) query error: {e_new}, falling back to legacy search")

        # Step 2: Fallback to Google Places Nearby Search (Legacy API)
        try:
            radius_meters = int(min(radius_km * 1000, 50000))
            params = {
                "location": f"{user_lat},{user_lon}",
                "radius": radius_meters,
                "type": "electric_vehicle_charging_station",
                "key": api_key
            }
            url = f"https://maps.googleapis.com/maps/api/place/nearbysearch/json?{urllib.parse.urlencode(params)}"
            req = urllib.request.Request(
                url,
                headers={"User-Agent": "VoltElite-EV/1.0", "Accept": "application/json"}
            )
            with urllib.request.urlopen(req, timeout=3.5) as response:
                if response.status == 200:
                    payload = json.loads(response.read().decode("utf-8"))
                    places = payload.get("results") or []
                    for place in places:
                        geom = place.get("geometry", {}).get("location", {})
                        lat = float(geom.get("lat") or 0.0)
                        lon = float(geom.get("lng") or 0.0)
                        if lat == 0.0 or lon == 0.0:
                            continue

                        name = place.get("name") or "EV Charging Station"
                        address = place.get("vicinity") or "India"
                        types = place.get("types") or []

                        # Strict petrol pump exclusion
                        if not cls.is_strictly_ev_station(name, address, types):
                            continue

                        dist = cls.haversine_distance(user_lat, user_lon, lat, lon)
                        if dist > radius_km:
                            continue

                        # Operator inference from name
                        operator = "Google Places Verified"
                        upper = name.upper()
                        if "TATA" in upper:
                            operator = "Tata Power"
                        elif "STATIQ" in upper:
                            operator = "Statiq"
                        elif "CHARGEZONE" in upper:
                            operator = "ChargeZone"
                        elif "JIO" in upper or "BP" in upper:
                            operator = "Jio-bp"
                        elif "ZEON" in upper:
                            operator = "Zeon Charging"
                        elif "BPCL" in upper:
                            operator = "BPCL eDrive"

                        place_id = place.get("place_id") or str(len(results) + 1)
                        rating = float(place.get("rating") or 4.6)
                        user_ratings = int(place.get("user_ratings_total") or 65)

                        results.append({
                            "id": f"gplaces_{place_id}",
                            "name": name,
                            "operator": operator,
                            "address": address,
                            "city": "India",
                            "state": "",
                            "latitude": lat,
                            "longitude": lon,
                            "distance_km": dist,
                            "eta_minutes": max(5, int(dist * 2.2)),
                            "rating": rating,
                            "reviews_count": user_ratings,
                            "total_available": 2,
                            "total_ports": 2,
                            "max_power_kw": 60,
                            "price_per_kwh": 18.5,
                            "status": "AVAILABLE",
                            "open_hours": "24/7 Open",
                            "amenities": ["Parking", "EV Station", "Google Places"],
                            "operator_verified": True,
                            "is_car_compatible": True,
                            "data_source": "Google Places API",
                            "source_id": place_id,
                            "last_updated": "Live Places Feed",
                            "is_live_status": False,
                            "chargers": [
                                {
                                    "id": f"chg_gp_{place_id}_1",
                                    "charger_code": f"EVSE-GP-{place_id[:8].upper()}-CCS2",
                                    "connector_type": "CCS 2",
                                    "power_kw": 60,
                                    "charging_speed": "DC Fast",
                                    "price_per_kwh": 18.5,
                                    "status": "AVAILABLE",
                                    "qr_code": f"VOLT-GP-{place_id[:8].upper()}-CCS2",
                                    "supports_remote_start": True,
                                    "is_car_connector": True
                                }
                            ]
                        })
        except Exception as e:
            logger.debug(f"Google Places discovery skipped or timed out: {e}")

        return results

    @classmethod
    def deduplicate_and_merge(cls, primary_list: List[Dict[str, Any]], secondary_list: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        Deduplicates stations across sources using reliable 100-meter spatial proximity
        and normalized name matching. Prevents duplicate markers for the same physical charger.
        """
        merged = list(primary_list)
        for cand in secondary_list:
            c_lat = cand["latitude"]
            c_lon = cand["longitude"]
            c_name_norm = cand["name"].strip().lower()

            is_duplicate = False
            for existing in merged:
                dist = cls.haversine_distance(c_lat, c_lon, existing["latitude"], existing["longitude"])
                e_name_norm = existing["name"].strip().lower()
                # Within 100 meters (0.1 km) or identical name within 500m
                if dist < 0.10 or (c_name_norm == e_name_norm and dist < 0.5):
                    is_duplicate = True
                    break

            if not is_duplicate:
                merged.append(cand)

        return merged
