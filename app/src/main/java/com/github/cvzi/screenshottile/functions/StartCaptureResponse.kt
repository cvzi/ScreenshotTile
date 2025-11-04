package com.github.cvzi.screenshottile.functions

import androidx.appfunctions.AppFunctionSerializable


@AppFunctionSerializable
data class StartCaptureResponse(
    val requestId: String,
    val status: String
)
