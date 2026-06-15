package com.ratio.launcher

import android.app.Application
import io.sentry.android.core.SentryAndroid

class RatioApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SentryAndroid.init(this) { options ->
            options.dsn = "https://daf61cb6000a0efc5b5ef89b606b58a2@o4511551541673984.ingest.de.sentry.io/4511551546392656"
            options.sessionReplay.onErrorSampleRate = 1.0
            options.sessionReplay.sessionSampleRate = 0.1
        }
    }
}
