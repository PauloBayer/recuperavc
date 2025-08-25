package com.recupeavc

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppRoomDatabase.getInstance(this)
    }
}