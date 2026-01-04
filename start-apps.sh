#!/bin/bash

# RCA Insight Engine - Quick Start Script
# This script starts both backend and frontend applications

set -e

PROJECT_ROOT="/Users/saideepak.b/work/self/ConfluenceAiAnalyser"
BACKEND_LOG="$PROJECT_ROOT/backend.log"
FRONTEND_LOG="$PROJECT_ROOT/frontend/frontend.log"

echo "=========================================="
echo "RCA Insight Engine - Starting Applications"
echo "=========================================="
echo ""

# Check prerequisites
echo "Checking prerequisites..."

# Check Java
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Please install Java 25."
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
echo "✓ Java found (version: $JAVA_VERSION)"

# Check Node.js
if ! command -v node &> /dev/null; then
    echo "❌ Node.js not found. Please install Node.js 18+."
    exit 1
fi
NODE_VERSION=$(node -v)
echo "✓ Node.js found ($NODE_VERSION)"

# Check npm
if ! command -v npm &> /dev/null; then
    echo "❌ npm not found. Please install npm."
    exit 1
fi
echo "✓ npm found"

echo ""
echo "Starting applications..."
echo ""

# Start Backend
echo "1. Starting Backend (Spring Boot)..."
cd "$PROJECT_ROOT"
export JAVA_HOME=/opt/homebrew/opt/openjdk

# Check if port 8080 is in use
if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null 2>&1 ; then
    echo "   ⚠️  Port 8080 is already in use. Stopping existing process..."
    lsof -ti:8080 | xargs kill -9 2>/dev/null || true
    sleep 2
fi

./gradlew bootRun > "$BACKEND_LOG" 2>&1 &
BACKEND_PID=$!
echo "   ✓ Backend started (PID: $BACKEND_PID)"
echo "   📝 Logs: $BACKEND_LOG"

# Wait for backend to be ready
echo "   ⏳ Waiting for backend to start..."
for i in {1..30}; do
    if curl -s http://localhost:8080/api/health > /dev/null 2>&1; then
        echo "   ✓ Backend is ready!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "   ⚠️  Backend took too long to start. Check logs: $BACKEND_LOG"
    fi
    sleep 1
done

echo ""

# Start Frontend
echo "2. Starting Frontend (React + Vite)..."
cd "$PROJECT_ROOT/frontend"

# Check if port 3000 is in use
if lsof -Pi :3000 -sTCP:LISTEN -t >/dev/null 2>&1 ; then
    echo "   ⚠️  Port 3000 is already in use. Stopping existing process..."
    lsof -ti:3000 | xargs kill -9 2>/dev/null || true
    sleep 2
fi

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
    echo "   📦 Installing dependencies..."
    npm install
fi

npm run dev > "$FRONTEND_LOG" 2>&1 &
FRONTEND_PID=$!
echo "   ✓ Frontend started (PID: $FRONTEND_PID)"
echo "   📝 Logs: $FRONTEND_LOG"

# Wait for frontend to be ready
echo "   ⏳ Waiting for frontend to start..."
sleep 5

echo ""
echo "=========================================="
echo "✅ Applications Started Successfully!"
echo "=========================================="
echo ""
echo "📍 Access URLs:"
echo "   Frontend:  http://localhost:3000"
echo "   Backend:   http://localhost:8080"
echo "   Swagger:   http://localhost:8080/swagger-ui.html"
echo ""
echo "📊 Process IDs:"
echo "   Backend:  $BACKEND_PID"
echo "   Frontend: $FRONTEND_PID"
echo ""
echo "📝 Logs:"
echo "   Backend:  tail -f $BACKEND_LOG"
echo "   Frontend: tail -f $FRONTEND_LOG"
echo ""
echo "🛑 To stop both applications:"
echo "   kill $BACKEND_PID $FRONTEND_PID"
echo "   # Or use: pkill -f 'gradlew bootRun' && pkill -f 'vite'"
echo ""
echo "Press Ctrl+C to stop this script (applications will continue running)"
echo ""

# Keep script running and show logs
tail -f "$BACKEND_LOG" "$FRONTEND_LOG" 2>/dev/null || {
    echo ""
    echo "Applications are running in the background."
    echo "Check logs manually if needed."
}

