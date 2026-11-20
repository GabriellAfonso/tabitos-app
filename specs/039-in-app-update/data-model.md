# Data model — 039 (somente em memória)

Nada aqui é persistido. Não muda o schema Room nem as chaves do DataStore.

## StoreUpdate (resposta da loja)

| Valor | Significado |
|---|---|
| `NotAvailable` | Não há versão nova, a loja está indisponível ou deu erro. |
| `Available` | Há versão nova e o modo flexível é permitido. |
| `Downloading` | O download está em andamento (PENDING ou DOWNLOADING). |
| `Downloaded` | O download terminou e falta instalar. |
| `Failed` | O download falhou ou foi cancelado. Só aparece em `installStates`. |

## AppUpdatePhase (estado da sessão)

| Fase | Mostra |
|---|---|
| `IDLE` | nada |
| `PROMPT` | o pedido da loja, uma vez |
| `DOWNLOADING` | nada |
| `READY` | a faixa "Atualização pronta" |
| `DISMISSED` | nada até o fim do processo |

### Transições (`AppUpdateRules`)

| Fase atual | Evento | Nova fase |
|---|---|---|
| `IDLE` | check `Available` | `PROMPT` |
| `IDLE` | check `Downloading` | `DOWNLOADING` |
| `IDLE` | check `Downloaded` | `READY` |
| `IDLE` | check `NotAvailable` ou `Failed` | `IDLE` |
| `PROMPT` | prompt aceito | `DOWNLOADING` |
| `PROMPT` | prompt recusado ou não iniciado | `DISMISSED` |
| `DOWNLOADING` | install `Downloaded` | `READY` |
| `DOWNLOADING` | install `Failed` | `IDLE` |
| `READY` | "Agora não" | `DISMISSED` |
| qualquer fase, exceto `DISMISSED` e `PROMPT` | install `Downloading` | `DOWNLOADING` |
| `DISMISSED` | qualquer evento | `DISMISSED` |

## UpdateNotice (faixa)

`updateNotice(phase, timerRunning)`:

- `READY` sem cronômetro contando: `UpdateNotice.READY`, com "Reiniciar" e "Agora não".
- `READY` com cronômetro contando: `UpdateNotice.WAITING_TIMER`, com o texto de espera e "Agora não".
- Qualquer outra fase: `null`.

## AppUpdateUiState

`prompt: Boolean` vem de `phase == PROMPT`. `notice: UpdateNotice?` vem de `updateNotice(...)`.
