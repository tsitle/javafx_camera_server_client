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

import org.openapitools.client.models.StatusProcBncSub

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Status of frame processor Brightness-and-Contrast
 *
 * @param brightness 
 * @param contrast 
 * @param gamma 
 */


data class StatusProcBnc (

    @Json(name = "brightness")
    val brightness: StatusProcBncSub? = null,

    @Json(name = "contrast")
    val contrast: StatusProcBncSub? = null,

    @Json(name = "gamma")
    val gamma: StatusProcBncSub? = null

) {


}

