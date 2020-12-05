package com.github.cvzi.screenshottile

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.github.cvzi.screenshottile.activities.NoDisplayActivity
import com.github.cvzi.screenshottile.activities.SettingDialogActivity


class SimpleWidgetBoth : SimpleWidget()
class SimpleWidgetScreenshot : SimpleWidget()
class SimpleWidgetSettings : SimpleWidget()

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
        val views = RemoteViews(context.packageName, R.layout.simple_widget)

        val settingsIntent = SettingDialogActivity.newIntent(context, true)
        val settingsPendingIntent = PendingIntent.getActivity(context, 0, settingsIntent, 0)

        val screenshotIntent = NoDisplayActivity.newIntent(context, true)
        val screenshotPendingIntent = PendingIntent.getActivity(context, 0, screenshotIntent, 0)

        when (this) {
            is SimpleWidgetScreenshot -> {
                views.setOnClickPendingIntent(R.id.image, screenshotPendingIntent)
                views.removeAllViews(R.id.linear)
            }
            is SimpleWidgetSettings -> {
                views.setOnClickPendingIntent(R.id.image, settingsPendingIntent)
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

