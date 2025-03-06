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
#export JAVA_HOME="/usr/local/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
export JAVA_HOME="/Library/Java/JavaVirtualMachines/graalvm-svm-gluon-java17-22.1.0-macos_x64/Contents/Home"

. .build-opencv.sh
