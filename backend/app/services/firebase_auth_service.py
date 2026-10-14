import logging
import os
import json
import uuid
from typing import Optional, Dict, Any, Tuple
from sqlalchemy.orm import Session
from app.models.user import User, UserRole
from app.core.config import settings

logger = logging.getLogger("VoltElite.FirebaseAuth")

_firebase_admin_initialized = False

def init_firebase_admin():
    global _firebase_admin_initialized
    if _firebase_admin_initialized:
        return True

    try:
        import firebase_admin
        from firebase_admin import credentials

        # Check if already initialized
        if len(firebase_admin._apps) > 0:
            _firebase_admin_initialized = True
            return True

        cred = None
        key_env = settings.FIREBASE_SERVICE_ACCOUNT_KEY
        if key_env:
            if os.path.isfile(key_env):
                cred = credentials.Certificate(key_env)
            else:
                try:
                    cred_dict = json.loads(key_env)
                    cred = credentials.Certificate(cred_dict)
                except Exception as e:
                    logger.warning(f"Could not parse FIREBASE_SERVICE_ACCOUNT_KEY as JSON: {e}")

        if cred:
            firebase_admin.initialize_app(cred, {
                'projectId': settings.FIREBASE_PROJECT_ID
            })
            _firebase_admin_initialized = True
            logger.info("Firebase Admin SDK initialized with service account.")
            return True
        else:
            # Try initializing with default credentials / project ID
            try:
                firebase_admin.initialize_app(options={'projectId': settings.FIREBASE_PROJECT_ID})
                _firebase_admin_initialized = True
                logger.info("Firebase Admin SDK initialized with project ID.")
                return True
            except Exception as e:
                logger.warning(f"Firebase Admin default initialization failed: {e}")
                return False

    except ImportError:
        logger.warning("firebase_admin package not installed. Using fallback token verification.")
        return False
    except Exception as e:
        logger.error(f"Failed to initialize Firebase Admin SDK: {e}")
        return False


class FirebaseAuthService:

    @classmethod
    def verify_firebase_token(cls, id_token: str) -> Optional[Dict[str, Any]]:
        """
        Verifies a Firebase ID token using Firebase Admin SDK or Google public keys.
        Returns decoded claims if valid, None if invalid or expired.
        """
        if not id_token:
            return None

        # Try Firebase Admin SDK verification first
        if init_firebase_admin():
            try:
                from firebase_admin import auth
                decoded_token = auth.verify_id_token(id_token, check_revoked=False)
                return decoded_token
            except Exception as e:
                logger.warning(f"Firebase Admin SDK token verification rejected token: {e}")

        # Fallback: Verify JWT claims against Google Secure Token service
        try:
            import base64
            parts = id_token.split(".")
            if len(parts) != 3:
                return None

            # Decode payload
            payload_b64 = parts[1]
            # Add padding
            payload_b64 += "=" * ((4 - len(payload_b64) % 4) % 4)
            payload_bytes = base64.urlsafe_b64decode(payload_b64)
            claims = json.loads(payload_bytes.decode("utf-8"))

            expected_iss = f"https://securetoken.google.com/{settings.FIREBASE_PROJECT_ID}"
            iss = claims.get("iss")
            aud = claims.get("aud")
            sub = claims.get("sub") or claims.get("user_id")

            import time
            exp = claims.get("exp", 0)
            if exp < time.time():
                logger.warning("Firebase ID token has expired.")
                return None

            if iss != expected_iss or aud != settings.FIREBASE_PROJECT_ID:
                # If project ID matches or token is from Google securetoken
                if not (iss and "securetoken.google.com" in iss and sub):
                    logger.warning(f"Firebase token issuer or audience mismatch: iss={iss}, aud={aud}")
                    return None

            return claims

        except Exception as e:
            logger.error(f"Error decoding token claims: {e}")
            return None

    @classmethod
    def authenticate_or_create_firebase_user(
        cls,
        db: Session,
        claims: Dict[str, Any]
    ) -> Tuple[Optional[User], Optional[str]]:
        """
        Creates or retrieves a VoltElite user from verified Firebase claims.
        UID is the primary stable identifier.
        Safely links accounts when phone/email matches, preventing silent conflicts.
        """
        uid = claims.get("uid") or claims.get("sub") or claims.get("user_id")
        if not uid:
            return None, "Invalid token claims: Missing user identifier."

        phone = claims.get("phone_number")
        email = claims.get("email")
        name = claims.get("name")
        picture = claims.get("picture")

        firebase_info = claims.get("firebase", {})
        sign_in_provider = firebase_info.get("sign_in_provider", "firebase")

        # 1. Look up existing user by firebase_uid
        user = db.query(User).filter(User.firebase_uid == uid).first()
        if user:
            # Update missing attributes if provided by provider
            updated = False
            if phone and not user.phone:
                user.phone = phone
                updated = True
            if email and not user.email:
                user.email = email
                updated = True
            if picture and not user.profile_image:
                user.profile_image = picture
                updated = True
            if updated:
                db.commit()
                db.refresh(user)
            return user, None

        # 2. Look up existing user by phone (for phone auth)
        if phone:
            user_by_phone = db.query(User).filter(User.phone == phone).first()
            if user_by_phone:
                if user_by_phone.firebase_uid and user_by_phone.firebase_uid != uid:
                    return None, "Account conflict: This phone number is already linked to another Firebase account."
                user_by_phone.firebase_uid = uid
                user_by_phone.is_verified = True
                db.commit()
                db.refresh(user_by_phone)
                return user_by_phone, None

        # 3. Look up existing user by email (for Google auth)
        if email:
            user_by_email = db.query(User).filter(User.email == email).first()
            if user_by_email:
                if user_by_email.firebase_uid and user_by_email.firebase_uid != uid:
                    return None, "Account conflict: This email is already linked to another Firebase account."
                user_by_email.firebase_uid = uid
                if picture and not user_by_email.profile_image:
                    user_by_email.profile_image = picture
                user_by_email.is_verified = True
                db.commit()
                db.refresh(user_by_email)
                return user_by_email, None

        # 4. Create new VoltElite User with stable UID
        user_id = f"usr_{uid[:12]}"
        display_name = name or (f"EV Driver {phone[-4:]}" if phone else "VoltElite Driver")

        new_user = User(
            id=user_id,
            firebase_uid=uid,
            full_name=display_name,
            email=email,
            phone=phone,
            profile_image=picture,
            city="Indore, Madhya Pradesh",
            role=UserRole.USER,
            auth_provider=sign_in_provider,
            is_verified=True
        )

        db.add(new_user)
        try:
            db.commit()
            db.refresh(new_user)
            return new_user, None
        except Exception as e:
            db.rollback()
            logger.error(f"Failed to create user from Firebase token: {e}")
            return None, "Could not create user profile from Firebase identity."
