import uuid
from typing import List
from sqlalchemy.orm import Session
from app.models.notification import Notification

class NotificationService:
    @staticmethod
    def get_user_notifications(db: Session, user_id: str) -> List[Notification]:
        return db.query(Notification).filter(
            Notification.user_id == user_id
        ).order_by(Notification.created_at.desc()).all()

    @staticmethod
    def create_notification(db: Session, user_id: str, title: str, message: str, notif_type: str = "charging") -> Notification:
        notif = Notification(
            id=f"notif_{uuid.uuid4().hex[:12]}",
            user_id=user_id,
            title=title,
            message=message,
            type=notif_type,
            is_read=False
        )
        db.add(notif)
        db.commit()
        db.refresh(notif)
        return notif

    @staticmethod
    def mark_as_read(db: Session, notification_id: str, user_id: str) -> bool:
        notif = db.query(Notification).filter(
            Notification.id == notification_id,
            Notification.user_id == user_id
        ).first()
        if notif:
            notif.is_read = True
            db.commit()
            return True
        return False
