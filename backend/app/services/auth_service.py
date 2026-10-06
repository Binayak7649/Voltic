import uuid
from typing import Optional, Tuple
from sqlalchemy.orm import Session
from app.models.user import User, UserRole
from app.schemas.auth import RegisterRequest, LoginRequest
from app.core.security import get_password_hash, verify_password, create_access_token, create_refresh_token
from app.core.config import settings

class AuthService:
    @staticmethod
    def register_user(db: Session, req: RegisterRequest) -> Tuple[Optional[User], Optional[str]]:
        if req.email:
            existing_email = db.query(User).filter(User.email == req.email).first()
            if existing_email:
                return None, "An account with this email already exists."

        if req.phone:
            existing_phone = db.query(User).filter(User.phone == req.phone).first()
            if existing_phone:
                return None, "An account with this phone number already exists."

        user_id = f"usr_{uuid.uuid4().hex[:12]}"
        password_hash = get_password_hash(req.password) if req.password else None

        new_user = User(
            id=user_id,
            full_name=req.full_name,
            email=req.email,
            phone=req.phone,
            password_hash=password_hash,
            city=req.city or "Indore, Madhya Pradesh",
            role=UserRole.USER,
            auth_provider="local",
            is_verified=True if not req.password else False
        )
        db.add(new_user)
        db.commit()
        db.refresh(new_user)
        return new_user, None

    @staticmethod
    def authenticate_user(db: Session, req: LoginRequest) -> Tuple[Optional[User], Optional[str]]:
        user = db.query(User).filter(
            (User.email == req.email_or_phone) | (User.phone == req.email_or_phone)
        ).first()

        if not user:
            return None, "Invalid email/phone or password."

        if not user.password_hash or not verify_password(req.password, user.password_hash):
            return None, "Invalid email/phone or password."

        return user, None

    @staticmethod
    def authenticate_or_create_phone_user(db: Session, phone: str, full_name: Optional[str] = None) -> User:
        user = db.query(User).filter(User.phone == phone).first()
        if not user:
            user = User(
                id=f"usr_{uuid.uuid4().hex[:12]}",
                full_name=full_name or f"EV Driver {phone[-4:]}",
                phone=phone,
                city="Indore, Madhya Pradesh",
                role=UserRole.USER,
                auth_provider="phone",
                is_verified=True
            )
            db.add(user)
            db.commit()
            db.refresh(user)
        else:
            user.is_verified = True
            db.commit()
            db.refresh(user)
        return user

    @staticmethod
    def authenticate_or_create_google_user(db: Session, google_id: str, email: str, name: str, picture: Optional[str] = None) -> User:
        user = db.query(User).filter((User.google_id == google_id) | (User.email == email)).first()
        if not user:
            user = User(
                id=f"usr_{uuid.uuid4().hex[:12]}",
                full_name=name,
                email=email,
                google_id=google_id,
                profile_image=picture,
                city="Indore, Madhya Pradesh",
                role=UserRole.USER,
                auth_provider="google",
                is_verified=True
            )
            db.add(user)
            db.commit()
            db.refresh(user)
        else:
            if not user.google_id:
                user.google_id = google_id
            if picture and not user.profile_image:
                user.profile_image = picture
            user.is_verified = True
            db.commit()
            db.refresh(user)
        return user

    @staticmethod
    def generate_token_payload(user: User) -> dict:
        access_token = create_access_token(user.id)
        refresh_token = create_refresh_token(user.id)
        return {
            "access_token": access_token,
            "refresh_token": refresh_token,
            "token_type": "bearer",
            "user": {
                "id": user.id,
                "full_name": user.full_name,
                "email": user.email,
                "phone": user.phone,
                "role": user.role.value if hasattr(user.role, 'value') else str(user.role),
                "city": user.city,
                "is_verified": user.is_verified,
                "profile_image": user.profile_image
            }
        }
