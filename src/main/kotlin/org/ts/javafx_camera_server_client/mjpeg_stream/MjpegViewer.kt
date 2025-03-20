package org.ts.javafx_camera_server_client.mjpeg_stream

import java.io.ByteArrayInputStream


interface MjpegViewer {
	fun mjpegSetRawImageData(rawByteArrayInputStream: ByteArrayInputStream)
	fun mjpegLogError(msg: String)
	fun mjpegLostConnection()
}
