from typing import Optional
from datetime import datetime
from pydantic import BaseModel

class PaymentCreateRequest(BaseModel):
    session_id: str
    amount: float
    currency: Optional[str] = "INR"
    payment_method: Optional[str] = "UPI"  # UPI, WALLET, CARD, PRE_AUTH

class PaymentVerifyRequest(BaseModel):
    payment_id: str
    gateway_order_id: Optional[str] = None
    gateway_payment_id: Optional[str] = None
    gateway_signature: Optional[str] = None
    simulation_status: Optional[str] = "SUCCESS"  # SUCCESS, FAILED, CANCELLED for demo

class PaymentResponse(BaseModel):
    id: str
    session_id: Optional[str]
    amount: float
    currency: str
    payment_method: str
    transaction_id: str
    status: str
    invoice_number: Optional[str]
    created_at: datetime

    class Config:
        from_attributes = True
