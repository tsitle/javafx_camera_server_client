#!/usr/bin/env bash

# use the same JDK as has been used to compile OpenCV
#export JAVA_HOME="/usr/local/opt/openjdk/libexec/openjdk.jdk/Contents/Home"

export JAVA_HOME="/Library/Java/JavaVirtualMachines/graalvm-svm-gluon-java17-22.1.0-macos_x64/Contents/Home"

java -jar openapi-generator-cli.jar "$@"
