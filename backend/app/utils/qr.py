import urllib.parse
import re
from typing import Dict, Any, Optional

class QRParser:
    """
    Parses diverse official EV charging network QR formats across India:
    - Provider URLs (Tata Power, ChargeZone, Statiq, Jio-bp, Zeon, BPCL)
    - OCPI 2.2 EVSE identifiers (e.g. IN*TPA*E001*1)
    - VoltElite custom deep links (volt-elite://charge?station=...&evse=...&connector=...)
    - Direct EVSE hardware codes (e.g. EVSE-08, EVSE-CZ-120A, EVSE-STQ-50A)
    - Demo simulation presets
    - Safely rejects invalid / non-EV codes
    """
    @staticmethod
    def parse_payload(payload: str) -> Dict[str, Any]:
        raw = (payload or "").strip()
        if not raw:
            return {
                "is_valid": False,
                "error": "Scanned QR code is empty. Please align your camera with the EV charger code."
            }

        # 1. Reject common non-EV / generic URLs
        lower_raw = raw.lower()
        if any(bad in lower_raw for bad in ["google.com", "youtube.com", "facebook.com", "wikipedia.org", "instagram.com"]):
            return {
                "is_valid": False,
                "error": "Scanned code is a generic web link, not an EV charging station. Please scan the QR code located on the EVSE charging connector."
            }

        result = {
            "is_valid": True,
            "raw": raw,
            "station_id": None,
            "evse_id": None,
            "connector_type": "CCS 2",
            "operator": "Tata Power",
            "is_demo_preset": False
        }

        # 2. VoltElite Custom Deep Link
        # Example: volt-elite://charge?station=stn_idr_01&evse=EVSE-08&connector=ccs2
        if raw.startswith("volt-elite://"):
            try:
                parsed_url = urllib.parse.urlparse(raw)
                params = urllib.parse.parse_qs(parsed_url.query)
                result["station_id"] = params.get("station", [None])[0]
                result["evse_id"] = params.get("evse", [None])[0]
                if "connector" in params:
                    conn = params["connector"][0].upper()
                    result["connector_type"] = "Type 2" if "TYPE2" in conn else "CCS 2"
                if "operator" in params:
                    result["operator"] = params["operator"][0]
                return result
            except Exception:
                pass

        # 3. Tata Power EZ Charge URLs & Deep Links
        # Example: https://ezcharge.tatapower.com/charge?id=EVSE-08
        if "tatapower" in lower_raw or "ezcharge" in lower_raw or raw.startswith("IN*TPA*"):
            result["operator"] = "Tata Power"
            if "IN*TPA*" in raw:
                # OCPI format: map to EVSE code
                parts = raw.split("*")
                evse_part = parts[2] if len(parts) >= 3 else "EVSE-08"
                result["evse_id"] = f"EVSE-{evse_part[-2:]}" if len(evse_part) >= 2 else "EVSE-08"
                if len(parts) >= 4 and parts[3] == "2":
                    result["connector_type"] = "Type 2"
                return result

            try:
                parsed = urllib.parse.urlparse(raw)
                q = urllib.parse.parse_qs(parsed.query)
                result["evse_id"] = q.get("id", [None])[0] or q.get("evse", [None])[0] or "EVSE-08"
                return result
            except Exception:
                result["evse_id"] = "EVSE-08"
                return result

        # 4. ChargeZone URLs & Presets
        # Example: https://chargezone.co/qr/CZ-PHX-120A or CZ-PHX-120KW-CCS2
        if "chargezone" in lower_raw or "cz-phx" in lower_raw:
            result["operator"] = "ChargeZone"
            if "120b" in lower_raw:
                result["evse_id"] = "EVSE-CZ-120B"
            else:
                result["evse_id"] = "EVSE-CZ-120A"
            return result

        # 5. Statiq URLs & Presets
        # Example: https://statiq.in/charge?evse=EVSE-STQ-50A or STATIQ-PAL
        if "statiq" in lower_raw:
            result["operator"] = "Statiq"
            if "22" in lower_raw or "type2" in lower_raw:
                result["evse_id"] = "EVSE-STQ-22A"
                result["connector_type"] = "Type 2"
            else:
                result["evse_id"] = "EVSE-STQ-50A"
            return result

        # 6. Jio-bp pulse URLs & Presets
        if "jiobp" in lower_raw or "jio-bp" in lower_raw or "jbp" in lower_raw:
            result["operator"] = "Jio-bp"
            result["evse_id"] = "EVSE-JBP-60A"
            return result

        # 7. Kazam / Incompatible Port Test
        if "kazam" in lower_raw or "incompatible" in lower_raw:
            result["operator"] = "Kazam"
            result["connector_type"] = "Type 2"
            result["evse_id"] = "EVSE-KZ-02"
            return result

        # 8. Demo Presets
        if "demo" in lower_raw:
            result["is_demo_preset"] = True
            if "120" in lower_raw:
                result["operator"] = "ChargeZone"
                result["evse_id"] = "EVSE-CZ-120A"
            elif "type2" in lower_raw or "11kw" in lower_raw or "incompatible" in lower_raw:
                result["operator"] = "Kazam"
                result["connector_type"] = "Type 2"
                result["evse_id"] = "EVSE-KZ-02"
            else:
                result["operator"] = "Tata Power"
                result["evse_id"] = "EVSE-08"
            return result

        # 9. Direct EVSE Codes (e.g. EVSE-08, EVSE-09, EVSE-10, EVSE-CZ-120A)
        clean_code = raw.strip().upper()
        if clean_code.startswith("EVSE-") or clean_code.startswith("CHG-") or clean_code in ["EVSE-08", "EVSE-09", "EVSE-10"]:
            result["evse_id"] = clean_code
            if "10" in clean_code or "KZ" in clean_code:
                result["connector_type"] = "Type 2"
            return result

        # 10. Direct Station + Charger ID pattern (e.g. STN01-EVSE08)
        evse_match = re.search(r"EVSE-?[A-Z0-9]+", clean_code)
        if evse_match:
            result["evse_id"] = evse_match.group(0)
            return result

        # 11. If payload does not match any known EV format, reject safely
        return {
            "is_valid": False,
            "error": f"Unrecognized EV charger QR code ('{raw[:25]}...'). Please scan an authorized charger QR code or select a preset."
        }

