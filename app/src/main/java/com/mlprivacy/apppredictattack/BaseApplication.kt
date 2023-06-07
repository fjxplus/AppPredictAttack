package com.mlprivacy.apppredictattack

import android.app.Application
import android.content.Context

class BaseApplication: Application() {
    companion object {
        lateinit var appContext: Context
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        base?.let { context ->
            appContext = context
        }
    }
}