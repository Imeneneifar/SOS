@echo off
title API Gateway (Port 8080)
cd /d "%~dp0infrastructure\api-gateway"
echo ==============================================
echo Demarrage de API Gateway sur le port 8080...
echo ==============================================
call mvnw.cmd spring-boot:run
pause
