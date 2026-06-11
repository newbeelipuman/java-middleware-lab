$ErrorActionPreference = "Stop"

Write-Host "Stopping Redis to exercise controlled database fallback"
docker compose stop redis-v2-redis

Write-Host "Run the degraded-path request or k6 scenario now"
Write-Host "GET http://127.0.0.1:8090/api/schedules/1?policy=CACHE_ASIDE"

Read-Host "Press Enter to restart Redis"
docker compose start redis-v2-redis
docker compose ps redis-v2-redis
