# Para rodar o projeto:

O modelo do Whispper não vem junto com o repositório, pois ele é muito pesado. Ele precisa ser baixado a parte de depois colocado na pasta correta. Siga os passos:

1. Baixe o modelo aqui: https://drive.google.com/file/d/1kajwLNlkBH-_FD3Y5vSH-R8N05lPmUuG/view?usp=sharing
2. Coloque na pasta: recuperavc\app\src\main\assets\models

# RecuperAVC – Sistema para Apoio à Recuperação Pós‑AVC

RecuperAVC é um aplicativo móvel de **reabilitação pós‑AVC** destinado a pacientes, cuidadores e profissionais de saúde.  O sistema integra múltiplos testes que avaliam fala, compreensão de linguagem e motricidade fina, registra métricas de desempenho, mantém histórico local e gera relatórios que podem ser compartilhados com a equipe de reabilitação.  O projeto foi desenvolvido como Trabalho de Conclusão de Curso na Universidade Federal do Paraná e foi pensado para ampliar o acesso a práticas de reabilitação e permitir o acompanhamento contínuo da evolução do paciente.  Todo o processamento ocorre no dispositivo, sem depender de servidores externos, e o código‑fonte completo está disponível neste repositório.

## Funcionalidades principais

### Teste de fala

* O usuário lê em voz alta uma frase apresentada na tela.  O áudio é gravado e **transcrito localmente** pelo modelo _Whisper.cpp_, uma versão otimizada do OpenAI Whisper para dispositivos móveis.
* As métricas calculadas incluem **Palavras por Minuto (WPM)** e **Taxa de Erro de Palavras (WER)**.  Estas métricas são armazenadas no banco de dados embarcado e exibidas em relatórios, permitindo o acompanhamento de tendências ao longo das sessões.
* Para cada sessão são gerados relatórios detalhados (para análise clínica) e relatórios simplificados que indicam se o usuário está dentro, acima ou abaixo da média populacional.

### Teste de arranjo de frases

* Inspirado em paradigmas de avaliação linguística, este módulo apresenta as palavras de uma frase em ordem aleatória e solicita ao usuário que reordene os termos de forma coesa.
* O sistema mede o **tempo gasto**, o **número de erros por tentativa** e a **consistência das soluções**, compondo um **índice de coerência sintático‑semântica** ao longo do tempo.

### Teste de motricidade fina (finger tapping)

* Avalia coordenação motora e velocidade de toques usando um botão alvo.
* Dois modos estão disponíveis: **modo com movimento**, no qual o botão se reposiciona aleatoriamente a cada acerto, e **modo sem movimento**, em que o botão permanece fixo no centro.
* O usuário informa a mão utilizada (direita/esquerda) e se ela é a mão dominante.  A duração padrão é de 30 s para o modo com movimento e 20 s para o modo fixo.
* O relatório registra métricas como **cliques por minuto**, **total de cliques**, **missed clicks** (toques fora do alvo) e indicações da mão utilizada e do modo de movimento.

### Acessibilidade e personalização

O aplicativo foi projetado para usuários com sequelas motoras, visuais ou cognitivas.  Além de fluxos curtos e previsíveis, o **RecuperAVC** oferece:

* Ajuste do **tamanho da fonte** em toda a aplicação para melhorar a legibilidade.
* Alternância entre **modo claro**, **modo escuro** e **modo de alto contraste**, com paletas de cores adaptadas para pessoas com baixa visão.
* **Alvos de toque ampliados** e textos sucintos para reduzir a carga motora e cognitiva.
* **Efeitos sonoros** opcionais que auxiliam usuários com deficiência visual.

### Persistência de dados e relatórios

* Os resultados de cada teste são armazenados localmente usando **Android Room** (SQLite) com consultas tipadas, garantindo privacidade e uso offline.
* As informações são agrupadas em relatórios por sessão e por tipo de teste.  O módulo de relatórios permite filtrar por data, visualizar o histórico de evolução e **exportar relatórios em PDF** para compartilhamento com profissionais de saúde.

### Arquitetura técnica

* **Linguagem e UI:** o aplicativo utiliza **Kotlin** com **Jetpack Compose** em uma arquitetura **MVVM**.  Os estados da interface são gerenciados por _ViewModels_ e fluxos reativos, permitindo transições suaves e desacoplamento entre UI e lógica de negócios.
* **Persistência:** implementação da biblioteca **Android Room** com _DAOs_ para cada entidade (`AudioReportDao`, `CoherenceReportDao`, `MotionReportDao`, etc.).
* **Biblioteca de fala:** integração com **Whisper.cpp**, compilada como biblioteca nativa no módulo `lib/`.  O modelo é carregado dos assets e processa os dados de áudio no próprio dispositivo.
* **Ajustes de sistema:** a classe `SettingsViewModel` armazena preferências do usuário (tema, contraste, tamanho de texto).  A função `PaintSystemBars` aplica as cores da barra de status e navegação de acordo com o tema selecionado.

## Instalação

1. **Clone o repositório** ou baixe o ZIP deste projeto.
2. **Baixe o modelo Whisper**: o arquivo de modelo não é versionado por ser volumoso.  Obtenha o modelo no [Google Drive](https://drive.google.com/file/d/1kajwLNlkBH-_FD3Y5vSH-R8N05lPmUuG/view?usp=sharing) e copie para `app/src/main/assets/models`.
3. **Abra o projeto no Android Studio** (Arctic Fox ou superior), aguarde a sincronização do Gradle e conecte um dispositivo Android ou emulador.
4. **Compile e execute** a aplicação.  Na primeira execução o aplicativo solicitará permissão para gravar áudio.  Permita o acesso para realizar o teste de fala.

## Utilização

### Navegação

Ao abrir o RecuperAVC você encontrará a **tela inicial**, onde poderá escolher:

* **Teste de Fala** – avalia velocidade e precisão na leitura de uma frase.
* **Arranjo de Frases** – mede compreensão sintático‑semântica reordenando palavras embaralhadas.
* **Teste de Motricidade Fina** – avalia rapidez e coordenação dos toques.
* **Relatórios** – permite visualizar o histórico de resultados e exportar relatórios em PDF.
* **Preferências** – ajuste de tema, contraste, tamanho de texto e sons.

### Métricas de avaliação

| Teste | Métricas principais (descrições curtas) |
|------|-----------------------------------------|
| **Fala** | **WPM**: Palavras por minuto; **WER**: Taxa de erro de palavras |
| **Arranjo de Frases** | Tempo total de arranjo; erros por tentativa; índice de coerência |
| **Motricidade Fina** | Cliques por minuto (CPM); total de cliques; número de toques fora do alvo |

#### Classificação de WPM e WER

| WER | WPM | Interpretação | Cor |
|-----|-----|--------------|----|
| ≤ 10 % | ≥ 120 | Fala dentro do esperado | 🟢 Normal |
| ≤ 20 % | ≥ 60  | Possível alteração leve | 🟠 Atenção |
| > 20 % | < 60  | Procure avaliação médica | 🔴 Preocupante |

> **Aviso médico:** este aplicativo é uma **ferramenta de apoio**.  Ele **não substitui** uma consulta médica.  Em caso de alteração significativa, procure um profissional de saúde.