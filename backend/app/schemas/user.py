from typing import Optional
from datetime import datetime
from pydantic import BaseModel, EmailStr

class UserBase(BaseModel):
    full_name: str
    email: Optional[EmailStr] = None
    phone: Optional[str] = None
    city: Optional[str] = "Indore, Madhya Pradesh"
    profile_image: Optional[str] = None

class UserUpdate(BaseModel):
    full_name: Optional[str] = None
    email: Optional[EmailStr] = None
    phone: Optional[str] = None
    city: Optional[str] = None
    profile_image: Optional[str] = None

class PasswordChangeRequest(BaseModel):
    old_password: str
    new_password: str

class UserResponse(UserBase):
    id: str
    role: str
    auth_provider: str
    is_verified: bool
    created_at: datetime

    class Config:
        from_attributes = True
