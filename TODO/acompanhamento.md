# Acompanhamento de hábito — redesenho (rascunho)

Protótipo: `prototypes/acompanhamento/index.html` (2026-11-18).

Telas em discussão: painel do acompanhamento (hoje `TrackerHubDialog`), criar/editar perguntas
(`TrackerFormContent`) e preencher registro (`TrackerEntryContent`), em `features/trackers/TrackerContent.kt`.

## Opções apresentadas
- A — Folha arrumada: painel em folha inferior, editor em tela cheia com linhas rótulo→valor e folha por pergunta.
- B — Página com abas: Registros · Evolução · Perguntas; editor com cartões expansíveis; registro em tela cheia.
- C — Calendário e modelos: mês colorido pela nota, criação por modelo + pré-visualização, uma pergunta por tela.

## Comum a todas (proposta)
- Data por seletor de calendário, sem campo aaaa-mm-dd.
- Regras da spec 009 inalteradas; sem mudança de schema.

## Decisão (2026-11-18)
- Escolhida a opção B inteira (painel, editor e registro). Registrada em `specs/034-tracker-page/spec.md`.
- Por consequência: sem modelos prontos, histórico em lista por semana, preencher numa tela só.

## Pendências
- Validar no celular (tamanho da faixa de escala 0–10, barra Adicionar com fonte grande).
