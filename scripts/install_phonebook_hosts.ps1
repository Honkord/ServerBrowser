# Run in PowerShell as Administrator on Windows (when browser runs on Windows/WSL).
$Marker = "# Server Browser phonebook (server_browser.org)"
$HostsPath = "$env:SystemRoot\System32\drivers\etc\hosts"
$Snippet = Join-Path $PSScriptRoot "..\hosts\phonebook.hosts"

if (-not (Test-Path $Snippet)) {
    Write-Error "Missing $Snippet"
    exit 1
}

$content = Get-Content $HostsPath -Raw
if ($content -match [regex]::Escape($Marker)) {
    Write-Host "Phonebook hosts already installed."
    exit 0
}

$lines = Get-Content $Snippet | Where-Object { $_ -notmatch '^\s*#' -and $_.Trim() -ne '' }
Add-Content -Path $HostsPath -Value "`n$Marker"
Add-Content -Path $HostsPath -Value $lines
Write-Host "Installed. Open https://example.server_browser.org:8443/ (server must be running)."
