@echo off
echo ==========================================================
echo Demarrage de l'architecture microservices Safe Route
echo Validation 1 (Semaine 4) - MS4 SoS Service (Imen)
echo ==========================================================
echo 1. Lancement de Eureka Server...
start "Eureka Server [8761]" "%~dp0start-eureka.bat"

echo Attente de 15 secondes pour laisser Eureka initialiser...
timeout /t 15 /nobreak > nul

echo 2. Lancement de MS4 SoS Service...
start "MS4 SoS Service [8084]" "%~dp0start-sos.bat"

echo Attente de 15 secondes pour laisser SoS Service s'enregistrer dans Eureka...
timeout /t 15 /nobreak > nul

echo 3. Lancement de API Gateway...
start "API Gateway [8080]" "%~dp0start-gateway.bat"

echo ==========================================================
echo Tout est lance !
echo Eureka Dashboard : http://localhost:8761
echo Swagger UI SoS   : http://localhost:8084/swagger-ui/index.html
echo Gateway API      : http://localhost:8080/sos-service/sos/active
echo ==========================================================
pause
