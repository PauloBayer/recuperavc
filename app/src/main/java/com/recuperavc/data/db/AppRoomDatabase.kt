package com.recuperavc.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.recuperavc.models.User
import com.recuperavc.models.PatternAudioPhrase
import com.recuperavc.models.AudioFile
import com.recuperavc.models.AudioReport
import com.recuperavc.models.AudioReportGroup
import com.recuperavc.models.PatternCoherencePhrase
import com.recuperavc.models.CoherenceReport
import com.recuperavc.models.CoherenceReportGroup
import com.recuperavc.models.db.Converters

import com.recuperavc.dao.UserDao
import com.recuperavc.dao.AudioFileDao
import com.recuperavc.dao.AudioReportDao
import com.recuperavc.dao.CoherenceReportDao
import com.recuperavc.dao.PatternAudioPhraseDao
import com.recuperavc.dao.PatternCoherencePhraseDao

@Database(
    entities = [
        User::class,
        PatternAudioPhrase::class, AudioFile::class,
        AudioReport::class, AudioReportGroup::class,
        PatternCoherencePhrase::class, CoherenceReport::class, CoherenceReportGroup::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppRoomDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun audioFileDao(): AudioFileDao
    abstract fun audioReportDao(): AudioReportDao
    abstract fun coherenceReportDao(): CoherenceReportDao
    abstract fun patternAudioPhraseDao(): PatternAudioPhraseDao
    abstract fun patternCoherencePhraseDao(): PatternCoherencePhraseDao

    companion object {
        @Volatile private var INSTANCE: AppRoomDatabase? = null
        fun getInstance(context: Context): AppRoomDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppRoomDatabase::class.java,
                    "app.db"
                )
                    // .fallbackToDestructiveMigration() // <- só ativar isso aqui para ele limpar sozinho tudo do db se mudar o schema
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
