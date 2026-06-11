param(
    [int]$Rounds = 3,
    [int]$Vus = 5,
    [string]$Duration = "10s",
    [string]$BaseUrl = "http://host.docker.internal:8091"
)

$ErrorActionPreference = "Stop"
for ($round = 1; $round -le $Rounds; $round++) {
    Write-Host "RocketMQ V2 create-shift round=$round"
    docker compose --profile benchmark run --rm `
        -e BASE_URL=$BaseUrl `
        -e VUS=$Vus `
        -e DURATION=$Duration `
        mq-v2-k6
}
