import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

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