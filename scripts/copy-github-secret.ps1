param(
    [Parameter(Mandatory = $true)]
    [ValidateSet(
        'KEYSTORE_B64', 'SIGNATURE_PUBLIC_CERT_B64', 'KEYSTORE_PASSWORD',
        'JWT_SECRET', 'DB_URL', 'DB_USERNAME', 'DB_PASSWORD',
        'MINIO_ACCESS_KEY', 'MINIO_SECRET_KEY'
    )]
    [string] $Name
)

$ErrorActionPreference = 'Stop'
$projectDir = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $projectDir '.env'

$values = @{}
Get-Content -LiteralPath $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith('#')) {
        $parts = $line.Split('=', 2)
        $values[$parts[0]] = $parts[1]
    }
}

switch ($Name) {
    'KEYSTORE_B64' {
        $value = [Convert]::ToBase64String(
            [IO.File]::ReadAllBytes((Join-Path $projectDir 'src/main/resources/keystore.p12')))
    }
    'SIGNATURE_PUBLIC_CERT_B64' {
        $value = [Convert]::ToBase64String(
            [IO.File]::ReadAllBytes((Join-Path $projectDir 'src/main/resources/signature-public.cer')))
    }
    'KEYSTORE_PASSWORD' { $value = $values['TLS_KEYSTORE_PASSWORD'] }
    default { $value = $values[$Name] }
}

if (-not $value) { throw "Value for $Name was not found." }
$value | Set-Clipboard
Write-Host "$Name copied to the clipboard. Paste it into the GitHub secret Value field." -ForegroundColor Green
