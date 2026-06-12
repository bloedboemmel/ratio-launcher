package com.ratio.launcher

import android.app.Application

class RatioApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Sentry auto-initializes via manifest meta-data (io.sentry.dsn)
        // No manual init needed — SDK 8.34.0 handles everything
    }
}
