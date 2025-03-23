# Compiling the application from source

## Prerequisites

- JDK with bundled JavaFX, version >= 21 (e.g. [Bellsoft's Liberica Full JDK](https://bell-sw.com/pages/downloads/))
- only on MS Windows for `gradlew jpackage`: [WiX Toolset 3.x](https://github.com/wixtoolset/wix3/releases)
- only on Linux for `gradlew jpackage`: package *fakeroot*

Even though the JDK includes JavaFX, for creating a launcher without bundled JRE (e.g. `$ ./gradlew distTar`) Gradle still needs the JavaFX SDK.  
But Gradle will automatically download the JavaFX SDK and therefor no manual installation or configuration of JavaFX is required.

### Manually installing the JDK

Since the JDK cannot be provisioned automatically with Gradle - because the *foojay-resolver* plugin
doesn't provide JDKs that are bundled with JavaFX - we need to install a JDK manually.

Once the JDK has been installed you'll need to set the environment variable `JAVA_HOME` to point to the
installation directory.

#### Examples

##### macOS

For Intel Macs:

```
# to permanantly set the environment variable for the current user (will become available in next Terminal session):
$ echo 'export JAVA_HOME=/Library/Java/JavaVirtualMachines/liberica-jdk-21.0.6-full-macos_x64/Contents/Home' >> ~/.zprofile

# to set the environment variable only for the current terminal session:
$ export JAVA_HOME=/Library/Java/JavaVirtualMachines/liberica-jdk-21.0.6-full-macos_x64/Contents/Home
```

For newer Macs with an ARM processor replace `x64` with `aarch64` in the above commands.

##### Linux

```
# to permanantly set the environment variable for the current user (will become available in next Terminal session):
$ echo 'export JAVA_HOME=/opt/liberica-jdk-21.0.6-full-linux_x64' >> ~/.profile

# to set the environment variable only for the current terminal session:
$ export JAVA_HOME=/opt/liberica-jdk-21.0.6-full-linux_x64
```

For PCs with an ARM processor replace `x64` with `aarch64` in the above commands.

##### MS Windows

```
# to permanantly set the environment variable for the current user (will become available in next Terminal session):
> setx JAVA_HOME "C:\Program Files\Java\liberica-jdk-21.0.6-full-win_x64"

# to set the environment variable only for the current terminal session:
> set JAVA_HOME=C:\Program Files\Java\liberica-jdk-21.0.6-full-win_x64
```

### WiX Toolset

On MS Windows the *WiX Toolset v3.x* is required for creating installer packages with `gradlew jpackage`.

- unzip to e.g. `c:\java\wix_toolset-3.14-binaries-win-x64`
- add to environment variable `PATH`: e.g. `> setx PATH "%PATH%;c:\java\wix_toolset-3.14-binaries-win-x64"`

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

## Creating a new launcher package without bundled JRE

macOS / Linux:

```
$ ./gradlew distTar
```

MS Windows:

```
> gradlew distZip
```

## Creating a new launcher package with bundled JRE

macOS / Linux:

```
$ ./gradlew runtimeTar
```

MS Windows:

```
> gradlew runtimeZip
```

## Creating a new portable app package with bundled JRE

macOS / Linux:

```
$ ./gradlew jpackageImageTar
```

MS Windows:

```
> gradlew jpackageImageZip
```

## Creating a new installer package with bundled JRE

macOS / Linux:

```
$ ./gradlew jpackage
```

MS Windows:

```
> gradlew jpackage
```
