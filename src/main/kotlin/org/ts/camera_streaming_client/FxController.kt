package org.ts.camera_streaming_client

import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.AnchorPane
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.videoio.VideoCapture
import org.ts.camera_streaming_client.utils.Utils
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.round

fun Double.format(digits: Int) = "%.${digits}f".format(this)

open class FxController {
	@FXML
	private lateinit var connectBtn: Button
	@FXML
	private lateinit var imageAnchorPane: AnchorPane
	@FXML
	private lateinit var bottomAnchorPane: AnchorPane
	@FXML
	private lateinit var currentFrame: ImageView
	@FXML
	private lateinit var statusLbl: Label

	// a timer for acquiring the video stream
	private var timer: ScheduledExecutorService? = null

	// the OpenCV object that realizes the video capture
	private var capture: VideoCapture? = null

	// a flag to change the button behavior
	private var cameraActive = false

	// width to height ratio of the camera image
	private var cameraRatio = -1.0

	// the ID of the locally connected Webcam to be used
	private val cameraId = 0

	// collection of MJPEG stream URLs
	private val cameraUrls = arrayOf(
			"http://61.211.241.239/nphMotionJpeg?Resolution=320x240&Quality=Standard",
			"http://195.196.36.242/mjpg/video.mjpg",  // cannot set resolution
			"http://webcam.mchcares.com/mjpg/video.mjpg",  // cannot set resolution
			"http://takemotopiano.aa1.netvolante.jp:8190/nphMotionJpeg?Resolution=640x480&Quality=Standard&Framerate=30",
			"http://pendelcam.kip.uni-heidelberg.de/mjpg/video.mjpg" // cannot set resolution
		)

	// the index of the stream URL to be used
	private val cameraIxUrl = 1

	// use locally connected Webcam or stream?
	private val cameraUseLocal = false

	// output width of the camera image
	private val cameraOutputWidth: Int = 320

	// output FPS (frames per second) of camera stream
	private val cameraOutputFps: Int = 24

	// convert the output image to grayscale?
	private val cameraToGray: Boolean = false

	@FXML
	protected fun initialize() {
		// load the native OpenCV library
		println("Load lib '${Core.NATIVE_LIBRARY_NAME}'")
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME)

		//
		this.capture = VideoCapture()
		this.timer = Executors.newSingleThreadScheduledExecutor()

