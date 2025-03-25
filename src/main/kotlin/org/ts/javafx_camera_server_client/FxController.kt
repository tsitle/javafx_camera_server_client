package org.ts.javafx_camera_server_client

import javafx.application.Platform
import javafx.concurrent.Task
import javafx.embed.swing.SwingFXUtils
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import org.openapitools.client.apis.DefaultApi
import org.openapitools.client.models.StatusCams
import org.ts.javafx_camera_server_client.mjpeg_stream.MjpegStream
import org.ts.javafx_camera_server_client.mjpeg_stream.MjpegViewer
import java.awt.*
import java.awt.font.GlyphVector
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.net.ConnectException
import java.net.MalformedURLException
import java.net.UnknownHostException
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.round
import kotlin.random.Random


fun Double.format(digits: Int) = "%.${digits}f".format(this)

data class AreaSize(val w: Int, val h: Int)


open class FxController : MjpegViewer {
	// output width of the camera image
	private val cameraOutputWidth: Int = 800

	/**
	 * Output FPS (frames per second) of camera stream.
	 * If the value is higher than what the MJPEG stream provides it won't make any difference.
	 * But if the value is lower than what the MJPEG stream provides it will reduce CPU load.
	 */
	private val cameraOutputFps: Int = 25

	// output a new frame every x ms
	private val cameraOutputTimeoutMs = round(1000.0 / cameraOutputFps.toDouble()).toLong()

	// add timestamp to camera image?
	private val cameraAddTimestamp: Boolean = false

	// ------------------------------------------------------------------------

	@FXML
	private lateinit var imageAnchorPane: AnchorPane
	@FXML
	private lateinit var bottomAnchorPane: AnchorPane
	@FXML
	private lateinit var currentFrame: ImageView
	@FXML
	private lateinit var statusLbl: Label
	@FXML
	private lateinit var conConnectBtn: Button
	@FXML
	private lateinit var conServerUrlTxtfld: TextField
	@FXML
	private lateinit var conServerApiKeyTxtfld: TextField
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

	// ------------------------------------------------------------------------

	// flag for stopping the 'get server status' thread
	private var doKillThreadStatus = false

	// the MJPEG stream decoder
	private var capture: MjpegStream = MjpegStream()

	// width to height ratio of the camera image
	private var cameraRatio = -1.0

	// observable properties
	private val uiProps =  UiPropsContainer()

	// API Client Functions Wrapper
	private var apiClientFncs: ApiClientFncs? = null

	// are the ObservableProperty listeners allowed to output to the console already?
	private var obsPropsPrintlnAllowed = false

	// last value of Slider "B&C: Brightness"
	private var lastBncBrightnVal = 0

	// last value of Slider "B&C: Contrast"
	private var lastBncContrVal = 0

	private var lastFrameOutputTime: Long = -1
	private var avgOutputTime: Long = 0
	private var avgOutputCounter: Int = 0
	private var scaledBufImg: BufferedImage? = null
	private var scaledGraph2d: Graphics2D? = null
	private var lastCameraOutputHeight = -1

	// -----------------------------------------------------------------------------------------------------------------

	/**
	 * Initialize UI controller
	 */
	@FXML
	protected fun initialize() {
		// get Server URL from env
		val tmpServerUrl = System.getenv("JFXCSC_URL") ?: ""
		if (tmpServerUrl.isNotEmpty()) {
			conServerUrlTxtfld.text = tmpServerUrl
		}
		// get Server API Key Hash from env
		val tmpServerApiKeyHash = System.getenv("JFXCSC_APIKEYHASH") ?: ""
		if (tmpServerApiKeyHash.isNotEmpty()) {
			conServerApiKeyTxtfld.text = tmpServerApiKeyHash
		}

		//
		initUiPropHandling()

		// start the 'get server status' thread
		val threadStatus = Thread(runnerGetServerStatusThread())
		threadStatus.isDaemon = false
		threadStatus.start()

		// load camera stream
		if (tmpServerUrl.isNotEmpty() && conServerApiKeyTxtfld.text.isNotEmpty()) {
			conConnectBtn.fire()
		}
	}

