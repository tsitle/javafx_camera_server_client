package org.ts.pnp_camera_server_client

import javafx.beans.property.*

class UiPropsContainer {
	val connectionOpen: BooleanProperty = SimpleBooleanProperty(false)
	val clientId: IntegerProperty = SimpleIntegerProperty(0)
	val statusMsg: StringProperty = SimpleStringProperty("-")
	val ctrlShowGrid: BooleanProperty = SimpleBooleanProperty(false)
}
