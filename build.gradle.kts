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

// get value of property 'rootProject' from 'settings.gradle' file
val tmpPropProj = project.findProperty("rootProject")!! as org.gradle.api.internal.project.DefaultProject
val propProjName = tmpPropProj.project!!.name  // tmpPropProj.project is actually nullable (Gradle 8.10)
// read 'appImageName' from 'gradle.properties'
val propAppImageName = project.findProperty("appImageName")!! as String

// set installerType (can be set from command line: e.g. './gradlew jpackage -PinstallerType=msi')
val propInstallerType = project.findProperty("installerType") as String? ?:
	when (osName) {
		"macos" -> "pkg"  // or dmg
		"linux" -> { if (lxDistroType == "debian") "deb" else "rpm" }
		else -> "exe"  // or msi
	}

// ---------------------------------------------------------------------------------------------------------------------

// jpackage output directory
val confJpackOutputDir = "${propProjName}-${osName}-${cpuArch}-${version}-pack-w_jre"  // default is 'jpackage/'

// output directory for distribution files (launchers and installers)
val confDistPreOutputDir = "distPre"

// ---------------------------------------------------------------------------------------------------------------------

tasks.compileJava.configure {
	options.encoding = "UTF-8"
}

application {
	mainClass = "org.ts.javafx_camera_server_client.ClientApplication"
	applicationDefaultJvmArgs += "-DappVersion=${version}"
	//applicationDefaultJvmArgs += "-Djdk.gtk.version=2"  // starting JavaFX 11 GTK 3 is the default
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
		distributionBaseName = "${propProjName}-${osName}"  // the version will autom. be appended

		contents {
			if (osName == "win") {
				from("src/launch_wrappers/launcher-${osName}.cmd")
			} else {
				from("src/launch_wrappers/launcher-${osName}.sh").rename("launcher-${osName}.sh", "launcher-${osName}")
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
	imageDir.set(File(layout.buildDirectory.get().toString(), "${propProjName}-${osName}-${cpuArch}-${version}-launcher-w_jre"))  // default is 'image/'
	imageZip.set(File(layout.buildDirectory.get().toString(), "${propProjName}-${osName}-${cpuArch}-${version}-launcher-w_jre.zip"))  // default is 'image.zip'

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
					"${propAppImageName}-${cpuArch}-${version}"
				}
				"linux" -> {
					// on Linux results in e.g. 'javafx_camera_server_client-linux-x64-1.2'
					//"${propProjName}-${osName}-${cpuArch}-${version}"
					// on Linux results in e.g. 'javafx_camera_server_client'
					propProjName
				}
				else -> {
					// on Win results in e.g. 'JavaFX Camera Server Client'
					propAppImageName
				}
			}
		installerType = propInstallerType
		installerName = when (osName) {
				"macos" -> "${propProjName}-${cpuArch}"  // version will be autom. appended - on macOS results in e.g. 'javafx_camera_server_client-x64-1.2.pkg'
				"linux" -> propProjName  // version and cpuArch will be autom. appended - on Linux results in e.g. 'javafx_camera_server_client-1.2-1.x86_64.rpm'
				else -> "${propProjName}-${cpuArch}-installer"  // version will be autom. appended - on Win results in e.g. 'javafx_camera_server_client-x64-installer-1.2.msi'
			}
		outputDir = confJpackOutputDir

		//
		///
		val tmpImageOptions: MutableList<String> = mutableListOf()
		val tmpInstOptions: MutableList<String> = mutableListOf()
		///
		if (osName == "macos") {
			tmpImageOptions.addAll(listOf("--mac-package-identifier", project.findProperty("macosPackageId")!! as String))
		} else if (osName == "linux") {
			resourceDir = File("src/main/resources/jpackage-${lxDistroType}")
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
	doFirst {
		if (osName != "win") {
			throw Exception("Do not use 'runtimeZip' under macOS/Linux. Use 'distTar' instead")
		}
	}

	doLast {
		val tmpFileObjOrg: File = imageZip.asFile
		if (! tmpFileObjOrg.exists()) {
			throw Exception("File '${tmpFileObjOrg}' not found")
		}
		val tmpFileObjTrg = File(confDistPreOutputDir, tmpFileObjOrg.name)
		println("Renaming '${tmpFileObjOrg}' to '${tmpFileObjTrg}'")
		tmpFileObjOrg.renameTo(tmpFileObjTrg)
	}
}

// org.beryx.runtime: Uses the jpackage tool to create a platform-specific application image
tasks.jpackageImage {
}

tasks {
	register<Copy>("jpackageImageTarZipPreCopy")
	named<Copy>("jpackageImageTarZipPreCopy") {
		dependsOn(":jpackageImage")

		/*
		 * We need to copy the app dir into a new subdir first,
		 * otherwise the resulting TAR-ball might contain the installer as well
		 */
		val tmpAppDir = jpackageImage.get().jpackageData.imageName + (if (osName == "macos") ".app" else "")
		val tmpSourceDirObj = File(layout.buildDirectory.dir(jpackageImage.get().jpackageData.outputDir).get().toString(), tmpAppDir)
		val tmpPreCopyDir = layout.buildDirectory.dir("jpackageImageTarZipPreCopy").get().toString()
		//println("Copying app image from '${tmpSourceDirObj}' to '${tmpPreCopyDir}/'...")
		from(tmpSourceDirObj)
		into(File(tmpPreCopyDir, tmpAppDir))
	}

	// --------------------------------------------------------

	register<DefaultTask>("jpackageImageTarRename")
	named<DefaultTask>("jpackageImageTarRename") {
		dependsOn(":jpackageImageTarZipPreCopy")

		doFirst {
			if (osName == "win") {
				throw Exception("Do not use 'jpackageImageTar' under MS Windows. Use 'jpackageImageZip' instead")
			}
		}

		doLast {
			if (osName == "linux") {
				val tmpPreCopyDir = layout.buildDirectory.dir("jpackageImageTarZipPreCopy").get().toString()
				val tmpBaseFilenTrg = "${propProjName}-${osName}-${cpuArch}-${version}-portable"

				if (! File(tmpPreCopyDir, propProjName).exists()) {
					throw Exception("Directory '${tmpPreCopyDir}/${propProjName}' does not exist")
				}
				if (! File(tmpPreCopyDir, propProjName).renameTo(
							File(tmpPreCopyDir, tmpBaseFilenTrg)
						)) {
					throw Exception("Could not rename '${tmpPreCopyDir}/${propProjName}'")
				}
			}
		}
	}

	register<Tar>("jpackageImageTarMain")
	named<Tar>("jpackageImageTarMain") {
		dependsOn(":jpackageImageTarRename")

		doFirst {
			if (osName == "win") {
				throw Exception("Do not use 'jpackageImageTar' under MS Windows. Use 'jpackageImageZip' instead")
			}
		}

		val tmpPreCopyDir = layout.buildDirectory.dir("jpackageImageTarZipPreCopy").get().toString()
		val tmpBaseFilenTrg = "${propProjName}-${osName}-${cpuArch}-${version}-portable"
		val tmpFilenTrg = "${tmpBaseFilenTrg}.tgz"
		println("Creating '${confDistPreOutputDir}/${tmpFilenTrg}'...")
		from(File(tmpPreCopyDir))
		archiveFileName.set(tmpFilenTrg)
		destinationDirectory.set(File(confDistPreOutputDir))
		compression = Compression.GZIP
	}

	register<DefaultTask>("jpackageImageTarCleanUp")
	named<DefaultTask>("jpackageImageTarCleanUp") {
		dependsOn(":jpackageImageTarMain")

		doFirst {
			if (osName == "win") {
				throw Exception("Do not use 'jpackageImageTar' under MS Windows. Use 'jpackageImageZip' instead")
			}
		}

		doLast {
			val tmpPreCopyDir = layout.buildDirectory.dir("jpackageImageTarZipPreCopy").get().toString()
			if (! File(tmpPreCopyDir).exists()) {
				throw Exception("Directory '${tmpPreCopyDir}' does not exist")
			}
			File(tmpPreCopyDir).deleteRecursively()
		}
	}

	register<DefaultTask>("jpackageImageTar")
	named<DefaultTask>("jpackageImageTar") {
		dependsOn(":jpackageImageTarCleanUp")

		doFirst {
			if (osName == "win") {
				throw Exception("Do not use 'jpackageImageTar' under MS Windows. Use 'jpackageImageZip' instead")
			}
		}
	}

	// --------------------------------------------------------

	register<DefaultTask>("jpackageImageZipRename")
	named<DefaultTask>("jpackageImageZipRename") {
		dependsOn(":jpackageImageTarZipPreCopy")

		doFirst {
			if (osName != "win") {
				throw Exception("Do not use 'jpackageImageZip' under macOS/Linux. Use 'jpackageImageTar' instead")
			}
		}

		doLast {
			val tmpPreCopyDir = layout.buildDirectory.dir("jpackageImageTarZipPreCopy").get().toString()
			val tmpBaseFilenTrg = "${propProjName}-${osName}-${cpuArch}-${version}-portable"

			if (! File(tmpPreCopyDir, propAppImageName).exists()) {
				throw Exception("Directory '${tmpPreCopyDir}/${propAppImageName}' does not exist")
			}
			if (! File(tmpPreCopyDir, propAppImageName).renameTo(
							File(tmpPreCopyDir, tmpBaseFilenTrg)
					)) {
				throw Exception("Could not rename '${tmpPreCopyDir}/${propAppImageName}'")
			}
		}
	}

	register<Zip>("jpackageImageZipMain")
	named<Zip>("jpackageImageZipMain") {
		dependsOn(":jpackageImageZipRename")

		doFirst {
			if (osName != "win") {
				throw Exception("Do not use 'jpackageImageZip' under macOS/Linux. Use 'jpackageImageTar' instead")
			}
		}

		val tmpPreCopyDir = layout.buildDirectory.dir("jpackageImageTarZipPreCopy").get().toString()
		val tmpBaseFilenTrg = "${propProjName}-${osName}-${cpuArch}-${version}-portable"
		val tmpFilenTrg = "${tmpBaseFilenTrg}.zip"
		println("Creating '${confDistPreOutputDir}/${tmpFilenTrg}'...")
		from(File(tmpPreCopyDir))
		archiveFileName.set(tmpFilenTrg)
		destinationDirectory.set(File(confDistPreOutputDir))
	}

	register<DefaultTask>("jpackageImageZipCleanUp")
	named<DefaultTask>("jpackageImageZipCleanUp") {
		dependsOn(":jpackageImageZipMain")

		doFirst {
			if (osName != "win") {
				throw Exception("Do not use 'jpackageImageZip' under macOS/Linux. Use 'jpackageImageTar' instead")
			}
		}

		doLast {
			val tmpPreCopyDir = layout.buildDirectory.dir("jpackageImageTarZipPreCopy").get().toString()
			if (! File(tmpPreCopyDir).exists()) {
				throw Exception("Directory '${tmpPreCopyDir}' does not exist")
			}
			File(tmpPreCopyDir).deleteRecursively()
		}
	}

	register<DefaultTask>("jpackageImageZip")
	named<DefaultTask>("jpackageImageZip") {
		dependsOn(":jpackageImageZipCleanUp")

		doFirst {
			if (osName != "win") {
				throw Exception("Do not use 'jpackageImageZip' under macOS/Linux. Use 'jpackageImageTar' instead")
			}
		}
	}
}

// org.beryx.runtime: Uses the jpackage tool to create a platform-specific application installer
tasks.jpackage {
	doFirst {
		//project.getProperty("installerType")!!  // throws exception if its missing

		println("InstallerType: $propInstallerType")
	}
	doLast {
		val tmpFilenOrg: String = when (osName) {
				"macos" -> {
					"${propProjName}-${cpuArch}-${version}.${propInstallerType}"
				}
				"linux" -> {
					val tmpCpuArchDistro = when (propInstallerType) {
							"rpm" -> when (cpuArch) {
									"x64" -> "x86_64"
									else -> "aarch64"  // @TODO
								}
							else -> when (cpuArch) {
									"x64" -> "amd64"
									else -> "arm64"
								}
						}
					when (propInstallerType) {
							"rpm" -> "${propProjName}-${version}-1.${tmpCpuArchDistro}.${propInstallerType}"
							else -> "${propProjName.replace("_", "-")}_${version}_${tmpCpuArchDistro}.${propInstallerType}"
						}
				}
				else -> {
					"${propProjName}-${cpuArch}-installer-${version}.${propInstallerType}"
				}
			}

		val tmpFileObjOrg = File(
				layout.buildDirectory.dir(confJpackOutputDir).get().toString(),
				tmpFilenOrg
		)
		if (!tmpFileObjOrg.exists()) {
			throw Exception("File '${tmpFileObjOrg}' not found")
		}
		val tmpFilenTrg = when (osName) {
				"macos", "linux" -> {
					"${propProjName}-${osName}-${cpuArch}-${version}.${propInstallerType}"
				}
				else -> {
					"${propProjName}-${osName}-${cpuArch}-${version}-installer.${propInstallerType}"
				}
			}
		val tmpFileObjTrg = File(confDistPreOutputDir, tmpFilenTrg)
		println("Renaming '${tmpFileObjOrg}' to '${tmpFileObjTrg}'")
		tmpFileObjOrg.renameTo(tmpFileObjTrg)
	}
}

tasks {
	register<Copy>("runtimeTarPreCopy")
	named<Copy>("runtimeTarPreCopy") {
		dependsOn(":runtime")

		doFirst {
			if (osName == "win") {
				throw Exception("Do not use 'runtimeTar' under MS Windows. Use 'runtimeZip' instead")
			}
		}

		/*
		 * We need to copy the launcher dir into a new subdir first,
		 * otherwise the resulting TAR-ball would not contain a parent directory
		 */
		val tmpPreCopyDir = layout.buildDirectory.dir("runtimeTarPreCopy").get().toString()
		val tmpBaseFilenTrg = runtime.get().imageDirAsFile.name
		from(runtime.get().imageDir)
		into(File(tmpPreCopyDir, tmpBaseFilenTrg))
	}

	register<Tar>("runtimeTarMain")
	named<Tar>("runtimeTarMain") {
		dependsOn(":runtimeTarPreCopy")

		doFirst {
			if (osName == "win") {
				throw Exception("Do not use 'runtimeTar' under MS Windows. Use 'runtimeZip' instead")
			}
		}

		val tmpPreCopyDir = layout.buildDirectory.dir("runtimeTarPreCopy").get().toString()
		val tmpBaseFilenTrg = runtime.get().imageDirAsFile.name
		println("Creating '${confDistPreOutputDir}/${tmpBaseFilenTrg}.tgz'...")
		from(File(tmpPreCopyDir))
		archiveFileName.set("${tmpBaseFilenTrg}.tgz")
		destinationDirectory.set(File(confDistPreOutputDir))
		compression = Compression.GZIP
	}

	register<DefaultTask>("runtimeTarCleanUp")
	named<DefaultTask>("runtimeTarCleanUp") {
		dependsOn(":runtimeTarMain")

		doFirst {
			if (osName == "win") {
				throw Exception("Do not use 'runtimeTar' under MS Windows. Use 'runtimeZip' instead")
			}
		}

		doLast {
			val tmpPreCopyDir = layout.buildDirectory.dir("runtimeTarPreCopy").get().toString()
			if (! File(tmpPreCopyDir).exists()) {
				throw Exception("Directory '${tmpPreCopyDir}' does not exist")
			}
			File(tmpPreCopyDir).deleteRecursively()
		}
	}

	register<DefaultTask>("runtimeTar")
	named<DefaultTask>("runtimeTar") {
		dependsOn(":runtimeTarCleanUp")

		doFirst {
			if (osName == "win") {
				throw Exception("Do not use 'runtimeTar' under MS Windows. Use 'runtimeZip' instead")
			}
		}
	}
}

tasks.distTar {
	compression = Compression.GZIP
	archiveExtension = "tgz"

	doFirst {
		if (osName == "win") {
			throw Exception("Do not use 'distTar' under MS Windows. Use 'distZip' instead")
		}
	}

	doLast {
		val tmpFilenOrg = "${distributions.main.get().distributionBaseName.get()}-${version}.tgz"
		val tmpFileObjOrg = File(
				layout.buildDirectory.dir("distributions").get().toString(),
				tmpFilenOrg
			)
		if (! tmpFileObjOrg.exists()) {
			throw Exception("File '${tmpFileObjOrg}' not found")
		}
		val tmpFilenTrg = "${distributions.main.get().distributionBaseName.get()}-${version}-launcher-no_jre.tgz"
		val tmpFileObjTrg = File(confDistPreOutputDir, tmpFilenTrg)
		println("Renaming '${tmpFileObjOrg}' to '${tmpFileObjTrg}'")
		tmpFileObjOrg.renameTo(tmpFileObjTrg)
	}
}

tasks.distZip {
	doFirst {
		if (osName != "win") {
			throw Exception("Do not use 'distZip' under macOS/Linux. Use 'distTar' instead")
		}
	}

	doLast {
		val tmpFilenOrg = "${distributions.main.get().distributionBaseName.get()}-${version}.zip"
		val tmpFileObjOrg = File(
				layout.buildDirectory.dir("distributions").get().toString(),
				tmpFilenOrg
			)
		if (! tmpFileObjOrg.exists()) {
			throw Exception("File '${tmpFileObjOrg}' not found")
		}
		val tmpFilenTrg = "${distributions.main.get().distributionBaseName.get()}-${version}-launcher-no_jre.zip"
		val tmpFileObjTrg = File(confDistPreOutputDir, tmpFilenTrg)
		println("Renaming '${tmpFileObjOrg}' to '${tmpFileObjTrg}'")
		tmpFileObjOrg.renameTo(tmpFileObjTrg)
	}
}
