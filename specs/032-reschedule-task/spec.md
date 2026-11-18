# Remarcar tarefa pelos detalhes

Feature 032 · 2026-11-18 · Implementada · Branch de trabalho `master`.
Origem e decisões de design em [prompt](../../TODO/specify_remarcar_tarefa_app.md).

## User Scenarios & Testing

### US1 · Escolher o novo prazo (P1)

Na terça-feira, quero trazer para quinta uma tarefa prevista para sábado. Abro seus detalhes, toco em
**Remarcar**, escolho quinta no calendário e encontro a tarefa na quinta com o horário original.
Também posso escolher Hoje ou Amanhã em um toque. Esse é o fluxo principal.

- Prazo hoje → Amanhã e Escolher data. Prazo amanhã → Hoje e Escolher data. Nos demais casos, ambas as datas
  rápidas aparecem nessa ordem, seguidas de Escolher data. Cada data rápida inclui dia da semana, data e hora
  quando houver. O calendário exclui o prazo atual e os dias anteriores a hoje.
- Se vencida, a folha avisa que o prazo ficará registrado como perdido sem penalidade. Confirmar qualquer
  destino válido registra a perda e o adiamento. Em uma série, a folha informa que só esta ocorrência muda.
- Depois de salvar, os detalhes fecham. A mensagem “Remarcada para hoje/amanhã/<dia>” permite desfazer ou fechar.

### US2 · Manter os lembretes e desfazer (P1)

Ao antecipar uma tarefa em dois dias, espero o aviso fixo de 09:00 dois dias antes e o relativo de uma hora
uma hora antes do novo prazo. Desfazer deve restaurar os momentos e a data original, inclusive se vencida.
Teste independente com dois avisos, conferindo horários antes de remarcar, após remarcar e depois de desfazer.

### US3 · Entender a mudança no Histórico (P2)

Quero distinguir ter antecipado uma tarefa de tê-la adiado. O histórico geral, o filtro Tarefas e o da série
mostram a mudança com as duas datas e identificam o desfazer como ajuste. Somente mover para depois acrescenta
um adiamento no período em que ocorreu. Os registros anteriores conservam sua interpretação.

### Edge cases

É permitido coincidir com outra ocorrência da série. Uma tarefa resolvida, sem data ou com prazo semanal
não oferece Remarcar. Avisos deslocados para um momento já passado não devem gerar entrega tardia.

## Requirements

### Escopo e restrições

Somente a ação nos detalhes da tarefa. O editor, cartões, hábitos, notificações com Adiar, conclusão, regras de
recorrência, recompensas e revisão seguem suas especificações vigentes. Funciona offline com os dados locais.
Uma falha ao salvar deve apresentar erro sem anunciar sucesso. Preservar toda a rotina existente.

### Functional Requirements

- FR-001 Oferecer Remarcar nas ocorrências abertas com prazo em dia específico. Exibir o prazo atual e informar
  a manutenção do horário quando houver. Oferecer as datas e restrições descritas em US1.
- FR-002 Mover apenas a ocorrência escolhida preservando série, demais campos e horário. Aplicar a regra de
  revisão ao prazo vencido, sem confirmação adicional ou impacto em moedas e XP.
- FR-003 Deslocar datas fixas pelo mesmo número de dias civis e manter sua hora local. Relativos acompanham
  o prazo. Preservar regras de dias da semana, fuso e recuperação. Salvar a tarefa e os lembretes juntos.
- FR-004 Desfazer restaura a data e os momentos dos lembretes e acrescenta a correção ao histórico. A mensagem
  usa as cores do tema e fecha após oito segundos, ajustados às preferências de acessibilidade.
- FR-005 Registrar a direção comparando os prazos anterior e novo. Exibir “Antecipada de <de> para <para>” e
  “Antecipação desfeita: voltou para <data>”. Preservar a contagem de Adiadas e o tratamento de edições antigas.
- FR-006 Apresentar cada opção de data com rótulo e data acessíveis e alvo de ao menos 48 dp. Permitir alcançar
  todas as opções e avisos com fonte a 200%.

### Key Entities

Uma ocorrência pertence a uma série e tem prazo próprio. Cada remarcação e cada correção têm ocorrência,
momento, prazo anterior e novo dia. Um lembrete pode pertencer à série ou à ocorrência.

### Decisões de apresentação confirmadas em 2026-11-18

Abrir o seletor em amanhã, ou em hoje quando amanhã for o prazo atual. A folha informa o prazo atual e o
calendário o desabilita. As datas bastam para escolher, sem dica adicional sobre a direção.

## Success Criteria

- SC-001 Os quatro casos de prazo (vencido, hoje, amanhã, futuro) oferecem exatamente os destinos de US1.
- SC-002 No exemplo terça/sábado, quinta passa a ser o único prazo da ocorrência. As demais mantêm suas datas.
- SC-003 Antecipar e desfazer produzem duas entradas distinguíveis e zero incremento em Adiadas, moedas e XP.
- SC-004 Nos dois sentidos, os horários do cenário US2 são preservados, incluindo a restauração ao desfazer.
- SC-005 Em claro e escuro a mensagem fecha automaticamente e o fluxo completo é acessível com fonte ampliada.
