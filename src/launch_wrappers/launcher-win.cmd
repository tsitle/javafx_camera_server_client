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

@REM If this script part of a JLink Launcher release we don't need to set the JAVA_OPTS
set TMPDIRNAME=%~dp0
if exist %TMPDIRNAME%\bin\java.exe goto allReady

@REM examples for JAVA_HOME under Windows:
@REM set JAVA_HOME=C:\Program Files\Java\openjdk-22.0.2-win_x64

if not "%JAVA_HOME%" == "" goto haveJavaHome
echo.
echo Error: 'JAVA_HOME' is not set.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.
exit /b 1

:haveJavaHome

if exist %JAVA_HOME%\bin\java.exe goto haveJavaExe
echo.
echo Error: Cannot find '%JAVA_HOME%\bin\java.exe'.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.
exit /b 1

@REM -------------------------------------------------------------------------------------------------------------------

:haveJavaExe

set JAVA_OPTS=--module-path lib --add-modules=javafx.controls,javafx.fxml

:allReady

call bin\javafx_camera_server_client.bat
