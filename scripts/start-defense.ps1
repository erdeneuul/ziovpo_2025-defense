$ErrorActionPreference = 'Stop'

$projectDir = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectDir

if (-not (Test-Path -LiteralPath '.env')) {
    throw 'Missing .env. Copy .env.example to .env and fill the values.'
}

Get-Content -LiteralPath '.env' | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith('#')) {
        $parts = $line.Split('=', 2)
        [Environment]::SetEnvironmentVariable($parts[0], $parts[1], 'Process')
    }
}

if (-not (Test-Path -LiteralPath 'src/main/resources/keystore.p12')) {
    throw 'Missing src/main/resources/keystore.p12.'
}

docker info *> $null
if ($LASTEXITCODE -ne 0) {
    throw 'Docker Desktop is not running. Start Docker Desktop and run this script again.'
}

Write-Host 'Starting PostgreSQL and MinIO...' -ForegroundColor Cyan
docker compose up -d
if ($LASTEXITCODE -ne 0) {
    throw 'docker compose up failed.'
}

docker compose ps
Write-Host ''
Write-Host 'Starting the API at https://localhost:8443' -ForegroundColor Green
Write-Host 'Keep this terminal open. Stop the API with Ctrl+C.' -ForegroundColor Yellow
mvn spring-boot:run
