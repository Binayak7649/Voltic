from typing import Optional
from pydantic import BaseModel, EmailStr

class RegisterRequest(BaseModel):
    full_name: str
    email: Optional[EmailStr] = None
    phone: Optional[str] = None
    password: Optional[str] = None
    city: Optional[str] = "Indore, Madhya Pradesh"

class LoginRequest(BaseModel):
    email_or_phone: str
    password: str

class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    user: dict

class SendOtpRequest(BaseModel):
    phone: str

class SendOtpResponse(BaseModel):
    message: str
    phone: str
    dev_otp: Optional[str] = None  # Returned only when DEV_OTP_MODE=True

class VerifyOtpRequest(BaseModel):
    phone: str
    otp: str
    full_name: Optional[str] = None

class GoogleAuthRequest(BaseModel):
    id_token: str

class RefreshTokenRequest(BaseModel):
    refresh_token: str
