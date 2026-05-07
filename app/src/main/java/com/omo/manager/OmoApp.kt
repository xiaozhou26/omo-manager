package com.omo.manager

import android.app.Application

class OmoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: OmoApp
            private set
    }
}
