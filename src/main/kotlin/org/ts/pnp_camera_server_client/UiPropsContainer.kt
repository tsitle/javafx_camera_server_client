package org.ts.pnp_camera_server_client

import javafx.beans.property.*

class UiPropsContainer {
	val connectionOpen: BooleanProperty = SimpleBooleanProperty(false)
	val clientId: IntegerProperty = SimpleIntegerProperty(0)
	val serverAppVersion: StringProperty = SimpleStringProperty("n/a")
	val statusMsg: StringProperty = SimpleStringProperty("-")
	val ctrlShowGrid: BooleanProperty = SimpleBooleanProperty(false)
	val ctrlCamAvailLeft: BooleanProperty = SimpleBooleanProperty(false)
	val ctrlCamAvailBoth: BooleanProperty = SimpleBooleanProperty(false)
	val ctrlCamAvailRight: BooleanProperty = SimpleBooleanProperty(false)
	val ctrlCamActive: IntegerProperty = SimpleIntegerProperty(0)
	val ctrlZoomLevel: IntegerProperty = SimpleIntegerProperty(0)
	val ctrlZoomAllowed: BooleanProperty = SimpleBooleanProperty(false)
	val ctrlBrightnVal: IntegerProperty = SimpleIntegerProperty(0)
	val ctrlBrightnMin: IntegerProperty = SimpleIntegerProperty(0)
	val ctrlBrightnMax: IntegerProperty = SimpleIntegerProperty(0)
	val ctrlBrightnAllowed: BooleanProperty = SimpleBooleanProperty(false)
}
