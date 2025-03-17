@REM ----------------------------------------------------------------------------
@REM Launcher for the Camera Server Client app.
@REM
@REM Requires that either env var JAVA_HOME is set or '> java' can find a Java executable (JDK or JRE).
@REM The JDK/JRE version needs to be >= 21
@REM
@REM by TS, Mar 2025
@REM ----------------------------------------------------------------------------

@echo off

set LCFG_JAVA_MIN_LANG_LEVEL=21

set LCFG_OS_TYPE=win

@REM -------------------------------------------------------------------------------------------------------------------

@REM set title of command window
title %0

@setlocal

@REM -------------------------------------------------------------------------------------------------------------------

@REM examples for JAVA_HOME under Windows:
@REM set JAVA_HOME=C:\Program Files\Java\openjdk-22.0.2-win_x64

@REM -------------------------------------------------------------------------------------------------------------------

set LVAR_ARCH=x64

set LVAR_JAVA_LIB_PATH_CUSTOM=lib_opencv-%LCFG_OS_TYPE%-%LVAR_ARCH%
set LVAR_JAVA_LIB_PATH_ENV=%OpenCV_DIR%\java\%LVAR_ARCH%

set LVAR_FFMPEG_LIB_PATH_ENV=%OpenCV_DIR%\%LVAR_ARCH%\vc16\bin

set TMP_JAVA_LIB_PATH=%LVAR_JAVA_LIB_PATH_CUSTOM%
if exist "%TMP_JAVA_LIB_PATH%" goto foundLib
set TMP_JAVA_LIB_PATH=%LVAR_JAVA_LIB_PATH_ENV%
if exist "%TMP_JAVA_LIB_PATH%" goto foundGlobalLib
echo Error: Could not find Java library path
echo Tried:
echo   %LVAR_JAVA_LIB_PATH_CUSTOM%\
echo   %LVAR_JAVA_LIB_PATH_ENV%\
exit /B 1

:foundGlobalLib
echo Note: The directory '%LVAR_FFMPEG_LIB_PATH_ENV%' needs to be included in the env var 'PATH'.
echo If it is not the application won't be able to open the camera stream.
echo.

:foundLib
set JAVA_OPTS=-Djava.library.path="%TMP_JAVA_LIB_PATH%" --module-path lib --add-modules=javafx.controls,javafx.fxml

call bin\pnp_camera_server_client.bat
