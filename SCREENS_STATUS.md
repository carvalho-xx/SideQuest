# Mapeamento de Telas — GameVault (Status Atual)

Este documento consolida o estado real da implementação no código Android e aponta o que já foi entregue em UI, arquitetura e regras de negócio.

---

## 1. Resumo geral

| Escopo | Concluído | Pendente | Total |
| :--- | ---: | ---: | ---: |
| Telas e fluxos principais | 5 | 3 | 8 |
| Modais e diálogos | 4 | 2 | 6 |
| Funcionalidades de negócio | 8 | 4 | 12 |

---

## 2. Fluxos principais e status

| # | Fluxo / Tela | Componente principal | Status | Observações |
| :-: | :--- | :--- | :---: | :--- |
| 1 | Dashboard / Início | `DashboardScreen.kt` | ✅ | Tela principal implementada com jogo em foco, faixa de metas, próximos lançamentos e timer de sessão. |
| 2 | Biblioteca & Backlog | `LibraryScreen.kt` | ✅ | Busca, filtros, abas, roleta da fila, ações de sincronização e importação CSV. |
| 3 | Detalhes do Jogo | `GameDetailScreen.kt` | ✅ | Nota 0–11, memória afetiva, tempo jogado, edição, exclusão e marcação como zerado. |
| 4 | Agenda / Planner | `PlannerScreen.kt` + `PlannerViewModel.kt` | ✅ | Cálculo de ritmo, sobreposição, duração indefinida e estado `DURATION_TBD_LOCK`. |
| 5 | Novo Jogo / Edição | `GameFormDialog` em `GameDialogs.kt` | ✅ | Formulário dinâmico por status com lógica de Day One, prioridades e concluído. |
| 6 | Lista de Desejos | `wishlist` placeholder | ⚠️ | Tela de rota existente, mas feature ainda não foi implementada de forma completa. |
| 7 | GOTYs por ano | futura implementação | ⚠️ | Requisito do projeto, ainda não entregue. |
| 8 | Perfil do Usuário | futura implementação | ⚠️ | Meta anual, ritmo global e personalização ainda pendentes. |

---

## 3. Modais e diálogos

| # | Componente | Status | Observações |
| :-: | :--- | :---: | :--- |
| 1 | `GameFormDialog` | ✅ | Criação e edição de jogos com campos condicionais por status. |
| 2 | `ManualTimePickerDialog` | ✅ | Ajuste manual de tempo em HH:MM:SS. |
| 3 | `CompleteGameDialog` | ✅ | Marcação de jogo como zerado com nota, memória e confirmações. |
| 4 | `BacklogRouletteDialog` | ✅ | Roleta ponderada por prioridade para backlog. |
| 5 | `Delete confirmation` | ✅ | Confirmação de exclusão na tela de detalhes. |
| 6 | `GotyTiebreakerDialog` | ⚠️ | Ainda pendente. |

---

## 4. Entidades e regras de negócio implementadas

### Base do banco

- `GameEntity` em `data/local/entity/GameEntity.kt`
- Geração de tabela `games` em Room
- Índices para status, foco, Hall da Fama e GOTY pessoal

### Regras já funcionando

- tempo jogado com cronômetro em sessão
- pausa/retomada de jogo
- duração efetiva com fallback seguro
- `DurationResolution.DurationTbdLock` para jogos sem estimativa
- avaliação de 0 a 11 com campo especial 11/10
- marcação de `isHallOfFame`
- sincronização de metadados com capa e HLTB
- importação CSV
- roleta da fila por prioridade

### Ainda pendentes

- perfil do usuário com meta anual e ritmo global
- wishlist de promoções
- agrupamento de GOTYs por ano de conclusão
- desempate de GOTY pessoal
- integração com WorkManager para alertas de promoção

---

## 5. Arquitetura atual percebida no código

A arquitetura real do projeto é:

- `ui/`: telas, navegação, componentes visuais, viewmodels
- `domain/`: enums, utilitários e use cases de lógica de negócio
- `data/`: Room, repository, importação e serviços de metadados

Fluxo principal:

- tela coleta `StateFlow` do `ViewModel`
- `ViewModel` observa consultas em `GameRepository`
- `GameRepository` acessa `GameDao` e atualiza `Room`
- a UI reage automaticamente às mudanças via `StateFlow`

---

## 6. Checklist do status prático

- [x] Dashboard funcional
- [x] Biblioteca com filtros e busca
- [x] Formulário dinâmico de jogo
- [x] Detalhes do jogo
- [x] Agenda/planner funcional
- [x] Rodada de roleta da fila
- [x] Importação CSV
- [x] Sincronização de metadados
- [ ] Wishlist completa
- [ ] Perfil do usuário
- [ ] GOTY por ano de conclusão
- [ ] Desempate de GOTY
- [ ] Promoções automáticas e alertas

---

## 7. Observação final

O repositório já está com a base principal em um estado funcional e coeso. A documentação de arquitetura foi atualizada para refletir o código real, e a relação entre app, viewmodel, repository e Room está consistente com o que está sendo implementado no projeto.
