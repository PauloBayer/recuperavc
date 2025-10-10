package com.recuperavc

import android.app.Application
import com.recuperavc.data.db.AppRoomDatabase
import com.recuperavc.data.db.DbProvider
import com.recuperavc.library.PhraseLibrary
import com.recuperavc.models.Phrase
import com.recuperavc.models.enums.PhraseType
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
            db.audioReportDao().deletePartialReports()

            // Aqui pega as frases que tão no banco pra adicionar as que faltam
            val phraseDao = db.phraseDao()
            val existingDescriptions = phraseDao.getAll().map { it.description }.toSet()

            // Inserindo as frases curtas
            for (desc in PhraseLibrary.getAllShortPhrases()) {
                if (!existingDescriptions.contains(desc)) {
                    phraseDao.upsert(
                        Phrase(
                            description = desc,
                            type = PhraseType.SHORT
                        )
                    )
                }
            }

            // Inserindo as frases médias
            for (desc in PhraseLibrary.getAllMiddlePhrases()) {
                if (!existingDescriptions.contains(desc)) {
                    phraseDao.upsert(
                        Phrase(
                            description = desc,
                            type = PhraseType.MEDIUM
                        )
                    )
                }
            }

            // Inserindo as frases longas
            for (desc in PhraseLibrary.getAllLongPhrases()) {
                if (!existingDescriptions.contains(desc)) {
                    phraseDao.upsert(
                        Phrase(
                            description = desc,
                            type = PhraseType.BIG
                        )
                    )
                }
            }
        }
    }
}
