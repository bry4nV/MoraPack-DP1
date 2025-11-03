# Script para ejecutar el backend y guardar logs
# Uso: ./run-with-logs.ps1

Write-Host "Iniciando MoraPack Backend..." -ForegroundColor Green
Write-Host "Los logs se guardaran en: simulation.log" -ForegroundColor Cyan
Write-Host ""
Write-Host "Espera a que aparezca 'Started MorapackApplication'..." -ForegroundColor Yellow
Write-Host "Luego abre: http://localhost:8080/simulation-test.html" -ForegroundColor Yellow
Write-Host ""
Write-Host "Para detener: Presiona Ctrl+C" -ForegroundColor Red
Write-Host "=======================================================" -ForegroundColor Gray
Write-Host ""

# Ejecutar Spring Boot y guardar logs en archivo Y mostrar en pantalla
./mvnw.cmd spring-boot:run 2>&1 | Tee-Object -FilePath "simulation.log"
