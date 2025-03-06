module org.ts.camera_streaming_client {
	requires javafx.controls;
	requires javafx.fxml;
	requires kotlin.stdlib;

	requires org.controlsfx.controls;
	requires org.kordamp.bootstrapfx.core;
	requires opencv;
	requires javafx.swing;

	opens org.ts.camera_streaming_client to javafx.fxml;
	exports org.ts.camera_streaming_client;
}
