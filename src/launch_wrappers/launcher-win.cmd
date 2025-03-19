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

set JAVA_OPTS=--module-path lib --add-modules=javafx.controls,javafx.fxml

call bin\pnp_camera_server_client.bat
