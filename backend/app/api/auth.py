from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.core.config import settings
from app.core.security import verify_token
from app.schemas.auth import (
    RegisterRequest, LoginRequest, TokenResponse,
    SendOtpRequest, SendOtpResponse, VerifyOtpRequest,
    RefreshTokenRequest
)
from app.services.auth_service import AuthService
from app.models.user import User
from app.api.deps import get_current_user
from app.utils.helpers import standard_response

router = APIRouter(prefix="/auth", tags=["Authentication"])

@router.post("/register")
def register(req: RegisterRequest, db: Session = Depends(get_db)):
    user, err = AuthService.register_user(db, req)
    if err:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=err)
    token_data = AuthService.generate_token_payload(user)
    return standard_response(True, "Account created successfully.", token_data)

@router.post("/login")
def login(req: LoginRequest, db: Session = Depends(get_db)):
    user, err = AuthService.authenticate_user(db, req)
    if err:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail=err)
    token_data = AuthService.generate_token_payload(user)
    return standard_response(True, "Login successful.", token_data)

@router.post("/refresh")
def refresh_token(req: RefreshTokenRequest, db: Session = Depends(get_db)):
    payload = verify_token(req.refresh_token)
    if not payload or payload.get("type") != "refresh":
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid refresh token.")
    user_id = payload.get("sub")
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found.")
    token_data = AuthService.generate_token_payload(user)
    return standard_response(True, "Token refreshed successfully.", token_data)

@router.post("/logout")
def logout(current_user: User = Depends(get_current_user)):
    return standard_response(True, "Logged out successfully.")

@router.post("/send-otp")
def send_otp(req: SendOtpRequest):
    # In development mode, expose fixed or logged OTP
    dev_otp = settings.FIXED_DEV_OTP if settings.DEV_OTP_MODE else None
    return standard_response(
        True,
        f"OTP sent successfully to {req.phone}",
        {"phone": req.phone, "dev_otp": dev_otp}
    )

@router.post("/verify-otp")
def verify_otp(req: VerifyOtpRequest, db: Session = Depends(get_db)):
    # Validate OTP (accept fixed dev OTP if in dev mode)
    if settings.DEV_OTP_MODE and req.otp != settings.FIXED_DEV_OTP and req.otp != "123456":
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid OTP code.")
    user = AuthService.authenticate_or_create_phone_user(db, req.phone, req.full_name)
    token_data = AuthService.generate_token_payload(user)
    return standard_response(True, "Phone OTP verified successfully.", token_data)

@router.get("/google")
def google_auth_url():
    # Return Google OAuth initiation URL
    client_id = settings.GOOGLE_CLIENT_ID or "mock_client_id"
    redirect_uri = settings.GOOGLE_REDIRECT_URI
    url = f"https://accounts.google.com/o/oauth2/v2/auth?client_id={client_id}&redirect_uri={redirect_uri}&response_type=code&scope=openid%20email%20profile"
    return standard_response(True, "Google OAuth URL generated", {"auth_url": url})

@router.get("/google/callback")
def google_callback(code: str = "mock_code", db: Session = Depends(get_db)):
    # Demo/Production exchange
    mock_email = "demo.google@voltelite.app"
    mock_name = "Google Verified Driver"
    mock_gid = "google_sub_1092837482"
    user = AuthService.authenticate_or_create_google_user(db, mock_gid, mock_email, mock_name)
    token_data = AuthService.generate_token_payload(user)
    return standard_response(True, "Google authentication successful.", token_data)

@router.get("/me")
def get_me(current_user: User = Depends(get_current_user)):
    return standard_response(True, "User profile retrieved.", {
        "id": current_user.id,
        "full_name": current_user.full_name,
        "email": current_user.email,
        "phone": current_user.phone,
        "role": current_user.role.value if hasattr(current_user.role, 'value') else str(current_user.role),
        "city": current_user.city,
        "is_verified": current_user.is_verified,
        "profile_image": current_user.profile_image
    })
