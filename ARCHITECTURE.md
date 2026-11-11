# GameVault - Arquitetura Atual do Projeto

Este documento descreve a arquitetura real implementada no código do app, alinhando as decisões de estrutura, persistência e UI com o que já está funcionando no projeto.

---

## 1. Visão geral do produto

O GameVault é um aplicativo Android em Jetpack Compose para organizar uma biblioteca de jogos, acompanhar o tempo jogado em sessão, controlar a fila de backlog, comparar ritmo com lançamentos Day One e registrar avaliações pessoais e memórias afetivas.

A base atual do app já implementa:

- dashboard com jogo em foco e próximos lançamentos;
- biblioteca com filtros, busca e roleta da fila;
- detalhes de jogo com avaliação, tempo, memória e conclusão;
- agenda/planner com cálculo de sobreposição e estado de duração indefinida;
- persistência local em Room com fluxo reativo via StateFlow;
- sincronização de metadados online e formatação de tempo em HH:MM:SS.

---

## 2. Stack tecnológica implementada

- Linguagem: Kotlin
- UI: Jetpack Compose + Material 3
- Arquitetura: MVVM + camadas de UI/ViewModel + Repository + DAO/Entity
- Banco local: Room
- Reatividade: Kotlin Flow + StateFlow
- Navegação: Navigation Compose
- Persistência de metadados: Room + API local de resolução de dados
- Tema visual: dark mode com paleta Carbon Matte / Ember Orange
- Formatação de tempo: utilitários próprios para HH:MM:SS e countdown

Observação: a aplicação não está em uma Clean Architecture estrita com módulos separados (`data`, `domain`, `ui` totalmente isolados), mas a separação funcional já está bem definida no código:

- `data/`: acesso ao banco, importação, repository e serviços remotos
- `domain/`: modelos e use cases de regra de negócio
- `ui/`: estados, telas, navegação e componentes visuais

---

## 3. Estrutura de diretórios real

```text
app/src/main/java/io/github/carvalhoxx/gamevault/
├── data/
│   ├── importer/
│   │   └── CsvGameImporter.kt
│   ├── local/
│   │   ├── converter/
│   │   ├── dao/
│   │   │   └── GameDao.kt
│   │   ├── entity/
│   │   │   └── GameEntity.kt
│   │   ├── GameVaultDatabase.kt
│   │   └── UserPlatformStore.kt
│   ├── remote/
│   │   ├── GameMetadataModels.kt
│   │   ├── GameMetadataService.kt
│   │   └── HltbReferenceCatalog.kt
│   └── repository/
│       └── GameRepository.kt
├── di/
│   └── AppContainer.kt
├── domain/
│   ├── model/
│   │   ├── CompletionStyle.kt
│   │   ├── DurationResolution.kt
│   │   ├── GameStatus.kt
│   │   ├── PriorityTier.kt
│   │   └── ScheduleOverlapResult.kt
│   ├── usecase/
│   │   ├── CalculateScheduleOverlapUseCase.kt
│   │   ├── GetEffectiveDurationUseCase.kt
│   │   └── GetGlobalDailyRhythmUseCase.kt
│   └── util/
│       └── TimeFormatUtils.kt
├── ui/
│   ├── components/
│   │   ├── AppIcons.kt
│   │   ├── GameDialogs.kt
│   │   └── RemoteImageCache.kt
│   ├── navigation/
│   │   └── SideQuestNavGraph.kt
│   ├── screens/
│   │   ├── dashboard/
│   │   ├── details/
│   │   ├── library/
│   │   └── planner/
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   └── viewmodel/
│       ├── GameVaultViewModel.kt
│       ├── PlannerUiState.kt
│       └── PlannerViewModel.kt
├── GameVaultApplication.kt
├── MainActivity.kt
└── ui/
```

---

## 4. Modelo de dados principal

A entidade central do domínio é `GameEntity`, mapeada para a tabela `games` do Room.

### 4.1. `GameEntity`

Campos principais já presentes no código:

