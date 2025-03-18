import org.gradle.internal.classpath.Instrumented.systemProperty
import org.gradle.internal.os.OperatingSystem

plugins {
	id("java")
	id("application")
	id("org.jetbrains.kotlin.jvm") version "2.1.10"
	id("org.openjfx.javafxplugin") version "0.1.0"  // see https://github.com/openjfx/javafx-gradle-plugin
	//id("org.barfuin.gradle.taskinfo") version "2.1.0"  // show Gradle Task Graph by '$ ./gradlew tiTree <TASK_NAME>'
}

group = "org.ts"
version = "${project.findProperty("appVersion")}"  // read version from 'gradle.properties'

println("Client app version: ${version}")

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

val winEnvOpenCvDir: String? = if (osName == "win") {
		System.getenv("OpenCV_DIR")
	} else {
		""
	}
if (osName == "win" && winEnvOpenCvDir != null && winEnvOpenCvDir.contains("\"")) {
	throw Error("Env var 'OpenCV_DIR' may not contain quotes: '${winEnvOpenCvDir}'")
}

val dpathOpenCvLibCustom = "${dpathOpenCvBuildCustom}/lib"
val dpathOpenCvLibBrew = "/usr/local/opt/opencv/share/java/opencv4"  // (symlink to '/usr/local/Cellar/opencv/${versionOpenCvCustom}_1/share/java/opencv4')
val dpathOpenCvLibRh = "/usr/lib/java"
val dpathOpenCvLibDeb = "/usr/lib/jni"
val dpathOpenCvLibWin = "${winEnvOpenCvDir}\\java\\${cpuArch}"
val dpathOpenCvLibFfmpegWin = "${winEnvOpenCvDir}\\${cpuArch}\\vc16\\bin"  // directory where 'opencv_videoio_ffmpeg<VERSION>_64.dll' is. Needs to be in env var 'PATH'

val fpathOpenCvLibCustomBuild = dpathOpenCvLibCustom + (when (osName) {
		"macos" -> "/libopencv_java${nodotsVersionOpenCvCustom}.dylib"
		"linux" -> "/libopencv_java${nodotsVersionOpenCvCustom}.so"
		else -> "/libopencv_java${nodotsVersionOpenCvCustom}.dll"
	})

val fpathOpenCvJarCustom = "${dpathOpenCvBuildCustom}/bin/opencv-${nodotsVersionOpenCvCustom}.jar"
val fpathOpenCvJarBrew = "/usr/local/opt/opencv/share/java/opencv4/opencv-${nodotsVersionOpenCvCustom}.jar"
val fpathOpenCvJarRh = "/usr/lib/java/opencv.jar"
val fpathOpenCvJarDeb = "/usr/share/java/opencv.jar"
val fpathOpenCvJarWin = "${winEnvOpenCvDir}\\java\\opencv-${nodotsVersionOpenCvCustom}.jar"

var openCvJar = ""
var openCvLibFolder = ""
if (File(fpathOpenCvLibCustomBuild).exists() && File(fpathOpenCvJarCustom).exists()) {
	println("Using custom build of OpenCV $versionOpenCvCustom")
	openCvLibFolder = dpathOpenCvLibCustom
	openCvJar = fpathOpenCvJarCustom
} else {
	if (osName == "macos") {
		if (File(dpathOpenCvLibBrew).exists() && File(fpathOpenCvJarBrew).exists()) {
			println("Using system packaged version of OpenCV (BREW)")
			System.err.println("Don't use the system packaged version of OpenCV for a distribution release on macOS !")
			openCvLibFolder = dpathOpenCvLibBrew
			openCvJar = fpathOpenCvJarBrew
		} else {
			if (! File(dpathOpenCvLibBrew).exists()) {
				System.err.println("Could not find '${dpathOpenCvLibBrew}/' (BREW)")
			}
			if (! File(fpathOpenCvJarBrew).exists()) {
				System.err.println("Could not find '${fpathOpenCvJarBrew}' (BREW)")
			}
		}
	} else if (osName == "linux") {
		if (File(dpathOpenCvLibRh).exists() && File(fpathOpenCvJarRh).exists()) {
			// Redhat
			println("Using system packaged version of OpenCV (RH)")
			openCvLibFolder = dpathOpenCvLibRh
			openCvJar = fpathOpenCvJarRh
		} else if (File(dpathOpenCvLibDeb).exists() && File(fpathOpenCvJarDeb).exists()) {
			// Debian
			println("Using system packaged version of OpenCV (DEB)")
			openCvLibFolder = dpathOpenCvLibDeb
			openCvJar = fpathOpenCvJarDeb
		} else {
			if (! File(dpathOpenCvLibRh).exists()) {
				System.err.println("Could not find '${dpathOpenCvLibRh}/' (RH)")
			}
			if (! File(fpathOpenCvJarRh).exists()) {
				System.err.println("Could not find '${fpathOpenCvJarRh}' (RH)")
			}
			if (! File(dpathOpenCvLibDeb).exists()) {
				System.err.println("Could not find '${dpathOpenCvLibDeb}/' (DEB)")
			}
			if (! File(fpathOpenCvJarDeb).exists()) {
				System.err.println("Could not find '${fpathOpenCvJarDeb}' (DEB)")
			}
		}
	} else if (osName == "win") {
		if (winEnvOpenCvDir == null) {
			throw Error("Missing env var 'OpenCV_DIR'")
		}
		if (File(dpathOpenCvLibWin).exists() && File(dpathOpenCvLibFfmpegWin).exists() && File(fpathOpenCvJarWin).exists()) {
			println("Using system packaged version of OpenCV (WIN)")
			openCvLibFolder = dpathOpenCvLibWin
			openCvJar = fpathOpenCvJarWin
		} else {
			if (! File(dpathOpenCvLibWin).exists()) {
				System.err.println("Could not find '${dpathOpenCvLibWin}\\' (WIN)")
			}
			if (! File(dpathOpenCvLibFfmpegWin).exists()) {
				System.err.println("Could not find '${dpathOpenCvLibFfmpegWin}\\' (WIN)")
			}
			if (! File(fpathOpenCvJarWin).exists()) {
				System.err.println("Could not find '${fpathOpenCvJarWin}' (WIN)")
			}
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
	applicationDefaultJvmArgs += "-DappVersion=${version}"
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

tasks.run {
	if (osName == "win") {
		if (System.getenv("PATH")?.contains(dpathOpenCvLibFfmpegWin) != true) {
			throw Error("The directory '${dpathOpenCvLibFfmpegWin}' needs to be included in the env var 'PATH' !")
		}
	}
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
					from(dpathOpenCvLibCustom, fpathOpenCvJarCustom)
					include("*.dylib", "*.so", "*.jar")
				}
			} else if (osName == "win") {
				into("lib_opencv-${osName}-${cpuArch}") {
					from(dpathOpenCvLibWin, dpathOpenCvLibFfmpegWin, fpathOpenCvJarWin)
					include("opencv_java*.dll", "opencv_*_ffmpeg*.dll", "*.jar")
				}
			}
			if (osName == "win") {
				from("src/launch_wrappers/launcher-${osName}.cmd")
			} else {
				from("src/launch_wrappers/launcher-${osName}.sh")
			}
		}
	}
}
