package org.ts.pnp_camera_server_client

import javafx.application.Application
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import javafx.stage.WindowEvent
import org.kordamp.bootstrapfx.BootstrapFX
import org.opencv.core.Core


class ClientApplication : Application() {
	override fun start(primaryStage: Stage) {
		loadOpenCvLib()

		try {
			// load the FXML resource
			val loader = FXMLLoader(ClientApplication::class.java.getResource("ClientUi.fxml"))

			// store the root element so that the controllers can use it
			val rootElement = loader.load<Any>() as BorderPane

			// create and style a scene
			val scene = Scene(rootElement, 800.0, 600.0)

			// create the stage with the given title and the previously created scene
			primaryStage.title = "Camera Streaming Client"
			primaryStage.scene = scene

			// add CSS to scene
			scene.stylesheets.add(BootstrapFX.bootstrapFXStylesheet())
			scene.stylesheets.add(ClientApplication::class.java.getResource("styles.css")?.toExternalForm())

			// show the GUI
			primaryStage.show()

			// set the proper behavior on closing the application
			val controller: FxController = loader.getController<FxController>()
			primaryStage.onCloseRequest = EventHandler<WindowEvent?> { controller.setClosed() }

			// add event handler for window size changes
			scene.widthProperty().addListener { _, oldSceneWidth, newSceneWidth ->
					if (oldSceneWidth.toInt() < 0) {
						println("dummy output to stop IDE from complaining about unused var")
					}
					//println("window width changed: $oldSceneWidth -> $newSceneWidth (h=${scene.height.toInt()})")
					controller.updateWindowSize(newSceneWidth.toInt(), scene.height.toInt())
				}
			scene.heightProperty().addListener { _, oldSceneHeight, newSceneHeight ->
					if (oldSceneHeight.toInt() < 0) {
						println("dummy output to stop IDE from complaining about unused var")
					}
					//println("window height changed: $oldSceneHeight -> $newSceneHeight (w=${scene.width.toInt()})")
					controller.updateWindowSize(scene.width.toInt(), newSceneHeight.toInt())
				}

			Platform.runLater {
				//println("runLater: Main window has been opened")
				controller.initWindowSize()
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	/**
	 * Load the native OpenCV library
	 */
	private fun loadOpenCvLib() {
		var tmpOpenCvLibNameNoVers = Core.NATIVE_LIBRARY_NAME
		tmpOpenCvLibNameNoVers = tmpOpenCvLibNameNoVers.replace(Core.VERSION.replace(".", ""), "")
		try {
			println("Load lib '${tmpOpenCvLibNameNoVers}' (bundled JAR version=${Core.VERSION})")
			System.loadLibrary(tmpOpenCvLibNameNoVers)
		} catch (ex: UnsatisfiedLinkError) {
			val tmpOpenCvLibName460 = "${tmpOpenCvLibNameNoVers}460"
			try {
				println("Failed to load lib '${tmpOpenCvLibNameNoVers}'. Trying to load '${tmpOpenCvLibName460}'")
				System.loadLibrary(tmpOpenCvLibName460)
			} catch (ex: UnsatisfiedLinkError) {
				println("Failed to load lib '${tmpOpenCvLibName460}'. Trying to load '${Core.NATIVE_LIBRARY_NAME}'")
				System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
			}
		}
	}
}

fun main() {
	Application.launch(ClientApplication::class.java)
}