- `id`: identificador local
- `externalApiId`, `dealApiId`: referências externas
- `title`, `coverUrl`, `backdropUrl`: identidade visual do jogo
- `releaseDate`, `releaseYear`: data de estreia e ano
- `genres`, `platform`: metadados e plataforma
- `status`: `GameStatus`
- `priority`: `PriorityTier`
- `isMainFocus`: foco atual para agenda
- `hltbMainSeconds`, `hltbExtraSeconds`, `hltbCompletionistSeconds`: referências de tempo
- `userCustomEstimateSeconds`: estimativa do usuário
- `targetCompletionStyle`: objetivo de conclusão
- `timePlayedSeconds`, `activeTimerStartTime`, `startDate`, `completionDate`: cronômetro e histórico
- `personalRating`, `isHallOfFame`, `coreMemoryNote`, `isPersonalGoty`: nota e memória
- `isOwned`, `targetPrice`, `currentLowestPrice`, `discountPercentage`: wishlist/promoção

A entidade também expõe métodos de regra de negócio:

- `getOfficialHltbTargetSeconds()`: retorna a duração oficial conforme estilo de finalização
- `getEffectiveDurationSeconds()`: fallback em 3 níveis
- `resolveDuration()`: retorna `DurationResolution`
- `getLiveTimePlayedSeconds()`: inclui sessão ativa em execução

### 4.2. Enums do domínio

`GameStatus`:

- `PLAYING`
- `BACKLOG`
- `DAY_ONE`
- `WISHLIST`
- `COMPLETED`
- `PAUSED`
- `DROPPED`

`PriorityTier`:

- `TIER_S`, `TIER_A`, `TIER_B`, `TIER_C`

`CompletionStyle`:

- `MAIN_STORY`
- `MAIN_EXTRA`
- `COMPLETIONIST_100`

---

## 5. Camadas de arquitetura e fluxo de dados

### 5.1. Camada de dados

Localizada em `data/`.

- `GameVaultDatabase`: base do Room
- `GameDao`: DAO com queries para jogos, status, foco, Hall da Fama, GOTY e biblioteca
- `GameRepository`: orquestra operações de salvar, remover, atualizar, sincronizar e iniciar/pausar cronômetro
- `CsvGameImporter`: importação de CSV da biblioteca
- `GameMetadataService`: resolução online de capa, data e tempo de jogo

### 5.2. Camada de domínio

Localizada em `domain/`.

- `GameStatus`, `PriorityTier`, `CompletionStyle`: modelos do domínio
- `DurationResolution`: representação de duração resolvida ou indefinida
- `CalculateScheduleOverlapUseCase`: calcula conflito entre foco atual e lançamentos
- `GetEffectiveDurationUseCase`: resolve duração real usando usuário e HLTB
- `GetGlobalDailyRhythmUseCase`: cálculo do ritmo global do usuário
- `TimeFormatUtils`: formatação em strings amigáveis

### 5.3. Camada de apresentação

Localizada em `ui/`.

- `SideQuestNavGraph`: controlador principal da navegação
- `DashboardScreen`: tela inicial
- `LibraryScreen`: biblioteca + filtros + roleta da fila
- `GameDetailScreen`: detalhes do jogo, nota e memória afetiva
- `PlannerScreen`: agenda e planejamento de Day One
- `GameVaultViewModel`: estado e ações da biblioteca e dashboard
- `PlannerViewModel`: estado da agenda e cálculo de sobreposição

---

## 6. ViewModels e estados reativos

### 6.1. `GameVaultViewModel`

Responsável por:

- observação de todos os jogos em `allGames`
- filtros da biblioteca em `LibraryFilterState`
- dashboard em `DashboardUiState`
- temporizador em tempo real (`_nowMillis`)
- mensagens de status (`_statusMessage`)
- sincronização de biblioteca
- roleta da fila

Principais ações do ViewModel:

