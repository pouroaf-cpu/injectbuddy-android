package com.injectbuddy.android

import android.app.Application
import com.injectbuddy.android.di.ServiceLocator

class InjectBuddyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
