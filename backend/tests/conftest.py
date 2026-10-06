import pytest
import sys
import os
from fastapi.testclient import TestClient

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.main import app
from app.core.database import get_db, SessionLocal
from seed import seed

@pytest.fixture(scope="session", autouse=True)
def setup_test_db():
    seed()
    yield

@pytest.fixture
def client():
    with TestClient(app) as c:
        yield c