- `toggleSessionTimer(game)`
- `updateExactTimePlayed(gameId, hours, minutes, seconds)`
- `updateRatingAndMemory(gameId, rating, memoryNote, isPersonalGoty)`
- `markGameCompleted(game)`
- `saveGame(game)`
- `refreshGameMetadata(gameId)`
- `syncMissingLibraryMetadata()`
- `deleteGame(gameId)`
- `spinBacklogRoulette()`, `dismissRouletteWinner()`

### 6.2. `PlannerViewModel`

Responsável por calcular o estado da tela de agenda com base em:

- jogo em foco (`observeMainFocusGame()`)
- lançamentos Day One (`observeDayOneGames()`)
- todos os jogos da biblioteca
- ritmo diário global
- duração efetiva do jogo (`GetEffectiveDurationUseCase` e `DurationResolution`)

Estados possíveis:

- `Idle`
- `Loading`
- `Ready`
- `AgendaDedicadaTbd`

Esse estado é usado pela tela `PlannerScreen` para renderizar a agenda ou o modo de duração indefinida.

---

## 7. Navegação atual

A navegação principal está em `SideQuestNavGraph` e usa `NavHost` com as rotas:

- `dashboard`
- `library`
- `game_details/{gameId}`
- `planner`
- `wishlist`

Regras importantes implementadas:

- a barra inferior destaca a aba ativa
- ao abrir detalhes de jogo, a aba de origem permanece visível
- clicar na aba ativa pode levar de volta à listagem da origem
- `navigateToTab()` usa `popBackStack`/`navigate` para manter comportamento esperado

---

## 8. Regras de negócio implementadas no código

### 8.1. Cronômetro e tempo jogado

O cronômetro live funciona com a combinação de:

- `timePlayedSeconds`
- `activeTimerStartTime`
- `getLiveTimePlayedSeconds(now)`

Quando a sessão é iniciada, `GameRepository.startSessionTimer()` salva o timestamp e muda o status para `PLAYING`.
Quando é parada, `stopSessionTimer()` soma o tempo decorrido ao `timePlayedSeconds`.

### 8.2. Fallback seguro para duração de Day One

A regra de negócio foi implementada conforme a especificação original:

1. tenta `userCustomEstimateSeconds`
2. depois usa `hltbMainSeconds` / `hltbExtraSeconds` / `hltbCompletionistSeconds`
3. se ambos forem nulos, retorna `DurationResolution.DurationTbdLock`

Isso evita crash em jogos ainda não lançados como GTA VI.

### 8.3. Nota 0 a 11 / Hall da Fama

A lógica atual preserva:

- `personalRating` em faixa de `0.0f` a `11.0f`
- `isHallOfFame` quando `rating >= 11.0f`
- memórias afetivas em `coreMemoryNote`
- GOTY pessoal em `isPersonalGoty`

### 8.4. Biblioteca e roleta da fila

- a roleta considera apenas jogos com status `BACKLOG`
- a prioridade afeta o peso do sorteio
- o resultado pode definir o jogo como foco principal para a jornada

---

## 9. Estado atual de implementação

### Já implementado

- Dashboard principal
- Biblioteca com busca e filtros
- Detalhes do jogo
- Formulário de jogo e modais auxiliares
- Agenda/Planner
- Lógica de duração a definir / Day One
- Persistência em Room
- Importação CSV
- Sincronização de metadados

### Em desenvolvimento ou pendente

- perfil do usuário e ritmo global configurável
- wishlist de promoções completa
- GOTY por ano de conclusão com desempate
- seção de promoções e alertas automáticos
- refinamento do fluxo de Day One para perfil do usuário

---

## 10. Observações de manutenção

- O app usa `StateFlow` em `ViewModel` e coleta em `Compose` com `collectAsState()`/`collectAsStateWithLifecycle()`.
- O tema segue a identidade visual “console ui” com fundo escuro e destaques em laranja/ember.
- O código já encontra um equilíbrio entre implementação funcional e estrutura de domínio, sem depender de DI pesada para o momento.
- O projeto está pronto para evoluir em direção a uma arquitetura mais puramente modular, mas a base atual já está estável e funcional.
