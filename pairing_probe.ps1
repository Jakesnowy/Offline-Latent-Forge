# pairing_probe.ps1 — Latent Forge host-mode control-API probe (dev-only).
# Reads the debug build's pairing token straight off the attached device, so
# there is nothing to type or mask. Probes /info (no token) and /models
# (token) through `adb forward` exactly the way a controller would.
# Usage:  .\pairing_probe.ps1            (host = 127.0.0.1 via adb forward)
#         .\pairing_probe.ps1 192.168.68.55   (direct LAN address, no forward)

param([string]$Host_ = "")

$adb = 'C:\Android\Sdk\platform-tools\adb.exe'
$pkg = 'io.github.jakesnowy.offlinelatentforge.debug'

if ($Host_ -eq "") {
    & $adb forward tcp:8808 tcp:8808 | Out-Null
    $Host_ = '127.0.0.1'
}

$prefs = (& $adb shell run-as $pkg cat shared_prefs/app_prefs.xml) | Out-String
$tok = [regex]::Match($prefs, 'remote_pair_token">([^<]+)<').Groups[1].Value
if (-not $tok) { Write-Error "no remote_pair_token in $pkg prefs (is the debug build installed?)"; exit 1 }
Write-Host "token: $($tok.Substring(0,4))-**** (len $($tok.Length))"

'--- GET /info (no token) ---'
try { (Invoke-WebRequest "http://${Host_}:8808/info" -UseBasicParsing -TimeoutSec 8).Content }
catch { Write-Warning "info failed: $($_.Exception.Message)" }

'--- GET /models (token) ---'
try { (Invoke-WebRequest "http://${Host_}:8808/models" -Headers @{ Authorization = "Bearer $tok" } -UseBasicParsing -TimeoutSec 20).Content }
catch { Write-Warning "models failed: $($_.Exception.Message)" }
