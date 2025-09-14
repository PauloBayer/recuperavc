package com.recuperavc.library

import android.content.Context
import android.content.SharedPreferences
import java.text.Normalizer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PhraseManager(private val context: Context) {
    private val preferences: SharedPreferences = context.applicationContext.getSharedPreferences("phrase_cooldown", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    
    private val cooldownHours = 1L // aqui fica o tempo que a frase não irá voltar, dá pra mudar o tempo
    private val cooldownMillis = cooldownHours * 60 * 60 * 1000
    
    private var lastUsedPhrase: String? = null
    
    suspend fun getNextPhrase(tamanho: String = "curta"): String = mutex.withLock {
        val primaryPhrases = getAvailablePhrases(tamanho)
        val secondaryPhrases = getAvailablePhrases(if (tamanho == "curta") "media" else "curta")
        val availablePhrases = primaryPhrases + secondaryPhrases
        val selectedPhrase = if (availablePhrases.isEmpty()) {
            val allPrimary = getAllPhrases(tamanho)
            val allSecondary = getAllPhrases(if (tamanho == "curta") "media" else "curta")
            val all = allPrimary + allSecondary
            all.minByOrNull { preferences.getLong(keyFor(it), 0L) } ?: allPrimary.random()
        } else {
            availablePhrases.firstOrNull { it != lastUsedPhrase }
                ?: availablePhrases.random()
        }
        markPhraseAsUsed(selectedPhrase)
        lastUsedPhrase = selectedPhrase
        return selectedPhrase
    }
    
    private fun getAvailablePhrases(tamanho: String): List<String> {
        val allPhrases = when (tamanho.lowercase()) {
            "curta" -> PhraseLibrary.getAllShortPhrases()
            "media" -> PhraseLibrary.getAllMiddlePhrases()
            "longa" -> PhraseLibrary.getAllLongPhrases()
            else -> PhraseLibrary.getAllShortPhrases()
        }
        
        val currentTime = System.currentTimeMillis()
        
        return allPhrases.filter { phrase ->
            val lastUsedTime = preferences.getLong(keyFor(phrase), 0L)
            (currentTime - lastUsedTime) >= cooldownMillis
        }
    }
    
    private fun getAllPhrases(tamanho: String): List<String> {
        return when (tamanho.lowercase()) {
            "curta" -> PhraseLibrary.getAllShortPhrases()
            "media" -> PhraseLibrary.getAllMiddlePhrases()
            "longa" -> PhraseLibrary.getAllLongPhrases()
            else -> PhraseLibrary.getAllShortPhrases()
        }
    }
    
    private fun markPhraseAsUsed(phrase: String) {
        preferences.edit()
            .putLong(keyFor(phrase), System.currentTimeMillis())
            .commit()
    }

    private fun keyFor(phrase: String): String {
        val normalized = Normalizer.normalize(phrase, Normalizer.Form.NFKC)
        return normalized.lowercase().trim().replace("\\s+".toRegex(), " ")
    }
}
