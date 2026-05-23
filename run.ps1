javac -d out src\com\reconcileguard\Main.java
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

if (-not $env:RG_API_TOKEN) {
    $env:RG_API_TOKEN = "dev-token-change-me"
}
if (-not $env:RG_DATA_DIR) {
    $env:RG_DATA_DIR = "data"
}

Write-Host "Starting ReconcileGuard on http://localhost:8080"
Write-Host "API token: $env:RG_API_TOKEN"
java -cp out com.reconcileguard.Main
