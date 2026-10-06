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