		// load camera stream
		connectBtn.fire()
	}

	/**
	 * Event: Button "Connect" pressed
	 *
	 * @param event
	 */
	@FXML
	protected fun evtConnect(event: ActionEvent?) {
		if (! this.cameraActive) {
			// start the video capture
			if (cameraUseLocal) {
				capture?.open(cameraId)
			} else {
				if (cameraIxUrl >= cameraUrls.size) {
					throw RuntimeException("cameraIxUrl out of bounds")
				}
				capture?.open(cameraUrls[cameraIxUrl])
			}

			// is the video stream available?
			if (capture?.isOpened == true) {
				this.cameraActive = true

				// grab a frame every x ms (== 1000 / y frames/sec)
				val frameGrabber = Runnable {  // effectively grab and process a single frame
					val frame = grabFrame()
					// convert and show the frame
					val imageToShow: Image? = Utils.mat2Image(frame)
					updateImageView(currentFrame, imageToShow!!)
				}

				if (this.timer?.isShutdown == true) {
					this.timer = Executors.newSingleThreadScheduledExecutor()
				}
				this.timer?.scheduleAtFixedRate(
						frameGrabber,
						0,
						round(1000.0 / cameraOutputFps.toDouble()).toLong(),
						TimeUnit.MILLISECONDS
					)

				// update UI
				connectBtn.text = "Disconnect"
				statusLbl.text = "Connected"
			} else {
				System.err.println("Could not open camera connection...")
			}
		} else {
			// the camera is not active at this point
			this.cameraActive = false
			// update UI
			connectBtn.text = "Connect"
			statusLbl.text = "Disconnected"

			// stop the timer
			this.stopAcquisition()
		}
	}

	/**
	 * Get a frame from the opened video stream (if any)
	 *
	 * @return the [Mat] to show
	 */
	private fun grabFrame(): Mat {
		var frame = Mat()

		// check if the capture is open
		if (capture?.isOpened == true) {
			try {
				// read the current frame
				capture?.read(frame)

				// if the frame is not empty, process it
				if (! frame.empty()) {
					if (cameraToGray) {
						Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY)
					}

					val lastCameraRatio = cameraRatio

					// scale the image
					val orgWidth = frame.width()
					val orgHeight = frame.height()
					cameraRatio = orgWidth.toDouble() / orgHeight.toDouble()
					val resizedFrame = Mat()
					val sz = Size(cameraOutputWidth.toDouble(), cameraOutputWidth.toDouble() / cameraRatio)
					Imgproc.resize(frame, resizedFrame, sz)
					frame = resizedFrame

					// update
					if (cameraRatio > 0.0 && lastCameraRatio != cameraRatio && imageAnchorPane.width > 0.0) {
						updateWindowSize(imageAnchorPane.width.toInt(), imageAnchorPane.height.toInt() + bottomAnchorPane.height.toInt())
					}
				}
			} catch (e: Exception) {
				System.err.println("Exception during image processing: $e")
			}
		}

		return frame
	}

	/**
	 * Stop the acquisition from the camera and release all the resources
	 */
	private fun stopAcquisition() {
		if (this.timer != null && ! timer!!.isShutdown) {
			try {
				// stop the timer
				timer!!.shutdown()
				var cnt = 0
				while (! timer!!.awaitTermination(500, TimeUnit.MILLISECONDS)) {
					System.err.println("Timer still active")
					if (++cnt == 10) {
						System.err.println("Ignoring timer (this might crash the application)")
						break
					}
				}
			} catch (e: InterruptedException) {
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... $e")
			}
		}

		if (capture?.isOpened == true) {
			// release the camera
			capture?.release()
		}
	}

	/**
	 * Update the [ImageView] in the JavaFX main thread
	 *
	 * @param view the [ImageView] to update
	 * @param image the [Image] to show
	 */
	private fun updateImageView(view: ImageView, image: Image) {
		Utils.onFXThread(view.imageProperty(), image)
	}

	/**
	 * On application close, stop the acquisition from the camera
	 */
	fun setClosed() {
		this.stopAcquisition()
	}

	fun updateWindowSize(newWidth: Int, newHeight: Int) {
		//println("updateWindowSize: wnd ${newWidth}x${newHeight}")

		//println("updateWindowSize: ratio=${cameraRatio.format(2)}")
		if (cameraRatio < 0.0) {
			return
		}

		if ((newWidth.toDouble() / cameraRatio) + bottomAnchorPane.height > newHeight) {
			//println("updateWindowSize: case 1 - window wider than image")
			currentFrame.fitHeight = imageAnchorPane.height
			currentFrame.fitWidth = imageAnchorPane.height * cameraRatio
			//
			currentFrame.x = (imageAnchorPane.width - currentFrame.fitWidth) / 2.0
			currentFrame.y = 0.0
		} else {
			//println("updateWindowSize: case 2 - window higher than image")
			currentFrame.fitWidth = newWidth.toDouble()
			currentFrame.fitHeight = newWidth.toDouble() / cameraRatio
			//
			currentFrame.x = 0.0
			currentFrame.y = (imageAnchorPane.height - currentFrame.fitHeight) / 2.0
		}
		//println("updateWindowSize: img ${currentFrame.fitWidth}x${currentFrame.fitHeight} (anchor.w: ${imageAnchorPane.width.toInt()}) (bot.h: ${bottomAnchorPane.height.toInt()})")
	}

	fun externalInitWindowSize() {
		//println("externalInitWindowSize: anchor ${imageAnchorPane.width.toInt()}x${imageAnchorPane.height.toInt()}")
		cameraRatio = imageAnchorPane.width / imageAnchorPane.height
		updateWindowSize(imageAnchorPane.width.toInt(), imageAnchorPane.height.toInt() + bottomAnchorPane.height.toInt())
	}
}
