$ErrorActionPreference = 'Stop'
$baseUrl = 'https://localhost:8443'
$isPowerShell7 = $PSVersionTable.PSVersion.Major -ge 7
if (-not $isPowerShell7) {
    [System.Net.ServicePointManager]::ServerCertificateValidationCallback = { $true }
}

function Invoke-JsonApi {
    param(
        [Parameter(Mandatory)] [string] $Method,
        [Parameter(Mandatory)] [string] $Path,
        [object] $Body,
        [string] $Token
    )

    $parameters = @{
        Uri = "$baseUrl$Path"
        Method = $Method
    }
    if ($isPowerShell7) { $parameters.SkipCertificateCheck = $true }
    if ($Token) {
        $parameters.Headers = @{ Authorization = "Bearer $Token" }
    }
    if ($null -ne $Body) {
        $parameters.ContentType = 'application/json'
        $parameters.Body = $Body | ConvertTo-Json -Depth 10
    }
    Invoke-RestMethod @parameters
}

function Get-HttpStatus {
    param(
        [Parameter(Mandatory)] [string] $Method,
        [Parameter(Mandatory)] [string] $Uri,
        [object] $Body,
        [string] $Token
    )
    $parameters = @{ Uri = $Uri; Method = $Method; UseBasicParsing = $true }
    if ($isPowerShell7) {
        $parameters.SkipCertificateCheck = $true
        $parameters.SkipHttpErrorCheck = $true
    }
    if ($Token) { $parameters.Headers = @{ Authorization = "Bearer $Token" } }
    if ($null -ne $Body) {
        $parameters.ContentType = 'application/json'
        $parameters.Body = $Body | ConvertTo-Json -Depth 10
    }
    try {
        $response = Invoke-WebRequest @parameters
        return [int]$response.StatusCode
    } catch {
        if ($_.Exception.Response) { return [int]$_.Exception.Response.StatusCode }
        throw
    }
}

function Show-Step([string] $Text) {
    Write-Host "`n=== $Text ===" -ForegroundColor Cyan
}

Show-Step '1. JWT access/refresh and roles'
$admin = Invoke-JsonApi POST '/auth/login' @{ username = 'admin'; password = 'AdminDemo1!' }
$adminToken = $admin.accessToken
$oldRefresh = $admin.refreshToken
Write-Host 'ADMIN access and refresh tokens received.' -ForegroundColor Green

$refreshed = Invoke-JsonApi POST '/auth/refresh' @{ refreshToken = $oldRefresh }
$adminToken = $refreshed.accessToken
$replayStatus = Get-HttpStatus POST "$baseUrl/auth/refresh" @{ refreshToken = $oldRefresh }
if ($replayStatus -ne 401) { throw "Expected refresh replay status 401, got $replayStatus" }
Write-Host 'Refresh rotation works; replay of old token returned 401.' -ForegroundColor Green

