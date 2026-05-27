<#
.SYNOPSIS
  One-command local startup for wow-explorer: Postgres + Spring Boot backend + Vite frontend.

.DESCRIPTION
  - Loads .env into this process (Maven/spring-boot:run does NOT read .env on its own).
  - Brings up the Postgres container and waits for its healthcheck.
  - Launches the backend and frontend, each in its own PowerShell window, so logs stay separate.

.EXAMPLE
  .\start.ps1
  .\start.ps1 -SkipFrontend     # DB + backend only
#>
[CmdletBinding()]
param(
    [switch]$SkipFrontend,
    [switch]$SkipBackend
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot

# --- 1. Load .env into this process so child processes inherit the vars ---
$envFile = Join-Path $root '.env'
if (-not (Test-Path $envFile)) {
    throw ".env not found at $envFile. Copy .env.example to .env and fill in your Battle.net credentials."
}
Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq '' -or $line.StartsWith('#')) { return }
    $idx = $line.IndexOf('=')
    if ($idx -lt 1) { return }
    $key = $line.Substring(0, $idx).Trim()
    $val = $line.Substring($idx + 1).Trim()
    Set-Item -Path "env:$key" -Value $val
}
Write-Host "[start] Loaded .env (POSTGRES_PORT=$env:POSTGRES_PORT)" -ForegroundColor Cyan

# --- 2. Postgres ---
Write-Host "[start] Bringing up Postgres..." -ForegroundColor Cyan
docker compose up -d
if ($LASTEXITCODE -ne 0) { throw "docker compose up failed." }

Write-Host "[start] Waiting for Postgres healthcheck..." -ForegroundColor Cyan
$deadline = (Get-Date).AddSeconds(60)
do {
    $status = (docker inspect -f '{{.State.Health.Status}}' wow-explorer-postgres 2>$null)
    if ($status -eq 'healthy') { break }
    if ((Get-Date) -gt $deadline) { throw "Postgres did not become healthy within 60s (last status: '$status')." }
    Start-Sleep -Milliseconds 1000
} while ($true)
Write-Host "[start] Postgres healthy." -ForegroundColor Green

# --- 3. Backend (new window; inherits the .env vars set above) ---
if (-not $SkipBackend) {
    Write-Host "[start] Launching backend (mvn spring-boot:run)..." -ForegroundColor Cyan
    $backendDir = Join-Path $root 'backend'
    Start-Process powershell -ArgumentList @(
        '-NoExit', '-Command',
        "Set-Location '$backendDir'; mvn spring-boot:run"
    )
}

# --- 4. Frontend (new window) ---
if (-not $SkipFrontend) {
    Write-Host "[start] Launching frontend (npm run dev)..." -ForegroundColor Cyan
    $frontendDir = Join-Path $root 'frontend'
    $installIfNeeded = "if (-not (Test-Path 'node_modules')) { npm install }"
    Start-Process powershell -ArgumentList @(
        '-NoExit', '-Command',
        "Set-Location '$frontendDir'; $installIfNeeded; npm run dev"
    )
}

Write-Host ""
Write-Host "[start] Up. Backend: http://localhost:8080  Frontend: http://localhost:5173" -ForegroundColor Green
Write-Host "[start] Postgres stays running. Stop it with: docker compose down" -ForegroundColor DarkGray