	/**
	 * Initialize uiProps handling
	 */
	private fun initUiPropHandling() {
		uiProps.apiClientLostConnection.subscribe { it -> if (it) {
			Platform.runLater {
				uiProps.connectionOpen.value = false
				uiProps.statusMsg.value = "Connection lost"
				//
				connectionClose()
			}
		}}

		//
		uiProps.clientId.subscribe { it -> if (obsPropsPrintlnAllowed && ! doKillThreadStatus) {
			println("Client ID: ${it.toInt()}")
		}}
		uiProps.clientId.value = (Random.nextDouble() * 1000.0).toInt()

		//
		statusLbl.textProperty().bind(uiProps.statusMsg)
		statusLbl.textProperty().subscribe { it -> if (obsPropsPrintlnAllowed && ! doKillThreadStatus) {
			println("Status: '$it'")
		}}

		//
		uiProps.ctrlShowGrid.subscribe { it -> if (! doKillThreadStatus) {
			if (obsPropsPrintlnAllowed) {
				println("Show Grid: $it")
			}
			ctrlShowGridCbx.isSelected = it
		}}

		//
		uiProps.connectionOpen.subscribe { it -> if (! doKillThreadStatus) {
			conConnectBtn.styleClass.removeAll("btn-danger", "btn-default", "btn-success")
			conConnectBtn.styleClass.add(if (it) "btn-danger" else "btn-success")
			conConnectBtn.text = (if (it) "Disconnect" else "Connect")
			setTooltipOfButton(conConnectBtn, if (it) "Disconnect from server" else "Connect to server")

			setIsDisabledForAllConnectionControls(value = it, doSetConnectBtn = false)

			handleUiPropChangeForCtrlCamButtons()
			ctrlShowGridCbx.isDisable = ! it
			handleUiPropChangeForCtrlZoomButtons()

			bncBrightnSlid.isDisable = ! it
			bncContrSlid.isDisable = ! it
		}}

		//
		uiProps.serverAppVersion.subscribe { it -> if (obsPropsPrintlnAllowed && ! doKillThreadStatus) {
			println("Server app version: $it")
		}}

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
	 * Enable/disable all controls in the "Connection" tab
	 */
	private fun setIsDisabledForAllConnectionControls(value: Boolean, doSetConnectBtn: Boolean) {
		if (doSetConnectBtn) {
			conConnectBtn.isDisable = value
		}
		conServerUrlTxtfld.isDisable = value
		conServerApiKeyTxtfld.isDisable = value
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
			if (++readStatusTo == 10 && ! doKillThreadStatus) {
				if (! uiProps.apiClientLostConnection.value && capture.isOpened) {
					apiClientFncs?.getServerStatus(true)
				}
				readStatusTo = 0
			}
		}
		println("ending threadStatus")
	}

