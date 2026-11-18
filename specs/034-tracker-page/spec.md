# Feature Specification: Página de acompanhamento com abas

**Feature Branch**: `034-tracker-page`
**Created**: 2026-11-18
**Status**: Implemented (interface only)
**Origem**: protótipo `prototypes/acompanhamento/index.html`, opção B "Página com abas", escolhida pelo usuário
em 2026-11-18. Decisões e pendências em `TODO/acompanhamento.md`.

## Contexto

O acompanhamento de hábito (spec 009) abria num diálogo longo que misturava preenchimento, data digitada
(`aaaa-mm-dd`), gráfico, pontos em texto, registros e arquivamento. O formulário era outro diálogo com todos os
campos abertos e quatro botões "+". Esta spec troca só a interface; regras, versões, lembretes e schema (7) da
spec 009 continuam iguais.

## Requisitos

### Página do acompanhamento (FR-001 a FR-008)
- **FR-001**: "Acompanhar" no pop-up do hábito abre uma página em tela cheia com Voltar, título
  "Acompanhamento" e o nome do hábito como subtítulo. Voltar (botão ou gesto) retorna ao pop-up do hábito.
- **FR-002**: Sem acompanhamento, a página mostra o ícone do hábito, uma explicação curta, "Criar acompanhamento"
  (só se o hábito não estiver excluído) e "Não muda moedas, XP nem sequência".
- **FR-003**: Com acompanhamento, uma faixa "Hoje" fica fixa no topo: sem registro hoje mostra "Ainda não
  preenchido" e "Preencher"; com registro mostra o valor principal e "Abrir". Com "Vários por dia" o botão é
  sempre "Preencher" (novo registro). Arquivado ou hábito excluído mostra o aviso de somente consulta no lugar.
- **FR-004**: Abas Registros · Evolução · Perguntas; a aba escolhida é só estado de tela.
- **FR-005**: Registros: dias de hoje para trás agrupados por semana (segunda a domingo: "Esta semana",
  "Semana passada", "Semana de 3 nov"), a partir da criação. Dia com registro mostra o valor principal e as opções
  marcadas; dia vazio preenchível mostra "Sem registro" e toca para preencher. Mostra 4 semanas e "Ver mais".
  "Preencher outro dia" abre o calendário (criação até hoje), sem digitar data.
- **FR-006**: Evolução: períodos 7/14/30/90 dias, gráfico de pontos da escala principal (sem média nem soma),
  contagem de registros e dias sem dados, frequência das opções em barras por pergunta e o aviso de que contagens
  não indicam causa. A lista de pontos em texto sai; o gráfico mantém a descrição para leitores de tela.
- **FR-007**: Perguntas: lista numerada com o tipo e marcações (obrigatória, no gráfico) e "Editar perguntas".
- **FR-008**: Menu ⋮ com Arquivar/Restaurar acompanhamento (só com hábito não excluído).

### Criar/editar perguntas (FR-009 a FR-014)
- **FR-009**: Tela cheia com Fechar, título "Novo acompanhamento" ou "Editar acompanhamento", subtítulo do hábito
  e Salvar. Editando, mostra "Mudanças valem para os próximos registros".
- **FR-010**: "Registros por dia" em controle segmentado Um · Vários.
- **FR-011**: Cada pergunta é um cartão; só um fica aberto. Fechado mostra o texto (ou "Pergunta sem texto") e o
  tipo; aberto mostra texto, tipo (Escala · Uma opção · Várias · Texto), campos do tipo, Obrigatória e Remover.
- **FR-012**: Escala: mínimo e máximo por − / + (−1000 a 1000), descrições dos extremos e "Mostrar em Evolução"
  (indicador principal, exclusivo entre as escalas). Trocar o tipo desmarca o indicador e cria uma opção vazia
  quando vira escolha.
- **FR-013**: Escolha: opções em linhas com arrastar, nome, descrição opcional e remover; "Adicionar opção".
- **FR-014**: Barra inferior "Adicionar" com Escala · Uma opção · Várias · Texto; a pergunta nova abre já expandida.
  Lembrete diário continua no formulário. Perguntas e opções reordenam por arrastar com ações do TalkBack.

### Preencher registro (FR-015 a FR-017)
- **FR-015**: Tela cheia com Fechar, título com a data (Hoje/Ontem/"ter, 17 nov") e o hábito; um cartão por
  pergunta; barra inferior com Excluir (editando), Cancelar e Salvar.
- **FR-016**: Escala com até 11 valores numa faixa de botões; até 21 valores em chips; acima disso campo numérico.
  Cada valor diz "Valor 6 na escala de 0 a 10". Escolhas sem descrição viram chips; com descrição, linhas com
  rádio/caixa.
- **FR-017**: Registro de versão antiga e "Preenchido em" aparecem como hoje; perguntas opcionais respondidas têm
  "Limpar resposta".

## Fora de escopo
- Mudança de regra, schema, notificação ou lembrete; modelos prontos (opção C); evolução em Estatísticas.

## Critérios de aceite
- Nenhum campo de data digitado nas telas de acompanhamento.
- Calendário, aviso "Preencher" de Hoje e notificação abrem o registro como antes.
- Testes JVM das funções de apresentação (semanas, faixa Hoje, tipos, frequência) passam.
