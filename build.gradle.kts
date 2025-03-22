import org.gradle.internal.os.OperatingSystem

plugins {
	id("java")
	id("application")
	id("org.jetbrains.kotlin.jvm") version "2.1.10"
	id("org.openjfx.javafxplugin") version "0.1.0"  // see https://github.com/openjfx/javafx-gradle-plugin
	id("org.beryx.runtime") version "1.13.1"  // see https://badass-runtime-plugin.beryx.org/releases/latest/
	id("com.google.osdetector") version "1.7.3"  // see https://github.com/google/osdetector-gradle-plugin
	//id("org.barfuin.gradle.taskinfo") version "2.1.0"  // show Gradle Task Graph by '$ ./gradlew tiTree <TASK_NAME>'
}

repositories {
	mavenCentral()
}

// ---------------------------------------------------------------------------------------------------------------------

group = "org.ts"
version = "${project.findProperty("appVersion")}"  // read 'appVersion' from 'gradle.properties'

println("Client app version: $version")

// get value of property 'rootProject' from 'settings.gradle' file
val tmpPropProj = project.findProperty("rootProject")!! as org.gradle.api.internal.project.DefaultProject
val tmpProjName = tmpPropProj.project!!.name  // tmpPropProj.project is actually nullable (Gradle 8.10)
// read 'appImageName' from 'gradle.properties'
val tmpAppImageName = project.findProperty("appImageName")!! as String

// ---------------------------------------------------------------------------------------------------------------------

fun getOperatingSystemName(): String {
	return if (OperatingSystem.current().isMacOsX) {
			"macos"
		} else if (OperatingSystem.current().isLinux) {
			"linux"
		} else if (OperatingSystem.current().isWindows) {
			"win"
		} else {
			throw Error("Operating System not supported")
		}
}

fun getCpuArchitecture(): String {
	return when (System.getProperty("os.arch")) {
			"x86_64", "x64", "amd64" -> "x64"
			"aarch64" -> "aarch64"
			else -> throw Error("CPU Architecture not supported")
		}
}

fun getLinuxDistroType(): String {
	if (getOperatingSystemName() != "linux") {
		return "none"
	}
	val tmpRel = osdetector.release
	if (tmpRel.isLike("debian")) {
		return "debian"
	}
	if (tmpRel.isLike("redhat") || tmpRel.isLike("fedora")) {
		return "redhat"
	}
	throw Error("Linux distribution type '${tmpRel}' not supported")
}

val osName: String = getOperatingSystemName()
val cpuArch: String = getCpuArchitecture()
if (osName == "win" && cpuArch != "x64") {
	throw Error("Cannot build for ${osName}-${cpuArch}")
}
val lxDistroType: String = getLinuxDistroType()

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

	// for OpenJFX version 23 the minimum JDK version is 21
	/*sourceCompatibility = JavaVersion.VERSION_21*/
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(21))
		vendor.set(JvmVendorSpec.BELLSOFT)  // we need a JDK that includes the JavaFX SDK - like Bellsoft's 'Full' Liberica JDK
	}
}

kotlin {
	jvmToolchain(21)
}

