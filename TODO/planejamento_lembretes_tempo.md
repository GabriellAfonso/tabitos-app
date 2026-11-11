# Lembretes, notificações e acompanhamento de tempo

Requisitos adicionados ao plano pelo usuário. Documento de design, não prompt de specify pronto.
Não autoriza implementação nem amplia automaticamente o escopo do ponto 3.

## Registros consolidados posteriores

- Lembretes/notificações: `TODO/planejamento_ponto_7.md` e
  `TODO/specify_lembretes_notificacoes_app.md`.
- Cronômetros/metas de tempo: `TODO/planejamento_ponto_8.md`, com seis decisões iniciais e
  quatro detalhes finais aprovados. Essas decisões substituem as antigas questões abaixo
  quando o assunto já estiver resolvido, inclusive pausa/retomada de recompensas iniciadas na compra.

## Intenções confirmadas

- Incluir lembretes e notificações no app. Podem ser tratados no próximo specify próprio;
  a ordem exata das entregas ainda não foi fechada.
- Permitir tarefas/atividades por duração, como meia hora de leitura: o usuário toca para iniciar
  a contagem do tempo.
- Permitir metas de tempo por período, como três horas de uma atividade por semana, acompanhadas
  por sessões iniciadas pelo usuário.
- Considerar cronômetro para recompensas com duração, como duas horas de gameplay, iniciado
  pelo usuário ao utilizar a recompensa. O usuário quer essa possibilidade no plano; detalhes
  de resgate, consumo do tempo e cobrança ainda não foram definidos.

## Separação proposta

- Um specify para lembretes/notificações, após existir o domínio de tarefas e seus prazos.
- Um specify para sessões e metas de tempo; integrar hábitos/recompensas quando esses domínios
  existirem. Não é preciso implementar todos os usos em uma única entrega.
- O cronômetro acompanha a atividade informada pelo usuário. Bloquear jogos/outros aplicativos
  ou verificar automaticamente a atividade não foi solicitado.

## Questões a discutir no momento apropriado

- Cronômetro crescente ou contagem regressiva, pausa/retomada e uma ou várias sessões simultâneas.
- Persistência da sessão ao fechar o app, bloquear a tela, reiniciar o aparelho ou mudar horário.
- Divisão de sessões entre dias/semanas e correção manual de tempo esquecido.
- Conclusão automática por duração versus confirmação, e momento de conceder moedas/XP.
- Lembretes por horário/antecedência, notificações de sessão e de meta cumprida, adiamento,
  permissões e comportamento quando notificações estiverem desativadas.
- Recompensa temporal: resgate versus início de uso, sessões parciais, saldo de minutos,
  vencimento e eventual cancelamento/estorno.
- Tempo esgotado: aviso e registro; não assumir punição ou bloqueio não combinados.

## Ideia futura: registro de tempo (2026-11-11)

Na spec 016 o cronômetro livre (contar para cima em tarefa/hábito normal) foi removido: ele é um
registro de tempo, não uma tarefa nem um hábito. Ideia pendente: uma funcionalidade própria de
"registro de tempo" (cronometrar uma atividade qualquer e ver o histórico), a discutir antes de virar spec.
