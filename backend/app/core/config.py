import os
from typing import List
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    PROJECT_NAME: str = "VoltElite EV Charging API"
    VERSION: str = "1.0.0"
    API_V1_STR: str = "/api"

    # Environment
    DEBUG: bool = True
    PORT: int = 8000
    HOST: str = "0.0.0.0"

    # Database: defaults to SQLite if Postgres not running locally
    DATABASE_URL: str = os.getenv("DATABASE_URL", "sqlite:///./voltelite.db")

    # JWT Security
    JWT_SECRET: str = os.getenv("JWT_SECRET", "super_secret_volt_elite_jwt_key_change_in_production_9988776655")
    JWT_ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 24  # 24 hours
    REFRESH_TOKEN_EXPIRE_DAYS: int = 7

    # CORS
    BACKEND_CORS_ORIGINS: List[str] = [
        "*",
        "http://localhost:3000",
        "http://localhost:8080",
        "http://10.0.2.2:8000"
    ]

    # Google OAuth
    GOOGLE_CLIENT_ID: str = os.getenv("GOOGLE_CLIENT_ID", "")
    GOOGLE_CLIENT_SECRET: str = os.getenv("GOOGLE_CLIENT_SECRET", "")
    GOOGLE_REDIRECT_URI: str = os.getenv("GOOGLE_REDIRECT_URI", "http://localhost:8000/api/auth/google/callback")

    # Razorpay
    RAZORPAY_KEY_ID: str = os.getenv("RAZORPAY_KEY_ID", "rzp_test_YourTestKeyId")
    RAZORPAY_KEY_SECRET: str = os.getenv("RAZORPAY_KEY_SECRET", "YourTestKeySecret")

    # Phone OTP
    OTP_PROVIDER: str = os.getenv("OTP_PROVIDER", "mock")
    OTP_API_KEY: str = os.getenv("OTP_API_KEY", "")
    DEV_OTP_MODE: bool = os.getenv("DEV_OTP_MODE", "true").lower() == "true"
    FIXED_DEV_OTP: str = "123456"

    # Maps
    GOOGLE_MAPS_API_KEY: str = os.getenv("GOOGLE_MAPS_API_KEY", "")

    # Hardware Simulation
    SIMULATION_MODE: bool = os.getenv("SIMULATION_MODE", "true").lower() == "true"
    SIMULATION_TICK_SECONDS: int = 2

    class Config:
        case_sensitive = True
        env_file = ".env"

settings = Settings()
