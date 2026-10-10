import logging
from fastapi import FastAPI, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from app.core.config import settings
from app.core.database import engine, Base

# Import all models to ensure metadata registration
from app.models.user import User
from app.models.station import Station
from app.models.charger import Charger
from app.models.charging_session import ChargingSession
from app.models.payment import Payment
from app.models.booking import Booking
from app.models.notification import Notification
from app.models.transaction import Transaction

# Create DB tables automatically on startup
Base.metadata.create_all(bind=engine)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("VoltElite")

app = FastAPI(
    title="VoltElite EV Charging API",
    description="Unified backend REST API and WebSocket service for VoltElite EV Charging Super App across India.",
    version=settings.VERSION,
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/openapi.json"
)

from fastapi.middleware.gzip import GZipMiddleware

# GZip Compression Middleware for payload optimization
app.add_middleware(GZipMiddleware, minimum_size=1000)

# CORS Middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# Custom Validation Exception Handler
@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    errors = exc.errors()
    first_error = errors[0]["msg"] if errors else "Invalid request payload."
    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content={
            "success": False,
            "message": first_error,
            "error_code": "VALIDATION_ERROR",
            "details": errors
        }
    )

# Generic Exception Handler
@app.exception_handler(Exception)
async def generic_exception_handler(request: Request, exc: Exception):
    logger.error(f"Unhandled error at {request.url.path}: {str(exc)}")
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "success": False,
            "message": "Internal server error occurred. Please try again.",
            "error_code": "SERVER_ERROR"
        }
    )

# Include API Routers
from app.api.auth import router as auth_router
from app.api.users import router as users_router
from app.api.stations import router as stations_router
from app.api.chargers import router as chargers_router
from app.api.qr import router as qr_router
from app.api.charging import router as charging_router
from app.api.payments import router as payments_router
from app.api.bookings import router as bookings_router
from app.api.notifications import router as notifications_router
from app.api.history import router as history_router
from app.api.analytics import router as analytics_router

api_v1 = settings.API_V1_STR
app.include_router(auth_router, prefix=api_v1)
app.include_router(users_router, prefix=api_v1)
app.include_router(stations_router, prefix=api_v1)
app.include_router(chargers_router, prefix=api_v1)
app.include_router(qr_router, prefix=api_v1)
app.include_router(charging_router, prefix=api_v1)
app.include_router(payments_router, prefix=api_v1)
app.include_router(bookings_router, prefix=api_v1)
app.include_router(notifications_router, prefix=api_v1)
app.include_router(history_router, prefix=api_v1)
app.include_router(analytics_router, prefix=api_v1)

@app.get("/")
def root():
    return {
        "success": True,
        "app": settings.PROJECT_NAME,
        "version": settings.VERSION,
        "status": "online",
        "docs": "/docs",
        "simulation_mode": settings.SIMULATION_MODE
    }

@app.get("/health")
def health_check():
    return {"status": "healthy", "database": "connected"}
