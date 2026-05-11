package com.github.cvzi.screenshottile.utils

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Rect
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ClickableSpan
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.core.graphics.toColorInt
import com.github.cvzi.screenshottile.ClickableStringResult

/**
 * Adjust font size to fill the available space of a text view
 */
fun fillTextHeight(textView: TextView, maxHeight: Int, startSize: Float? = null) {
    var currentTextSize: Float = startSize ?: textView.textSize
    val text = textView.text.toString()
    val bounds = Rect()
    val paint = Paint().apply {
        textView.typeface
        textSize = currentTextSize
        getTextBounds(text, 0, text.length, bounds)
    }
    while (bounds.height() > maxHeight) {
        currentTextSize--
        paint.run {
            textSize = currentTextSize
            getTextBounds(text, 0, text.length, bounds)
        }
    }
    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, currentTextSize)
}

/**
 * Make activity links clickable. Example: "This is a link [Tutorial,.TutorialActivity] to the tutorial"
 */
fun makeActivityClickableFromText(text: String, context: Context): ClickableStringResult {
    val builder = SpannableStringBuilder("")
    val activities = ArrayList<String>()
    for (content in text.split("]")) {
        val startIndex = content.indexOf("[")
        if (startIndex == -1) {
            builder.append(content)
            continue
        }
        val value = content.subSequence(startIndex, content.length).trim()
        val labelEnd = value.indexOf(',')
        val activityName = value.subSequence(labelEnd + 1, value.length).trim()
        activities.add("com.github.cvzi.screenshottile.activities$activityName")
        val label = value.subSequence(1, labelEnd).trim()
        var newContent = content.subSequence(0, startIndex).toString()
        newContent += label
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(textView: View) {
                val intent = Intent()
                intent.setClassName(
                    context,
                    "com.github.cvzi.screenshottile.activities$activityName"
                )
                context.startActivity(intent)
            }
        }

        val spannableString = SpannableString(newContent)
        spannableString.setSpan(
            clickableSpan,
            startIndex,
            startIndex + label.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        builder.append(spannableString)
    }
    return ClickableStringResult(builder, activities)
}

fun parseColorString(colorString: String?): Int? {
    if (!colorString.isNullOrBlank() && colorString.trim().lowercase() != "null") {
        if (colorString.startsWith("i")) {
            try {
                return colorString.substring(1).toInt()
            } catch (_: NumberFormatException) {
            }
        }
        try {
            return colorString.toColorInt()
        } catch (_: IllegalArgumentException) {
            try {
                return "#$colorString".toColorInt()
            } catch (_: IllegalArgumentException) {
            }
        }
        Log.e(UTILSKT, "Could not parse color '$colorString'")
    }
    return null
}
