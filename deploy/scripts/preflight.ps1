param(
    [Parameter(Mandatory = $true)]
    [string] $EnvironmentFile
)

$ErrorActionPreference = 'Stop'
$required = @(
    'LIFEOS_IMAGE', 'LIFEOS_MIGRATION_IMAGE',
    'DB_URL', 'DB_USER', 'DB_PASSWORD',
    'MIGRATION_DB_URL', 'MIGRATION_DB_USER', 'MIGRATION_DB_PASSWORD',
    'JWT_SECRET', 'JWT_KID', 'NOTIFICATION_UNSUBSCRIBE_SECRET',
    'APP_BASE_URL', 'API_BASE_URL', 'TRUSTED_PROXIES',
    'SMTP_HOST', 'SMTP_USER', 'SMTP_PASSWORD', 'MAIL_FROM'
)

if (-not (Test-Path -LiteralPath $EnvironmentFile -PathType Leaf)) {
    throw "Environment file not found: $EnvironmentFile"
}

$values = @{}
foreach ($line in Get-Content -LiteralPath $EnvironmentFile) {
    $trimmed = $line.Trim()
    if (-not $trimmed -or $trimmed.StartsWith('#')) { continue }
    $parts = $trimmed.Split('=', 2)
    if ($parts.Count -ne 2) { throw "Invalid KEY=VALUE line for key name near: $($parts[0])" }
    $values[$parts[0].Trim()] = $parts[1]
}

$errors = [System.Collections.Generic.List[string]]::new()
foreach ($name in $required) {
    if (-not $values.ContainsKey($name) -or [string]::IsNullOrWhiteSpace($values[$name])) {
        $errors.Add("missing $name")
    } elseif ($values[$name] -match '<[^>]+>|change-me|replace-me|example\.com') {
        $errors.Add("placeholder $name")
    }
}

if ($values['JWT_SECRET'] -and $values['JWT_SECRET'].Length -lt 32) { $errors.Add('JWT_SECRET shorter than 32 characters') }
if ($values['NOTIFICATION_UNSUBSCRIBE_SECRET'] -and $values['NOTIFICATION_UNSUBSCRIBE_SECRET'].Length -lt 32) {
    $errors.Add('NOTIFICATION_UNSUBSCRIBE_SECRET shorter than 32 characters')
}
if ($values['JWT_SECRET'] -and $values['JWT_SECRET'] -eq $values['NOTIFICATION_UNSUBSCRIBE_SECRET']) {
    $errors.Add('JWT_SECRET and NOTIFICATION_UNSUBSCRIBE_SECRET must differ')
}
if ($values['TRUSTED_PROXIES'] -match '^\.?\*$|\.\*|0\.0\.0\.0') { $errors.Add('TRUSTED_PROXIES is too broad') }

if ($errors.Count -gt 0) {
    $errors | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Host "Preflight passed for $($required.Count) required names; values were not printed."
