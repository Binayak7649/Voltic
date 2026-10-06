from typing import List, Dict, Any
from pydantic import BaseModel

class UserAnalyticsResponse(BaseModel):
    total_charging_sessions: int
    total_energy_consumed_kwh: float
    total_money_spent_rupees: float
    average_charging_duration_min: int
    most_used_station: str
    co2_savings_kg: float
    monthly_stats: List[Dict[str, Any]]
    operator_distribution: Dict[str, int]

class AdminAnalyticsResponse(BaseModel):
    total_users: int
    total_stations: int
    total_chargers: int
    active_chargers: int
    total_sessions: int
    total_energy_delivered_kwh: float
    total_revenue_rupees: float
    average_session_duration_min: int
    co2_saved_kg: float
    daily_stats: List[Dict[str, Any]]
    station_performance: List[Dict[str, Any]]
