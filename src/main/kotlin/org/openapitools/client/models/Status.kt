/**
 *
 * Please note:
 * This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * Do not edit this file manually.
 *
 */

@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport"
)

package org.openapitools.client.models

import org.openapitools.client.models.StatusCameraReady
import org.openapitools.client.models.StatusCams
import org.openapitools.client.models.StatusEnabledProc
import org.openapitools.client.models.StatusProcBnc
import org.openapitools.client.models.StatusProcCal
import org.openapitools.client.models.StatusProcGrid
import org.openapitools.client.models.StatusProcPt
import org.openapitools.client.models.StatusProcRoi
import org.openapitools.client.models.StatusProcTr
import org.openapitools.client.models.StatusResolution

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Status of all properties
 *
 * @param result 
 * @param availOutputCams 
 * @param cameraReady 
 * @param cpuTemperature 
 * @param enabledProc 
 * @param framerate 
 * @param inputStreamSourceType 
 * @param outputCams 
 * @param procBnc 
 * @param procCal 
 * @param procGrid 
 * @param procPt 
 * @param procRoi 
 * @param procTr 
 * @param resolutionInputStream 
 * @param resolutionOutput 
 * @param version 
 */


data class Status (

    @Json(name = "result")
    val result: Status.Result? = null,

    @Json(name = "availOutputCams")
    val availOutputCams: StatusCams? = null,

    @Json(name = "cameraReady")
    val cameraReady: StatusCameraReady? = null,

    @Json(name = "cpuTemperature")
    val cpuTemperature: kotlin.Float? = null,

    @Json(name = "enabledProc")
    val enabledProc: StatusEnabledProc? = null,

    @Json(name = "framerate")
    val framerate: kotlin.Int? = null,

    @Json(name = "inputStreamSourceType")
    val inputStreamSourceType: Status.InputStreamSourceType? = null,

    @Json(name = "outputCams")
    val outputCams: StatusCams? = null,

    @Json(name = "procBnc")
    val procBnc: StatusProcBnc? = null,

    @Json(name = "procCal")
    val procCal: StatusProcCal? = null,

    @Json(name = "procGrid")
    val procGrid: StatusProcGrid? = null,

    @Json(name = "procPt")
    val procPt: StatusProcPt? = null,

    @Json(name = "procRoi")
    val procRoi: StatusProcRoi? = null,

    @Json(name = "procTr")
    val procTr: StatusProcTr? = null,

    @Json(name = "resolutionInputStream")
    val resolutionInputStream: StatusResolution? = null,

    @Json(name = "resolutionOutput")
    val resolutionOutput: StatusResolution? = null,

    @Json(name = "version")
    val version: kotlin.String? = null

) {

    /**
     * 
     *
     * Values: success,error
     */
    @JsonClass(generateAdapter = false)
    enum class Result(val value: kotlin.String) {
        @Json(name = "success") success("success"),
        @Json(name = "error") error("error");
    }
    /**
     * 
     *
     * Values: gstreamer,mjpeg,unspecified
     */
    @JsonClass(generateAdapter = false)
    enum class InputStreamSourceType(val value: kotlin.String) {
        @Json(name = "gstreamer") gstreamer("gstreamer"),
        @Json(name = "mjpeg") mjpeg("mjpeg"),
        @Json(name = "unspecified") unspecified("unspecified");
    }

}

