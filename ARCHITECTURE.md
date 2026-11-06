# GameVault & DayOne Planner - Especificação Técnica e Arquitetura

> **Instrução para a IA (Antigravity):** Este documento contém a arquitetura, modelagem de dados, regras de negócio e o plano de implementação em fases de um aplicativo Android nativo para gestão de biblioteca de jogos, cronômetro de sessões, otimização de cronograma (Backlog vs. Lançamentos Day One) e rastreamento de promoções. Siga rigorosamente esta stack e consulte a **Seção 3 (Mapeamento de Telas e Imagens de Referência)** ao construir a UI em Jetpack Compose.

---

## 1. Visão Geral do Produto
Aplicativo Android desenvolvido para substituir planilhas complexas de controle de jogos. O sistema automatiza a busca de metadados, oferece um cronômetro integrado de sessões com precisão de segundos (`HH:MM:SS`), calcula estimativas reais de conclusão sem travar o jogador em barras de progresso rígidas, previne conflitos de agenda com grandes lançamentos (*Day One* — mesmo quando ainda não possuem duração estimada), monitora promoções da *Wishlist* e registra memórias afetivas com um sistema exclusivo de avaliação de 0 a 11.

---

## 2. Tech Stack Recomendada
* **Linguagem:** Kotlin (2.0+)
* **UI Framework:** Jetpack Compose (Material Design 3, Tema Escuro Nativo com paleta Deep Slate `#0D0C14`, Roxo Neon `#B85BFF` e Dourado `#F5C451` para Hall of Fame/GOTY).
* **Arquitetura:** MVVM (Model-View-ViewModel) + Clean Architecture simplificada (`data`, `domain`, `ui`).
* **Injeção de Dependência:** Hilt (Dagger).
* **Banco de Dados Local:** Room Database (SQLite) com suporte a Kotlin Coroutines e `Flow`.
* **Rede / APIs:** Retrofit2 + OkHttp3 + Kotlinx Serialization.
* **Carregamento de Imagens:** Coil (`AsyncImage`).
* **Tarefas em Background & Timer:** Android `WorkManager` (promoções/alertas) + `ForegroundService` (ou persistência de timestamp inicial no `DataStore`) para o Cronômetro de Sessão de Jogo ativo.

---

## 3. Mapeamento de Telas e Imagens de Referência (Design Specs)

Todas as imagens exportadas do Figma ficarão na pasta `design_refs/` na raiz do projeto. Ao gerar o código de cada tela na **Fase 2, 4, 5 e 6**, o Antigravity deve ler o arquivo de imagem correspondente como referência visual fiel:

| Nome do Arquivo na pasta `design_refs/` | Tela Correspondente no App (Jetpack Compose) | Descrição dos Componentes Visuais |
| :--- | :--- | :--- |
| **`01_dashboard_home.png`** | `DashboardScreen.kt` | Card Hero "Currently Playing" com Cronômetro (`HH:MM:SS`), comparador flexível com HLTB (sem barra que quebra se passar de 100%), carrossel *Day One Radar*, galeria dourada *Hall of Fame (11/10)* e card de *Personal GOTY*. |
| **`02_library_backlog.png`** | `LibraryScreen.kt` | Lista e grade da Biblioteca com abas de filtro (*Playing, Backlog, Completed, Dropped*), filtros por plataforma/prioridade e botão da *Roleta do Backlog*. |
| **`03_game_details_rating11.png`** | `GameDetailScreen.kt` | Header com arte do jogo, pílulas de referência HLTB, controle de tempo exato (`HH:MM:SS`), Seletor de Nota de `0` a `10` + Botão Dourado **`11/10 Life-Changing`**, card de *Core Memory* (Memória Afetiva) e botão *Personal GOTY*. |
| **`04_day_one_planner.png`** | `PlannerScreen.kt` | Configurador de ritmo (`h/dia`), linha do tempo comparando o jogo atual com lançamentos *Day One*, estado de **"Agenda Dedicada / HLTB Pendente (TBD)"** (para jogos não lançados como GTA VI) e sugestões de *Gap Filler*. |
| **`05_wishlist_and_goty_tiebreaker.png`**| `WishlistDealsScreen.kt` & `GotyTiebreakerDialog.kt` | Lista de desejos com preço-alvo, badge de desconto verde (`-60%`) e modal de **Desempate de GOTY Pessoal** colocando jogos empatados lado a lado. |

---

## 4. Modelagem do Banco de Dados (Room Entities)

