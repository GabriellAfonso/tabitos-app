# Spec 018 — Descartar contagem do cronômetro

Estado: implementado (2026-11-11). Ajuste sobre as specs 008 e 016.

## Problema
Tocar sem querer no ▶ de uma tarefa/hábito com cronômetro começa a contar na hora. Até aqui o único jeito de
desfazer era pausar, abrir o diálogo, rolar até "Sessões e trechos" e excluir o trecho fechado.

## Regras
- "Contagem atual" é o trecho mais recente, não manual e ativo da sessão aberta — o tempo contado desde o
  último Iniciar/Retomar. Descartar nunca apaga trechos anteriores nem tempo manual: uma sessão de hábito com
  meta diária/semanal pode juntar vários dias, e esse tempo continua creditado.
- Descartar fecha o trecho (se estiver contando), marca-o inativo, deixa a sessão pausada e grava a auditoria
  `DISCARDED` ("Contagem descartada"). Se a sessão não tiver outro tempo ativo, ela é encerrada e some do topo
  de Hoje, como se nunca tivesse começado.
- Indisponível para cronômetros de recompensa (o tempo foi comprado e debitado; descartar exigiria estorno) e
  para sessões cujo objetivo já foi atingido (a conclusão automática tem seu próprio Desfazer, spec 016).
- Com menos de 1 minuto contado, descarta sem perguntar; com 1 minuto ou mais, pede confirmação mostrando o
  tempo que será descartado.
- Ao iniciar ou retomar, aparece "Cronômetro iniciado · Desfazer". Desfazer descarta essa contagem. A mensagem
  some sozinha após o tempo recomendado pela acessibilidade (base de 8 s); perdê-la não perde dados.

## Onde fica
- Cartão "Sessão ativa/pausada" no topo de Hoje: menu ⋮ no canto com "Descartar contagem atual". Não é um
  botão ao lado de Pausar, para não criar outro toque acidental.
- Diálogo do cronômetro: botão de texto em cor de erro logo abaixo de Pausar/Retomar.
- Fora: chip ▶ do cartão da lista, notificação do serviço e cartões de recompensa.

## Fora do escopo
- Desfazer o próprio descarte.
- Mudança de schema: a auditoria é gravada pelo nome do enum.
