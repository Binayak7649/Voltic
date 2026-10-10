import uuid
import logging
from datetime import datetime
from typing import Optional, Tuple
from sqlalchemy.orm import Session
from app.models.charging_session import ChargingSession, SessionStatus
from app.models.charger import Charger, ChargerStatus
from app.models.station import Station
from app.models.payment import Payment, PaymentStatus
from app.models.transaction import Transaction
from app.services.charging_provider import get_charging_provider

logger = logging.getLogger(__name__)

class ChargingService:
    @staticmethod
    def start_session(
        db: Session,
        user_id: str,
        charger_id: str,
        start_percentage: float = 20.0,
        target_percentage: float = 85.0,
        payment_method: str = "UPI",
        is_demo: bool = False,
        idempotency_key: Optional[str] = None
    ) -> Tuple[Optional[ChargingSession], Optional[str]]:
        # 1. Idempotency Check: Prevent duplicate transaction creation
        if idempotency_key:
            existing = db.query(ChargingSession).filter(
                ChargingSession.user_id == user_id,
                ChargingSession.idempotency_key == idempotency_key
            ).first()
            if existing:
                logger.info(f"Idempotency hit for key {idempotency_key}: Returning session {existing.id}")
                return existing, None

        # 2. Check for existing active charging session for user
        active_sess = db.query(ChargingSession).filter(
            ChargingSession.user_id == user_id,
            ChargingSession.status == SessionStatus.CHARGING
        ).first()
        if active_sess:
            return None, f"You already have an active charging session ({active_sess.evse_id} - Session #{active_sess.id[:8]}). Please stop it before starting a new session."

        # 3. Locate charger in database
        charger = db.query(Charger).filter(
            (Charger.id == charger_id) |
            (Charger.charger_code == charger_id) |
            (Charger.qr_code == charger_id)
        ).first()
        if not charger:
            return None, f"Charger hardware '{charger_id}' not found in VoltElite network."

        # 4. Connector availability verification
        if not is_demo and charger.status != ChargerStatus.AVAILABLE:
            return None, f"Connector {charger.charger_code} is currently {charger.status.value}. Please select an available connector."

        station = db.query(Station).filter(Station.id == charger.station_id).first()
        if not station or not station.is_active:
            return None, "Charging station is currently offline."

        # 5. Charging Provider Adapter Dispatch
        provider = get_charging_provider(operator=station.operator, is_demo=is_demo)
        connected, connect_msg = provider.is_connected()
        if not connected and not is_demo:
            return None, f"Provider '{station.operator}' is NOT CONNECTED: {connect_msg}"

        # 6. Request Authorization
        auth_res = provider.request_authorization(
            user_id=user_id,
            id_tag=f"VOLT-{user_id[:6].upper()}",
            charger_code=charger.charger_code,
            is_demo=is_demo
        )
        if not auth_res.get("authorized", True):
            return None, f"Provider authorization rejected: {auth_res.get('message', 'RFID / User tag not accepted.')}"

        session_id = f"sess_{uuid.uuid4().hex[:12]}"
        txn_id = f"TXN-{uuid.uuid4().hex[:8].upper()}"

        # 7. Dispatch start command to provider / CMS
        start_res = provider.start_charging_transaction(
            session_id=session_id,
            charger_code=charger.charger_code,
            evse_id=charger.charger_code,
            id_tag=f"VOLT-{user_id[:6].upper()}",
            meter_start=0.0,
            is_demo=is_demo
        )
        if start_res.get("status") != "Accepted":
            return None, f"Hardware rejected remote start command: {start_res.get('message', 'Charging command timed out or rejected.')}"

        real_txn = start_res.get("transaction_id", txn_id)

        # 8. Create Charging Session
        new_session = ChargingSession(
            id=session_id,
            user_id=user_id,
            charger_id=charger.id,
            start_time=datetime.utcnow(),
            status=SessionStatus.CHARGING,
            energy_consumed_kwh=0.5 if is_demo else 0.0,
            charging_duration=10 if is_demo else 0,
            start_percentage=start_percentage,
            current_percentage=start_percentage,
            target_percentage=target_percentage,
            power_kw=charger.power_kw,
            tariff_per_kwh=charger.price_per_kwh,
            session_fee=0.0,
            estimated_cost=round(0.5 * charger.price_per_kwh, 2) if is_demo else 0.0,
            final_cost=0.0,
            evse_id=charger.charger_code,
            txn_id=real_txn,
            payment_method=payment_method,
            is_demo=is_demo,
            idempotency_key=idempotency_key,
            provider_name=provider.provider_name
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

        # Live progression
        if session.status == SessionStatus.CHARGING:
            now = datetime.utcnow()
            elapsed_seconds = int((now - session.start_time).total_seconds())

            if session.is_demo:
                # Documented simulation progression for Demo Mode
                elapsed_seconds = elapsed_seconds + 30
                session.charging_duration = elapsed_seconds

                rate_kwh_per_sec = (session.power_kw / 3600.0) * 0.90
                simulated_energy = round(elapsed_seconds * rate_kwh_per_sec, 2)
                session.energy_consumed_kwh = max(0.5, simulated_energy)

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
            else:
                # Real charging mode: strictly genuine meter readings
                session.charging_duration = elapsed_seconds
                provider = get_charging_provider(
                    operator=session.charger.station.operator if session.charger and session.charger.station else "ocpp",
                    is_demo=False
                )
                meter = provider.get_meter_values(session.txn_id or session.id, session.power_kw)
                if "energy_kwh" in meter:
                    session.energy_consumed_kwh = float(meter["energy_kwh"])
                    session.estimated_cost = round(session.energy_consumed_kwh * session.tariff_per_kwh, 2)

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

        # Hardware stop notification through provider
        provider = get_charging_provider(
            operator=session.charger.station.operator if session.charger and session.charger.station else "simulation",
            is_demo=session.is_demo
        )
        provider.stop_remote_charging(
            transaction_id=session.txn_id or session.id,
            charger_code=session.evse_id,
            meter_stop=session.energy_consumed_kwh
        )

        # Free charger
        charger = db.query(Charger).filter(Charger.id == session.charger_id).first()
        if charger:
            charger.status = ChargerStatus.AVAILABLE

        # Generate digital payment & invoice
        payment_id = f"pay_{uuid.uuid4().hex[:12]}"
        invoice_number = f"INV-{uuid.uuid4().hex[:8].upper()}"

        payment_desc = f"[DEMO] EV Charging at {session.charger.station.name if session.charger and session.charger.station else 'VoltElite Hub'}" if session.is_demo else f"EV Charging at {session.charger.station.name if session.charger and session.charger.station else 'VoltElite Hub'}"

        payment = Payment(
            id=payment_id,
            user_id=user_id,
            session_id=session.id,
            amount=session.final_cost,
            currency="INR",
            payment_method=f"{session.payment_method} (Demo)" if session.is_demo else session.payment_method,
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
            description=payment_desc,
            reference_id=invoice_number,
            status="SUCCESS"
        )

        db.add(payment)
        db.add(txn)
        db.commit()
        db.refresh(session)
        return session, None