### 4.1. `GameEntity` (Tabela Principal: `games`)
| Campo | Tipo | Descrição |
| :--- | :--- | :--- |
| `id` | `Long` (PK, Auto) | Identificador único local. |
| `externalApiId` | `String?` | ID do jogo na API (RAWG/IGDB). |
| `dealApiId` | `String?` | ID do jogo na API de promoções (CheapShark/ITAD). |
| `title` | `String` | Nome do jogo. |
| `coverUrl` | `String?` | URL da capa vertical. |
| `backdropUrl` | `String?` | URL da arte horizontal para cabeçalhos. |
| `releaseDate` | `Long?` | Timestamp da data de lançamento (crucial para *Day One*). |
| `genres` | `String` | Lista de gêneros separados por vírgula. |
| `platform` | `String` | Plataforma escolhida para jogar (ex: PC, PS5, Switch 2). |
| `status` | `GameStatus` (Enum) | `PLAYING`, `BACKLOG`, `DAY_ONE`, `WISHLIST`, `COMPLETED`, `PAUSED`, `DROPPED`. |
| `priority` | `PriorityTier` (Enum)| `TIER_S` (Urgente), `TIER_A`, `TIER_B`, `TIER_C`. |
| `isMainFocus` | `Boolean` | Define se é o jogo principal do momento no cálculo do cronograma. |
| **Métricas de Tempo (Flexíveis e Precisas)** | | |
| `hltbMainSeconds` | `Long?` | Referência HLTB Campanha Principal em segundos (**Nullable** para jogos não lançados). |
| `hltbExtraSeconds` | `Long?` | Referência HLTB Campanha + Extras em segundos (**Nullable**). |
| `hltbCompletionistSeconds`| `Long?` | Referência HLTB 100%/Platina em segundos (**Nullable**). |
| `userCustomEstimateSeconds`| `Long?` | Estimativa manual opcional caso o jogo ainda não tenha HLTB ou o jogador queira definir sua própria meta de tempo. |
| `targetCompletionStyle` | `CompletionStyle` | `MAIN_STORY`, `MAIN_EXTRA`, `COMPLETIONIST_100`. |
| `timePlayedSeconds` | `Long` | Tempo real jogado em **segundos** (exibido sempre formatado como `HH:MM:SS`). |
| `activeTimerStartTime` | `Long?` | Timestamp (`System.currentTimeMillis()`) de quando o cronômetro foi iniciado, ou `null` se pausado. |
| `startDate` | `Long?` | Data em que começou a jogar. |
| `completionDate` | `Long?` | Data em que zerou (usado para o filtro de GOTY por ano). |
| **Avaliação (0 a 11) e GOTY** | | |
| `personalRating` | `Float?` | Nota de `0.0` a `10.0`, permitindo o valor especial **`11.0` ("Jogo da Vida")**. |
| `isHallOfFame` | `Boolean` | `true` automaticamente quando `personalRating == 11.0f`. |
| `coreMemoryNote` | `String?` | Relato pessoal/afetivo (ex: "Primeiro co-op zerado com meu amigo"). |
| `isPersonalGoty` | `Boolean` | Marca se este jogo foi eleito pelo usuário como o GOTY do ano em que foi zerado. |
| **Wishlist e Promoções** | | |
| `isOwned` | `Boolean` | Se o usuário já comprou o jogo ou apenas deseja. |
| `targetPrice` | `Double?` | Preço-alvo que o usuário deseja pagar para receber alerta. |
| `currentLowestPrice` | `Double?` | Último menor preço registrado pelo `WorkManager`. |
| `discountPercentage` | `Int?` | Porcentagem de desconto atual. |

---

## 5. Regras de Negócio Exclusivas (Core Features)

### 5.1. Cronômetro Integrado (`HH:MM:SS`) e Comparador de Ritmo Flexível (Sem Barra Rígida)
* **Registro de Tempo:** O usuário nunca usa botões genéricos de `+1h`. Ele tem duas formas de registrar seu tempo:
  1. **Live Session Timer (Cronômetro no App):** Botão `▶ Start Session` no card do jogo atual. Conta o tempo em tempo real (`00:45:12`) salvando o `activeTimerStartTime` para não perder a contagem se o app fechar. Ao parar, soma o tempo decorrido ao `timePlayedSeconds`.
  2. **Input Manual Exato (`HH:MM:SS`):** Ao clicar no tempo jogado ou ao marcar o jogo como `COMPLETED`, abre-se um seletor numérico de `Horas : Minutos : Segundos` para digitar exatamente o tempo que consta no save do console/PC.
