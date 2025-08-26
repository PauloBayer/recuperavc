package com.recuperavc

import android.app.Application
import com.recuperavc.data.db.AppRoomDatabase

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("AppInit", "before DB init")
        AppRoomDatabase.getInstance(applicationContext)
        android.util.Log.d("AppInit", "after DB init")
    }
}