$registrationStatus = Get-HttpStatus POST "$baseUrl/auth/register" `
    @{ username = 'student'; password = 'Student1!'; role = 'ROLE_ADMIN' }
if ($registrationStatus -notin 200, 400) { throw "USER registration failed: $registrationStatus" }
$student = Invoke-JsonApi POST '/auth/login' @{ username = 'student'; password = 'Student1!' }
$userToken = $student.accessToken
$forbiddenStatus = Get-HttpStatus POST "$baseUrl/api/admin/signatures/files/presigned-urls" @{ ids = @() } $userToken
if ($forbiddenStatus -ne 403) { throw "Expected USER status 403, got $forbiddenStatus" }
Write-Host 'Public registration created USER; ADMIN endpoint returned 403.' -ForegroundColor Green

Show-Step '2-3. Licenses, Ticket and electronic signature'
$licenseType = Invoke-JsonApi POST '/license-types' @{
    name = 'Demo 30 days'; defaultDurationInDays = 30; description = 'Defense demonstration'
} $adminToken
$product = Invoke-JsonApi POST '/products' @{ name = 'ZIiVPO Antivirus'; isBlocked = $false } $adminToken
$license = Invoke-JsonApi POST '/license/create' @{
    productId = $product.id
    typeId = $licenseType.id
    ownerId = [guid]::NewGuid().ToString()
    deviceCount = 2
    description = 'License for commission demo'
} $adminToken
$deviceId = [guid]::NewGuid().ToString()
$activation = Invoke-JsonApi POST '/license/activate' @{
    code = $license.code
    deviceId = $deviceId
    deviceName = 'Commission laptop'
    macAddress = '00-11-22-33-44-55'
} $userToken
if (-not $activation.ticket -or -not $activation.digitalSignature) { throw 'Signed Ticket was not returned.' }
$null = Invoke-JsonApi GET "/license/check?code=$($license.code)&deviceId=$deviceId" $null $userToken
$renewed = Invoke-JsonApi POST "/license/renew/$($license.id)" $null $adminToken
Write-Host "License: $($license.code)" -ForegroundColor DarkGray
Write-Host ($activation | ConvertTo-Json -Depth 8)
Write-Host 'Create, activate, check and renew succeeded; Ticket has a Base64 EDS.' -ForegroundColor Green

Show-Step '4. Eight antivirus signature operations'
$signatureBody = @{
    threatName = 'Demo.Trojan'
    firstBytesHex = '4d5a900003000000'
    remainderHashHex = '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef'
    remainderLength = 1024
    fileType = 'exe'
    offsetStart = 0
    offsetEnd = 1031
}
$signature = Invoke-JsonApi POST '/api/signatures' $signatureBody $adminToken
$signatureBody.threatName = 'Demo.Trojan.Updated'
$signatureBody.remainderLength = 2048
$signatureBody.offsetEnd = 2055
$updated = Invoke-JsonApi PUT "/api/signatures/$($signature.id)" $signatureBody $adminToken
$history = Invoke-JsonApi GET "/api/signatures/$($signature.id)/history" $null $adminToken
$audit = Invoke-JsonApi GET "/api/signatures/$($signature.id)/audit" $null $adminToken
$byIds = Invoke-JsonApi POST '/api/signatures/by-ids' @{ ids = @($signature.id) } $userToken
$fullBeforeDelete = Invoke-JsonApi GET '/api/signatures' $null $userToken
if (-not $updated.digitalSignatureBase64 -or $history.Count -lt 1 -or $audit.Count -lt 1) {
    throw 'Signature EDS, History or Audit check failed.'
}
Write-Host 'Create, update, full, by-ids, History and Audit succeeded.' -ForegroundColor Green

Show-Step '5. Binary multipart API'
$binaryParameters = @{
    Uri = "$baseUrl/api/binary/signatures/full"
    Method = 'GET'
    UseBasicParsing = $true
    Headers = @{ Authorization = "Bearer $userToken" }
}
if ($isPowerShell7) { $binaryParameters.SkipCertificateCheck = $true }
$binary = Invoke-WebRequest @binaryParameters
$contentType = [string]$binary.Headers['Content-Type']
$binaryText = if ($binary.Content -is [byte[]]) {
    [Text.Encoding]::GetEncoding(28591).GetString($binary.Content)
} else { [string]$binary.Content }
if ($contentType -notlike 'multipart/mixed*' -or
    $binaryText -notlike '*MF-Erdeneuul*' -or $binaryText -notlike '*DB-Erdeneuul*') {
    throw 'Binary multipart or magic number check failed.'
}
Write-Host "Content-Type: $contentType"
Write-Host 'Magic numbers found: MF-Erdeneuul and DB-Erdeneuul.' -ForegroundColor Green

$deletedStatus = Get-HttpStatus DELETE "$baseUrl/api/signatures/$($signature.id)" $null $adminToken
$fullAfterDelete = Invoke-JsonApi GET '/api/signatures' $null $userToken
$increment = Invoke-JsonApi GET '/api/signatures/increment?since=2000-01-01T00:00:00Z' $null $userToken
$deletedInFull = @($fullAfterDelete | Where-Object id -eq $signature.id).Count
$deletedInIncrement = @($increment | Where-Object { $_.id -eq $signature.id -and $_.status -eq 'DELETED' }).Count
if ($deletedStatus -ne 204 -or $deletedInFull -ne 0 -or $deletedInIncrement -ne 1) {
    throw 'Logical delete/full/increment behavior check failed.'
}
Write-Host 'Delete is logical: absent from full export and present as DELETED in increment.' -ForegroundColor Green

Show-Step '6. Private MinIO, upload and pre-signed URL'
$sampleFile = Get-Item -LiteralPath (Join-Path (Split-Path -Parent $PSScriptRoot) 'demo/sample.txt')
$uploadJson = & curl.exe -k -sS -X POST `
    -H "Authorization: Bearer $adminToken" `
    -F "file=@$($sampleFile.FullName)" `
    "$baseUrl/api/admin/signatures/files/upload"
$upload = $uploadJson | ConvertFrom-Json
$urls = Invoke-JsonApi POST '/api/admin/signatures/files/presigned-urls' `
    @{ ids = @($upload.signature.id) } $adminToken
$property = $urls.PSObject.Properties | Where-Object Name -eq ([string]$upload.signature.id)
if (-not $property.Value) { throw 'Pre-signed URL was not returned.' }
$downloadStatus = [int](& curl.exe -sS -o NUL -w '%{http_code}' $property.Value)
$anonymousStatus = [int](& curl.exe -sS -o NUL -w '%{http_code}' 'http://localhost:9000/av-signature/')
if ($downloadStatus -ne 200 -or $anonymousStatus -notin 401, 403) {
    throw 'MinIO pre-signed/private bucket check failed.'
}
Write-Host "Object: $($upload.objectName)"
Write-Host 'Upload and pre-signed download succeeded; anonymous bucket access is forbidden.' -ForegroundColor Green

Write-Host "`nALL DEFENSE DEMO CHECKS PASSED" -ForegroundColor Green
