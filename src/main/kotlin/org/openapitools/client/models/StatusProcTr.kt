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

import org.openapitools.client.models.StatusProcTrSub

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 
 *
 * @param dynDelta 
 * @param fixDelta 
 */


data class StatusProcTr (

    @Json(name = "dynDelta")
    val dynDelta: StatusProcTrSub? = null,

    @Json(name = "fixDelta")
    val fixDelta: StatusProcTrSub? = null

) {


}

