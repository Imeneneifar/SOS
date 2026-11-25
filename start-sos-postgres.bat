@echo off
title SoS Service - MS4 Imen (Port 8084 - PostgreSQL Local)
cd /d "%~dp0microservices\sos-service"
echo ===============================================================
echo Demarrage de MS4 SoS Service sur le port 8084 (PostgreSQL)...
echo Base attendue : jdbc:postgresql://localhost:5432/sos_db
echo ===============================================================
call mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
pause
