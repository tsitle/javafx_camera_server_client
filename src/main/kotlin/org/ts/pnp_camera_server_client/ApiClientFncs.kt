package org.ts.pnp_camera_server_client

import javafx.beans.property.BooleanProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.StringProperty
import org.openapitools.client.apis.DefaultApi
import org.openapitools.client.infrastructure.ApiClient
import org.openapitools.client.models.Status
import org.openapitools.client.models.StatusCams


class ApiClientFncs(serverUrl: String = "", apiKey: String = "") {
	private var apiInstance: DefaultApi? = null
	private var uiPropConnection: BooleanProperty? = null
	private var uiPropClientId: IntegerProperty? = null
	private var uiPropStatus: StringProperty? = null
	private var uiPropCtrlShowGrid: BooleanProperty? = null

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
				uiPropConnection: BooleanProperty,
				uiPropClientId: IntegerProperty,
				uiPropStatus: StringProperty,
				uiPropShowGrid: BooleanProperty
			) : this(serverUrl, apiKey) {
		this.uiPropConnection = uiPropConnection
		this.uiPropClientId = uiPropClientId
		this.uiPropStatus = uiPropStatus
		this.uiPropCtrlShowGrid = uiPropShowGrid
	}

	fun getServerStatus(isForTimer: Boolean): Status? {
		if (! uiPropConnection!!.value) {
			return null
		}
		//
		val resultStat: Status
		try {
			resultStat = apiInstance!!.getStatus(uiPropClientId!!.get())
		} catch (e: Exception) {
			uiPropStatus!!.set("Exception calling DefaultApi#getStatus: ${e.message}")
			//e.printStackTrace()
			return null
		}
		if (resultStat.result != Status.Result.success) {
			uiPropStatus!!.set("Error reading status from server")
		} else {
			if (isForTimer) {
				uiPropStatus!!.value = "Connected [${
						resultStat.cpuTemperature?.toDouble()?.format(2)
					} °C, FPS ${resultStat.framerate}]"
				uiPropCtrlShowGrid!!.value = resultStat.procGrid?.show ?: false
			}
			return resultStat
		}
		return null
	}

	fun setShowGrid(doShow: Boolean) {
		val resultStat : Status = getServerStatus(false) ?: return

		try {
			if (resultStat.result != Status.Result.success) {
				uiPropStatus!!.set("Error reading status from server")
			} else {
				val resultPost : Status = apiInstance!!.procGridShow(if (doShow) 1 else 0)
				if (resultPost.result != Status.Result.success) {
					uiPropStatus!!.set("Could not toggle ShowGrid")
				}
			}
		} catch (e: Exception) {
			uiPropStatus!!.set("Exception calling DefaultApi#procGridShow: ${e.message}")
			//e.printStackTrace()
		}
	}

	fun setActiveCam(cam: DefaultApi.CamOutputCamEnable) {
		val resultStat : Status = getServerStatus(false) ?: return

		try {
			if (resultStat.result != Status.Result.success) {
				uiPropStatus!!.set("Error reading status from server")
			} else {
				var doNothing = false
				var doSwap = false
				var doCamEnable : DefaultApi.CamOutputCamEnable? = null
				var doCamDisable : DefaultApi.CamOutputCamDisable? = null
				if ((cam == DefaultApi.CamOutputCamEnable.L && resultStat.outputCams == StatusCams.L) ||
						(cam == DefaultApi.CamOutputCamEnable.R && resultStat.outputCams == StatusCams.R) ||
						(cam == DefaultApi.CamOutputCamEnable.BOTH && resultStat.outputCams == StatusCams.BOTH)) {
					doNothing = true
				} else if ((cam == DefaultApi.CamOutputCamEnable.L && resultStat.outputCams == StatusCams.R) ||
						(cam == DefaultApi.CamOutputCamEnable.R && resultStat.outputCams == StatusCams.L)) {
					doSwap = true
				} else if (cam == DefaultApi.CamOutputCamEnable.BOTH && resultStat.outputCams == StatusCams.L) {
					doCamEnable = DefaultApi.CamOutputCamEnable.R
				} else if (cam == DefaultApi.CamOutputCamEnable.BOTH && resultStat.outputCams == StatusCams.R) {
					doCamEnable = DefaultApi.CamOutputCamEnable.L
				} else if (cam == DefaultApi.CamOutputCamEnable.L && resultStat.outputCams == StatusCams.BOTH) {
					doCamDisable = DefaultApi.CamOutputCamDisable.R
				} else if (cam == DefaultApi.CamOutputCamEnable.R && resultStat.outputCams == StatusCams.BOTH) {
					doCamDisable = DefaultApi.CamOutputCamDisable.L
				}
				if (! doNothing && doCamEnable != null) {
					val resultPost : Status = apiInstance!!.outputCamEnable(doCamEnable)
					if (resultPost.result != Status.Result.success) {
						uiPropStatus!!.set("Could not enable camera $doCamEnable")
					}
				} else if (! doNothing && doCamDisable != null) {
					val resultPost : Status = apiInstance!!.outputCamDisable(doCamDisable)
					if (resultPost.result != Status.Result.success) {
						uiPropStatus!!.set("Could not deactivate camera $doCamDisable")
					}
				} else if (! doNothing && doSwap) {
					val resultPost : Status = apiInstance!!.outputCamSwap()
					if (resultPost.result != Status.Result.success) {
						uiPropStatus!!.set("Could not swap active camera")
					}
				}
			}
		} catch (e: Exception) {
			uiPropStatus!!.set("Exception calling DefaultApi#outputCamXxx: ${e.message}")
			//e.printStackTrace()
		}
	}
}
