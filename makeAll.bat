call make1.bat %*
@if %errorlevel% NEQ 0 (exit /b %errorlevel%)
call make2.bat %*
@if %errorlevel% NEQ 0 (exit /b %errorlevel%)
call make3.bat %*
@if %errorlevel% NEQ 0 (exit /b %errorlevel%)
call make4.bat %*
@if %errorlevel% NEQ 0 (exit /b %errorlevel%)
@echo All stages compiled SUCCESSFULLY
