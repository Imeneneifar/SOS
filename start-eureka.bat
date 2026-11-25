@echo off
title Eureka Server (Port 8761)
cd /d "%~dp0infrastructure\eureka-server"
echo ==============================================
echo Demarrage de Eureka Server sur le port 8761...
echo ==============================================
call mvnw.cmd spring-boot:run
pause
