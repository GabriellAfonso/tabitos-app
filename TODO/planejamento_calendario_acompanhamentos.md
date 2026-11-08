# Calendário e acompanhamentos personalizados — design inicial

Registro de conversa. Não é prompt pronto, não executa Spec Kit nem autoriza implementação.

## Intenção expressa pelo usuário

- Planejar o calendário completo junto de uma nova funcionalidade de acompanhamento.
- Cada pessoa deve poder montar seu acompanhamento para um hábito positivo ou negativo.
- Não implementar uma ficha fixa de compulsões/pornografia ou copiar integralmente o texto
  de referência produzido em outro chat. Os exemplos são possibilidades, não requisitos fechados.
- Incluir a possibilidade de dar uma nota pela barra de intensidade da vontade daquele contexto.
- Exemplos de perguntas personalizáveis: Quanto a vontade prejudicou sua rotina? com opções
  Nada/Levemente/Moderadamente/Muito e descrições; Quais gatilhos você percebeu? com seleção
  de opções. Escolha única/múltipla e edição das opções precisam ser detalhadas.
- Objetivo: registrar contexto e rastrear melhor a evolução de hábitos, não apenas marcar ocorrência.

## Texto de referência: não equivale a decisões aprovadas

O usuário trouxe ideias de formulário diário, gatilhos, comportamentos, duração, observações,
histórico/calendário, dashboards, metas, privacidade, exportação e exclusão definitiva.
Não presumir que todo esse escopo foi aceito. Confirmar o recorte do construtor e das análises.
Não incorporar exemplos íntimos como conteúdo obrigatório, diagnóstico ou perfil do usuário.

## Decisões aprovadas na rodada seguinte

- Cada hábito pode ter um formulário próprio editável pelo usuário.
- Primeira versão oferece escala numérica, escolha única, escolha múltipla e texto livre.
- Usuário define quais perguntas são obrigatórias ou opcionais.
- Usuário escolhe entre um registro por dia ou múltiplos registros diários; padrão é um por dia.
- Incluir lembrete em horário escolhido para preencher o acompanhamento. O usuário quer um aviso
  persistente até resolver; viabilidade e limites do Android precisam ser documentados, sem prometer
  notificação impossível de dispensar nem confundir lembrete com foreground service obrigatório.
- Permitir escolher a data do registro, inclusive ontem quando esqueceu de preencher. Distinguir
  data de referência e momento real de preenchimento.
- Aviso oferece Preencher e Adiar. Abrir o formulário não resolve a pendência; salvar um registro
  válido a resolve. A notificação persistente deve respeitar as limitações do Android; dispensá-la
  não elimina a pendência no app. Não prometer notificação impossível de remover.
- Permitir datas desde a criação do acompanhamento até hoje; não aceitar datas futuras ou
  anteriores à criação. No modo de um registro por dia, escolher uma data preenchida abre a
  edição do registro existente, sem duplicá-lo.
- Preencher ou deixar de preencher o acompanhamento não altera moedas/XP, não conclui prática
  nem registra recaída/ocorrência automaticamente. As respostas são contexto separado do hábito.
- Permitir marcar uma escala como indicador principal para destaque e gráfico, configurando
  nome, mínimo, máximo e descrições dos extremos; padrão de escala 0 a 10.
- O usuário pode reordenar as seções para escolher o que fica acima/abaixo. A escala principal
  não deve ser forçada ao topo contra essa ordenação; ser principal define destaque/análise,
  não posição fixa. A ordem definida deve ser preservada.
- Perguntas de seleção permitem criar, renomear e reordenar opções, escolhendo resposta única
  ou múltipla por pergunta. Remover uma opção a retira de novos preenchimentos, preservando
  respostas e rótulos históricos de registros anteriores que a utilizaram.
- Calendário com visões mensal, semanal e diária; filtros por lista, hábito e tipo
  (tarefas, hábitos e acompanhamentos).
- Tocar em uma data abre seus itens/registros e permite preencher ou editar o acompanhamento
  daquele dia, respeitando configuração de quantidade e limites de data.
- Dias com acompanhamento têm indicador discreto; ausência de preenchimento aparece como
  Sem dados, nunca como intensidade zero, abstinência confirmada ou falha.
- Selecionar um acompanhamento permite ver evolução de sua escala principal e frequência
  das opções escolhidas, em períodos de 7, 14, 30 e 90 dias. Não misturar escalas de hábitos
  diferentes nem apresentar associação de respostas como diagnóstico/causalidade.
- Mudanças no formulário valem para novos registros; preservar perguntas, opções, escalas
  e respostas históricas. Editar um registro antigo usa a versão original, sem exigir campos
  adicionados depois. Reordenação atual não deve reescrever silenciosamente a estrutura histórica.
- No modo de múltiplos registros diários, apresentar cada registro individualmente no histórico
  e nos gráficos. Não somar nem calcular média automática das notas diárias.
- Lembretes de acompanhamento usam texto neutro, como Você tem um acompanhamento para preencher,
  sem expor nome do hábito, perguntas ou respostas na notificação.

## Estado consolidado

Propostas de calendário/análises e os três detalhes finais foram aprovados. Pronto para consolidar
prompt(s) com escopo coeso. Não importar automaticamente todo o texto de referência: biometria/PIN,
exportação, criptografia específica e módulos clínicos não foram definidos como funcionalidades
de primeira entrega. Preservar privacidade local e regras existentes sem inventar esses requisitos.

Prompt consolidado em `TODO/specify_calendario_acompanhamentos_app.md` (2026-11-07), em uma
única spec com histórias priorizadas. Pontos não discutidos ficaram como suposições mínimas
explícitas: horário diário único, resolução da pendência no modo múltiplo, exclusão individual
de registro, ciclo de vida igual ao do hábito e apresentação de escala com faixa alterada.

## Propostas ainda em discussão

- Formulário por hábito: métricas numéricas, perguntas de escolha única/múltipla e texto opcional.
- Histórico/calendário para acessar registros e visualizar métricas escolhidas, sem confundir
  falta de registro com intensidade zero, falha ou comportamento ausente.
- Separar respostas de acompanhamento de registros que alteram metas/moedas/XP: não transformar
  uma resposta ou intensidade em ocorrência/penalidade automática sem decisão explícita.
- Privacidade das respostas e das notificações, edição histórica do formulário e registros.
- Dividir calendário/construtor/análises em specifies coesos se necessário, sem impor uma entrega única.
