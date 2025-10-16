package com.recuperavc.library

object PhraseLibrary {

    private val shortPhrases = listOf(
        "O rato roeu a roupa do rei de Roma.",
        "O sucesso nasce com foco diário.",
        "Sonhar abre caminho para um plano.",
        "Coragem é avançar mesmo com medo.",
        "A simplicidade guia a escolha no trabalho.",
        "O aluno escreve uma página com atenção.",
        "A equipe define uma meta em um quadro.",
        "O treino começa sempre pela manhã.",
        "A voz melhora com leitura em voz alta.",
        "O estudante revisa o resumo depois do estudo.",
        "A terapeuta registra o progresso em uma folha.",
        "O paciente repete a frase três vezes.",
        "O aplicativo mostra a tarefa na tela.",
        "O treinador indica um passo para hoje.",
        "A mente foca na tarefa por trinta minutos.",
        "O professor corrige o texto rapidamente.",
        "O grupo inicia a reunião no horário combinado.",
        "A agenda reserva um bloco para leitura.",
        "O desenhista anota ideias em um cartão pequeno.",
        "A pesquisa começa após um café.",
        "O aluno pratica a dicção com uma gravação.",
        "O time registra erros no quadro.",
        "A escrita flui melhor com um esboço.",
        "O objetivo guia escolhas durante a semana.",
        "O leitor marca a página com um marcador."
    )

    private val middlePhrases = listOf(
        "A jornada começa agora com um passo firme.",
        "Pequenos hábitos constroem um resultado estável.",
        "Acredite em si para entregar uma tarefa por vez.",
        "A disciplina mantém a meta perto da ação.",
        "Plante hoje para colher amanhã com tranquilidade.",
        "O foco separa a tarefa principal da fila.",
        "Comece pequeno para sustentar o progresso diário.",
        "Registrar métricas orienta a próxima decisão com clareza.",
        "Revisar o plano remove ruído da execução.",
        "Um ritual simples prepara a mente para o trabalho.",
        "Respire fundo para iniciar o passo seguinte.",
        "Proteja a manhã para fazer o essencial primeiro.",
        "Limite distrações para manter a atenção no alvo.",
        "Dê um nome claro à sua meta.",
        "Prepare o ambiente para reduzir atritos.",
        "Revise prioridades antes de abrir mensagens.",
        "Quebre o projeto em partes menores.",
        "Entregue uma versão funcional antes do polimento.",
        "Aprender pede prática regular com correções honestas.",
        "Use um cronômetro para sustentar seu tempo de foco.",
        "Faça pausas breves para renovar a energia.",
        "Bloqueie o celular para evitar interrupções.",
        "Feche abas sem uso antes de abrir uma nova.",
        "Defina um término claro para a tarefa atual.",
        "Avalie o dia para ajustar o plano amanhã."
    )

    private val longPhrases = listOf(
        "O progresso chega em silêncio com a repetição de um bom passo diário.",
        "A confiança cresce com cada entrega completa sob revisão simples.",
        "A mente foca melhor após um início calmo com um ambiente organizado.",
        "O plano funciona melhor com limite claro de tempo para cada tarefa.",
        "A paciência sustenta a execução em dias longos sem depender de motivação instável.",
        "A energia retorna com pausa breve e água por perto durante o trabalho.",
        "A meta ganha tração com medidas simples ao final de cada sessão.",
        "O erro vira lição com registro imediato em um caderno dedicado.",
        "O corpo rende mais com sono adequado durante toda a semana.",
        "O objetivo fica visível com um quadro na mesa em posição central.",
        "A mente evita atalhos com passos explícitos dentro de um checklist simples.",
        "O aprendizado acelera com revisão curta logo após a prática do dia.",
        "O projeto avança com escopo delimitado no calendário.",
        "A rotina facilita a ação com horário fixo para iniciar a tarefa.",
        "O dia rende mais com uma lista curta ao lado do teclado.",
        "A atenção melhora com silêncio controlado no ambiente de trabalho.",
        "A mente trabalha melhor com metas visíveis perto do local de estudo.",
        "O caminho fica seguro com passos constantes ao redor do mesmo objetivo.",
        "O hábito se firma com repetição diária após o despertar do corpo.",
        "O resultado aparece com prática constante sob um método simples.",
        "O foco melhora com fone fechado durante tarefas que pedem atenção.",
        "A escrita flui melhor com um esboço direto antes do texto final.",
        "A leitura rende mais com marcação leve ao final de cada seção.",
        "A fala sai clara com treino curto diante do espelho.",
        "A decisão fica fácil com critérios fixos na abertura de cada projeto."
    )

    fun getFrase(tamanho: String = "curta"): String {
        return when (tamanho.lowercase()) {
            "curta" -> shortPhrases.random()
            "media" -> middlePhrases.random()
            "longa" -> longPhrases.random()
            else -> middlePhrases.random()
        }
    }
    
    fun getAllShortPhrases(): List<String> = shortPhrases
    fun getAllMiddlePhrases(): List<String> = middlePhrases
    fun getAllLongPhrases(): List<String> = longPhrases
}
