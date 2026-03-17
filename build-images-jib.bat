@echo off
REM Jib Docker Image Builder Script for Windows
REM Builds all microservices using Google Jib Maven plugin

setlocal enabledelayedexpansion

echo ============================================
echo Building Docker Images with Jib
echo ============================================
echo.

REM Configuration
set REGISTRY=%1
set IMAGE_TAG=%2

if "%REGISTRY%"=="" (
    set REGISTRY=localhost:5000
)
if "%IMAGE_TAG%"=="" (
    set IMAGE_TAG=latest
)

echo [i] Using registry: %REGISTRY%
echo [i] Using tag: %IMAGE_TAG%
echo.

REM Services to build
set SERVICES=eureka-server config-server api-gateway auth-service user-service order-service inventory-service payment-service

REM Build all services
for %%S in (%SERVICES%) do (
    echo [i] Building %%S...
    
    if "%REGISTRY%"=="localhost:5000" (
        REM Build to local Docker daemon
        call mvn clean compile jib:dockerBuild ^
            -pl %%S ^
            -DskipTests ^
            -Djib.to.image="%REGISTRY%/%%S:%IMAGE_TAG%"
    ) else (
        REM Build and push to remote registry
        call mvn clean compile jib:build ^
            -pl %%S ^
            -DskipTests ^
            -Djib.to.image="%REGISTRY%/%%S:%IMAGE_TAG%"
    )
    
    if %ERRORLEVEL% EQU 0 (
        echo [OK] %%S built successfully
    ) else (
        echo [ERROR] Failed to build %%S
        exit /b 1
    )
)

echo.
echo ============================================
echo All Docker images built successfully!
echo ============================================
echo.

if "%REGISTRY%"=="localhost:5000" (
    echo Images available locally in Docker:
    docker images | find "%REGISTRY%"
) else (
    echo Images pushed to: %REGISTRY%
    echo.
    echo To run images locally, pull first:
    for %%S in (%SERVICES%) do (
        echo docker run -d %REGISTRY%/%%S:%IMAGE_TAG%
    )
)

pause
