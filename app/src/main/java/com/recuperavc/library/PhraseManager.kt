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

    /**
     * If [requestedType] is provided, pick strictly from that type.
     * If null, use the rotating sequence SHORT -> MEDIUM -> BIG.
     */
    suspend fun getNextPhrase(requestedType: PhraseType? = null): Phrase = mutex.withLock {
        val dao = DbProvider.db(context).phraseDao()

        var currentType: PhraseType = requestedType ?: getCurrentPhraseType()
        var finalType = currentType

        // No labelled returns here; just use if/else
        val pickFromType: suspend (PhraseType) -> Phrase? = { type ->
            val allOfType = dao.getByType(type)
            if (allOfType.isEmpty()) {
                null
            } else {;
                val available = allOfType.filter { isPhraseOutOfCooldown(it.description) }
                val pool = if (available.isNotEmpty()) {
                    val filtered = available.filter { it.description != lastUsedPhrase }
                    if (filtered.isNotEmpty()) filtered else available
                } else {
                    emptyList()
                }

                if (pool.isNotEmpty()) {
                    pool.random()
                } else {
                    val oldest = allOfType.minByOrNull { preferences.getLong(keyFor(it.description), 0L) }
                        ?: allOfType.first()
                    val oldestStamp = preferences.getLong(keyFor(oldest.description), 0L)
                    val candidates = allOfType.filter {
                        preferences.getLong(keyFor(it.description), 0L) == oldestStamp
                    }
                    candidates.randomOrNull() ?: oldest
                }
            }
        }

        val selected: Phrase? = if (requestedType != null) {
            // Strictly from requested type
            pickFromType(requestedType)
        } else {
            // Rotate as before
            var chosen: Phrase? = null
            repeat(3) {
                chosen = pickFromType(currentType)
                if (chosen != null) {
                    finalType = currentType
                    return@repeat
                }
                currentType = getNextTypeInSequence(currentType)
            }
            chosen
        }

        val result = selected ?: run {
            val allOfType = dao.getByType(finalType)
            allOfType.randomOrNull() ?: dao.getAll().first()
        }

        markPhraseAsUsed(result.description)
        if (requestedType == null) saveCurrentPhraseType(finalType)
        lastUsedPhrase = result.description
        return@withLock result
    }

    private fun getCurrentPhraseType(): PhraseType {
        val typeOrdinal = preferences.getInt("current_phrase_type", PhraseType.SHORT.ordinal)
        return PhraseType.values().getOrNull(typeOrdinal) ?: PhraseType.SHORT
    }

    private fun getNextTypeInSequence(current: PhraseType): PhraseType =
        when (current) {
            PhraseType.SHORT -> PhraseType.MEDIUM
            PhraseType.MEDIUM -> PhraseType.BIG
            PhraseType.BIG -> PhraseType.SHORT
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
