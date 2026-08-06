@echo off
title SaaS Frontend (3000)

echo ========================================
echo   SaaS Frontend Start
echo ========================================

cd /d "%~dp0frontend"
if not exist node_modules\ call npm install
call npm run dev
pause
