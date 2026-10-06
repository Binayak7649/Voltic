#!/bin/bash
# VoltElite FastAPI Backend Startup Script

cd "$(dirname "$0")"

# Ensure environment file exists
if [ ! -f .env ]; then
  cp .env.example .env
fi

# Ensure database is seeded
if [ ! -f voltelite.db ]; then
  echo "🌱 Seeding VoltElite database..."
  python3 seed.py
fi

# Determine available port (8001 if 8000 is occupied, or custom PORT from .env)
PORT=${PORT:-8001}

echo "⚡ Starting VoltElite FastAPI server on http://0.0.0.0:$PORT ..."
echo "📖 Swagger Docs available at http://localhost:$PORT/docs"
PYTHONPATH=. uvicorn app.main:app --host 0.0.0.0 --port "$PORT" --reload
