# How to Start the RCA Insight Engine Application

This guide provides exact steps to start both the backend and frontend applications.

## Prerequisites

Before starting, ensure you have:

1. **Java 25** installed and configured
   ```bash
   java -version  # Should show version 25
   ```

2. **Node.js 18+** and npm installed
   ```bash
   node -v  # Should show v18 or higher
   npm -v
   ```

3. **PostgreSQL** running (optional for basic testing, required for full functionality)
   ```bash
   pg_isready -h localhost -p 5432
   ```

4. **Project dependencies** installed
   - Backend: Gradle will download dependencies automatically
   - Frontend: Run `npm install` in the `frontend/` directory

## Starting the Applications

### Option 1: Start Backend First, Then Frontend (Recommended)

#### Step 1: Start the Backend (Spring Boot)

1. Open a terminal and navigate to the project root:
   ```bash
   cd /Users/saideepak.b/work/self/ConfluenceAiAnalyser
   ```

2. Set Java home (if not already set):
   ```bash
   export JAVA_HOME=/opt/homebrew/opt/openjdk
   ```

3. Start the backend application:
   ```bash
   ./gradlew bootRun
   ```

4. Wait for the application to start. You should see:
   ```
   Started ConfluenceAiAnalyzer in X.XXX seconds
   Tomcat started on port 8080
   ```

5. Verify the backend is running:
   ```bash
   curl http://localhost:8080/api/health
   ```
   Expected response:
   ```json
   {"service":"Confluence AI Analyzer","status":"UP","timestamp":"..."}
   ```

#### Step 2: Start the Frontend (React + Vite)

1. Open a **new terminal window** (keep the backend running)

2. Navigate to the frontend directory:
   ```bash
   cd /Users/saideepak.b/work/self/ConfluenceAiAnalyser/frontend
   ```

3. Install dependencies (first time only):
   ```bash
   npm install
   ```

4. Start the frontend development server:
   ```bash
   npm run dev
   ```

5. Wait for Vite to start. You should see:
   ```
   VITE v5.x.x  ready in XXX ms

   ➜  Local:   http://localhost:3000/
   ➜  Network: use --host to expose
   ```

6. The frontend will automatically open in your browser, or navigate to:
   ```
   http://localhost:3000
   ```

### Option 2: Start Both in Background (Advanced)

#### Start Backend in Background

```bash
cd /Users/saideepak.b/work/self/ConfluenceAiAnalyser
export JAVA_HOME=/opt/homebrew/opt/openjdk
./gradlew bootRun > backend.log 2>&1 &
```

#### Start Frontend in Background

```bash
cd /Users/saideepak.b/work/self/ConfluenceAiAnalyser/frontend
npm run dev > frontend.log 2>&1 &
```

#### Check Logs

```bash
# Backend logs
tail -f /Users/saideepak.b/work/self/ConfluenceAiAnalyser/backend.log

# Frontend logs
tail -f /Users/saideepak.b/work/self/ConfluenceAiAnalyser/frontend/frontend.log
```

## Verification

### Check Backend Status

```bash
# Health check
curl http://localhost:8080/api/health

# Stats endpoint
curl http://localhost:8080/api/v1/stats

# Swagger UI (open in browser)
open http://localhost:8080/swagger-ui.html
```

### Check Frontend Status

```bash
# Check if frontend is responding
curl http://localhost:3000

# Open in browser
open http://localhost:3000
```

### Verify Frontend-Backend Connection

1. Open the frontend in your browser: `http://localhost:3000`
2. Navigate to the **Statistics** page
3. The page should load without errors (may show empty stats if no data)
4. Check browser console (F12) for any connection errors

## Access URLs

Once both applications are running:

- **Frontend UI**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Documentation**: http://localhost:8080/api-docs
- **Health Check**: http://localhost:8080/api/health

## Stopping the Applications

### Stop Backend

If running in foreground: Press `Ctrl+C` in the terminal

If running in background:
```bash
pkill -f "gradlew bootRun"
# Or kill by port
lsof -ti:8080 | xargs kill -9
```

### Stop Frontend

If running in foreground: Press `Ctrl+C` in the terminal

