import uuid
from datetime import datetime
from typing import Optional, Tuple
from sqlalchemy.orm import Session
from app.models.charging_session import ChargingSession, SessionStatus
from app.models.charger import Charger, ChargerStatus
from app.models.station import Station
from app.models.payment import Payment, PaymentStatus
from app.models.transaction import Transaction
from app.services.charging_provider import get_charging_provider

class ChargingService:
    @staticmethod
    def start_session(
        db: Session,
        user_id: str,
        charger_id: str,
        start_percentage: float = 20.0,
        target_percentage: float = 85.0,
        payment_method: str = "UPI"
    ) -> Tuple[Optional[ChargingSession], Optional[str]]:
        charger = db.query(Charger).filter(Charger.id == charger_id).first()
        if not charger:
            return None, "Charger not found."

        if charger.status != ChargerStatus.AVAILABLE:
            return None, f"Charger is currently {charger.status.value}. Please choose an available port."

        station = db.query(Station).filter(Station.id == charger.station_id).first()
        if not station or not station.is_active:
            return None, "Charging station is currently offline."

        # Verify hardware / simulation provider
        provider = get_charging_provider()
        res = provider.start_remote_charging(charger.charger_code, charger.id, user_id)

        session_id = f"sess_{uuid.uuid4().hex[:12]}"
        txn_id = f"TXN-{uuid.uuid4().hex[:8].upper()}"

        new_session = ChargingSession(
            id=session_id,
            user_id=user_id,
            charger_id=charger.id,
            start_time=datetime.utcnow(),
            status=SessionStatus.CHARGING,
            energy_consumed_kwh=0.5,
            charging_duration=30,  # 30s initial
            start_percentage=start_percentage,
            current_percentage=start_percentage,
            target_percentage=target_percentage,
            power_kw=charger.power_kw,
            tariff_per_kwh=charger.price_per_kwh,
            estimated_cost=round(0.5 * charger.price_per_kwh, 2),
            final_cost=0.0,
            evse_id=charger.charger_code,
            txn_id=txn_id,
            payment_method=payment_method
        )

        charger.status = ChargerStatus.OCCUPIED

        db.add(new_session)
        db.commit()
        db.refresh(new_session)
        return new_session, None

    @staticmethod
    def get_live_session(db: Session, session_id: str) -> Optional[ChargingSession]:
        session = db.query(ChargingSession).filter(ChargingSession.id == session_id).first()
        if not session:
            return None

        # If session is active, simulate ticking progress
        if session.status == SessionStatus.CHARGING:
            now = datetime.utcnow()
            elapsed_seconds = int((now - session.start_time).total_seconds()) + 30
            session.charging_duration = elapsed_seconds

            # Realistic battery increase based on power rating (e.g. 60 kW delivers 1 kWh per minute)
            # 60 kW = 1 kWh/min = 0.0166 kWh/sec
            rate_kwh_per_sec = (session.power_kw / 3600.0) * 0.85
            simulated_energy = round(elapsed_seconds * rate_kwh_per_sec, 2)
            session.energy_consumed_kwh = max(0.5, simulated_energy)

            # 40 kWh EV pack: each kWh is ~2.5% battery
            battery_gain = (session.energy_consumed_kwh / 40.0) * 100.0
            new_pct = min(session.target_percentage, session.start_percentage + battery_gain)
            session.current_percentage = round(new_pct, 1)

            session.estimated_cost = round(session.energy_consumed_kwh * session.tariff_per_kwh, 2)

            if session.current_percentage >= session.target_percentage:
                session.status = SessionStatus.COMPLETED
                session.end_time = now
                session.final_cost = session.estimated_cost
                if session.charger:
                    session.charger.status = ChargerStatus.AVAILABLE

            db.commit()
            db.refresh(session)

        return session

    @staticmethod
    def stop_session(db: Session, session_id: str, user_id: str) -> Tuple[Optional[ChargingSession], Optional[str]]:
        session = db.query(ChargingSession).filter(
            ChargingSession.id == session_id,
            ChargingSession.user_id == user_id
        ).first()

        if not session:
            return None, "Charging session not found."

        if session.status not in [SessionStatus.CHARGING, SessionStatus.PENDING]:
            return None, f"Session is already {session.status.value}."

        now = datetime.utcnow()
        session.end_time = now
        session.status = SessionStatus.COMPLETED
        session.final_cost = max(10.0, round(session.energy_consumed_kwh * session.tariff_per_kwh, 2))

        # Hardware notification
        provider = get_charging_provider()
        provider.stop_remote_charging(session.evse_id, session.txn_id or session.id)

        # Free charger
        charger = db.query(Charger).filter(Charger.id == session.charger_id).first()
        if charger:
            charger.status = ChargerStatus.AVAILABLE

        # Generate digital payment & invoice
        payment_id = f"pay_{uuid.uuid4().hex[:12]}"
        invoice_number = f"INV-{uuid.uuid4().hex[:8].upper()}"

        payment = Payment(
            id=payment_id,
            user_id=user_id,
            session_id=session.id,
            amount=session.final_cost,
            currency="INR",
            payment_method=session.payment_method,
            transaction_id=session.txn_id or f"TXN-{uuid.uuid4().hex[:8].upper()}",
            status=PaymentStatus.SUCCESS,
            invoice_number=invoice_number
        )

        txn = Transaction(
            id=f"txn_{uuid.uuid4().hex[:12]}",
            user_id=user_id,
            payment_id=payment_id,
            amount=session.final_cost,
            type="DEBIT",
            description=f"EV Charging at {session.charger.station.name if session.charger and session.charger.station else 'VoltElite Hub'}",
            reference_id=invoice_number,
            status="SUCCESS"
        )

        db.add(payment)
        db.add(txn)
        db.commit()
        db.refresh(session)
        return session, None
