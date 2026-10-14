def test_root_endpoint(client):
    res = client.get("/")
    assert res.status_code == 200
    assert res.json()["status"] == "online"

def test_login_flow(client):
    res = client.post("/api/auth/login", json={
        "email_or_phone": "demo@voltelite.app",
        "password": "Password123!"
    })
    assert res.status_code == 200
    data = res.json()
    assert data["success"] is True
    assert "access_token" in data["data"]

def test_nearby_stations(client):
    res = client.get("/api/stations/nearby?latitude=22.7196&longitude=75.8577&radius=50")
    assert res.status_code == 200
    data = res.json()
    assert data["success"] is True
    assert len(data["data"]) > 0
    first = data["data"][0]
    assert "operator" in first
    assert "distance_km" in first
    assert "data_source" in first

def test_discovery_status_endpoint(client):
    res = client.get("/api/stations/discovery/status")
    assert res.status_code == 200
    data = res.json()
    assert data["success"] is True
    assert "configured_sources" in data["data"]
    assert "setup_message" in data["data"]

def test_petrol_pump_exclusion():
    from app.services.discovery_service import EVDiscoveryService
    # Ordinary petrol pump without EV should be excluded
    assert EVDiscoveryService.is_strictly_ev_station("Indian Oil Petrol Bunk", "AB Road, Indore", ["gas_station"]) is False
    assert EVDiscoveryService.is_strictly_ev_station("HP Petrol Pump & Car Service", "Palasia", ["gas_station", "car_repair"]) is False
    # Verified EV station at fuel pump should be kept
    assert EVDiscoveryService.is_strictly_ev_station("Jio-bp pulse EV Charging Station", "Bypass Road", ["gas_station", "electric_vehicle_charging_station"]) is True
    assert EVDiscoveryService.is_strictly_ev_station("Tata Power EZ Charge Station", "Vijay Nagar", []) is True


def test_qr_verification_valid(client):
    res = client.post("/api/qr/verify", json={
        "qr_payload": "volt-elite://charge?station=stn_idr_01&evse=EVSE-08&connector=ccs2",
        "vehicle_make_model": "Tata Nexon EV",
        "vehicle_connector": "CCS 2"
    })
    assert res.status_code == 200
    data = res.json()
    assert data["success"] is True
    assert data["data"]["evse_id"] == "EVSE-08"
    assert data["data"]["compatibility"]["is_compatible"] is True

def test_qr_verification_incompatible(client):
    res = client.post("/api/qr/verify", json={
        "qr_payload": "INCOMPATIBLE-AC-TYPE2",
        "vehicle_make_model": "Tata Nexon EV",
        "vehicle_connector": "CCS 2"
    })
    assert res.status_code == 200
    data = res.json()
    assert data["data"]["compatibility"]["is_compatible"] is False

def test_start_and_stop_charging_workflow(client):
    # 1. Login to get token
    login_res = client.post("/api/auth/login", json={
        "email_or_phone": "demo@voltelite.app",
        "password": "Password123!"
    })
    token = login_res.json()["data"]["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    # 2. Start charging
    start_res = client.post("/api/charging/start", json={
        "charger_id": "chg_idr_01_a",
        "start_percentage": 25.0,
        "target_percentage": 85.0,
        "payment_method": "UPI"
    }, headers=headers)
    assert start_res.status_code == 200
    sess_id = start_res.json()["data"]["session_id"]

    # 3. Poll active session
    active_res = client.get("/api/charging/active", headers=headers)
    assert active_res.status_code == 200
    assert active_res.json()["data"]["session_id"] == sess_id

    # 4. Stop session
    stop_res = client.post(f"/api/charging/{sess_id}/stop", headers=headers)
    assert stop_res.status_code == 200
    assert stop_res.json()["data"]["status"] == "COMPLETED"
    assert stop_res.json()["data"]["final_cost"] > 0

def test_analytics(client):
    login_res = client.post("/api/auth/login", json={
        "email_or_phone": "demo@voltelite.app",
        "password": "Password123!"
    })
    token = login_res.json()["data"]["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    res = client.get("/api/analytics/user", headers=headers)
    assert res.status_code == 200
    data = res.json()["data"]
    assert "total_charging_sessions" in data
    assert "co2_savings_kg" in data

def test_firebase_auth_invalid_token(client):
    # Reject invalid Firebase ID token with 401
    res = client.post("/api/auth/firebase", json={"id_token": "invalid.firebase.token"})
    assert res.status_code == 401

def test_firebase_user_creation_and_linking(db_session):
    from app.services.firebase_auth_service import FirebaseAuthService
    from app.models.user import User

    # Test creating user with stable Firebase UID
    mock_claims = {
        "uid": "fb_test_uid_998877",
        "phone_number": "+919876500001",
        "name": "Firebase Test Driver",
        "firebase": {"sign_in_provider": "phone"}
    }
    user, err = FirebaseAuthService.authenticate_or_create_firebase_user(db_session, mock_claims)
    assert err is None
    assert user is not None
    assert user.firebase_uid == "fb_test_uid_998877"
    assert user.phone == "+919876500001"
    assert user.is_verified is True

    # Test retrieving existing user with same UID
    user2, err2 = FirebaseAuthService.authenticate_or_create_firebase_user(db_session, mock_claims)
    assert err2 is None
    assert user2.id == user.id
