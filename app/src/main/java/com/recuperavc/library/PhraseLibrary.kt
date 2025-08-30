package com.recuperavc.library

object PhraseLibrary {

    private val shortPhrases = listOf(
        "O rato roeu a roupa do rei de roma.",
        "O sucesso vem com a persistência.",
        "Sonhar é o primeiro passo.",
        "Coragem é agir com medo mesmo assim.",
        "Simplicidade é a chave da felicidade."
    )

    private val middlePhrases = listOf(
        "A jornada de mil milhas começa com um único passo.",
        "Grandes conquistas começam com pequenas atitudes.",
        "Acredite em você e tudo será possível.",
        "A disciplina é a ponte entre metas e realizações.",
        "O que você planta hoje, colherá amanhã."
    )

    private val longPhrases = listOf(
        "Não importa a velocidade com que você caminha, desde que não pare de seguir em frente.",
        "A vida é como andar de bicicleta: para manter o equilíbrio, você precisa se manter em movimento.",
        "O sucesso não é definitivo e o fracasso não é fatal: o que conta é a coragem de continuar.",
        "Cada dia é uma nova oportunidade para aprender, crescer e se tornar melhor.",
        "Grandes mudanças acontecem quando pequenas atitudes se repetem todos os dias."
    )

    fun getFrase(tamanho: String = "curta"): String {
        return when (tamanho.lowercase()) {
            "curta" -> shortPhrases.random()
            "media" -> middlePhrases.random()
            "longa" -> longPhrases.random()
            else -> shortPhrases.random()
        }
    }
}
