import org.gradle.internal.os.OperatingSystem

plugins {
	id("java")
	id("application")
	id("org.jetbrains.kotlin.jvm") version "2.1.10"
	id("org.openjfx.javafxplugin") version "0.1.0"  // see https://github.com/openjfx/javafx-gradle-plugin
	id("org.beryx.runtime") version "1.13.1"  // see https://badass-runtime-plugin.beryx.org/releases/latest/
	//id("org.barfuin.gradle.taskinfo") version "2.1.0"  // show Gradle Task Graph by '$ ./gradlew tiTree <TASK_NAME>'
}

group = "org.ts"
version = "${project.findProperty("appVersion")}"  // read 'appVersion' from 'gradle.properties'

println("Client app version: $version")

repositories {
	mavenCentral()
}

// ---------------------------------------------------------------------------------------------------------------------

val osName: String = if (OperatingSystem.current().isMacOsX) {
		"macos"
	} else if (OperatingSystem.current().isLinux) {
		"linux"
	} else if (OperatingSystem.current().isWindows) {
		"win"
	} else {
		throw Error("Operating System not supported")
	}
val cpuArch: String = when (System.getProperty("os.arch")) {
		"x86_64", "amd64" -> "x64"
		"aarch64" -> "aarch64"
		else -> throw Error("CPU Architecture not supported")
	}
if (osName == "win" && cpuArch != "x64") {
	throw Error("Cannot build for ${osName}-${cpuArch}")
}

println("Host: ${osName}-${cpuArch}")

// ---------------------------------------------------------------------------------------------------------------------

tasks.compileJava.configure {
	options.encoding = "UTF-8"
}

application {
	mainClass = "org.ts.javafx_camera_server_client.ClientApplication"
	applicationDefaultJvmArgs += "-DappVersion=${version}"
}

java {
	/*
	 * Show auto-detectable JDKs:
	 *   $ ./gradlew -q javaToolchains
	 */

	// select e JDK version that is >= as the one that was used to build the OpenCV library with
	/*sourceCompatibility = JavaVersion.VERSION_21*/
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(21))
		vendor.set(JvmVendorSpec.BELLSOFT)  // we need a JDK that includes the JavaFX SDK
	}
}

kotlin {
	jvmToolchain(21)
}

javafx {
	version = "23.0.2"  // for OpenJFX 23 the minimum JDK is 21
	modules("javafx.controls", "javafx.fxml", "javafx.swing")
}

dependencies {
	implementation("org.kordamp.bootstrapfx:bootstrapfx-core:0.4.0")  // see https://github.com/kordamp/bootstrapfx

	// for OpenAPI generated client:
	implementation("com.squareup.moshi:moshi-kotlin:1.15.2") {  // v1.13.0 through 1.15.2 have a "Split package" problem
		//exclude(group="com.squareup.moshi", module="moshi")  // this doesn't seem to do anything
	}
	implementation("com.squareup.moshi:moshi:1.15.2") {  // v1.13.0 through 1.15.2 have a "Split package" problem
		//exclude(group="com.squareup.moshi", module="moshi")  // this doesn't seem to do anything
	}
	implementation("com.squareup.okhttp3:okhttp:4.12.0")
}

distributions {
	main {
		// get value of property 'rootProject' from 'settings.gradle' file
		val tmpPropProj = project.findProperty("rootProject")!! as org.gradle.api.internal.project.DefaultProject
		val tmpProjName = tmpPropProj.project!!.name  // tmpPropProj.project is actually nullable (Gradle 8.10)

		distributionBaseName = "${tmpProjName}-${osName}-${cpuArch}"

		contents {
			if (osName == "win") {
				from("src/launch_wrappers/launcher-${osName}.cmd")
			} else {
				from("src/launch_wrappers/launcher-${osName}.sh")
			}
		}
	}
}

// org.beryx.runtime: Creates an image containing the application, a custom JRE, and appropriate start scripts
runtime {
	// some default options
	addOptions("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages")
	// list of required modules (instead of using modules-info.java)
	modules.set(listOf(
		"javafx.base",
		"javafx.controls",
		"javafx.fxml",
		"javafx.swing",
		"java.logging"  // for java.util.logging
	))

	jpackage {
		val tmpAppImageName = project.findProperty("appImageName")!! as String  // read 'appImageName' from 'gradle.properties'

		installerOptions = listOf(
				"--name",
				"${tmpAppImageName}-installer",  // installer name (the version number will be appended automatically)
				"--description",
				project.description,  // 'description' from 'gradle.properties'
				"--copyright",
				project.findProperty("appCopyright")!! as String,  // read 'appCopyright' from 'gradle.properties'
				"--vendor",
				project.findProperty("appVendor")!! as String  // read 'appVendor' from 'gradle.properties'
			)
		imageName = tmpAppImageName

		// set installerType (can be set from command line: e.g. './gradlew jpackage -PinstallerType=msi')
		installerType = project.findProperty("installerType") as String? ?:
				when (osName) {
					"macos" -> "pkg"  // dmg
					"linux" -> "deb"  // rpm
					else -> "exe"  // msi
				}
		println("installerType=$installerType")

		//
		///
		val tmpImageOptions: MutableList<String> = mutableListOf()
		val tmpInstOptions: MutableList<String> = mutableListOf()
		///
		if (osName == "macos") {
			tmpImageOptions.addAll(listOf("--mac-package-identifier", tmpAppImageName))
		}
		///
		/*
		when (installerType) {
			"pkg" -> {
				tmpImageOptions.addAll(listOf("--icon", "src/main/resources/icon.icns"))
				tmpInstOptions.addAll(listOf("--license-file", "package/LICENSE-OS-Installer.txt"))
			}
			"dmg" -> {
			}
			"deb", "rpm" -> {
				tmpImageOptions.addAll(listOf("--icon", "src/main/resources/icon_256x256.png"))
				tmpInstOptions.addAll(listOf(
						"--linux-menu-group", "Utility",
						"--linux-shortcut"
					))
				if (installerType == "deb") {
					tmpInstOptions.addAll(listOf("--linux-deb-maintainer", "nobody@nowhere.org"))
				} else {
					tmpInstOptions.addAll(listOf("--linux-rpm-license-type", "GPLv3"))
				}
			}
			"exe" -> {
				tmpImageOptions.addAll(listOf("--icon", "src/main/resources/icon.ico"))
				tmpInstOptions.addAll(listOf(
						// "--win-per-user-install",  // install only for current user
						// "--win-console",  // shows what Java outputs to the console
						"--win-dir-chooser",
						"--win-menu", "--win-shortcut"
					))
			}
			else -> {  // msi
			}
		}
		*/
		///
		imageOptions = tmpImageOptions
		installerOptions = tmpInstOptions
	}
}

// org.beryx.runtime: Creates a zip archive of the custom runtime image
tasks.runtimeZip {
}

// org.beryx.runtime: Uses the jpackage tool to create a platform-specific application image
tasks.jpackageImage {
}

// org.beryx.runtime: Uses the jpackage tool to create a platform-specific application installer
tasks.jpackage {
	// Could be used for pre-checks;
	// e.g., are certain command line arguments defined?
	doFirst {
		// project.findProperty("installerOs")
		//    (example: -PinstallerOs=mac)
		// project.getProperty("installerType")!!  // throws exception if its missing
	}
}
