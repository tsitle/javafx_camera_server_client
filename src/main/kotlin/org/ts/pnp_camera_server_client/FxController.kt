package org.ts.pnp_camera_server_client

import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import org.openapitools.client.apis.DefaultApi
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

data class AreaSize(val w: Int, val h: Int)


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
	@FXML
	private lateinit var bncBrightnSlid: Slider
	@FXML
	private lateinit var bncContrSlid: Slider

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

	// has the server connection been lost?
	private var connectionLost = false

	// last value of Slider "B&C: Brightness"
	private var lastBncBrightnVal = 0

	// last value of Slider "B&C: Contrast"
	private var lastBncContrVal = 0

	/**
	 * Initialize UI controller
	 */
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

	/**
	 * Initialize uiProps handling
	 */
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
				conConnectBtn.text = (if (it) "Disconnect" else "Connect")
				setTooltipOfButton(conConnectBtn, if (it) "Disconnect from server" else "Connect to server")

				serverUrlTxtfld.isDisable = it
				serverApiKeyTxtfld.isDisable = it

				handleUiPropChangeForCtrlCamButtons()
				ctrlShowGridCbx.isDisable = ! it
				handleUiPropChangeForCtrlZoomButtons()

				bncBrightnSlid.isDisable = ! it
				bncContrSlid.isDisable = ! it
			}

		//
		uiProps.serverAppVersion.subscribe { it -> println("Server app version: $it")}

		//
		///
		uiProps.ctrlCamAvailLeft.subscribe { _ -> handleUiPropChangeForCtrlCamButtons() }
		uiProps.ctrlCamAvailBoth.subscribe { _ -> handleUiPropChangeForCtrlCamButtons() }
		uiProps.ctrlCamAvailRight.subscribe { _ -> handleUiPropChangeForCtrlCamButtons() }
		uiProps.ctrlCamActive.subscribe { _ -> handleUiPropChangeForCtrlCamButtons() }
		///
		uiProps.ctrlZoomLevel.subscribe { _ -> handleUiPropChangeForCtrlZoomButtons() }
		uiProps.ctrlZoomAllowed.subscribe { _ -> handleUiPropChangeForCtrlZoomButtons() }

		//
		///
		uiProps.bncBrightnVal.subscribe { _ -> handleUiPropChangeForBncBrightn() }
		uiProps.bncBrightnMin.subscribe { _ -> handleUiPropChangeForBncBrightn() }
		uiProps.bncBrightnMax.subscribe { _ -> handleUiPropChangeForBncBrightn() }
		uiProps.bncBrightnAllowed.subscribe { _ -> handleUiPropChangeForBncBrightn() }
		///
		uiProps.bncContrVal.subscribe { _ -> handleUiPropChangeForBncContr() }
		uiProps.bncContrMin.subscribe { _ -> handleUiPropChangeForBncContr() }
		uiProps.bncContrMax.subscribe { _ -> handleUiPropChangeForBncContr() }
		uiProps.bncContrAllowed.subscribe { _ -> handleUiPropChangeForBncContr() }
	}

	/**
	 * Handle changes in uiProps for buttons "Controls: CAM x"
	 */
	private fun handleUiPropChangeForCtrlCamButtons() {
		val tmpConnOpen = uiProps.connectionOpen.value
		val tmpCtrlCamActive = uiProps.ctrlCamActive.value
		ctrlCamLeftBtn.isDisable = ! (tmpConnOpen && uiProps.ctrlCamAvailLeft.value && tmpCtrlCamActive != StatusCams.L.ordinal)
		ctrlCamBothBtn.isDisable = ! (tmpConnOpen && uiProps.ctrlCamAvailBoth.value && tmpCtrlCamActive != StatusCams.BOTH.ordinal)
		ctrlCamRightBtn.isDisable = ! (tmpConnOpen && uiProps.ctrlCamAvailRight.value && tmpCtrlCamActive != StatusCams.R.ordinal)

		if (tmpConnOpen) {
			setTooltipOfButton(ctrlCamLeftBtn, if (tmpCtrlCamActive != StatusCams.L.ordinal) "Activate left camera" else "Left camera active")
			setTooltipOfButton(ctrlCamBothBtn, if (tmpCtrlCamActive != StatusCams.BOTH.ordinal) "Activate both cameras" else "Both cameras active")
			setTooltipOfButton(ctrlCamRightBtn, if (tmpCtrlCamActive != StatusCams.R.ordinal) "Activate right camera" else "Right camera active")
		} else {
			for (btn in arrayOf(ctrlCamLeftBtn, ctrlCamBothBtn, ctrlCamRightBtn)) {
				setTooltipOfButton(btn, "Cannot activate camera(s)")
			}
		}

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

	/**
	 * Handle changes in uiProps for buttons "Controls: Zoom x"
	 */
	private fun handleUiPropChangeForCtrlZoomButtons() {
		val tmpConnOpen = uiProps.connectionOpen.value
		val tmpCtrlZoomLev = uiProps.ctrlZoomLevel.value
		val tmpCtrlZoomAllowed = uiProps.ctrlZoomAllowed.value
		val canBeEnabled = (tmpConnOpen && tmpCtrlZoomAllowed && tmpCtrlZoomLev >= 0)
		ctrlZoomPlusBtn.isDisable = ! (canBeEnabled && tmpCtrlZoomLev > 10)
		ctrlZoomMinusBtn.isDisable = ! (canBeEnabled && tmpCtrlZoomLev < 100)
		ctrlZoom100Btn.isDisable = ! (canBeEnabled && tmpCtrlZoomLev < 100)

		setTooltipOfButton(ctrlZoomPlusBtn, if (ctrlZoomPlusBtn.isDisable) "Zoom not possible" else "Zoom in")
		setTooltipOfButton(ctrlZoomMinusBtn, if (ctrlZoomMinusBtn.isDisable) "Zoom not possible" else "Zoom out")
		setTooltipOfButton(ctrlZoom100Btn, if (ctrlZoom100Btn.isDisable) "Zoom not possible" else "Reset zoom to 100%")
	}

	/**
	 * Handle changes in uiProps for slider "B&C: Brightness"
	 */
	private fun handleUiPropChangeForBncBrightn() {
		val tmpConnOpen = uiProps.connectionOpen.value
		val tmpBrightnAllowed = uiProps.bncBrightnAllowed.value
		val canBeEnabled = (tmpConnOpen && tmpBrightnAllowed)
		bncBrightnSlid.isDisable = ! canBeEnabled

		bncBrightnSlid.min = uiProps.bncBrightnMin.value.toDouble()
		bncBrightnSlid.max = uiProps.bncBrightnMax.value.toDouble()
		bncBrightnSlid.value = uiProps.bncBrightnVal.value.toDouble()
		lastBncBrightnVal = uiProps.bncBrightnVal.value
	}

	/**
	 * Handle changes in uiProps for slider "B&C: Contrast"
	 */
	private fun handleUiPropChangeForBncContr() {
		val tmpConnOpen = uiProps.connectionOpen.value
		val tmpContrAllowed = uiProps.bncContrAllowed.value
		val canBeEnabled = (tmpConnOpen && tmpContrAllowed)
		bncContrSlid.isDisable = ! canBeEnabled

		bncContrSlid.min = uiProps.bncContrMin.value.toDouble()
		bncContrSlid.max = uiProps.bncContrMax.value.toDouble()
		bncContrSlid.value = uiProps.bncContrVal.value.toDouble()
		lastBncContrVal = uiProps.bncContrVal.value
	}

	/**
	 * Set value of tooltip of button (and its parent label)
	 */
	private fun setTooltipOfButton(btn: Button, text: String) {
		btn.tooltip.text = text
		if (btn.parent != null && btn.parent is Label) {
			(btn.parent as Label).tooltip.text = btn.tooltip.text
		}
	}

	/**
	 * Runner for "Get server status" thread
	 */
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

	/**
	 * Sub-Runner for "Get server status" thread - does the actual polling of the server's status
	 */
	private fun runnerGetServerStatusSub() = Runnable {
		if (connectionLost) {
			uiProps.connectionOpen.value = false
			uiProps.statusMsg.value = "Connection lost"
			connectionLost = false
		} else if (capture?.isOpened == true) {
			apiClientFncs?.getServerStatus(true)
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
				val tmpRes = capture?.read(frame) ?: false
				if (! tmpRes) {
					connectionLost = true
					capture?.release()
				}

				// if the frame is not empty, process it
				if (tmpRes && ! frame.empty()) {
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

	/**
	 * Initialize the window size related properties
	 */
	fun initWindowSize() {
		//println("externalInitWindowSize: anchor ${imageAnchorPane.width.toInt()}x${imageAnchorPane.height.toInt()}")
		cameraRatio = imageAnchorPane.width / imageAnchorPane.height
		updateWindowSize(imageAnchorPane.width.toInt(), imageAnchorPane.height.toInt() + bottomAnchorPane.height.toInt())
	}

	/**
	 * Update the window size related properties - this allows the camera image view's size to be responsive
	 */
	fun updateWindowSize(newWidth: Int, newHeight: Int) {
		//println("updateWindowSize: wnd ${newWidth}x${newHeight}")

		//println("updateWindowSize: ratio=${cameraRatio.format(2)}")
		if (cameraRatio < 0.0) {
			return
		}

		/**
		 * When entering/exiting fullscreen mode on macOS the size of the imageAnchorPane
		 * doesn't properly update for some reason. Probably because of a race condition.
		 * Using its parent BorderPane here instead.
		 */
		if (imageAnchorPane.parent !is BorderPane) {
			System.err.println("parent of #imageAnchorPane needs to be BorderPane. Is '${imageAnchorPane.parent.typeSelector}'")
			return
		}
		val tmpBp = imageAnchorPane.parent as BorderPane
		val areaForImage = AreaSize(w = tmpBp.width.toInt(), h = (tmpBp.height - bottomAnchorPane.height).toInt())
		if ((newWidth.toDouble() / cameraRatio) + bottomAnchorPane.height > newHeight) {
			//println("updateWindowSize: case 1 - window wider than image")
			currentFrame.fitHeight = areaForImage.h.toDouble()
			currentFrame.fitWidth = round(areaForImage.h * cameraRatio)

			if (areaForImage.h + bottomAnchorPane.height > newHeight) {
				// if the image is now too high we need to make it narrower again
				currentFrame.fitHeight = round(newHeight.toDouble() - bottomAnchorPane.height)
				currentFrame.fitWidth = round(currentFrame.fitHeight * cameraRatio)
			}
			//
			currentFrame.x = round((areaForImage.w - currentFrame.fitWidth) / 2.0)
			currentFrame.y = 0.0
		} else {
			//println("updateWindowSize: case 2 - window higher than image")
			currentFrame.fitWidth = newWidth.toDouble()
			currentFrame.fitHeight = round(newWidth.toDouble() / cameraRatio)
			//
			currentFrame.x = 0.0
			currentFrame.y = round((areaForImage.h - currentFrame.fitHeight) / 2.0)
		}
		//println("updateWindowSize: img ${currentFrame.fitWidth}x${currentFrame.fitHeight}")
		//println("updateWindowSize:     bot.h: ${bottomAnchorPane.height.toInt()}")
		//println("updateWindowSize:     afi ${areaForImage.w}x${areaForImage.h}")
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
				uiProps.statusMsg.set("Connected")
			} else {
				System.err.println("Could not open server connection...")
				uiProps.statusMsg.set("Could not open server connection")
			}
		} else {
			// the camera is not active at this point
			uiProps.connectionOpen.value = false
			// update UI
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
		var nextZoomLevel = uiProps.ctrlZoomLevel.value
		when (event!!.target) {
			ctrlZoomPlusBtn -> nextZoomLevel -= 10
			ctrlZoomMinusBtn -> nextZoomLevel += 10
			else -> nextZoomLevel = 100
		}
		apiClientFncs?.setZoom(nextZoomLevel)
		//
		moveFocusAwayFromControls()
	}

	/**
	 * Event: Slider "B&C: Brightness" mouse dragged
	 *
	 * @param event
	 */
	@FXML
	protected fun evtBncBrightnSlidMouseDragged(event: MouseEvent?) {
		bncBrightnSlidValueChanged()
	}

	/**
	 * Event: Slider "B&C: Brightness" mouse clicked
	 *
	 * @param event
	 */
	@FXML
	protected fun evtBncBrightnSlidMouseClicked(event: MouseEvent?) {
		bncBrightnSlidValueChanged()
	}

	/**
	 * Event: Slider "B&C: Brightness" key released
	 *
	 * @param event
	 */
	@FXML
	protected fun evtBncBrightnSlidKeyRel(event: KeyEvent?) {
		bncBrightnSlidValueChanged()
	}

	/**
	 * Handle value change of Slider "B&C: Brightness"
	 */
	private fun bncBrightnSlidValueChanged() {
		val newVal = round(bncBrightnSlid.value).toInt()
		if (newVal == lastBncBrightnVal) {
			return
		}
		lastBncBrightnVal = newVal
		//
		apiClientFncs?.setBrightness(newVal)
	}

	/**
	 * Event: Slider "B&C: Contrast" mouse dragged
	 *
	 * @param event
	 */
	@FXML
	protected fun evtBncContrSlidMouseDragged(event: MouseEvent?) {
		bncContrSlidValueChanged()
	}

	/**
	 * Event: Slider "B&C: Contrast" mouse clicked
	 *
	 * @param event
	 */
	@FXML
	protected fun evtBncContrSlidMouseClicked(event: MouseEvent?) {
		bncContrSlidValueChanged()
	}

	/**
	 * Event: Slider "B&C: Contrast" key released
	 *
	 * @param event
	 */
	@FXML
	protected fun evtBncContrSlidKeyRel(event: KeyEvent?) {
		bncContrSlidValueChanged()
	}

	/**
	 * Handle value change of Slider "B&C: Contrast"
	 */
	private fun bncContrSlidValueChanged() {
		val newVal = round(bncContrSlid.value).toInt()
		if (newVal == lastBncContrVal) {
			return
		}
		lastBncContrVal = newVal
		//
		apiClientFncs?.setContrast(newVal)
	}
}
