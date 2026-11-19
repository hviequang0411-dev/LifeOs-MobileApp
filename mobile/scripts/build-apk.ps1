# Build APK release LifeOS trỏ tới backend tự host (vd. Cloudflare Quick Tunnel).
# Cách dùng (từ thư mục mobile/):
#   powershell -ExecutionPolicy Bypass -File scripts/build-apk.ps1 -ApiBaseUrl https://xxxx.trycloudflare.com/api/v1
# Cần android/key.properties trỏ tới keystore riêng (docs/ops/SELF_HOST_TUNNEL.md mục 2).
param(
    [Parameter(Mandatory = $true)]
    [string]$ApiBaseUrl
)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)

if ($ApiBaseUrl -notmatch '^https://[^/\s]+/api/v1/?$') {
    throw "ApiBaseUrl phải có dạng https://<host>/api/v1 (nhận được: $ApiBaseUrl)"
}
$ApiBaseUrl = $ApiBaseUrl.TrimEnd('/')

if (-not (Test-Path 'android/key.properties')) {
    throw 'Thiếu android/key.properties — tạo keystore theo docs/ops/SELF_HOST_TUNNEL.md mục 2 trước.'
}

& flutter build apk --release `
    "--dart-define=LIFEOS_ENV=prod" `
    "--dart-define=LIFEOS_API_BASE_URL=$ApiBaseUrl"
if ($LASTEXITCODE -ne 0) { throw 'flutter build apk thất bại' }

$apk = Join-Path (Get-Location) 'build/app/outputs/flutter-apk/app-release.apk'
Write-Host ''
Write-Host "APK: $apk"
Write-Host "Backend: $ApiBaseUrl"
Write-Host 'Chép file này sang điện thoại rồi mở để cài (cho phép cài từ nguồn không xác định).'
