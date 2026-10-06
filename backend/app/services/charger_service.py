from typing import List, Optional
from sqlalchemy.orm import Session
from app.models.charger import Charger, ChargerStatus

class ChargerService:
    @staticmethod
    def get_chargers_for_station(db: Session, station_id: str) -> List[Charger]:
        return db.query(Charger).filter(
            Charger.station_id == station_id,
            Charger.is_active == True
        ).all()

    @staticmethod
    def get_charger_by_id(db: Session, charger_id: str) -> Optional[Charger]:
        return db.query(Charger).filter(Charger.id == charger_id).first()

    @staticmethod
    def get_charger_by_code_or_qr(db: Session, code_or_qr: str) -> Optional[Charger]:
        return db.query(Charger).filter(
            (Charger.charger_code == code_or_qr) |
            (Charger.qr_code == code_or_qr)
        ).first()

    @staticmethod
    def update_charger_status(db: Session, charger_id: str, new_status: ChargerStatus) -> Optional[Charger]:
        charger = db.query(Charger).filter(Charger.id == charger_id).first()
        if charger:
            charger.status = new_status
            db.commit()
            db.refresh(charger)
        return charger
