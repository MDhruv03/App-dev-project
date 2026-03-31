param(
    [string]$PackageName = "com.example.myapplication",
    [string]$DeviceSerial = ""
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

function Resolve-AdbPath {
    $adbCmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($null -ne $adbCmd) {
        return $adbCmd.Source
    }

    $candidates = @()

    if ($env:LOCALAPPDATA) {
        $candidates += (Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe")
    }

    $localPropsPath = Join-Path $root "local.properties"
    if (Test-Path $localPropsPath) {
        $sdkLine = Get-Content $localPropsPath | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
        if ($sdkLine) {
            $sdkDir = $sdkLine -replace '^sdk\.dir=', ''
            $sdkDir = $sdkDir -replace '\\:', ':'
            $sdkDir = $sdkDir -replace '\\\\', '\\'
            if ($sdkDir) {
                $candidates += (Join-Path $sdkDir "platform-tools\adb.exe")
            }
        }
    }

    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path $candidate)) {
            return $candidate
        }
    }

    return $null
}

$adbPath = Resolve-AdbPath
if ($null -eq $adbPath) {
    Write-Error "adb not found. Install Android platform-tools or ensure Android SDK path exists in local.properties."
    exit 1
}

Write-Host "Using adb: $adbPath"

$adbTargetArgs = @()
if ($DeviceSerial -and $DeviceSerial.Trim()) {
    $adbTargetArgs = @("-s", $DeviceSerial.Trim())
}

if ($adbTargetArgs.Count -eq 0) {
    $deviceRows = & $adbPath devices | Select-Object -Skip 1
    $onlineDevices = @()
    foreach ($row in $deviceRows) {
        $trimmed = $row.Trim()
        if ($trimmed -and -not ($trimmed -like "*offline*")) {
            $parts = $trimmed -split "\s+"
            if ($parts.Length -ge 2 -and $parts[1] -eq "device") {
                $onlineDevices += $parts[0]
            }
        }
    }

    if ($onlineDevices.Count -gt 1) {
        Write-Error "Multiple online devices detected: $($onlineDevices -join ', '). Re-run with -DeviceSerial <serial>."
        exit 1
    }

    if ($onlineDevices.Count -eq 1) {
        $adbTargetArgs = @("-s", $onlineDevices[0])
        Write-Host "Target device: $($onlineDevices[0])"
    }
}

# Full logcat dump
$fullLog = Join-Path $runDir "logcat_full.txt"
& $adbPath @adbTargetArgs logcat -d *:V > $fullLog

# Filtered startup/runtime lines
$filteredLog = Join-Path $runDir "startup_runtime_filtered.txt"
Get-Content $fullLog | Select-String -Pattern "StartupDiag|AndroidRuntime|OpportunityHub_|FATAL EXCEPTION|Caused by" | Set-Content $filteredLog

# Pull internal diagnostics log via run-as (debuggable builds)
$internalDiag = Join-Path $runDir "startup_diagnostics_internal.txt"
$diagText = & $adbPath @adbTargetArgs shell run-as $PackageName cat files/startup_diagnostics.log 2>&1
$diagText | Out-File -FilePath $internalDiag -Encoding utf8

# Pull crash folder from external app-specific storage if available
$crashPullDir = Join-Path $runDir "crashes"
New-Item -ItemType Directory -Path $crashPullDir | Out-Null
& $adbPath @adbTargetArgs pull "/storage/emulated/0/Android/data/$PackageName/files/crashes" "$crashPullDir" > $null 2>&1

Write-Host "Logs collected in: $runDir"
Write-Host "- logcat_full.txt"
Write-Host "- startup_runtime_filtered.txt"
Write-Host "- startup_diagnostics_internal.txt"
Write-Host "- crashes/ (if available)"
