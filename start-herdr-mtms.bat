@echo off
title MTMS Herdr Agent Launcher
cd /d "D:\Project_CaNhan\mtms"

echo ========================================================
echo  KHOI DONG TOAN BO 6 AGENTS MTMS TREN HERDR
echo ========================================================
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "D:\Project_CaNhan\mtms\scripts\start-mtms-agents.ps1"

echo.
echo ========================================================
echo  DANH SACH AGENTS DANG HOAT DONG:
echo ========================================================
herdr agent list

echo.
echo [OK] Cac Agent da san sang tren Herdr Workspace w1 (mtms)!
timeout /t 5
