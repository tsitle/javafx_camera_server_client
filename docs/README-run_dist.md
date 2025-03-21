# Running the application from a precompiled distribution

## Prerequisites

- JRE or JDK version >= 21
- Linux only: OpenCV version >= 4.6 (for macOS and MS Windows the distribution package comes bundled with OpenCV)
- macOS only: ffmpeg

### Manually installed JRE or JDK

If the JRE/JDK has been installed manually you can set the environment variable `JAVA_HOME` to point to the
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

Then install *ffmpeg*:

```
$ brew install ffmpeg
```

### Installation of dependencies on Red Hat-based OSs (like Fedora or Rocky Linux)

With the JRE (Java Runtime Environment):

```
$ sudo dnf install java-21-openjdk opencv-java
```

or with the JDK (Java Development Kit):

```
$ sudo dnf install java-21-openjdk-devel opencv-java
```

### Installation of dependencies on Debian-based OSs (like Ubuntu or KUbuntu)

With the JRE (Java Runtime Environment):

```
$ sudo apt install openjdk-21-jre libopencv-java
```

or with the JDK (Java Development Kit):

```
$ sudo apt install openjdk-21-jdk libopencv-java
```

## Running the application

Download the distribution package for your OS from [https://github.com/tsitle/javafx_camera_server_client/releases](https://github.com/tsitle/javafx_camera_server_client/releases).  
Then extract the downloaded archive.  
From the extracted application directory run:

```
$ ./launcher-macos.sh

or

$ ./launcher-linux.sh

or

> launcher-win.cmd
```
