package com.omniagent.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.omniagent.app.MainActivity
import com.omniagent.app.R

/**
 * Home Screen Widget Provider — Quick actions for OmniAgent.
 */
class OmniAgentWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.omni_widget)

        // ASK Action -> Opens Chat tab in MainActivity
        val askIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("target_tab", "CHAT")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val askPendingIntent = PendingIntent.getActivity(
            context, 0, askIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_widget_ask, askPendingIntent)

        // SCAN Action -> Triggers scan or opens app to Scan
        val scanIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("target_tab", "OUTPUT")
            putExtra("trigger_action", "SCAN")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val scanPendingIntent = PendingIntent.getActivity(
            context, 1, scanIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_widget_scan, scanPendingIntent)

        // RESUME Action -> Opens Career/Resume tab
        val resumeIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("target_tab", "CAREER")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val resumePendingIntent = PendingIntent.getActivity(
            context, 2, resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_widget_resume, resumePendingIntent)

        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