If running in background:
```bash
pkill -f "vite"
# Or kill by port
lsof -ti:3000 | xargs kill -9
```

### Stop Both at Once

```bash
pkill -f "gradlew bootRun"
pkill -f "vite"
lsof -ti:8080 | xargs kill -9 2>/dev/null
lsof -ti:3000 | xargs kill -9 2>/dev/null
```

## Troubleshooting

### Backend Won't Start

**Issue**: Port 8080 already in use
```bash
# Find and kill process using port 8080
lsof -ti:8080 | xargs kill -9
```

**Issue**: Java not found
```bash
# Set JAVA_HOME explicitly
export JAVA_HOME=/opt/homebrew/opt/openjdk
# Or find Java installation
/usr/libexec/java_home -V
```

**Issue**: Build fails
```bash
# Clean and rebuild
./gradlew clean build
./gradlew bootRun
```

**Issue**: Database connection error
- This is expected if PostgreSQL is not running
- The app will start but database operations will fail
- For testing UI without database, this is acceptable

### Frontend Won't Start

**Issue**: Port 3000 already in use
```bash
# Find and kill process using port 3000
lsof -ti:3000 | xargs kill -9
# Or change port in vite.config.ts
```

**Issue**: Dependencies not installed
```bash
cd frontend
npm install
```

**Issue**: Module not found errors
```bash
# Clear node_modules and reinstall
cd frontend
rm -rf node_modules package-lock.json
npm install
```

### Frontend Can't Connect to Backend

**Issue**: CORS errors or connection refused
- Verify backend is running: `curl http://localhost:8080/api/health`
- Check Vite proxy configuration in `frontend/vite.config.ts`
- Ensure backend is on port 8080 (default)

**Issue**: API calls return 404
- Verify backend endpoints are correct
- Check Swagger UI: http://localhost:8080/swagger-ui.html
- Check browser console for exact error messages

## Quick Start Script

Create a script to start both applications:

```bash
#!/bin/bash
# save as start-apps.sh

# Start backend
cd /Users/saideepak.b/work/self/ConfluenceAiAnalyser
export JAVA_HOME=/opt/homebrew/opt/openjdk
./gradlew bootRun > backend.log 2>&1 &
BACKEND_PID=$!

# Wait for backend to start
echo "Waiting for backend to start..."
sleep 15

# Start frontend
cd frontend
npm run dev > frontend.log 2>&1 &
FRONTEND_PID=$!

echo "Backend PID: $BACKEND_PID"
echo "Frontend PID: $FRONTEND_PID"
echo "Backend: http://localhost:8080"
echo "Frontend: http://localhost:3000"
echo ""
echo "To stop: kill $BACKEND_PID $FRONTEND_PID"
```

Make it executable:
```bash
chmod +x start-apps.sh
./start-apps.sh
```

## Development Tips

1. **Hot Reload**: Both applications support hot reload
   - Backend: Restart required for Java changes
   - Frontend: Automatic reload on file changes

2. **Logs**: Monitor logs for errors
   - Backend: Check terminal output or `backend.log`
   - Frontend: Check terminal output or browser console

3. **Database**: For full functionality, ensure PostgreSQL is running and configured in `application.properties`

4. **Environment Variables**: Set required environment variables:
   ```bash
   export OPENAI_API_KEY=your_key_here
   export CONFLUENCE_BASE_URL=your_confluence_url
   export CONFLUENCE_PAT=your_pat_here
   export DB_USER=your_db_user
   export DB_PASSWORD=your_db_password
   ```

## Next Steps

After starting both applications:

1. Open http://localhost:3000 in your browser
2. Explore the UI:
   - **Search**: Test semantic search (requires data in database)
   - **Ingestion**: Start sync operations (requires Confluence credentials)
   - **Statistics**: View system stats
3. Check API documentation at http://localhost:8080/swagger-ui.html
4. Test API endpoints using Swagger UI or curl commands

## Support

If you encounter issues:

1. Check the logs for error messages
2. Verify all prerequisites are installed
3. Ensure ports 8080 and 3000 are available
4. Check that environment variables are set correctly
5. Review the troubleshooting section above

