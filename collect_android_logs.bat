@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0tools\collect_android_logs.ps1" %*
endlocal
