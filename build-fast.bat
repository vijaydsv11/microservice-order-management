@echo off
setlocal

REM Fast Maven build helper for Windows
REM Usage:
REM   build-fast.bat
REM   build-fast.bat clean
REM   build-fast.bat module <module-name>
REM   build-fast.bat module <module-name> clean

set "THREADS=1C"
set "GOAL=compile"
set "SKIP_TESTS=-DskipTests"

if /I "%~1"=="clean" (
    set "GOAL=clean compile"
    goto :run_all
)

if /I "%~1"=="module" (
    if "%~2"=="" (
        echo ERROR: Missing module name.
        echo Example: build-fast.bat module inventory-service
        exit /b 1
    )

    if /I "%~3"=="clean" (
        echo Running clean module build for %~2 ...
        call mvn -T %THREADS% %SKIP_TESTS% -pl %~2 -am clean compile
        exit /b %ERRORLEVEL%
    )

    echo Running fast module build for %~2 ...
    call mvn -T %THREADS% %SKIP_TESTS% -pl %~2 -am compile
    exit /b %ERRORLEVEL%
)

:run_all
echo Running %GOAL% for all modules ...
call mvn -T %THREADS% %SKIP_TESTS% %GOAL%
exit /b %ERRORLEVEL%
