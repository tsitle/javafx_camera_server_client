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

. .build-opencv.sh
