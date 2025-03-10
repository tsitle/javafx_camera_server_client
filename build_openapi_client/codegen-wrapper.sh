#!/usr/bin/env bash

# use the same JDK as has been used to compile OpenCV
export JAVA_HOME="/Library/Java/JavaVirtualMachines/liberica-jdk-21.0.6-full-macos_x64/Contents/Home"

"${JAVA_HOME}/bin/java" -jar openapi-generator-cli.jar "$@"
