package org.ts.pnp_camera_server_client

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
	fun getServerStatus(isForTimer: Boolean): Status? {
		if (! uiProps!!.connectionOpen.value) {
			return null
		}
		//
		val resultStat: Status
		try {
			resultStat = apiInstance!!.getStatus(uiProps!!.clientId.get())
		} catch (e: Exception) {
			uiProps!!.statusMsg.set("Exception calling DefaultApi#getStatus: ${e.message}")
			//e.printStackTrace()
			return null
		}
		if (resultStat.result != Status.Result.success) {
			uiProps!!.statusMsg.set("Error reading status from server")
		} else {
			if (isForTimer) {
				uiProps!!.serverAppVersion.value = resultStat.version
				//
				uiProps!!.statusMsg.value = "Connected [${
						resultStat.cpuTemperature?.toDouble()?.format(2)
					} °C, FPS ${resultStat.framerate}]"
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
				uiProps!!.ctrlZoomLevel.value = resultStat.procRoi?.sizePerc ?: -1
				uiProps!!.ctrlZoomAllowed.value = resultStat.enabledProc?.scale ?: false
				//
				uiProps!!.bncBrightnVal.value = resultStat.procBnc?.brightness?.`val` ?: 0
				uiProps!!.bncBrightnMin.value = resultStat.procBnc?.brightness?.min ?: 0
				uiProps!!.bncBrightnMax.value = resultStat.procBnc?.brightness?.max ?: 0
				uiProps!!.bncBrightnAllowed.value = resultStat.procBnc?.brightness?.supported ?: false
			}
			return resultStat
		}
		return null
	}

	/**
	 * Enable or disable grid overlay
	 *
	 * @param doShow Show grid?
	 */
	fun setShowGrid(doShow: Boolean) {
		try {
			val resultPost : Status = apiInstance!!.procGridShow(if (doShow) 1 else 0)
			if (resultPost.result != Status.Result.success) {
				uiProps!!.statusMsg.set("Could not toggle ShowGrid")
			}
		} catch (e: Exception) {
			uiProps!!.statusMsg.set("Exception calling DefaultApi#procGridShow: ${e.message}")
			//e.printStackTrace()
		}
	}

	/**
	 * Set active camera(s)
	 *
	 * @param cam Camera(s) to activate
	 */
	fun setActiveCam(cam: DefaultApi.CamOutputCamEnable) {
		try {
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
			if (! doNothing && doCamEnable != null) {
				val resultPost : Status = apiInstance!!.outputCamEnable(doCamEnable)
				if (resultPost.result != Status.Result.success) {
					uiProps!!.statusMsg.set("Could not enable camera $doCamEnable")
				}
			} else if (! doNothing && doCamDisable != null) {
				val resultPost : Status = apiInstance!!.outputCamDisable(doCamDisable)
				if (resultPost.result != Status.Result.success) {
					uiProps!!.statusMsg.set("Could not deactivate camera $doCamDisable")
				}
			} else if (! doNothing && doSwap) {
				val resultPost : Status = apiInstance!!.outputCamSwap()
				if (resultPost.result != Status.Result.success) {
					uiProps!!.statusMsg.set("Could not swap active camera")
				}
			}
		} catch (e: Exception) {
			uiProps!!.statusMsg.set("Exception calling DefaultApi#outputCamXxx: ${e.message}")
			//e.printStackTrace()
		}
	}

	/**
	 * Set zoom level
	 *
	 * @param valuePerc Zoom level in percent
	 */
	fun setZoom(valuePerc: Int) {
		try {
			val resultPost : Status = apiInstance!!.procRoiSize(valuePerc)
			if (resultPost.result != Status.Result.success) {
				uiProps!!.statusMsg.set("Could not set zoom level")
			}
		} catch (e: Exception) {
			uiProps!!.statusMsg.set("Exception calling DefaultApi#procRoiSize: ${e.message}")
			//e.printStackTrace()
		}
	}

	/**
	 * Set brightness
	 *
	 * @param valuePerc Brightness in percent
	 */
	fun setBrightness(valuePerc: Int) {
		try {
			val resultPost : Status = apiInstance!!.procBncBrightness(valuePerc)
			if (resultPost.result != Status.Result.success) {
				uiProps!!.statusMsg.set("Could not set brightness")
			}
		} catch (e: Exception) {
			uiProps!!.statusMsg.set("Exception calling DefaultApi#procBncBrightness: ${e.message}")
			//e.printStackTrace()
		}
	}
}
