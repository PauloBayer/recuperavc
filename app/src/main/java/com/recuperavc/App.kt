package com.recuperavc

import android.app.Application
import com.recuperavc.data.db.AppRoomDatabase
import com.recuperavc.data.db.DbProvider
import com.recuperavc.data.CurrentUser
import com.recuperavc.models.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("AppInit", "before DB init")
        AppRoomDatabase.getInstance(applicationContext)
        android.util.Log.d("AppInit", "after DB init")
        CoroutineScope(Dispatchers.IO).launch {
            val db = DbProvider.db(applicationContext)
            db.userDao().upsert(
                User(
                    id = CurrentUser.ID,
                    login = "demo",
                    password = "demo",
                    email = "demo@example.com",
                    wordsPerMinute = 0f,
                    wordErrorRate = 0f,
                    name = "Usuário Demo"
                )
            )
        }
    }
}
