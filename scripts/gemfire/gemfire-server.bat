@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  gemfire-server startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

@rem Add default JVM options here. You can also use JAVA_OPTS and SPRING_GEMFIRE_SERVER_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windowz variants

if not "%OS%" == "Windows_NT" goto win9xME_args
if "%@eval[2+2]" == "4" goto 4NT_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*
goto execute

:4NT_args
@rem Get arguments from the 4NT Shell from JP Software
set CMD_LINE_ARGS=%$

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\conf;%APP_HOME%\lib\spring-xd-gemfire-server-0.1.0.BUILD-SNAPSHOT.jar;%APP_HOME%\lib\commons-beanutils-1.6.jar;%APP_HOME%\lib\spring-data-gemfire-1.3.1.RELEASE.jar;%APP_HOME%\lib\log4j-1.2.17.jar;%APP_HOME%\lib\jcl-over-slf4j-1.7.5.jar;%APP_HOME%\lib\slf4j-log4j12-1.7.5.jar;%APP_HOME%\lib\commons-logging-1.1.1.jar;%APP_HOME%\lib\commons-collections-2.1.jar;%APP_HOME%\lib\slf4j-api-1.7.5.jar;%APP_HOME%\lib\antlr-2.7.7.jar;%APP_HOME%\lib\aspectjweaver-1.7.2.jar;%APP_HOME%\lib\spring-core-3.2.2.RELEASE.jar;%APP_HOME%\lib\gemfire-7.0.1.jar;%APP_HOME%\lib\spring-beans-3.2.2.RELEASE.jar;%APP_HOME%\lib\spring-data-commons-1.5.1.RELEASE.jar;%APP_HOME%\lib\aopalliance-1.0.jar;%APP_HOME%\lib\spring-aop-3.2.2.RELEASE.jar;%APP_HOME%\lib\spring-expression-3.2.2.RELEASE.jar;%APP_HOME%\lib\spring-context-3.2.2.RELEASE.jar;%APP_HOME%\lib\spring-context-support-3.2.2.RELEASE.jar;%APP_HOME%\lib\spring-tx-3.2.2.RELEASE.jar;%APP_HOME%\lib\commons-digester-1.4.1.jar;%APP_HOME%\lib\commons-logging-api-1.0.4.jar;%APP_HOME%\lib\commons-modeler-2.0.1.jar;%APP_HOME%\lib\jackson-core-asl-1.9.12.jar;%APP_HOME%\lib\jackson-mapper-asl-1.9.12.jar;%APP_HOME%\lib\aspectjrt-1.7.2.jar

@rem Execute gemfire-server
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %SPRING_GEMFIRE_SERVER_OPTS%  -classpath "%CLASSPATH%" org.springframework.xd.gemfire.CacheServer %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable SPRING_GEMFIRE_SERVER_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%SPRING_GEMFIRE_SERVER_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
