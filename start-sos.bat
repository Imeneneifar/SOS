@echo off
title SoS Service - MS4 Imen (Port 8084)
cd /d "%~dp0microservices\sos-service"
echo ===================================================
echo Demarrage de MS4 SoS Service sur le port 8084...
echo ===================================================
call mvnw.cmd spring-boot:run
pause
