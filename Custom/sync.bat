@echo off
echo === MC Resource Pack Auto-Sync ===
echo Source: C:\Users\konra\OneDrive\Pulpit\vipshop\Custom
echo Target: .minecraft\resourcepacks\Custom
echo.
echo Keep this open. Press Ctrl+C to stop.
echo After edits, press F3+T in-game to reload.
echo.
powershell -ExecutionPolicy Bypass -File "%~dp0sync.ps1"
pause
