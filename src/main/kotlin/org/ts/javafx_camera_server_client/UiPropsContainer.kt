package org.ts.javafx_camera_server_client

import javafx.beans.property.*

class UiPropsContainer {
	val connectionOpen: BooleanProperty = SimpleBooleanProperty(false)
	val apiClientLostConnection: BooleanProperty = SimpleBooleanProperty(false)
	val clientId: IntegerProperty = SimpleIntegerProperty(0)
	val serverAppVersion: StringProperty = SimpleStringProperty("n/a")
	val statusMsg: StringProperty = SimpleStringProperty("-")
	val inputFps: IntegerProperty = SimpleIntegerProperty(0)
	//
	val ctrlShowGrid: BooleanProperty = SimpleBooleanProperty(false)
	val ctrlCamAvailLeft: BooleanProperty = SimpleBooleanProperty(false)
	val ctrlCamAvailBoth: BooleanProperty = SimpleBooleanProperty(false)
	val ctrlCamAvailRight: BooleanProperty = SimpleBooleanProperty(false)
	val ctrlCamActive: IntegerProperty = SimpleIntegerProperty(0)
	val ctrlZoomLevel: IntegerProperty = SimpleIntegerProperty(0)
	val ctrlZoomAllowed: BooleanProperty = SimpleBooleanProperty(false)
	//
	val bncBrightnVal: IntegerProperty = SimpleIntegerProperty(0)
	val bncBrightnMin: IntegerProperty = SimpleIntegerProperty(0)
	val bncBrightnMax: IntegerProperty = SimpleIntegerProperty(0)
	val bncBrightnAllowed: BooleanProperty = SimpleBooleanProperty(false)
	//
	val bncContrVal: IntegerProperty = SimpleIntegerProperty(0)
	val bncContrMin: IntegerProperty = SimpleIntegerProperty(0)
	val bncContrMax: IntegerProperty = SimpleIntegerProperty(0)
	val bncContrAllowed: BooleanProperty = SimpleBooleanProperty(false)
}
