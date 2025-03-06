package org.ts.camera_streaming_client

import javafx.application.Application
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import javafx.stage.WindowEvent

class ClientApplication : Application() {
	override fun start(primaryStage: Stage) {
		try {
			// load the FXML resource
			val loader = FXMLLoader(ClientApplication::class.java.getResource("ClientUi.fxml"))

			// store the root element so that the controllers can use it
			val rootElement = loader.load<Any>() as BorderPane

			// create and style a scene
			val scene = Scene(rootElement, 800.0, 600.0)
			//scene.stylesheets.add(getResource("application.css").toExternalForm())

			// create the stage with the given title and the previously created scene
			primaryStage.title = "Camera Streaming Client"
			primaryStage.scene = scene

			// show the GUI
			primaryStage.show()

			// set the proper behavior on closing the application
			val controller: FxController = loader.getController<FxController>()
			primaryStage.onCloseRequest = (EventHandler<WindowEvent?> { controller.setClosed() })
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}
}

fun main() {
	Application.launch(ClientApplication::class.java)
}
