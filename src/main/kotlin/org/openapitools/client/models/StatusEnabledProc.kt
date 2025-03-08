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


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Which frame processors are enabled
 *
 * @param bnc 
 * @param cal 
 * @param flip 
 * @param overlCal 
 * @param overlCam 
 * @param pt 
 * @param roi 
 * @param tr 
 */


data class StatusEnabledProc (

    @Json(name = "bnc")
    val bnc: kotlin.Boolean? = null,

    @Json(name = "cal")
    val cal: kotlin.Boolean? = null,

    @Json(name = "flip")
    val flip: kotlin.Boolean? = null,

    @Json(name = "overlCal")
    val overlCal: kotlin.Boolean? = null,

    @Json(name = "overlCam")
    val overlCam: kotlin.Boolean? = null,

    @Json(name = "pt")
    val pt: kotlin.Boolean? = null,

    @Json(name = "roi")
    val roi: kotlin.Boolean? = null,

    @Json(name = "tr")
    val tr: kotlin.Boolean? = null

) {


}

