/*
 * Since we cannot use Gluon's GraalVM or Bellsoft's 'Full' Liberica JDK (which includes JavaFX)
 * the foojay-resolver plugin doesn't help us at all
 */
/*plugins {
	// automatically configure (and download) JVM according to toolchain config in build.gradle.kts
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"  // see https://plugins.gradle.org/plugin/org.gradle.toolchains.foojay-resolver-convention
}*/

rootProject.name = "javafx_camera_server_client"
