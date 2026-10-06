from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.schemas.payment import PaymentCreateRequest, PaymentVerifyRequest
from app.services.payment_service import PaymentService
from app.models.payment import Payment
from app.api.deps import get_current_user
from app.models.user import User
from app.utils.helpers import standard_response

router = APIRouter(prefix="/payments", tags=["Payments"])

@router.post("/create")
def create_payment(
    req: PaymentCreateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    payment, err = PaymentService.create_payment(
        db=db,
        user_id=current_user.id,
        session_id=req.session_id,
        amount=req.amount,
        payment_method=req.payment_method or "UPI"
    )
    if err:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=err)

    return standard_response(True, "Payment order created", {
        "payment_id": payment.id,
        "amount": payment.amount,
        "currency": payment.currency,
        "order_id": payment.gateway_order_id,
        "status": payment.status.value,
        "transaction_id": payment.transaction_id
    })

@router.post("/verify")
def verify_payment(
    req: PaymentVerifyRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    payment, err = PaymentService.verify_payment(
        db=db,
        payment_id=req.payment_id,
        simulation_status=req.simulation_status or "SUCCESS"
    )
    if err:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=err)

    return standard_response(True, "Payment verified successfully", {
        "payment_id": payment.id,
        "status": payment.status.value,
        "amount": payment.amount,
        "transaction_id": payment.transaction_id,
        "invoice_number": payment.invoice_number
    })

@router.get("/{payment_id}")
def get_payment(payment_id: str, db: Session = Depends(get_db)):
    p = db.query(Payment).filter(Payment.id == payment_id).first()
    if not p:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Payment not found")
    return standard_response(True, "Payment details retrieved", {
        "id": p.id,
        "amount": p.amount,
        "currency": p.currency,
        "status": p.status.value,
        "payment_method": p.payment_method,
        "transaction_id": p.transaction_id,
        "invoice_number": p.invoice_number,
        "created_at": p.created_at.isoformat()
    })

@router.get("/history")
def payment_history(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    payments = db.query(Payment).filter(Payment.user_id == current_user.id).order_by(Payment.created_at.desc()).all()
    return standard_response(True, "Payment history retrieved", [
        {
            "id": p.id,
            "amount": p.amount,
            "currency": p.currency,
            "status": p.status.value,
            "payment_method": p.payment_method,
            "transaction_id": p.transaction_id,
            "invoice_number": p.invoice_number,
            "created_at": p.created_at.isoformat()
        } for p in payments
    ])
