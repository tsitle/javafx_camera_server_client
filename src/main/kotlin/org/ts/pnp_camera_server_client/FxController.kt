package org.ts.pnp_camera_server_client

import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.AnchorPane
import org.openapitools.client.apis.DefaultApi
import org.openapitools.client.models.Status
import org.openapitools.client.models.StatusCams
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.videoio.VideoCapture
import org.ts.pnp_camera_server_client.utils.Utils
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.round
import kotlin.random.Random


fun Double.format(digits: Int) = "%.${digits}f".format(this)


open class FxController {
	@FXML
	private lateinit var conConnectBtn: Button
	@FXML
	private lateinit var imageAnchorPane: AnchorPane
	@FXML
	private lateinit var bottomAnchorPane: AnchorPane
	@FXML
	private lateinit var currentFrame: ImageView
	@FXML
	private lateinit var statusLbl: Label
	@FXML
	private lateinit var serverUrlTxtfld: TextField
	@FXML
	private lateinit var serverApiKeyTxtfld: TextField
	@FXML
	private lateinit var ctrlCamLeftBtn: Button
	@FXML
	private lateinit var ctrlCamBothBtn: Button
	@FXML
	private lateinit var ctrlCamRightBtn: Button
	@FXML
	private lateinit var ctrlShowGridCbx: CheckBox
	@FXML
	private lateinit var ctrlZoomPlusBtn: Button
	@FXML
	private lateinit var ctrlZoomMinusBtn: Button
	@FXML
	private lateinit var ctrlZoom100Btn: Button

	// a timer for acquiring the video stream
	private var timerFrames: ScheduledExecutorService? = null

	// flag for stopping the 'get server status' thread
	private var doKillThreadStatus = false

	// the OpenCV object that realizes the video capture
	private var capture: VideoCapture? = null

	// width to height ratio of the camera image
	private var cameraRatio = -1.0

	// output width of the camera image
	private val cameraOutputWidth: Int = 600

	// output FPS (frames per second) of camera stream
	private val cameraOutputFps: Int = 24

	// convert the output image to grayscale?
	private val cameraToGray: Boolean = false

	// observable properties
	private val uiProps =  UiPropsContainer()

	// API Client Functions Wrapper
	private var apiClientFncs: ApiClientFncs? = null

	@FXML
	protected fun initialize() {
		// load the native OpenCV library
		println("Load lib '${Core.NATIVE_LIBRARY_NAME}'")
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME)

		//
		initUiPropHandling()

		//
		this.capture = VideoCapture()
		this.timerFrames = Executors.newSingleThreadScheduledExecutor()

		// start the 'get server status' thread
		val threadStatus = Thread(runnerGetServerStatusThread())
		threadStatus.isDaemon = false
		threadStatus.start()

