package com.mavis.wc2026.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.*
import com.mavis.wc2026.data.WcRepository
import java.util.concurrent.TimeUnit

/**
 * Periodic worker — refreshes the widget UI every 15 min.
 * (For real-time updates during a live match, the Glance widget itself can
 *  call updateAll() on the receiving side after each fetch.)
 */
class WidgetUpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            // Warm the cache — this also acts as a connectivity check.
            WcRepository().loadAll()
            WC2026Widget().updateAll(applicationContext)
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE = "wc2026-widget-refresh"

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                15, TimeUnit.MINUTES
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE,
                ExistingPeriodicWorkPolicy.KEEP,
                req
            )
        }
    }
}
