/*
 * Interesting article on how to write a Gradle build file for
 * modular and non-modular Java projects:
 *   https://foojay.io/today/building-javafx-with-gradle/
 */

plugins {
	id("java")
	id("application")
	id("org.jetbrains.kotlin.jvm") version "2.1.10"
	id("org.openjfx.javafxplugin") version "0.1.0"  // see https://github.com/openjfx/javafx-gradle-plugin
}

group = "org.ts"
version = "1.0-SNAPSHOT"

repositories {
	mavenCentral()
}

//val javaJdkPathMac = "/Library/Java/JavaVirtualMachines/liberica-jdk-21.0.6-full-macos_x64/Contents/Home"
//val javaFxSdkPathMac = "/Library/Java/Extensions/javafx-sdk-23.0.2-osx_x64/lib"
//val javaFxJmodsPathMac = "/Library/Java/Extensions/javafx-jmods-23.0.2-osx_x64"

val openCvJar = "build_opencv/4.11.0/build/bin/opencv-4110.jar"
val openCvLibFolder = "build_opencv/4.11.0/build/lib/"


tasks.compileJava.configure {
	options.encoding = "UTF-8"
}

application {
	mainClass = "org.ts.pnp_camera_server_client.ClientApplication"
	applicationDefaultJvmArgs += "-Djava.library.path=$openCvLibFolder"
}

java {
	toolchain {
		// select same JDK version as was used to build the OpenCV library with
		languageVersion.set(JavaLanguageVersion.of(21))
		vendor.set(JvmVendorSpec.BELLSOFT)
	}
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