		// load camera stream
		if (serverUrlTxtfld.text.isNotEmpty() && serverApiKeyTxtfld.text.isNotEmpty()) {
			conConnectBtn.fire()
		}
	}

	private fun initUiPropHandling() {
		uiProps.clientId.subscribe { it -> println("Client ID: ${it.toInt()}") }
		uiProps.clientId.value = (Random.nextDouble() * 1000.0).toInt()

		//
		statusLbl.textProperty().bind(uiProps.statusMsg)
		statusLbl.textProperty().subscribe { it -> println("Status: '$it'") }

		//
		uiProps.ctrlShowGrid.subscribe { it ->
				println("Show Grid: $it")
				ctrlShowGridCbx.isSelected = it
			}

		//
		uiProps.connectionOpen.subscribe { it ->
				conConnectBtn.styleClass.removeAll("btn-danger", "btn-default", "btn-success")
				conConnectBtn.styleClass.add(if (it) "btn-danger" else "btn-success")

				serverUrlTxtfld.isDisable = it
				serverApiKeyTxtfld.isDisable = it

				handleUiPropChangeForCtrlCamButtons()

				handleUiPropChangeForCtrlZoomButtons()

				ctrlShowGridCbx.isDisable = ! it
			}

		//
		uiProps.ctrlCamAvailLeft.subscribe { _ -> handleUiPropChangeForCtrlCamButtons() }
		uiProps.ctrlCamAvailBoth.subscribe { _ -> handleUiPropChangeForCtrlCamButtons() }
		uiProps.ctrlCamAvailRight.subscribe { _ -> handleUiPropChangeForCtrlCamButtons() }
		uiProps.ctrlCamActive.subscribe { _ -> handleUiPropChangeForCtrlCamButtons() }

		//
		uiProps.ctrlZoom.subscribe { _ -> handleUiPropChangeForCtrlZoomButtons() }
	}

	private fun handleUiPropChangeForCtrlCamButtons() {
		val tmpConnOpen = uiProps.connectionOpen.value
		val tmpCtrlCamActive = uiProps.ctrlCamActive.value
		ctrlCamLeftBtn.isDisable = ! (tmpConnOpen && uiProps.ctrlCamAvailLeft.value && tmpCtrlCamActive != StatusCams.L.ordinal)
		ctrlCamBothBtn.isDisable = ! (tmpConnOpen && uiProps.ctrlCamAvailBoth.value && tmpCtrlCamActive != StatusCams.BOTH.ordinal)
		ctrlCamRightBtn.isDisable = ! (tmpConnOpen && uiProps.ctrlCamAvailRight.value && tmpCtrlCamActive != StatusCams.R.ordinal)

		val cssClassActive = "btn-primary"
		val cssClassInactive = "btn-default"
		for (btn in arrayOf(ctrlCamLeftBtn, ctrlCamBothBtn,  ctrlCamRightBtn)) {
			btn.styleClass.removeAll(cssClassInactive, cssClassActive)
		}
		when (tmpCtrlCamActive) {
			StatusCams.L.ordinal -> {
				ctrlCamLeftBtn.styleClass.add(cssClassActive)
				ctrlCamBothBtn.styleClass.add(cssClassInactive)
				ctrlCamRightBtn.styleClass.add(cssClassInactive)
			}
			StatusCams.BOTH.ordinal -> {
				ctrlCamLeftBtn.styleClass.add(cssClassInactive)
				ctrlCamBothBtn.styleClass.add(cssClassActive)
				ctrlCamRightBtn.styleClass.add(cssClassInactive)
			}
			StatusCams.R.ordinal -> {
				ctrlCamLeftBtn.styleClass.add(cssClassInactive)
				ctrlCamBothBtn.styleClass.add(cssClassInactive)
				ctrlCamRightBtn.styleClass.add(cssClassActive)
			}
		}
	}

	private fun handleUiPropChangeForCtrlZoomButtons() {
		val tmpConnOpen = uiProps.connectionOpen.value
		val tmpCtrlZoom = uiProps.ctrlZoom.value
		ctrlZoomPlusBtn.isDisable = ! (tmpConnOpen && tmpCtrlZoom >= 0 && tmpCtrlZoom > 10)
		ctrlZoomMinusBtn.isDisable = ! (tmpConnOpen && tmpCtrlZoom >= 0 && tmpCtrlZoom < 100)
		ctrlZoom100Btn.isDisable = ! (tmpConnOpen && tmpCtrlZoom >= 0 && tmpCtrlZoom < 100)
	}

	private fun runnerGetServerStatusThread() = Runnable {
		var readStatusTo = 0
		while (! doKillThreadStatus) {
			try {
				Thread.sleep(50)
			} catch (e: InterruptedException) {
				Thread.interrupted()
				break
			}
			if (++readStatusTo == 10) {
				Platform.runLater(runnerGetServerStatusSub())
				readStatusTo = 0
			}
		}
		println("ending threadStatus")
	}

	private fun runnerGetServerStatusSub() = Runnable {
		apiClientFncs?.getServerStatus(true)
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
					/*val orgWidth = frame.width()
					val orgHeight = frame.height()
					cameraRatio = orgWidth.toDouble() / orgHeight.toDouble()
					val resizedFrame = Mat()
					val sz = Size(cameraOutputWidth.toDouble(), cameraOutputWidth.toDouble() / cameraRatio)
					Imgproc.resize(frame, resizedFrame, sz)
					frame = resizedFrame

					// update
					if (cameraRatio > 0.0 && lastCameraRatio != cameraRatio && imageAnchorPane.width > 0.0) {
						updateWindowSize(imageAnchorPane.width.toInt(), imageAnchorPane.height.toInt() + bottomAnchorPane.height.toInt())
					}*/
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
		if (this.timerFrames != null && ! timerFrames!!.isShutdown) {
			try {
				// stop the timer
				timerFrames!!.shutdown()
				var cnt = 0
				while (! timerFrames!!.awaitTermination(500, TimeUnit.MILLISECONDS)) {
					System.err.println("TimerFrames still active")
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
		//
		this.doKillThreadStatus = true
	}

	fun initWindowSize() {
		//println("externalInitWindowSize: anchor ${imageAnchorPane.width.toInt()}x${imageAnchorPane.height.toInt()}")
		cameraRatio = imageAnchorPane.width / imageAnchorPane.height
		updateWindowSize(imageAnchorPane.width.toInt(), imageAnchorPane.height.toInt() + bottomAnchorPane.height.toInt())
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

			if (imageAnchorPane.height + bottomAnchorPane.height > newHeight) {
				// if the image is now too high we need to make it narrower again
				currentFrame.fitHeight = newHeight.toDouble() - bottomAnchorPane.height
				currentFrame.fitWidth = currentFrame.fitHeight * cameraRatio
			}
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

	/**
	 * Move focus away from last control (e.g. button) that has been clicked on
	 */
	private fun moveFocusAwayFromControls() {
		imageAnchorPane.requestFocus()
	}

	/**
	 * Event: Button "Connection: Connect" pressed
	 *
	 * @param event
	 */
	@FXML
	protected fun evtConConnect(event: ActionEvent?) {
		// update UI
		serverUrlTxtfld.isDisable = true
		serverApiKeyTxtfld.isDisable = true
		//
		if (! uiProps.connectionOpen.value) {
			if (serverUrlTxtfld.text.isNotEmpty() && serverApiKeyTxtfld.text.isNotEmpty()) {
				apiClientFncs = ApiClientFncs(
						serverUrlTxtfld.text,
						serverApiKeyTxtfld.text,
						uiProps
				)
				// start the video capture
				capture?.open("${serverUrlTxtfld.text}/stream.mjpeg?cid=${uiProps.clientId.value}")
			}

			// is the video stream available?
			if (capture?.isOpened == true) {
				uiProps.connectionOpen.value = true

				// grab a frame every x ms (== 1000 / y frames/sec)
				val frameGrabber = Runnable {  // effectively grab and process a single frame
					val frame = grabFrame()
					// convert and show the frame
					val imageToShow: Image? = Utils.mat2Image(frame)
					updateImageView(currentFrame, imageToShow!!)
				}

				if (this.timerFrames?.isShutdown == true) {
					this.timerFrames = Executors.newSingleThreadScheduledExecutor()
				}
				this.timerFrames?.scheduleAtFixedRate(
						frameGrabber,
						0,
						round(1000.0 / cameraOutputFps.toDouble()).toLong(),
						TimeUnit.MILLISECONDS
				)

				// update UI
				conConnectBtn.text = "Disconnect"
				uiProps.statusMsg.set("Connected")
			} else {
				System.err.println("Could not open server connection...")
				uiProps.statusMsg.set("Could not open server connection")
			}
		} else {
			// the camera is not active at this point
			uiProps.connectionOpen.value = false
			// update UI
			conConnectBtn.text = "Connect"
			uiProps.statusMsg.set("Disconnected")

			// stop the timer
			this.stopAcquisition()
		}

		//
		moveFocusAwayFromControls()
	}

	/**
	 * Event: Button "Controls: CAM L" pressed
	 *
	 * @param event
	 */
	@FXML
	protected fun evtCtrlCamLeft(event: ActionEvent?) {
		apiClientFncs?.setActiveCam(DefaultApi.CamOutputCamEnable.L)
		//
		moveFocusAwayFromControls()
	}

	/**
	 * Event: Button "Controls: CAM BOTH" pressed
	 *
	 * @param event
	 */
	@FXML
	protected fun evtCtrlCamBoth(event: ActionEvent?) {
		apiClientFncs?.setActiveCam(DefaultApi.CamOutputCamEnable.BOTH)
		//
		moveFocusAwayFromControls()
	}

	/**
	 * Event: Button "Controls: CAM R" pressed
	 *
	 * @param event
	 */
	@FXML
	protected fun evtCtrlCamRight(event: ActionEvent?) {
		apiClientFncs?.setActiveCam(DefaultApi.CamOutputCamEnable.R)
		//
		moveFocusAwayFromControls()
	}

	/**
	 * Event: Checkbox "Controls: Show Grid" changed
	 *
	 * @param event
	 */
	@FXML
	protected fun evtCtrlShowGrid(event: ActionEvent?) {
		apiClientFncs?.setShowGrid(ctrlShowGridCbx.isSelected)
		//
		moveFocusAwayFromControls()
	}

	/**
	 * Event: Button "Controls: Zoom +/-/100%" pressed
	 *
	 * @param event
	 */
	@FXML
	protected fun evtCtrlZoom(event: ActionEvent?) {
		var nextZoomLevel = uiProps.ctrlZoom.value
		when (event!!.target) {
			ctrlZoomPlusBtn -> nextZoomLevel -= 10
			ctrlZoomMinusBtn -> nextZoomLevel += 10
			else -> nextZoomLevel = 100
		}
		apiClientFncs?.setZoom(nextZoomLevel)
		//
		moveFocusAwayFromControls()
	}
}
