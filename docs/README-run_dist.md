# Running the application from a precompiled distribution

## Prerequisites

When using a distribution package that comes with a bundled JRE there are no further requirements.  
This includes the installer and portable packages.

When using a distribution package without a bundled JRE you'll need:

- JRE or JDK version >= 21

### Manually installed JRE or JDK

If the JRE/JDK has been installed manually you can set the environment variable `JAVA_HOME` to point to the
installation directory.

#### Examples

##### macOS

For Intel Macs:

```
# to permanantly set the environment variable for the current user (will become available in next Terminal session):
$ echo 'export JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk-22.0.2-macos_x64/Contents/Home' >> ~/.zprofile

# to set the environment variable only for the current terminal session:
$ export JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk-22.0.2-macos_x64/Contents/Home
```

For newer Macs with an ARM processor replace `x64` with `aarch64` in the above commands.

##### Linux

```
# to permanantly set the environment variable for the current user (will become available in next Terminal session):
$ echo 'export JAVA_HOME=/opt/openjdk-22.0.2-linux_x64' >> ~/.profile

# to set the environment variable only for the current terminal session:
$ export JAVA_HOME=/opt/openjdk-22.0.2-linux_x64
```

For PCs with an ARM processor replace `x64` with `aarch64` in the above commands.

##### MS Windows

```
# to permanantly set the environment variable for the current user (will become available in next Terminal session):
> setx JAVA_HOME "C:\Program Files\Java\openjdk-22.0.2-win_x64"

# to set the environment variable only for the current terminal session:
> set JAVA_HOME=C:\Program Files\Java\openjdk-22.0.2-win_x64
```

### Installation of dependencies on macOS

Install a complete JDK (Java Development Kit) with [Homebrew](https://docs.brew.sh/Installation):

```
$ brew install openjdk

or

$ brew install openjdk@21
```

### Installation of dependencies on Red Hat-based OSs (like Fedora or Rocky Linux)

Install a JRE (Java Runtime Environment):

```
$ sudo dnf install java-21-openjdk
```

or install a complete JDK (Java Development Kit):

```
$ sudo dnf install java-21-openjdk-devel
```

### Installation of dependencies on Debian-based OSs (like Ubuntu or KUbuntu)

Install a JRE (Java Runtime Environment):

```
$ sudo apt install openjdk-21-jre
```

or install a complete JDK (Java Development Kit):

```
$ sudo apt install openjdk-21-jdk
```

### Installation of dependencies on MS Windows

Download and extract something like OpenJDK from [https://jdk.java.net/24/](https://jdk.java.net/24/) to your harddrive.  
Then use `JAVA_HOME` as described [above](#Manually-installed-JRE-or-JDK).

## Running the application

Download the distribution package for your OS from [https://github.com/tsitle/javafx_camera_server_client/releases](https://github.com/tsitle/javafx_camera_server_client/releases).  

### Launchers (without bundled JRE)

Extract the downloaded archive.  
From the extracted application directory run:

```
$ ./launcher-macos.sh

or

$ ./launcher-linux.sh

or

> launcher-win.cmd
```

### Installers

The installers all come bundled with a JRE.

#### macOS

Simply execute the `javafx_camera_server_client-macos-xxx-x.x.pkg` installer to install the application.  
After the installation the app should show up in the Launchpad.

##### Linux

RedHat-based Linux distro (like Fedora or Rocky Linux):

```
$ sudo rpm -i javafx_camera_server_client-linux-xxx-x.x.rpm
```

Debian-based Linux distro (like Ubuntu):

```
$ sudo dpkg -i javafx_camera_server_client-linux-xxx-x.x.deb
```

##### MS Windows

Simply execute the `javafx_camera_server_client-win-x64-x.x-installer.exe` installer to install the application.  
After the installation a shortcut for the app should show up on the Windows desktop.
