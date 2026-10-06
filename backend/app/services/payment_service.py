import uuid
from typing import Optional, Tuple
from sqlalchemy.orm import Session
from app.models.payment import Payment, PaymentStatus
from app.models.charging_session import ChargingSession
from app.core.config import settings

class PaymentService:
    @staticmethod
    def create_payment(
        db: Session,
        user_id: str,
        session_id: str,
        amount: float,
        payment_method: str = "UPI"
    ) -> Tuple[Optional[Payment], Optional[str]]:
        session = db.query(ChargingSession).filter(ChargingSession.id == session_id).first()
        if not session:
            return None, "Charging session not found."

        payment_id = f"pay_{uuid.uuid4().hex[:12]}"
        txn_id = f"TXN-{uuid.uuid4().hex[:8].upper()}"
        invoice_no = f"INV-{uuid.uuid4().hex[:8].upper()}"

        payment = Payment(
            id=payment_id,
            user_id=user_id,
            session_id=session_id,
            amount=amount,
            currency="INR",
            payment_method=payment_method,
            transaction_id=txn_id,
            status=PaymentStatus.PENDING,
            invoice_number=invoice_no,
            gateway_order_id=f"order_{uuid.uuid4().hex[:14]}"
        )
        db.add(payment)
        db.commit()
        db.refresh(payment)
        return payment, None

    @staticmethod
    def verify_payment(
        db: Session,
        payment_id: str,
        simulation_status: str = "SUCCESS"
    ) -> Tuple[Optional[Payment], Optional[str]]:
        payment = db.query(Payment).filter(Payment.id == payment_id).first()
        if not payment:
            return None, "Payment record not found."

        if simulation_status == "SUCCESS":
            payment.status = PaymentStatus.SUCCESS
        elif simulation_status == "CANCELLED":
            payment.status = PaymentStatus.CANCELLED
        else:
            payment.status = PaymentStatus.FAILED

        db.commit()
        db.refresh(payment)
        return payment, None
