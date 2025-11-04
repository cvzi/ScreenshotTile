package com.github.cvzi.screenshottile.functions

import androidx.appfunctions.AppFunctionSerializable

@AppFunctionSerializable
data class GetCaptureResponse(
    val status: String? = "pending", // "pending" | "ready" | "failed"
    val contentUri: String? = null,
    val width: Long? = null,
    val height: Long? = null,
    val error: String? = null
)