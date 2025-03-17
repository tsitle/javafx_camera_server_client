# Compiling the application from source

## Prerequisites

- JDK version >= 21
- OpenCV version >= 4.6

Gradle will automatically download the JavaFX SDK. Therefor no manual installation or configuration of JavaFX is required.

If the JDK has been installed manually you can set the environment variable `JAVA_HOME` to point to the
installation directory.

Examples:

macOS:

```
export JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-jdk-22.0.2-macos_x64/Contents/Home
```

Linux:

```
export JAVA_HOME=/opt/liberica-jdk-21.0.6-full-linux_x64
```

### Installation of dependencies on macOS

First you'll need to install [Homebrew](https://docs.brew.sh/Installation) (Package Manager for macOS).

Then install the current *OpenJDK* version:

```
$ brew install openjdk
```

Installing *OpenCV for Java* with Homebrew requires some tweaking of the
Homebrew formula for *OpenCV*. The build script mentioned below will do everything that is required automatically.  
It will also install some dependencies like *ffmpeg* and *ant*.

Build and install *OpenCV*:

```
$ cd build_opencv/macos_brew
$ ./build-opencv-java-brew.sh
```

In order to uninstall *OpenCV* again simply run

```
$ brew uninstall opencv
```

### Installation of dependencies on Red Hat-based OSs (like Fedora or Rocky Linux)

```
$ sudo dnf install java-21-openjdk-devel opencv-java
```

### Installation of dependencies on Debian-based OSs (like Ubuntu or KUbuntu)

```
$ sudo apt install openjdk-21-jdk libopencv-java
```

## Compiling the source code

```
$ ./gradlew jar
```

## Running the application from source code

```
$ ./gradlew run
```
