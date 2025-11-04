package com.github.cvzi.screenshottile.functions

import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.util.Log
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.service.AppFunction
import com.github.cvzi.screenshottile.App
import java.util.UUID


class ScreenshotAppFunctions {

    @AppFunction
    fun startScreenCapture(ctx: AppFunctionContext): StartCaptureResponse {
        val requestId = UUID.randomUUID().toString()
        Log.d("ScreenshotAppFunctions", "startScreenCapture with id: $requestId")

        AppFunctionResultStore.prepare(requestId)

        App.getInstance().launchScreenshotFromAppFunction(ctx.context)

        return StartCaptureResponse(
            requestId = requestId,
            status = "initiated"
        )
    }

    @AppFunction
    fun getCaptureResult(
        ctx: AppFunctionContext,
        requestId: String
    ): GetCaptureResponse {
        val record = AppFunctionResultStore.peek(requestId)
        Log.d("ScreenshotAppFunctions", "getCaptureResult for $requestId: $record")
        return when {
            record == null -> GetCaptureResponse(status = "pending")
            record.pending -> GetCaptureResponse(status = "pending")
            record.failedMessage != null -> GetCaptureResponse(
                status = "failed",
                error = record.failedMessage
            )

            else -> {
                ctx.context.grantUriPermission("*", record.uri, FLAG_GRANT_READ_URI_PERMISSION)
                GetCaptureResponse(
                    status = "ready",
                    contentUri = record.uri.toString(),
                    width = record.width.toLong(),
                    height = record.height.toLong()
                )
            }
        }
    }
}
