@REM ----------------------------------------------------------------------------
@REM Launcher for the Camera Server Client app.
@REM
@REM Requires that either env var JAVA_HOME is set or '> java' can find a Java executable (JDK or JRE).
@REM The JDK/JRE version needs to be >= 21
@REM
@REM by TS, Mar 2025
@REM ----------------------------------------------------------------------------

set LCFG_JAVA_MIN_LANG_LEVEL=21

set LCFG_OS_TYPE="win"

@REM -------------------------------------------------------------------------------------------------------------------

@echo off
@REM set title of command window
title %0

@setlocal

@REM -------------------------------------------------------------------------------------------------------------------

@REM examples for JAVA_HOME under Windows:
@REM set JAVA_HOME=c:/java_sdks/openjdk-21.0.6-win_x64

@REM -------------------------------------------------------------------------------------------------------------------

set JAVA_OPTS="-Djava.library.path=lib_opencv-win-x64 --module-path lib --add-modules=javafx.controls,javafx.fxml"

call bin\pnp_camera_server_client.bat
