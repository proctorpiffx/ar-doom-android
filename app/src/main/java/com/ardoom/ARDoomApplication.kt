package com.ardoom

import android.app.Application
import android.util.Log

class ARDoomApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "AR Doom initializing — rip and tear")
    }

    companion object {
        private const val TAG = "ARDoomApp"
    }
}
