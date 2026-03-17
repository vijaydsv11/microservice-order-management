@echo off
REM Microservices Platform Build & Run Script for Windows

echo ============================================
echo Microservices Platform - Build ^& Run
echo ============================================
echo.

REM Check prerequisites
echo Checking prerequisites...

where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Java is not installed
    exit /b 1
)
echo [OK] Java is installed

where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Maven is not installed
    exit /b 1
)
echo [OK] Maven is installed

where docker >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Docker is not installed
    exit /b 1
)
echo [OK] Docker is installed

where docker-compose >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Docker Compose is not installed
    exit /b 1
)
echo [OK] Docker Compose is installed

echo.
echo Starting Docker infrastructure...
docker-compose up -d

echo.
echo [*] Waiting 30 seconds for services to be ready...
timeout /t 30

echo.
echo Building all services with Maven...
call mvn clean install -DskipTests -q

echo.
echo ============================================
echo Setup Complete!
echo ============================================
echo.
echo Services to start (in separate terminals):
echo.
echo 1. Eureka Server
echo    cd eureka-server ^&^& mvn spring-boot:run
echo.
echo 2. Auth Service
echo    cd auth-service ^&^& mvn spring-boot:run
echo.
echo 3. User Service
echo    cd user-service ^&^& mvn spring-boot:run
echo.
echo 4. Order Service
echo    cd order-service ^&^& mvn spring-boot:run
echo.
echo 5. API Gateway (start last)
echo    cd api-gateway ^&^& mvn spring-boot:run
echo.
echo ============================================
echo Service URLs:
echo ============================================
echo API Gateway: http://localhost:8080
echo Auth Service: http://localhost:8081
echo User Service: http://localhost:8082
echo Order Service: http://localhost:8083
echo Eureka Dashboard: http://localhost:8761
echo.
echo ============================================
echo Help:
echo ============================================
echo View logs: docker-compose logs -f
echo Stop services: docker-compose down
echo Reset databases: docker-compose down -v
echo.
pause
