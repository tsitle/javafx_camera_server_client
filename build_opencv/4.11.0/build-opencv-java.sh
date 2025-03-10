#!/usr/bin/env bash

#
# by TS, Aug 2023
#

COMPILE_WITH_GUI=false
COMPILE_WITH_FFMPEG=true
COMPILE_WITH_GSTREAMER=false
COMPILE_WITH_JAVA=true
NEED_LIBCAMERA=false  # only Raspberry Pi
COMPILE_STATIC_LIBS=true  # mainly for Java

# use a specific JDK to compile OpenCV such that it will be compatible with the Kotlin JVM
export JAVA_HOME="/Library/Java/JavaVirtualMachines/liberica-jdk-21.0.6-full-macos_x64/Contents/Home"

. .build-opencv.sh
