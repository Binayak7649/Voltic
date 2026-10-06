from datetime import datetime
from sqlalchemy import Column, String, Float, DateTime, ForeignKey
from app.core.database import Base

class Transaction(Base):
    __tablename__ = "transactions"

    id = Column(String, primary_key=True, index=True)
    user_id = Column(String, ForeignKey("users.id"), nullable=False, index=True)
    payment_id = Column(String, ForeignKey("payments.id"), nullable=True, index=True)
    amount = Column(Float, nullable=False)
    type = Column(String, default="DEBIT")  # DEBIT, CREDIT, REFUND
    description = Column(String, nullable=False)
    reference_id = Column(String, nullable=True)
    status = Column(String, default="SUCCESS")
    created_at = Column(DateTime, default=datetime.utcnow)
