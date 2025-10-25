package com.recuperavc.library

import android.content.Context
import android.content.SharedPreferences
import com.recuperavc.data.db.DbProvider
import com.recuperavc.models.Phrase
import com.recuperavc.models.enums.PhraseType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.Normalizer

class PhraseManager(private val context: Context) {
    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences("phrase_cooldown", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val cooldownHours = 1L
    private val cooldownMillis = cooldownHours * 60 * 60 * 1000

    private var lastUsedPhrase: String? = null

    suspend fun getNextPhrase(type: PhraseType = PhraseType.MEDIUM): Phrase = mutex.withLock {
        val dao = DbProvider.db(context).phraseDao()
        val primary = dao.getByType(type)
        val secondary = when (type) {
            PhraseType.SHORT -> dao.getByType(PhraseType.MEDIUM)
            PhraseType.MEDIUM -> dao.getByType(PhraseType.SHORT)
            PhraseType.BIG -> dao.getByType(PhraseType.MEDIUM)
        }
        val available = (primary + secondary).distinctBy { it.id }
            .filter { isPhraseOutOfCooldown(it.description) }

        val selected = if (available.isEmpty()) {
            val all = (primary + secondary).distinctBy { it.id }
            val oldest = all.minByOrNull { preferences.getLong(keyFor(it.description), 0L) }
                ?: all.first()
            val candidates = all.filter {
                preferences.getLong(keyFor(it.description), 0L) ==
                preferences.getLong(keyFor(oldest.description), 0L)
            }
            candidates.randomOrNull() ?: oldest
        } else {
            val filtered = available.filter { it.description != lastUsedPhrase }
            if (filtered.isNotEmpty()) {
                filtered.random()
            } else {
                available.random()
            }
        }

        markPhraseAsUsed(selected.description)
        lastUsedPhrase = selected.description
        return@withLock selected
    }

    private fun isPhraseOutOfCooldown(phrase: String): Boolean {
        val currentTime = System.currentTimeMillis()
        val lastUsedTime = preferences.getLong(keyFor(phrase), 0L)
        return (currentTime - lastUsedTime) >= cooldownMillis
    }

    private fun markPhraseAsUsed(phrase: String) {
        preferences.edit().putLong(keyFor(phrase), System.currentTimeMillis()).commit()
    }

    private fun keyFor(phrase: String): String {
        val normalized = Normalizer.normalize(phrase, Normalizer.Form.NFKC)
        return normalized.lowercase().trim().replace("\\s+".toRegex(), " ")
    }
}
