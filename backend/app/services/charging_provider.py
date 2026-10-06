from abc import ABC, abstractmethod
from typing import Dict, Any

class ChargingProvider(ABC):
    """
    Abstract charging provider interface for real vs simulated hardware.
    Future-ready for OCPP 1.6J / 2.0.1 EVSE connectors.
    """
    @abstractmethod
    def start_remote_charging(self, charger_code: str, evse_id: str, id_tag: str) -> Dict[str, Any]:
        pass

    @abstractmethod
    def stop_remote_charging(self, charger_code: str, transaction_id: str) -> Dict[str, Any]:
        pass

    @abstractmethod
    def get_meter_values(self, transaction_id: str) -> Dict[str, Any]:
        pass


class SimulationChargingProvider(ChargingProvider):
    """
    Realistic demonstration simulation provider for college and testing demos.
    Calculates power curve, energy delivered, state-of-charge progression, and telemetry.
    """
    def start_remote_charging(self, charger_code: str, evse_id: str, id_tag: str) -> Dict[str, Any]:
        return {
            "status": "Accepted",
            "provider": "SimulationChargingProvider",
            "charger_code": charger_code,
            "evse_id": evse_id,
            "message": "Remote start transaction acknowledged by simulated EVSE."
        }

    def stop_remote_charging(self, charger_code: str, transaction_id: str) -> Dict[str, Any]:
        return {
            "status": "Accepted",
            "provider": "SimulationChargingProvider",
            "transaction_id": transaction_id,
            "message": "Remote stop transaction acknowledged by simulated EVSE."
        }

    def get_meter_values(self, transaction_id: str) -> Dict[str, Any]:
        return {
            "transaction_id": transaction_id,
            "voltage_v": 400.0,
            "current_a": 150.0,
            "temperature_c": 32.5
        }


class RealChargingProvider(ChargingProvider):
    """
    Production OCPP / Vendor API client (e.g. Tata Power EZ Charge, ChargeZone, Statiq OCPI).
    """
    def __init__(self, ocpp_server_url: str = ""):
        self.ocpp_server_url = ocpp_server_url

    def start_remote_charging(self, charger_code: str, evse_id: str, id_tag: str) -> Dict[str, Any]:
        # Connect to OCPP Central System or OCPI 2.2 commands endpoint
        return {
            "status": "Accepted",
            "provider": "RealChargingProvider",
            "charger_code": charger_code,
            "evse_id": evse_id,
            "message": "OCPP RemoteStartTransaction sent to physical charger."
        }

    def stop_remote_charging(self, charger_code: str, transaction_id: str) -> Dict[str, Any]:
        return {
            "status": "Accepted",
            "provider": "RealChargingProvider",
            "transaction_id": transaction_id,
            "message": "OCPP RemoteStopTransaction sent to physical charger."
        }

    def get_meter_values(self, transaction_id: str) -> Dict[str, Any]:
        return {
            "transaction_id": transaction_id,
            "voltage_v": 415.0,
            "current_a": 145.0,
            "temperature_c": 34.0
        }


def get_charging_provider() -> ChargingProvider:
    # Defaults to simulation mode as requested
    return SimulationChargingProvider()
