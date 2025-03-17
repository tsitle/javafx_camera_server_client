# Compiling the application from source

## Prerequisites

- JDK version >= 21
- OpenCV version >= 4.6

Gradle will automatically download the JavaFX SDK. Therefor no manual installation or configuration of JavaFX is required.

### Manually installed JDK

If the JDK has been installed manually you can set the environment variable `JAVA_HOME` to point to the
installation directory.

#### Examples

##### macOS

```
$ export JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-jdk-22.0.2-macos_x64/Contents/Home
```

##### Linux

```
$ export JAVA_HOME=/opt/liberica-jdk-21.0.6-full-linux_x64
```

##### MS Windows

```
# to permanantly set the environment variable for the current user (will become available in next Terminal session):
> setx JAVA_HOME "C:\Program Files\Java\openjdk-22.0.2-win_x64"

# to set the environment variable only for the current terminal session:
> set JAVA_HOME=C:\Program Files\Java\openjdk-22.0.2-win_x64
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

### Installation of dependencies on MS Windows

- download *OpenCV* for Windows from [https://sourceforge.net/projects/opencvlibrary/files/](https://sourceforge.net/projects/opencvlibrary/files/)
- run the installer and choose a target directory 
- set the `OpenCV_DIR` environment variable to point to the installation directory:  
	In a terminal run  
	- to permanently set the env var:  
		e.g. `> setx OpenCV_DIR "C:\opencv\build"` and then close the terminal and open a new one.  
	- to temporarily set the env var:  
		e.g. `> set OpenCV_DIR=C:\opencv\build`
- add `%OpenCV_DIR%\x64\vc16\bin` to environment variable `PATH`:  
	In a terminal run
	- to permanently set the env var:  
		e.g. `> setx PATH "%OpenCV_DIR%\x64\vc16\bin;%PATH%"` and then close the terminal and open a new one.
	- to temporarily set the env var:  
		e.g. `> set PATH=%OpenCV_DIR%\x64\vc16\bin;%PATH%`

## Gradle's JDK auto-detection

Let *Gradle* show all JDKs/JREs that it can auto-detect:

macOS / Linux:

```
$ ./gradlew -q javaToolchains
```

MS Windows:

```
> gradlew -q javaToolchains
```

## Compiling the source code

macOS / Linux:

```
$ ./gradlew jar
```

MS Windows:

```
> gradlew jar
```

## Running the application from source code

macOS / Linux:

```
$ ./gradlew run
```

MS Windows:

```
> gradlew run
```

## Creating a new distribution package

macOS / Linux:

```
$ ./gradlew distTar
```

MS Windows:

```
> gradlew distZip
```
