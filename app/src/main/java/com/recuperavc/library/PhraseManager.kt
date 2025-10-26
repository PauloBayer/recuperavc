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
        var currentType = getCurrentPhraseType()

        var selected: Phrase? = null
        var finalType = currentType

        for (attempt in 0..2) {
            val allOfType = dao.getByType(currentType)
            val available = allOfType.filter { isPhraseOutOfCooldown(it.description) }

            if (available.isNotEmpty()) {
                val filtered = available.filter { it.description != lastUsedPhrase }
                selected = if (filtered.isNotEmpty()) {
                    filtered.random()
                } else {
                    available.random()
                }
                finalType = currentType
                break
            } else {
                currentType = getNextTypeInSequence(currentType)
            }
        }

        if (selected == null) {
            val allOfType = dao.getByType(currentType)
            val oldest = allOfType.minByOrNull { preferences.getLong(keyFor(it.description), 0L) }
                ?: allOfType.first()
            val candidates = allOfType.filter {
                preferences.getLong(keyFor(it.description), 0L) ==
                preferences.getLong(keyFor(oldest.description), 0L)
            }
            selected = candidates.randomOrNull() ?: oldest
            finalType = currentType
        }

        markPhraseAsUsed(selected.description)
        saveCurrentPhraseType(finalType)
        lastUsedPhrase = selected.description
        return@withLock selected
    }

    private fun getCurrentPhraseType(): PhraseType {
        val typeOrdinal = preferences.getInt("current_phrase_type", PhraseType.SHORT.ordinal)
        return PhraseType.values().getOrNull(typeOrdinal) ?: PhraseType.SHORT
    }

    private fun getNextTypeInSequence(current: PhraseType): PhraseType {
        return when (current) {
            PhraseType.SHORT -> PhraseType.MEDIUM
            PhraseType.MEDIUM -> PhraseType.BIG
            PhraseType.BIG -> PhraseType.SHORT
        }
    }

    private fun saveCurrentPhraseType(type: PhraseType) {
        preferences.edit().putInt("current_phrase_type", type.ordinal).commit()
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
