$ErrorActionPreference = "Stop"
$broker = docker compose ps -q mq-v2-broker
if (-not $broker) {
    throw "Broker container is not running."
}

docker pause $broker
try {
    $body = '{"staffCode":"BROKER-PAUSE","fromShift":"DAY","toShift":"NIGHT"}'
    $created = Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8091/api/shift-changes `
        -ContentType application/json -Body $body
    Write-Host "created request id=$($created.id) while broker paused"
    Start-Sleep -Seconds 3
    Invoke-RestMethod http://127.0.0.1:8091/api/admin/outbox | ConvertTo-Json -Depth 5
} finally {
    docker unpause $broker
}

Start-Sleep -Seconds 8
Invoke-RestMethod http://127.0.0.1:8091/api/admin/outbox | ConvertTo-Json -Depth 5
