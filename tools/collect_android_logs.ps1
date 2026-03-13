param(
    [string]$PackageName = "com.example.myapplication"
)

$ErrorActionPreference = "Continue"

$root = Split-Path -Parent $PSScriptRoot
$logsDir = Join-Path $root "logs"
if (!(Test-Path $logsDir)) {
    New-Item -ItemType Directory -Path $logsDir | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$runDir = Join-Path $logsDir "run_$timestamp"
New-Item -ItemType Directory -Path $runDir | Out-Null

Write-Host "Collecting Android logs for package: $PackageName"

$adbCheck = Get-Command adb -ErrorAction SilentlyContinue
if ($null -eq $adbCheck) {
    Write-Error "adb not found in PATH. Install Android platform-tools or run from Android Studio terminal with adb available."
    exit 1
}

# Full logcat dump
$fullLog = Join-Path $runDir "logcat_full.txt"
adb logcat -d *:V > $fullLog

# Filtered startup/runtime lines
$filteredLog = Join-Path $runDir "startup_runtime_filtered.txt"
Get-Content $fullLog | Select-String -Pattern "StartupDiag|AndroidRuntime|OpportunityHub_|FATAL EXCEPTION|Caused by" | Set-Content $filteredLog

# Pull internal diagnostics log via run-as (debuggable builds)
$internalDiag = Join-Path $runDir "startup_diagnostics_internal.txt"
$diagText = adb shell run-as $PackageName cat files/startup_diagnostics.log 2>&1
$diagText | Out-File -FilePath $internalDiag -Encoding utf8

# Pull crash folder from external app-specific storage if available
$crashPullDir = Join-Path $runDir "crashes"
New-Item -ItemType Directory -Path $crashPullDir | Out-Null
adb pull "/storage/emulated/0/Android/data/$PackageName/files/crashes" "$crashPullDir" > $null 2>&1

Write-Host "Logs collected in: $runDir"
Write-Host "- logcat_full.txt"
Write-Host "- startup_runtime_filtered.txt"
Write-Host "- startup_diagnostics_internal.txt"
Write-Host "- crashes/ (if available)"
