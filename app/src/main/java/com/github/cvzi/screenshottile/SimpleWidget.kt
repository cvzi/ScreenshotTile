package com.github.cvzi.screenshottile

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.github.cvzi.screenshottile.activities.NoDisplayActivity
import com.github.cvzi.screenshottile.activities.SettingDialogActivity

/**
 * Widget with settings (right) and screenshot (left) button
 */
class SimpleWidgetBoth : SimpleWidget()

/**
 * Widget as screenshot button
 */
class SimpleWidgetScreenshot : SimpleWidget()

/**
 * Widget as settings button
 */
class SimpleWidgetSettings : SimpleWidget()

/**
 * Widget to toggle floating button
 */
class SimpleWidgetFloatingButton : SimpleWidget()

/**
 * Implementation of App Widget functionality.
 */
open class SimpleWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // Construct the RemoteViews object
        val layout = if (this is SimpleWidgetFloatingButton) {
            R.layout.floating_button_widget
        } else {
            R.layout.simple_widget
        }
        val views = RemoteViews(context.packageName, layout)

        val settingsIntent = SettingDialogActivity.newIntent(context, true)
        val settingsPendingIntent =
            PendingIntent.getActivity(context, 0, settingsIntent, PendingIntent.FLAG_IMMUTABLE)

        val screenshotIntent = NoDisplayActivity.newIntent(context, true)
        val screenshotPendingIntent =
            PendingIntent.getActivity(context, 0, screenshotIntent, PendingIntent.FLAG_IMMUTABLE)

        val floatingButtonIntent = NoDisplayActivity.newFloatingButtonIntent(context)
        val floatingButtonPendingIntent = PendingIntent.getActivity(
            context,
            0,
            floatingButtonIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        when (this) {
            is SimpleWidgetScreenshot -> {
                views.setOnClickPendingIntent(R.id.image, screenshotPendingIntent)
                views.setContentDescription(R.id.image, context.getString(R.string.take_screenshot))
                views.removeAllViews(R.id.linear)
            }
            is SimpleWidgetSettings -> {
                views.setOnClickPendingIntent(R.id.image, settingsPendingIntent)
                views.setContentDescription(R.id.image, context.getString(R.string.open_settings))
                views.removeAllViews(R.id.linear)
            }
            is SimpleWidgetFloatingButton -> {
                views.setOnClickPendingIntent(R.id.image, floatingButtonPendingIntent)
                views.setContentDescription(
                    R.id.image,
                    context.getString(R.string.setting_floating_button)
                )
                views.removeAllViews(R.id.linear)
            }
            else -> {
                views.setOnClickPendingIntent(R.id.leftSide, screenshotPendingIntent)
                views.setOnClickPendingIntent(R.id.rightSide, settingsPendingIntent)
            }
        }

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}

