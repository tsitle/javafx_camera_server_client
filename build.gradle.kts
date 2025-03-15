import org.gradle.internal.os.OperatingSystem

plugins {
	id("java")
	id("application")
	id("org.jetbrains.kotlin.jvm") version "2.1.10"
	id("org.openjfx.javafxplugin") version "0.1.0"  // see https://github.com/openjfx/javafx-gradle-plugin
	//id("org.barfuin.gradle.taskinfo") version "2.1.0"  // show Gradle Task Graph by '$ ./gradlew tiTree <TASK_NAME>'
}

group = "org.ts"
version = "1.1"

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

// OpenCV version in 'build_opencv/'
val versionOpenCvCustom = "4.11.0"
// OpenCV build directory
val dpathOpenCvBuildCustom = "build_opencv/${versionOpenCvCustom}/build"

val nodotsVersionOpenCvCustom = versionOpenCvCustom.replace(".", "")

val dpathOpenCvLibCustom = "${dpathOpenCvBuildCustom}/lib"
val dpathOpenCvLibBrew = "/usr/lib/java/dont/know"  // @TODO
val dpathOpenCvLibRh = "/usr/lib/java"
val dpathOpenCvLibDeb = "/usr/lib/jni"
val dpathOpenCvLibWin = "c:/dont/know"  // @TODO

val fpathOpenCvLibCustomBuild = dpathOpenCvLibCustom + (when (osName) {
		"macos" -> "/libopencv_java${nodotsVersionOpenCvCustom}.dylib"
		"linux" -> "/libopencv_java${nodotsVersionOpenCvCustom}.so"
		else -> "/libopencv_java${nodotsVersionOpenCvCustom}.dll"  // @TODO
	})

val fpathOpenCvJarCustom = "${dpathOpenCvBuildCustom}/bin/opencv-${nodotsVersionOpenCvCustom}.jar"
val fpathOpenCvJarBrew = "/usr/lib/java/dont/know/opencv.jar"  // @TODO
val fpathOpenCvJarRh = "/usr/lib/java/opencv.jar"
val fpathOpenCvJarDeb = "/usr/share/java/opencv.jar"
val fpathOpenCvJarWin = "c:/dont/know/opencv.jar"  // @TODO

var openCvJar = ""
var openCvLibFolder = ""
if (File(fpathOpenCvLibCustomBuild).exists() && File(fpathOpenCvJarCustom).exists()) {
	println("Using custom build of OpenCV $versionOpenCvCustom")
	openCvLibFolder = dpathOpenCvLibCustom
	openCvJar = fpathOpenCvJarCustom
} else {
	if (osName == "macos") {
		if (File(dpathOpenCvLibBrew).exists() && File(fpathOpenCvJarBrew).exists()) {
			openCvLibFolder = dpathOpenCvLibBrew
			openCvJar = fpathOpenCvJarBrew
		}
	} else if (osName == "linux") {
		if (File(dpathOpenCvLibRh).exists() && File(fpathOpenCvJarRh).exists()) {
			// Redhat
			openCvLibFolder = dpathOpenCvLibRh
			openCvJar = fpathOpenCvJarRh
		} else {
			if (File(dpathOpenCvLibDeb).exists() && File(fpathOpenCvJarDeb).exists()) {
				// Debian
				openCvLibFolder = dpathOpenCvLibDeb
				openCvJar = fpathOpenCvJarDeb
			}
		}
	} else if (osName == "win") {
		if (File(dpathOpenCvLibWin).exists() && File(fpathOpenCvJarWin).exists()) {
			openCvLibFolder = dpathOpenCvLibWin
			openCvJar = fpathOpenCvJarWin
		}
	}
}

if (openCvJar.isEmpty() || openCvLibFolder.isEmpty()) {
	throw Error("Could not find OpenCV")
}

// ---------------------------------------------------------------------------------------------------------------------

tasks.compileJava.configure {
	options.encoding = "UTF-8"
}

application {
	mainClass = "org.ts.pnp_camera_server_client.ClientApplication"
	applicationDefaultJvmArgs += "-Djava.library.path=${openCvLibFolder}"
}

java {
	/*
	 * Show auto-detectable JDKs:
	 *   $ ./gradlew -q javaToolchains
	 */

	// select e JDK version that is >= as the one that was used to build the OpenCV library with
	sourceCompatibility = JavaVersion.VERSION_21
	toolchain {
		//languageVersion.set(JavaLanguageVersion.of(21))
		//vendor.set(JvmVendorSpec.BELLSOFT)
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
	implementation("com.squareup.moshi:moshi-kotlin:1.15.2")
	implementation("com.squareup.moshi:moshi:1.15.2")
	implementation("com.squareup.okhttp3:okhttp:4.12.0")

	implementation(files(openCvJar))
}

distributions {
	main {
		// get value of property 'rootProject' from 'settings.gradle' file
		val tmpPropProj = project.findProperty("rootProject")!! as org.gradle.api.internal.project.DefaultProject
		val tmpProjName = tmpPropProj.project!!.name  // tmpPropProj.project is actually nullable (Gradle 8.10)

		distributionBaseName = "${tmpProjName}-${osName}-${cpuArch}"

		contents {
			if (File(dpathOpenCvLibCustom).exists() && File(fpathOpenCvJarCustom).exists()) {
				into("lib_opencv-${osName}-${cpuArch}") {
					from(dpathOpenCvLibCustom)
				}
			}
			from("src/launch_wrappers/launcher-${osName}.sh")
		}
	}
}