javafx {
	version = "23.0.2"  // for OpenJFX version 23 the minimum JDK version is 21
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
		distributionBaseName = "${tmpProjName}-${osName}"  // the version will autom. be appended

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
	addOptions("--strip-debug", "--compress", "zip-6", "--no-header-files", "--no-man-pages")
	// list of required modules (instead of using modules-info.java)
	modules.set(listOf(
			"javafx.base",
			"javafx.controls",
			"javafx.fxml",
			"javafx.swing",
			"java.logging"  // for java.util.logging
		))
	imageDir.set(File(layout.buildDirectory.get().toString(), "${tmpProjName}-${osName}-${cpuArch}-${version}-launcher-w_jre"))  // default is 'image/'
	imageZip.set(File(layout.buildDirectory.get().toString(), "${tmpProjName}-${osName}-${cpuArch}-${version}-launcher-w_jre.zip"))  // default is 'image.zip'

	jpackage {
		installerOptions = listOf(
				"--description",
				project.description,  // 'description' from 'gradle.properties'
				"--copyright",
				project.findProperty("appCopyright")!! as String,  // read 'appCopyright' from 'gradle.properties'
				"--vendor",
				project.findProperty("appVendor")!! as String  // read 'appVendor' from 'gradle.properties'
			)
		imageName = when (osName) {
				"macos" -> {
					// on macOS results in e.g. 'JavaFX Camera Server Client-x64-1.2.app'
					"${tmpAppImageName}-${cpuArch}-${version}"
				}
				"linux" -> {
					// on Linux results in e.g. 'javafx_camera_server_client-linux-x64-1.2'
					//"${tmpProjName}-${osName}-${cpuArch}-${version}"
					// on Linux results in e.g. 'javafx_camera_server_client'
					tmpProjName
				}
				else -> {
					// on Win results in e.g. 'JavaFX Camera Server Client'
					tmpAppImageName
				}
			}
		installerName = when (osName) {
				"macos" -> "${tmpProjName}-${cpuArch}"  // version will be autom. appended - on macOS results in e.g. 'javafx_camera_server_client-x64-1.2.pkg'
				"linux" -> tmpProjName  // version and cpuArch will be autom. appended - on Linux results in e.g. 'javafx_camera_server_client-1.2-1.x86_64.rpm'
				else -> "${tmpProjName}-${cpuArch}-installer"  // version will be autom. appended - on Win results in e.g. 'javafx_camera_server_client-x64-installer-1.2.msi'
			}
		outputDir = "${tmpProjName}-${osName}-${cpuArch}-${version}-pack-w_jre"  // default is 'jpackage/'

		// set installerType (can be set from command line: e.g. './gradlew jpackage -PinstallerType=msi')
		installerType = project.findProperty("installerType") as String? ?:
				when (osName) {
					"macos" -> "pkg"  // or dmg
					"linux" -> { if (lxDistroType == "debian") "deb" else "rpm" }
					else -> "exe"  // or msi
				}
		println("InstallerType: $installerType")

		//
		///
		val tmpImageOptions: MutableList<String> = mutableListOf()
		val tmpInstOptions: MutableList<String> = mutableListOf()
		///
		if (osName == "macos") {
			tmpImageOptions.addAll(listOf("--mac-package-identifier", project.findProperty("macosPackageId")!! as String))
		}
		///
		when (installerType) {
			"dmg", "pkg" -> {
				/*
				tmpImageOptions.addAll(listOf("--icon", "src/main/resources/icon.icns"))
				tmpInstOptions.addAll(listOf("--license-file", "package/LICENSE-OS-Installer.txt"))
				*/
			}
			"deb", "rpm" -> {
				/*
				tmpImageOptions.addAll(listOf("--icon", "src/main/resources/icon_256x256.png"))
				*/
				tmpInstOptions.addAll(listOf(
						"--linux-menu-group", "Utility",
						"--linux-shortcut"
					))
				/*
				if (installerType == "deb") {
					tmpInstOptions.addAll(listOf("--linux-deb-maintainer", "nobody@nowhere.org"))
				} else {
					tmpInstOptions.addAll(listOf("--linux-rpm-license-type", "GPLv3"))
				}
				*/
			}
			"exe" -> {
				/*
				tmpImageOptions.addAll(listOf("--icon", "src/main/resources/icon.ico"))
				*/
				tmpInstOptions.addAll(listOf(
						// "--win-per-user-install",  // install only for current user
						// "--win-console",  // shows what Java outputs to the console
						"--win-dir-chooser",
						"--win-menu", "--win-shortcut"
					))
			}
			"msi" -> {}
			else -> throw Error("Invalid installerType: '$installerType'")
		}
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
