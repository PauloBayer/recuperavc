package com.recuperavc.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.recuperavc.models.User
import com.recuperavc.models.AudioFile
import com.recuperavc.models.AudioReport
import com.recuperavc.models.AudioReportGroup
import com.recuperavc.models.CoherenceReport
import com.recuperavc.models.CoherenceReportGroup
import com.recuperavc.models.db.Converters

import com.recuperavc.dao.UserDao
import com.recuperavc.dao.AudioFileDao
import com.recuperavc.dao.AudioReportDao
import com.recuperavc.dao.CoherenceReportDao
import com.recuperavc.dao.MotionReportDao
import com.recuperavc.dao.PhraseDao
import com.recuperavc.models.MotionReport
import com.recuperavc.models.Phrase

@Database(
    entities = [
        User::class,
        AudioFile::class,
        AudioReport::class,
        AudioReportGroup::class,
        Phrase::class,
        CoherenceReport::class,
        CoherenceReportGroup::class,
        MotionReport::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppRoomDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun audioFileDao(): AudioFileDao
    abstract fun audioReportDao(): AudioReportDao
    abstract fun coherenceReportDao(): CoherenceReportDao
    abstract fun MotionReportDao(): MotionReportDao
    abstract fun phraseDao(): PhraseDao

    companion object {
        @Volatile private var INSTANCE: AppRoomDatabase? = null
        fun getInstance(context: Context): AppRoomDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppRoomDatabase::class.java,
                    "app.db"
                )
                    //.fallbackToDestructiveMigration() // <- só ativar isso aqui para ele limpar sozinho tudo do db se mudar o schema
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
