# Chạy backend LifeOS tự host + Cloudflare Quick Tunnel, rồi in địa chỉ công khai.
# Cách dùng (từ thư mục backend/):  powershell -ExecutionPolicy Bypass -File scripts/tunnel-up.ps1
# Xem docs/ops/SELF_HOST_TUNNEL.md.
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)

$envFile = '.env.tunnel'

function New-Secret([int]$bytes) {
    $buffer = New-Object byte[] $bytes
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($buffer)
    $rng.Dispose()
    return $buffer
}

if (-not (Test-Path $envFile)) {
    # Sinh một lần rồi giữ nguyên: đổi JWT_SECRET sẽ làm mọi phiên đăng nhập hết hiệu lực.
    $jwt = [Convert]::ToBase64String((New-Secret 48))
    $snapshot = -join ((New-Secret 32) | ForEach-Object { $_.ToString('x2') })
    $unsubscribe = -join ((New-Secret 32) | ForEach-Object { $_.ToString('x2') })
    $lines = @(
        '# Secret riêng của máy này cho docker-compose.tunnel.yml. KHÔNG commit, KHÔNG chia sẻ.',
        "JWT_SECRET=$jwt",
        "SNAPSHOT_HMAC_SECRET=$snapshot",
        "NOTIFICATION_UNSUBSCRIBE_SECRET=$unsubscribe"
    )
    [System.IO.File]::WriteAllLines((Join-Path (Get-Location) $envFile), $lines)
    Write-Host "Đã sinh secret mới vào backend/$envFile"
}

$compose = @('compose', '-f', 'docker-compose.yml', '-f', 'docker-compose.tunnel.yml', '--env-file', $envFile)

& docker @compose up -d --build
if ($LASTEXITCODE -ne 0) { throw 'docker compose up thất bại' }

Write-Host 'Đang chờ Cloudflare cấp địa chỉ tunnel (tối đa 3 phút, lần đầu build backend có thể lâu hơn)...'
$url = $null
$deadline = (Get-Date).AddMinutes(3)
while (-not $url -and (Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 3
    $logs = (& docker @compose logs cloudflared | Out-String)
    # Bỏ qua https://api.trycloudflare.com (điểm cấp tunnel xuất hiện trong log lỗi), chỉ lấy địa chỉ được cấp.
    $found = [regex]::Matches($logs, 'https://(?!api\.)[a-z0-9-]+\.trycloudflare\.com')
    if ($found.Count -gt 0) { $url = $found[$found.Count - 1].Value }
}

if (-not $url) {
    Write-Host 'Chưa thấy địa chỉ tunnel. Xem log: docker compose -f docker-compose.yml -f docker-compose.tunnel.yml --env-file .env.tunnel logs cloudflared backend'
    exit 1
}

Write-Host ''
Write-Host "Địa chỉ backend công khai : $url"
Write-Host "API cho app               : $url/api/v1"
Write-Host 'Hộp thư xác minh email    : http://localhost:8025 (chỉ mở được trên máy này)'
Write-Host ''
Write-Host 'Build APK trỏ tới địa chỉ này (từ thư mục mobile/):'
Write-Host "  powershell -ExecutionPolicy Bypass -File scripts/build-apk.ps1 -ApiBaseUrl $url/api/v1"
Write-Host ''
Write-Host 'Lưu ý: địa chỉ ĐỔI mỗi khi container cloudflared khởi động lại (khởi động lại máy, Docker...). Khi đó chạy lại script này và build lại APK.'