* **Comportamento quando o tempo passa do HLTB ou termina antes:**
  * O tempo do HLTB é apenas um **Marcador de Referência** (`Ref. HLTB: ~40h`).
  * **Se `timePlayedSeconds < hltbTargetSeconds`:** Exibe um indicador visual suave mostrando quanto falta para a média da comunidade.
  * **Se `timePlayedSeconds > hltbTargetSeconds` (Ex: jogou 43h num jogo de 40h):** A barra **não quebra** nem trava em 100%. Ela muda para a cor dourada/roxa com o status **"Explorando além da média (+03h 15m)"** e a estimativa de término passa a exibir *"Na reta final / No seu próprio ritmo"*.
  * **Se o usuário clicar em "Marcar como Zerado" antes das 40h (Ex: Rushou em 28h):** O jogo vai imediatamente para `COMPLETED` com o tempo final cravado em `28h`, registrando que o usuário foi mais rápido que a média.

### 5.2. Otimizador de Cronograma e Tratamento de `Null` em Jogos *Day One* (Caso GTA VI)
* **Cenário 1 — Calcular se você vai zerar o jogo ATUAL antes do lançamento Day One:**
  * Exemplo: Jogando *Phantom Blade* (Meta 100%: `44h`, já jogou `14h`, faltam `30h`). *GTA VI* lança no dia 19.
  * Para este cálculo, **o HLTB do GTA VI não importa**. O app usa apenas as `30h` restantes do *Phantom Blade* divididas pelo seu ritmo diário e compara com os dias que faltam até o dia 19.
* **Cenário 2 — Como fica a agenda DEPOIS do lançamento Day One quando o HLTB é `null` (Jogo inédito):**
  * Jogos que ainda não lançaram terão `hltbMainSeconds = null`. O Planner **nunca deve quebrar com `NullPointerException`**.
  * **Lógica de Fallback em 3 Níveis (`getEffectiveDurationSeconds()`):**
    1. Tenta usar `userCustomEstimateSeconds` (se o usuário quis chutar ex: *"Acho que vou gastar 80h no GTA VI"*).
    2. Se `null`, tenta usar a estimativa oficial da API (`hltbMainSeconds` / `hltbCompletionistSeconds`).
    3. Se ambos forem `null` (padrão para lançamentos inéditos), o jogo entra no estado **`DURATION_TBD_LOCK` (Agenda Dedicada / Duração Indefinida)**.
  * **Efeito Visual do `DURATION_TBD_LOCK` no Planner:** A partir do dia 19 (lançamento do GTA VI), o calendário mostra um bloco especial: *"🔒 Agenda Dedicada: GTA VI (Sem previsão de término — Aguardando dados de duração)"*. Se o usuário quiser planejar jogos *depois* do GTA VI, um botão rápido *"Definir estimativa provisória (ex: 50h)"* fica disponível no próprio card.

### 5.3. Sistema de Nota 0 a 11 ("Jogo da Vida" / Hall da Fama) e Desempate de GOTY
* Notas vão de `0.0` a `10.0` + nível especial **`11.0` (Hall of Fame / Jogo da Vida)** com campo obrigatório/destacado de **Memória Afetiva (`coreMemoryNote`)**.
* **Desempate de GOTY:** Na retrospectiva do ano, se 2 ou mais jogos empatarem com a maior nota (ex: dois jogos nota 11 ou dois jogos nota 10 no mesmo ano), o app habilita o card **"Confronto GOTY [Ano]"** para o usuário escolher manualmente qual leva o troféu.

---

## 6. Roteiro de Implementação para o Antigravity (Prompts em Fases)

* **Fase 1: Configuração Base e Banco de Dados Local** (Room com campos de tempo em segundos `Long` e HLTB `Long?` nullable, Enums, DAOs e Importador CSV).
* **Fase 2: UI Principal, Cronômetro de Sessão e Biblioteca** (Guiado pelas imagens `01_dashboard_home.png`, `02_library_backlog.png` e `03_game_details_rating11.png`).
* **Fase 3: Integração com API de Jogos e Tempos** (Busca automática de capas, datas de lançamento e referências de tempo).
* **Fase 4: Motor do Cronograma e Tratamento de Day One Nullables** (Guiado pela imagem `04_day_one_planner.png`).
* **Fase 5: Hall da Fama Nota 11, Estatísticas e Desempate GOTY** (Guiado pelas imagens `01_dashboard_home.png` e `05_wishlist_and_goty_tiebreaker.png`).
* **Fase 6: Radar de Promoções da Wishlist com WorkManager** (Guiado pela imagem `05_wishlist_and_goty_tiebreaker.png`).