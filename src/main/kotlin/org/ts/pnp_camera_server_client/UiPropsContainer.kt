package org.ts.pnp_camera_server_client

import javafx.beans.property.*

class UiPropsContainer {
	val connectionOpen: BooleanProperty = SimpleBooleanProperty(false)
	val clientId: IntegerProperty = SimpleIntegerProperty(0)
	val statusMsg: StringProperty = SimpleStringProperty("-")
	val ctrlShowGrid: BooleanProperty = SimpleBooleanProperty(false)
	val ctrlCamAvailLeft: BooleanProperty = SimpleBooleanProperty(false)
	val ctrlCamAvailBoth: BooleanProperty = SimpleBooleanProperty(false)
	val ctrlCamAvailRight: BooleanProperty = SimpleBooleanProperty(false)
	val ctrlCamActive: IntegerProperty = SimpleIntegerProperty(0)
}
