# Contrato de interface da feature 035

Folha "Prazo" no formato da opção C de `prototypes/prazo/index.html` (escolhida em 2026-11-19).

- Lista: Sem data · Num dia · Até domingo · Até sexta (dias úteis) · Até uma data. Cada opção mostra seu valor à
  direita: as semanas mostram o intervalo ("16–22 nov"); Num dia e Até uma data mostram a data (e o horário)
  quando escolhidas. Para tarefa recorrente, Sem data e Até uma data não aparecem.
- Abaixo da lista, um cartão com linhas "rótulo … valor" só quando há algo a definir:
  - Num dia: Dia e Horário.
  - Até uma data: Até, "Começa: Hoje · Mudar" (texto com botão, lido como "Mudar início") e Horário.
  - Série nova: Começa em (e Horário em Num dia).
  - Até domingo/sexta: nada em tarefa nova (vale a semana atual); ao editar, linha Data para trocar a semana.
- Escolher "Até uma data" abre direto o calendário do fim (sem dias antes do início).
- Chip/linha Prazo: "Até 28 nov" ou "Até 28 nov 18:00". Repetir indisponível com "Até uma data" ("Não se repete").
- Hoje: grupo de período, do início ao fim. Detalhes: Remarcar disponível (Hoje/Amanhã/calendário).
