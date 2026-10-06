from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.services.analytics_service import AnalyticsService
from app.api.deps import get_current_user, get_current_admin
from app.models.user import User
from app.utils.helpers import standard_response

router = APIRouter(prefix="/analytics", tags=["Analytics"])

@router.get("/user")
def get_user_analytics(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    data = AnalyticsService.get_user_analytics(db, current_user.id)
    return standard_response(True, "User analytics generated", data)

@router.get("/admin")
def get_admin_analytics(db: Session = Depends(get_db), admin: User = Depends(get_current_admin)):
    data = AnalyticsService.get_admin_analytics(db)
    return standard_response(True, "Admin analytics generated", data)
