param(
    [string]$BaseUrl = "http://host.docker.internal:8090",
    [int]$Rounds = 3,
    [int]$Vus = 20,
    [string]$Duration = "30s"
)

$ErrorActionPreference = "Stop"
$policies = @("NONE", "CACHE_ASIDE", "LOCAL_MUTEX", "REDISSON_LOCK", "LOGICAL_EXPIRE")

foreach ($policy in $policies) {
    for ($round = 1; $round -le $Rounds; $round++) {
        Write-Host "Running policy=$policy round=$round"
        docker compose --profile benchmark run --rm `
            -e BASE_URL=$BaseUrl `
            -e CACHE_POLICY=$policy `
            -e VUS=$Vus `
            -e DURATION=$Duration `
            redis-v2-k6
    }
}
