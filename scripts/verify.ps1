$ErrorActionPreference = "Stop"

Set-Location (Split-Path -Parent $PSScriptRoot)

$env:RG_API_TOKEN = if ($env:RG_API_TOKEN) { $env:RG_API_TOKEN } else { "dev-token-change-me" }
$env:RG_DATA_DIR = if ($env:RG_DATA_DIR) { $env:RG_DATA_DIR } else { "data-verify" }
$env:RG_PORT = if ($env:RG_PORT) { $env:RG_PORT } else { "8080" }

javac -d out src\com\reconcileguard\Main.java
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$job = Start-Job -ScriptBlock {
    param($cwd, $token, $dataDir, $port)
    Set-Location $cwd
    $env:RG_API_TOKEN = $token
    $env:RG_DATA_DIR = $dataDir
    $env:RG_PORT = $port
    java -cp out com.reconcileguard.Main
} -ArgumentList (Get-Location).Path, $env:RG_API_TOKEN, $env:RG_DATA_DIR, $env:RG_PORT

try {
    Start-Sleep -Seconds 2
    $headers = @{ Authorization = "Bearer $env:RG_API_TOKEN"; "X-Operator-Id" = "ops.verify" }
    $base = "http://localhost:$env:RG_PORT"

    $health = Invoke-RestMethod -Uri "$base/api/health"
    $summary = Invoke-RestMethod -Uri "$base/api/summary" -Headers $headers
    $cases = Invoke-RestMethod -Uri "$base/api/cases" -Headers $headers
    Invoke-RestMethod -Uri "$base/api/reconcile/run" -Headers $headers -Method Post | Out-Null

    Write-Host "Health: $($health.status)"
    Write-Host "Transactions: $($summary.totalTransactions)"
    Write-Host "Cases: $($cases.Count)"
    Write-Host "Verification passed"
}
finally {
    Stop-Job $job -ErrorAction SilentlyContinue
    Remove-Job $job -Force -ErrorAction SilentlyContinue
}
