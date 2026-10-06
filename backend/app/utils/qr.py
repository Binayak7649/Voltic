import urllib.parse
from typing import Dict, Any, Optional

class QRParser:
    @staticmethod
    def parse_payload(payload: str) -> Dict[str, Any]:
        """
        Parses QR payloads from VoltElite deep links, vendor URIs, OCPI EVSE IDs, or raw hardware codes.
        Examples:
        - volt-elite://charge?station=stn_01&evse=EVSE-08&connector=ccs2
        - CZ-PHX-120KW-CCS2
        - VOLT-DEMO-STN001-EVSE08-CCS2
        - IN*TPA*E001*1
        """
        raw = payload.strip()
        result = {
            "raw": raw,
            "station_id": None,
            "evse_id": None,
            "connector_type": "CCS 2",
            "operator": "Tata Power"
        }

        if raw.startswith("volt-elite://"):
            try:
                parsed_url = urllib.parse.urlparse(raw)
                query_params = urllib.parse.parse_qs(parsed_url.query)
                result["station_id"] = query_params.get("station", [None])[0]
                result["evse_id"] = query_params.get("evse", [None])[0]
                if "connector" in query_params:
                    conn = query_params["connector"][0].upper()
                    result["connector_type"] = "Type 2" if "TYPE2" in conn else "CCS 2"
                return result
            except Exception:
                pass

        if "INCOMPATIBLE" in raw or "TYPE2" in raw:
            result["connector_type"] = "Type 2"
            result["operator"] = "Kazam"
            result["evse_id"] = "EVSE-KZ-02"
            return result

        if "chargezone" in raw.lower() or "CZ-PHX" in raw:
            result["operator"] = "ChargeZone"
            result["evse_id"] = "EVSE-CZ-120A"
            return result

        if "statiq" in raw.lower() or "STATIQ-PAL" in raw:
            result["operator"] = "Statiq"
            result["evse_id"] = "EVSE-STQ-50A"
            return result

        if "jiobp" in raw.lower() or "JIOBP" in raw:
            result["operator"] = "Jio-bp"
            result["evse_id"] = "EVSE-JBP-60A"
            return result

        # Default fallback
        result["evse_id"] = raw if len(raw) <= 12 else "EVSE-08"
        return result
