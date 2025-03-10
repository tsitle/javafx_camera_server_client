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


class ClientApplication : Application() {
	override fun start(primaryStage: Stage) {
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
					//println("window width changed: $oldSceneWidth -> $newSceneWidth")
					controller.updateWindowSize(newSceneWidth.toInt(), scene.height.toInt())
				}
			scene.heightProperty().addListener { _, oldSceneHeight, newSceneHeight ->
					//println("window height changed: $oldSceneHeight -> $newSceneHeight")
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
}

fun main() {
	Application.launch(ClientApplication::class.java)
}