	/**
	 * On application close, stop the acquisition from the camera
	 */
	fun setClosed() {
		doKillThreadStatus = true
		//
		connectionClose()
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
	 * Open server connection
	 */
	private fun connectionOpen() {
		if (conServerUrlTxtfld.text.isEmpty() || conServerApiKeyTxtfld.text.isEmpty()) {
			// update UI
			uiProps.statusMsg.set("Cannot connect because of missing URL or API Key")
			setIsDisabledForAllConnectionControls(value = false, doSetConnectBtn = true)
			return
		}

		//
		val that = this
		val task: Task<Boolean> = object : Task<Boolean>() {
			@Throws(java.lang.Exception::class)
			override fun call(): Boolean {
				// start the video capture
				capture.openStream(
						that,
						"${conServerUrlTxtfld.text}/stream.mjpeg?cid=${uiProps.clientId.value}"
					)

				return (capture.isOpened)  // is the video stream available?
			}
		}

		task.setOnSucceeded { _ ->  // we're on the JavaFX application thread here
			val result: Boolean = task.getValue()
			if (result) {
				obsPropsPrintlnAllowed = true
				// update UI
				uiProps.connectionOpen.value = true
				uiProps.statusMsg.set("Connected")
			} else {
				// update UI
				uiProps.statusMsg.set("Could not open server connection")
				setIsDisabledForAllConnectionControls(value = false, doSetConnectBtn = false)
			}
			// update UI
			conConnectBtn.isDisable = false
		}

		task.setOnFailed { _ ->  // we're on the JavaFX application thread here
			// update UI
			val errMsg: String = when (val tmpEx = task.exception) {
				is UnknownHostException -> "Unknown host"
				is ConnectException ->  "Could not connect to host"
				is IllegalArgumentException -> "Illegal argument: ${tmpEx.message}"
				is MalformedURLException -> "Malformed URL: ${tmpEx.message}"
				else -> tmpEx.toString()
			}
			uiProps.statusMsg.value = "Error: $errMsg"
			setIsDisabledForAllConnectionControls(value = false, doSetConnectBtn = true)
		}

		//
		apiClientFncs = ApiClientFncs(
				conServerUrlTxtfld.text,
				conServerApiKeyTxtfld.text,
				uiProps
			)
		// update UI
		uiProps.statusMsg.set("Connecting to '${conServerUrlTxtfld.text}'...")
		//
		uiProps.apiClientLostConnection.value = false
		// reset output FPS data
		lastFrameOutputTime = -1
		avgOutputTime = 0
		avgOutputCounter = 0
		//
		Thread(task).start()
	}

	/**
	 * Close server connection
	 */
	private fun connectionClose() {
		// the camera is not active at this point
		uiProps.connectionOpen.value = false
		// update UI
		if (! uiProps.apiClientLostConnection.value) {
			uiProps.statusMsg.set("Disconnected")
		}
		conConnectBtn.isDisable = false
		//
		capture.closeStream()
		//
		drawConnectionClosedOverFrame()
	}

	/**
	 * Adds a timestamp to the image
	 */
	private fun addTimestampToFrame(frame: BufferedImage) {
		val g2d = frame.graphics.create() as Graphics2D
		try {
			g2d.color = Color.WHITE
			g2d.drawString(Date().toString(), 10, frame.height - 50)
		} finally {
			g2d.dispose()
		}
	}

	/**
	 * Draw over the camera image in [currentFrame] to show that the connection has been closed
	 */
	private fun drawConnectionClosedOverFrame() {
		if (currentFrame.image == null) {
			return
		}
		val bufImg: BufferedImage = SwingFXUtils.fromFXImage(currentFrame.image, null)
		val g2d = bufImg.createGraphics()
		try {
			g2d.color = Color.DARK_GRAY
			g2d.composite = AlphaComposite.SrcOver.derive(0.8f)
			g2d.fillRect(0, 0, bufImg.width, bufImg.height)
			//
			///
			g2d.composite = AlphaComposite.SrcOver.derive(0.5f)
			g2d.font = Font(Font.SANS_SERIF, Font.BOLD, 24)
			val outpStr = "Connection " + (if (uiProps.apiClientLostConnection.value) "lost" else "closed")
			///
			/*
			val fm: FontMetrics = g2d.fontMetrics
			val stringWidth = fm.stringWidth(outpStr)
			g2d.color = Color.PINK
			g2d.drawString(outpStr, (bufImg.width / 2) - (stringWidth / 2), (bufImg.height / 2) + (fm.height / 2) - (fm.height / 2))
			*/
			///
			/*
			g2d.color = Color.CYAN
			val charBounds: Rectangle2D = g2d.font.getStringBounds(outpStr, g2d.fontRenderContext)
			g2d.drawRect(charBounds.x.toInt(), charBounds.y.toInt(), charBounds.width.toInt(), charBounds.height.toInt())
			*/
			///
			val gv: GlyphVector = g2d.font.layoutGlyphVector(
					g2d.fontRenderContext,
					outpStr.toCharArray(),
					0,
					outpStr.length,
					GlyphVector.FLAG_MASK
				)
			val bounds: Rectangle2D = gv.visualBounds
			///
			val ox = (bufImg.width / 2) - (bounds.width.toInt() / 2)
			val oy = (bufImg.height / 2) + (bounds.height.toInt() / 2) - 2
			g2d.color = Color.RED
			g2d.drawString(outpStr, ox, oy)
			///
			/*
			g2d.translate(ox, oy)
			g2d.color = Color.ORANGE
			g2d.drawRect(bounds.x.toInt(), bounds.y.toInt() - 2, bounds.width.toInt(), bounds.height.toInt() + 2)
			*/
		} finally {
			g2d.dispose()
		}
		val fxImg: Image = SwingFXUtils.toFXImage(bufImg, null)
		Platform.runLater {
			currentFrame.imageProperty().set(fxImg)
		}
	}

	// -----------------------------------------------------------------------------------------------------------------

	/**
	 * Update the [ImageView] with the last received camera image
	 *
	 * @param rawByteArrayInputStream Raw image data
	 */
	override fun mjpegSetRawImageData(rawByteArrayInputStream: ByteArrayInputStream) {
		val timeNow: Long = System.currentTimeMillis()
		val timeDelta: Long = (if (timeNow >= lastFrameOutputTime) {timeNow - lastFrameOutputTime} else {lastFrameOutputTime - timeNow})

		if (lastFrameOutputTime > 0 && timeDelta + 15 < cameraOutputTimeoutMs) {
			return  // skip frame
		}

		if (lastFrameOutputTime > 0) {
			avgOutputTime += timeDelta
			if (++avgOutputCounter == cameraOutputFps * 10) {  // output every 10s
				val tmpAvg = avgOutputTime.toDouble() / avgOutputCounter.toDouble()
				println(
						"Output: ${
							(1000.0 / tmpAvg).format(1)
						} fps (target=${cameraOutputFps.toDouble().format(1)}, input=${uiProps.inputFps.value})"
					)
				avgOutputCounter = 0
				avgOutputTime = 0
			}
		}

		//
		val lastCameraRatio = cameraRatio
		val fxImg: Image
		val image: BufferedImage = ImageIO.read(rawByteArrayInputStream)

		if (cameraAddTimestamp) {
			addTimestampToFrame(image)
		}

		cameraRatio = image.width.toDouble() / image.height.toDouble()

		// scale the image to a fixed width
		if (cameraOutputWidth != image.width) {
			val scaledWidth = cameraOutputWidth
			val scaledHeight = (cameraOutputWidth.toDouble() / cameraRatio).toInt()
			// scale with high quality settings
			if (lastCameraOutputHeight != scaledHeight || scaledBufImg == null) {
				lastCameraOutputHeight = scaledHeight
				//
				if (scaledGraph2d != null) {
					scaledGraph2d?.dispose()
				}
				if (scaledBufImg != null) {
					scaledBufImg?.flush()
				}
				scaledBufImg = BufferedImage(scaledWidth, scaledHeight, image.type)
				scaledGraph2d = scaledBufImg!!.createGraphics()
				scaledGraph2d?.composite = AlphaComposite.Src
				scaledGraph2d?.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
				scaledGraph2d?.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
				scaledGraph2d?.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
			}
			scaledGraph2d?.drawImage(image, 0, 0, scaledWidth, scaledHeight, null)

			// convert BufferedImage to JavaFX Image
			fxImg = SwingFXUtils.toFXImage(scaledBufImg!!, null)
		} else {
			// convert BufferedImage to JavaFX Image
			fxImg = SwingFXUtils.toFXImage(image, null)
		}

		Platform.runLater {
			// update window size due to [cameraRatio]
			if (cameraRatio > 0.0 && lastCameraRatio != cameraRatio && imageAnchorPane.width > 0.0) {
				updateWindowSize(imageAnchorPane.width.toInt(), imageAnchorPane.height.toInt() + bottomAnchorPane.height.toInt())
			}
			// output the new image
			currentFrame.imageProperty().set(fxImg)
			//
			lastFrameOutputTime = System.currentTimeMillis()
		}
	}

	override fun mjpegLogError(msg: String) {
		System.err.println("MjpegStream Error: $msg")
	}

	override fun mjpegLostConnection() {
		uiProps.apiClientLostConnection.value = true
	}

	// -----------------------------------------------------------------------------------------------------------------

	/**
	 * Event: Button "Connection: Connect" pressed
	 *
	 * @param event
	 */
	@FXML
	protected fun evtConConnect(event: ActionEvent?) {
		// update UI
		setIsDisabledForAllConnectionControls(value = true, doSetConnectBtn = true)
		//
		if (! uiProps.connectionOpen.value) {
			connectionOpen()
		} else {
			connectionClose()
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
