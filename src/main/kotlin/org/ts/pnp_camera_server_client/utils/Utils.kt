package org.ts.pnp_camera_server_client.utils

import javafx.application.Platform
import javafx.beans.property.ObjectProperty
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import org.opencv.core.Mat
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte

class Utils {
	companion object {
		/**
		 * Convert a Mat object (OpenCV) in the corresponding Image for JavaFX
		 *
		 * @param frame
		 * the [Mat] representing the current frame
		 * @return the [Image] to show
		 */
		fun mat2Image(frame: Mat): Image? {
			try {
				return SwingFXUtils.toFXImage(matToBufferedImage(frame), null)
			} catch (e: Exception) {
				System.err.println("Cannot convert the Mat object: $e")
				return null
			}
		}

		/**
		 * Generic method for putting element running on a non-JavaFX thread on the
		 * JavaFX thread, to properly update the UI
		 *
		 * @param property
		 * a [ObjectProperty]
		 * @param value
		 * the value to set for the given [ObjectProperty]
		 */
		fun <T> onFXThread(property: ObjectProperty<T>, value: T) {
			Platform.runLater {
				property.set(value)
			}
		}

		/**
		 * Support for the [] method
		 *
		 * @param original
		 * the [Mat] object in BGR or grayscale
		 * @return the corresponding [BufferedImage]
		 */
		private fun matToBufferedImage(original: Mat): BufferedImage {
			// init
			var image: BufferedImage? = null
			val width = original.width()
			val height = original.height()
			val channels = original.channels()
			val sourcePixels = ByteArray(width * height * channels)
			original[0, 0, sourcePixels]

			image = if (original.channels() > 1) {
					BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR)
				} else {
					BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
				}
			val targetPixels = (image.raster.dataBuffer as DataBufferByte).data
			System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.size)

			return image
		}
	}
}
