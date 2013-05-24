@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  xd-container startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

@rem Add default JVM options here. You can also use JAVA_OPTS and SPRING_XD_CONTAINER_OPTS to pass JVM options to this script.
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

set CLASSPATH=%APP_HOME%\lib\spring-xd-dirt-0.1.0.BUILD-SNAPSHOT.jar;%APP_HOME%\lib\spring-xd-analytics-0.1.0.BUILD-SNAPSHOT.jar;%APP_HOME%\lib\spring-xd-module-0.1.0.BUILD-SNAPSHOT.jar;%APP_HOME%\lib\spring-xd-hadoop-0.1.0.BUILD-SNAPSHOT.jar;%APP_HOME%\lib\spring-xd-http-0.1.0.BUILD-SNAPSHOT.jar;%APP_HOME%\lib\spring-web-3.2.2.RELEASE.jar;%APP_HOME%\lib\jackson-mapper-asl-1.9.12.jar;%APP_HOME%\lib\tomcat-embed-core-7.0.35.jar;%APP_HOME%\lib\tomcat-embed-logging-juli-7.0.35.jar;%APP_HOME%\lib\spring-integration-event-3.0.0.M2.jar;%APP_HOME%\lib\spring-integration-file-3.0.0.M2.jar;%APP_HOME%\lib\spring-integration-gemfire-3.0.0.M2.jar;%APP_HOME%\lib\spring-integration-redis-3.0.0.M2.jar;%APP_HOME%\lib\spring-data-redis-1.0.4.RELEASE.jar;%APP_HOME%\lib\lettuce-2.2.0.jar;%APP_HOME%\lib\spring-integration-syslog-3.0.0.M2.jar;%APP_HOME%\lib\spring-integration-twitter-3.0.0.M2.jar;%APP_HOME%\lib\args4j-2.0.16.jar;%APP_HOME%\lib\log4j-1.2.17.jar;%APP_HOME%\lib\jcl-over-slf4j-1.7.5.jar;%APP_HOME%\lib\slf4j-log4j12-1.7.5.jar;%APP_HOME%\lib\slf4j-api-1.7.5.jar;%APP_HOME%\lib\commons-logging-1.1.1.jar;%APP_HOME%\lib\spring-core-3.2.2.RELEASE.jar;%APP_HOME%\lib\aopalliance-1.0.jar;%APP_HOME%\lib\spring-beans-3.2.2.RELEASE.jar;%APP_HOME%\lib\spring-aop-3.2.2.RELEASE.jar;%APP_HOME%\lib\spring-expression-3.2.2.RELEASE.jar;%APP_HOME%\lib\spring-context-3.2.2.RELEASE.jar;%APP_HOME%\lib\spring-context-support-3.2.2.RELEASE.jar;%APP_HOME%\lib\spring-tx-3.2.2.RELEASE.jar;%APP_HOME%\lib\commons-pool-1.5.5.jar;%APP_HOME%\lib\jedis-2.1.0.jar;%APP_HOME%\lib\netty-3.5.9.Final.jar;%APP_HOME%\lib\jackson-core-asl-1.9.12.jar;%APP_HOME%\lib\spring-retry-1.0.2.RELEASE.jar;%APP_HOME%\lib\spring-integration-core-3.0.0.M2.jar;%APP_HOME%\lib\spring-xd-tuple-0.1.0.BUILD-SNAPSHOT.jar;%APP_HOME%\lib\spring-data-commons-core-1.4.0.RELEASE.jar;%APP_HOME%\lib\mongo-java-driver-2.9.1.jar;%APP_HOME%\lib\spring-data-mongodb-1.1.1.RELEASE.jar;%APP_HOME%\lib\joda-time-1.6.jar;%APP_HOME%\lib\spring-jdbc-3.2.2.RELEASE.jar;%APP_HOME%\lib\spring-batch-infrastructure-2.1.9.RELEASE.jar;%APP_HOME%\lib\xpp3_min-1.1.4c.jar;%APP_HOME%\lib\xstream-1.3.jar;%APP_HOME%\lib\jettison-1.1.jar;%APP_HOME%\lib\spring-batch-core-2.1.9.RELEASE.jar;%APP_HOME%\lib\commons-cli-1.2.jar;%APP_HOME%\lib\xmlenc-0.52.jar;%APP_HOME%\lib\commons-codec-1.4.jar;%APP_HOME%\lib\commons-math-2.1.jar;%APP_HOME%\lib\commons-lang-2.4.jar;%APP_HOME%\lib\commons-beanutils-core-1.8.0.jar;%APP_HOME%\lib\commons-collections-3.2.1.jar;%APP_HOME%\lib\commons-beanutils-1.7.0.jar;%APP_HOME%\lib\commons-digester-1.8.jar;%APP_HOME%\lib\commons-configuration-1.6.jar;%APP_HOME%\lib\oro-2.0.8.jar;%APP_HOME%\lib\commons-net-1.4.1.jar;%APP_HOME%\lib\jetty-util-6.1.26.jar;%APP_HOME%\lib\servlet-api-2.5-20081211.jar;%APP_HOME%\lib\jetty-6.1.26.jar;%APP_HOME%\lib\jasper-runtime-5.5.12.jar;%APP_HOME%\lib\jasper-compiler-5.5.12.jar;%APP_HOME%\lib\servlet-api-2.5-6.1.14.jar;%APP_HOME%\lib\jsp-api-2.1-6.1.14.jar;%APP_HOME%\lib\core-3.1.1.jar;%APP_HOME%\lib\ant-1.6.5.jar;%APP_HOME%\lib\jsp-2.1-6.1.14.jar;%APP_HOME%\lib\commons-el-1.0.jar;%APP_HOME%\lib\commons-httpclient-3.1.jar;%APP_HOME%\lib\jets3t-0.7.1.jar;%APP_HOME%\lib\kfs-0.3.jar;%APP_HOME%\lib\hsqldb-1.8.0.10.jar;%APP_HOME%\lib\hadoop-core-1.0.4.jar;%APP_HOME%\lib\hadoop-streaming-1.0.4.jar;%APP_HOME%\lib\hadoop-tools-1.0.4.jar;%APP_HOME%\lib\spring-data-hadoop-1.0.0.RELEASE.jar;%APP_HOME%\lib\commons-io-2.4.jar;%APP_HOME%\lib\aspectjweaver-1.7.2.jar;%APP_HOME%\lib\gemfire-7.0.1.jar;%APP_HOME%\lib\spring-data-commons-1.5.1.RELEASE.jar;%APP_HOME%\lib\aspectjrt-1.7.2.jar;%APP_HOME%\lib\spring-data-gemfire-1.3.1.RELEASE.jar;%APP_HOME%\lib\antlr-2.7.7.jar;%APP_HOME%\lib\commons-logging-api-1.0.4.jar;%APP_HOME%\lib\commons-modeler-2.0.1.jar;%APP_HOME%\lib\spring-integration-ip-3.0.0.M2.jar;%APP_HOME%\lib\spring-integration-stream-3.0.0.M2.jar;%APP_HOME%\lib\spring-social-core-1.0.1.RELEASE.jar;%APP_HOME%\lib\spring-security-crypto-3.1.0.RELEASE.jar;%APP_HOME%\lib\spring-social-twitter-1.0.1.RELEASE.jar

@rem Execute xd-container
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %SPRING_XD_CONTAINER_OPTS%  -classpath "%CLASSPATH%" org.springframework.xd.XDContainer %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable SPRING_XD_CONTAINER_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%SPRING_XD_CONTAINER_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
