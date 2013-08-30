@if "%DEBUG%" == "" @echo off
SETLOCAL
if not "%XD_HOME%" == "" (
	set XD_HOME=..
)
if not "%XD_TRANSPORT%" == "" (
	set XD_TRANSPORT=local
) 
if not "%XD_ANALYTICS%" == "" (
	set XD_ANALYTICS=memory
)
if not "%XD_STORE%" == "" (
	set XD_STORE=memory
)
call .\build_xd.bat  %*
ENDLOCAL
