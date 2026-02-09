@echo off
echo Uninstalling old app version...
adb uninstall com.example.myapplication
echo.
echo Installing fresh build...
adb install app\build\outputs\apk\debug\app-debug.apk
echo.
echo Launching app...
adb shell am start -n com.example.myapplication/.MainActivity
echo.
echo Done! Check logcat for output.
pause
