package com.github.cvzi.screenshottile.utils.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.heifwriter.HeifWriter
import com.github.cvzi.screenshottile.BuildConfig
import com.github.cvzi.screenshottile.CompressionOptions
import java.io.File
import java.io.OutputStream

private const val TAG = "ImageEncoder"

fun imageCompress(
    compressionOptions: CompressionOptions,
    bitmap: Bitmap,
    stream: OutputStream
): Boolean {
    if (compressionOptions.format == "AVIF") {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {  // Requires Android 10+ (Sdk 29)
            val mediaCodecSuccess = compressMediaCodec(compressionOptions, bitmap, stream)
            if (mediaCodecSuccess) {
                return true
            } else {
                Log.e(TAG, "MediaCodec failed, falling back to PNG")
                return bitmap.compress(Bitmap.CompressFormat.PNG, compressionOptions.quality, stream)
            }
        }
    } else if (compressionOptions.format == "HEIC") {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // Requires Android 9+ (SDK 28)
            val heicSuccess = compressHeicWriter(compressionOptions, bitmap, stream)
            if (heicSuccess) {
                return true
            } else {
                Log.e(TAG, "HeifWriter failed, falling back to PNG")
                return bitmap.compress(Bitmap.CompressFormat.PNG, compressionOptions.quality, stream)
            }
        }
    }
    return bitmap.compress(compressionOptions.bitmapFormat, compressionOptions.quality, stream)
}

@RequiresApi(Build.VERSION_CODES.P) // Requires Android 9+ (SDK 28)
fun compressHeicWriter(
    compressionOptions: CompressionOptions,
    bitmap: Bitmap,
    stream: OutputStream
): Boolean {
    var tempFile: File? = null

    try {
        tempFile = File.createTempFile("heifwriter", ".${compressionOptions.fileExtension}")

        val builder = HeifWriter.Builder(
            tempFile.absolutePath,
            bitmap.width,
            bitmap.height,
            HeifWriter.INPUT_MODE_BITMAP
        )
        builder.setQuality(compressionOptions.quality)
        builder.setMaxImages(1)

        val heifWriter = builder.build()
        heifWriter.start()
        heifWriter.addBitmap(bitmap)
        heifWriter.stop(0)

        if (tempFile.exists() && tempFile.length() > 0) {
            tempFile.inputStream().use { input ->
                input.copyTo(stream)
            }
            stream.flush()
            return true
        }
        return false
    } catch (e: Exception) {
        Log.e(TAG, "HeifWriter failed to compress bitmap", e)
        return false
    } finally {
        try { tempFile?.delete() } catch (_: Exception) {}
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
fun getMediaCodecCapabilities(mimeType: String): Pair<MediaCodecInfo, MediaCodecInfo.EncoderCapabilities>? {
    // Check the device actually has an encoder for this format
    val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
    var targetCodecInfo: MediaCodecInfo? = null

    for (codecInfo in codecList.codecInfos) {
        if (!codecInfo.isEncoder) continue
        if (codecInfo.supportedTypes.any { it.equals(mimeType, ignoreCase = true) }) {
            targetCodecInfo = codecInfo
            break
        }
    }

    if (targetCodecInfo == null) {
        Log.w(TAG, "No encoder found for $mimeType")
        return null
    }

    val capabilities = targetCodecInfo.getCapabilitiesForType(mimeType)
    val encoderCapabilities = capabilities.encoderCapabilities

    if (encoderCapabilities == null) {
        Log.w(TAG, "No capabilities found for $mimeType")
        return null
    }

    return Pair(targetCodecInfo, encoderCapabilities)
}


@RequiresApi(Build.VERSION_CODES.Q) // Requires Android 10+ (Sdk 29)
fun compressMediaCodec(
    compressionOptions: CompressionOptions,
    bitmap: Bitmap,
    stream: OutputStream
): Boolean {
    if (compressionOptions.fileExtension.lowercase() != "avif") {
        Log.w(TAG, "compressMediaCodec currently only supports AVIF format")
        return false
    }

    val mimeType = "image/avif"

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        Log.w(TAG, "AVIF requires Android 14+")
        return false
    }

    val codecAndCapabilities = getMediaCodecCapabilities(mimeType) ?: return false
    val targetCodecInfo = codecAndCapabilities.first
    val encoderCapabilities = codecAndCapabilities.second

    // Adjust quality based on the encoder quality settings
    val quality = encoderCapabilities.qualityRange.lower + (encoderCapabilities.qualityRange.upper - encoderCapabilities.qualityRange.lower) * compressionOptions.quality / 100

    if (BuildConfig.DEBUG)
        Log.d(TAG, "Using encoder '${targetCodecInfo.name}' with quality=$quality (${encoderCapabilities.qualityRange.lower}-${encoderCapabilities.qualityRange.upper})")

    var encoder: MediaCodec? = null
    var muxer: MediaMuxer? = null
    var inputSurface: Surface? = null
    var tempFile: File? = null

    try {
        val width = bitmap.width
        val height = bitmap.height

        // Single frame container
        val mediaFormat = MediaFormat.createVideoFormat(mimeType, width, height).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )
            setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ)
            setInteger(MediaFormat.KEY_QUALITY, quality)
            setInteger(MediaFormat.KEY_FRAME_RATE, 1)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        // encoder setup
        encoder = MediaCodec.createByCodecName(targetCodecInfo.name)
        encoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = encoder.createInputSurface()
        encoder.start()

        // render bitmap onto frame
        val canvas: Canvas = inputSurface.lockHardwareCanvas()
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        inputSurface.unlockCanvasAndPost(canvas)

        // mux temp output file
        tempFile = File.createTempFile("mediacodec", ".${compressionOptions.fileExtension}")
        muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        val bufferInfo = MediaCodec.BufferInfo()
        var trackIndex = -1
        var isFinished = false
        var framesSent = false

        while (!isFinished) {

            val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000L)
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                trackIndex = muxer.addTrack(encoder.outputFormat)
                muxer.start()
            } else if (outputBufferIndex >= 0) {
                val encodedData = encoder.getOutputBuffer(outputBufferIndex) ?: continue
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    bufferInfo.size = 0
                }
                if (bufferInfo.size > 0 && trackIndex >= 0) {
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    if (!framesSent) {
                        encoder.signalEndOfInputStream()
                        framesSent = true
                    }
                }
                encoder.releaseOutputBuffer(outputBufferIndex, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    isFinished = true
                }
            } else if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (framesSent && bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    isFinished = true
                }
            }
        }

        encoder.stop()
        encoder.release()
        encoder = null

        muxer.stop()
        muxer.release()
        muxer = null

        // Copy temp to output stream
        if (tempFile.exists() && tempFile.length() > 0) {
            tempFile.inputStream().use { input ->
                input.copyTo(stream)
            }
            stream.flush()
            return true
        }
        return false

    } catch (e: Exception) {
        e.printStackTrace()
        return false
    } finally {
        try { encoder?.release() } catch (_: Exception) {}
        try { muxer?.release() } catch (_: Exception) {}
        try { inputSurface?.release() } catch (_: Exception) {}
        try { tempFile?.delete() } catch (_: Exception) {}
    }
}
