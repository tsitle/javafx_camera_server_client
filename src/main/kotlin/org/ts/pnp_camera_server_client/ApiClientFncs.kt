package org.ts.pnp_camera_server_client

import javafx.concurrent.Task
import org.openapitools.client.apis.DefaultApi
import org.openapitools.client.infrastructure.ApiClient
import org.openapitools.client.models.Status
import org.openapitools.client.models.StatusCams


class ApiClientFncs(serverUrl: String = "", apiKey: String = "") {
	private var apiInstance: DefaultApi? = null
	private var uiProps: UiPropsContainer? = null

	init {
		if (serverUrl.isEmpty()) {
			throw IllegalArgumentException("Server URL is required")
		}
		if (apiKey.isEmpty()) {
			throw IllegalArgumentException("API Key is required")
		}
		ApiClient.apiKey["X-API-KEY"] = apiKey
		apiInstance = DefaultApi(serverUrl)
	}

	constructor(
				serverUrl: String,
				apiKey: String,
				uiProps: UiPropsContainer
			) : this(serverUrl, apiKey) {
		this.uiProps = uiProps
	}

	/**
	 * Get server's status
	 *
	 * @param isForTimer Is function call is for status thread?
	 */
	fun getServerStatus(isForTimer: Boolean) {
		if (! uiProps!!.connectionOpen.value) {
			return
		}

		//
		val task: Task<Status> = object : Task<Status>() {
			@Throws(java.lang.Exception::class)
			override fun call(): Status {
				return apiInstance!!.getStatus(uiProps!!.clientId.get())
			}
		}

		task.setOnSucceeded { _ ->  // we're on the JavaFX application thread here
			if (! isForTimer) {
				return@setOnSucceeded
			}
			val resultStat: Status = task.getValue()
			if (resultStat.result != Status.Result.success) {
				uiProps!!.statusMsg.set("Error reading status from server")
				return@setOnSucceeded
			}
			val tmpProcRoiEn = resultStat.enabledProc?.roi ?: false
			val tmpProcRoiSz = resultStat.procRoi?.sizePerc ?: -1
			val tmpZoomStr = (if (tmpProcRoiEn) ", Zoom ${(1.0 + (1.0 - (tmpProcRoiSz.toDouble() / 100.0))).format(1)}x" else "")

			uiProps!!.serverAppVersion.value = resultStat.version
			//
			uiProps!!.statusMsg.value = "Connected [${
				resultStat.cpuTemperature?.toDouble()?.format(1)
			} °C, FPS ${resultStat.framerate}${tmpZoomStr}]"
			//
			uiProps!!.ctrlShowGrid.value = resultStat.procGrid?.show ?: false
			//
			when (resultStat.availOutputCams) {
				StatusCams.L -> {
					uiProps!!.ctrlCamAvailLeft.value = true
					uiProps!!.ctrlCamAvailBoth.value = false
					uiProps!!.ctrlCamAvailRight.value = false
				}
				StatusCams.R -> {
					uiProps!!.ctrlCamAvailLeft.value = false
					uiProps!!.ctrlCamAvailBoth.value = false
					uiProps!!.ctrlCamAvailRight.value = true
				}
				else -> {
					uiProps!!.ctrlCamAvailLeft.value = true
					uiProps!!.ctrlCamAvailBoth.value = true
					uiProps!!.ctrlCamAvailRight.value = true
				}
			}
			uiProps!!.ctrlCamActive.value = resultStat.outputCams?.ordinal ?: -1
			//
			uiProps!!.ctrlZoomLevel.value = tmpProcRoiSz
			uiProps!!.ctrlZoomAllowed.value = (tmpProcRoiEn && (resultStat.enabledProc?.scale ?: false))
			//
			uiProps!!.bncBrightnVal.value = resultStat.procBnc?.brightness?.`val` ?: 0
			uiProps!!.bncBrightnMin.value = resultStat.procBnc?.brightness?.min ?: 0
			uiProps!!.bncBrightnMax.value = resultStat.procBnc?.brightness?.max ?: 0
			uiProps!!.bncBrightnAllowed.value = resultStat.procBnc?.brightness?.supported ?: false
			//
			uiProps!!.bncContrVal.value = resultStat.procBnc?.contrast?.`val` ?: 0
			uiProps!!.bncContrMin.value = resultStat.procBnc?.contrast?.min ?: 0
			uiProps!!.bncContrMax.value = resultStat.procBnc?.contrast?.max ?: 0
			uiProps!!.bncContrAllowed.value = resultStat.procBnc?.contrast?.supported ?: false
		}

		task.setOnFailed { _ ->  // we're on the JavaFX application thread here
			uiProps!!.statusMsg.set("Exception calling DefaultApi#getStatus: ${task.getException().message}")
		}

		Thread(task).start()
	}

	/**
	 * Enable or disable grid overlay
	 *
	 * @param doShow Show grid?
	 */
	fun setShowGrid(doShow: Boolean) {
		val task: Task<Status> = object : Task<Status>() {
			@Throws(java.lang.Exception::class)
			override fun call(): Status {
				return apiInstance!!.procGridShow(if (doShow) 1 else 0)
			}
		}

		task.setOnSucceeded { _ ->  // we're on the JavaFX application thread here
			val resultStat: Status = task.getValue()
			if (resultStat.result != Status.Result.success) {
				uiProps!!.statusMsg.set("Could not toggle ShowGrid")
			}
		}

		task.setOnFailed { _ ->  // we're on the JavaFX application thread here
			uiProps!!.statusMsg.set("Exception calling DefaultApi#procGridShow: ${task.getException().message}")
		}

		Thread(task).start()
	}

	/**
	 * Set active camera(s)
	 *
	 * @param cam Camera(s) to activate
	 */
	fun setActiveCam(cam: DefaultApi.CamOutputCamEnable) {
		val task: Task<Status?> = object : Task<Status?>() {
			@Throws(java.lang.Exception::class)
			override fun call(): Status? {
				var doNothing = false
				var doSwap = false
				var doCamEnable : DefaultApi.CamOutputCamEnable? = null
				var doCamDisable : DefaultApi.CamOutputCamDisable? = null
				val tmpOutputCams = uiProps!!.ctrlCamActive.value
				if ((cam == DefaultApi.CamOutputCamEnable.L && tmpOutputCams == StatusCams.L.ordinal) ||
						(cam == DefaultApi.CamOutputCamEnable.R && tmpOutputCams == StatusCams.R.ordinal) ||
						(cam == DefaultApi.CamOutputCamEnable.BOTH && tmpOutputCams == StatusCams.BOTH.ordinal)) {
					doNothing = true
				} else if ((cam == DefaultApi.CamOutputCamEnable.L && tmpOutputCams == StatusCams.R.ordinal) ||
						(cam == DefaultApi.CamOutputCamEnable.R && tmpOutputCams == StatusCams.L.ordinal)) {
					doSwap = true
				} else if (cam == DefaultApi.CamOutputCamEnable.BOTH && tmpOutputCams == StatusCams.L.ordinal) {
					doCamEnable = DefaultApi.CamOutputCamEnable.R
				} else if (cam == DefaultApi.CamOutputCamEnable.BOTH && tmpOutputCams == StatusCams.R.ordinal) {
					doCamEnable = DefaultApi.CamOutputCamEnable.L
				} else if (cam == DefaultApi.CamOutputCamEnable.L && tmpOutputCams == StatusCams.BOTH.ordinal) {
					doCamDisable = DefaultApi.CamOutputCamDisable.R
				} else if (cam == DefaultApi.CamOutputCamEnable.R && tmpOutputCams == StatusCams.BOTH.ordinal) {
					doCamDisable = DefaultApi.CamOutputCamDisable.L
				}

				//
				if (! doNothing && doCamEnable != null) {
					return apiInstance!!.outputCamEnable(doCamEnable)
				}
				if (! doNothing && doCamDisable != null) {
					return apiInstance!!.outputCamDisable(doCamDisable)
				}
				if (! doNothing && doSwap) {
					return apiInstance!!.outputCamSwap()
				}
				return null
			}
		}

		task.setOnSucceeded { _ ->  // we're on the JavaFX application thread here
			val resultStat: Status? = task.getValue()
			if (resultStat != null && resultStat.result != Status.Result.success) {
				uiProps!!.statusMsg.set("Could not change active camera")
			}
		}

		task.setOnFailed { _ ->  // we're on the JavaFX application thread here
			uiProps!!.statusMsg.set("Exception calling DefaultApi#outputCamXxx: ${task.getException().message}")
		}

		Thread(task).start()
	}

	/**
	 * Set zoom level
	 *
	 * @param valuePerc Zoom level in percent
	 */
	fun setZoom(valuePerc: Int) {
		val task: Task<Status> = object : Task<Status>() {
			@Throws(java.lang.Exception::class)
			override fun call(): Status {
				return apiInstance!!.procRoiSize(valuePerc)
			}
		}

		task.setOnSucceeded { _ ->  // we're on the JavaFX application thread here
			val resultStat: Status = task.getValue()
			if (resultStat.result != Status.Result.success) {
				uiProps!!.statusMsg.set("Could not set zoom level")
			}
		}

		task.setOnFailed { _ ->  // we're on the JavaFX application thread here
			uiProps!!.statusMsg.set("Exception calling DefaultApi#procRoiSize: ${task.getException().message}")
		}

		Thread(task).start()
	}

	/**
	 * Set brightness
	 *
	 * @param valuePerc Brightness in percent
	 */
	fun setBrightness(valuePerc: Int) {
		val task: Task<Status> = object : Task<Status>() {
			@Throws(java.lang.Exception::class)
			override fun call(): Status {
				return apiInstance!!.procBncBrightness(valuePerc)
			}
		}

		task.setOnSucceeded { _ ->  // we're on the JavaFX application thread here
			val resultStat: Status = task.getValue()
			if (resultStat.result != Status.Result.success) {
				uiProps!!.statusMsg.set("Could not set brightness")
			}
		}

		task.setOnFailed { _ ->  // we're on the JavaFX application thread here
			uiProps!!.statusMsg.set("Exception calling DefaultApi#procBncBrightness: ${task.getException().message}")
		}

		Thread(task).start()
	}

	/**
	 * Set contrast
	 *
	 * @param valuePerc Contrast in percent
	 */
	fun setContrast(valuePerc: Int) {
		val task: Task<Status> = object : Task<Status>() {
			@Throws(java.lang.Exception::class)
			override fun call(): Status {
				return apiInstance!!.procBncContrast(valuePerc)
			}
		}

		task.setOnSucceeded { _ ->  // we're on the JavaFX application thread here
			val resultStat: Status = task.getValue()
			if (resultStat.result != Status.Result.success) {
				uiProps!!.statusMsg.set("Could not set contrast")
			}
		}

		task.setOnFailed { _ ->  // we're on the JavaFX application thread here
			uiProps!!.statusMsg.set("Exception calling DefaultApi#procBncContrast: ${task.getException().message}")
		}

		Thread(task).start()
	}
}
