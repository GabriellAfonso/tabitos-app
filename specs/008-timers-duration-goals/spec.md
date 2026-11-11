# Feature Specification: Cronômetros, metas de tempo e recompensas temporizadas

> Spec 016 supersedes the entry points here: the timer is a variant chosen when creating a task or practice
> habit ("Com cronômetro"), started from the card; reaching the duration completes automatically (undoable),
> with an optional alarm. The free count-up stopwatch and goal editing inside the timer sheet were removed.

**Feature Branch**: existing working branch; no branch creation requested
**Created**: 2026-11-07
**Status**: Implemented 2026-11-07; evidence and limits in verification.md.
**Input**: `TODO/specify_cronometros_metas_tempo_app.md` (body after `---`); decisions in `TODO/planejamento_ponto_8.md`.
The current user authorized specify → plan → tasks → implement in this chat.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Contar tempo de uma tarefa ou hábito (Priority: P1)

Como usuário, inicio, pauso e retomo um cronômetro crescente ou uma contagem regressiva ligada a uma
tarefa ou hábito de prática, com no máximo um cronômetro contando por vez.
**Why this priority**: entrega o valor central (registrar tempo real de atividade) sem depender do resto.
**Independent Test**: iniciar uma tarefa, pausar, retomar, tentar iniciar outra e conferir o total.

**Acceptance Scenarios**:
1. **Given** uma tarefa sem meta de tempo, **When** inicio o cronômetro crescente, aguardo 5 minutos e
   pauso, **Then** a sessão mostra 5 minutos, mantém a origem e não inventa um tempo-objetivo.
2. **Given** uma tarefa com meta de 30 minutos, **When** alterno entre visão crescente e regressiva,
   **Then** decorrido + restante = 30 minutos em ambas e alternar ou reabrir não acrescenta tempo.
3. **Given** uma sessão pausada por 10 minutos, **When** retomo, **Then** o intervalo pausado não conta.
4. **Given** um cronômetro contando, **When** tento iniciar outra origem, **Then** o app pede confirmação
   para pausar o atual; confirmar pausa o atual preservando seu tempo e inicia o novo; cancelar não muda nada.
5. **Given** toques repetidos em Iniciar ou recriação da Activity, **When** o estado é recarregado,
   **Then** existe uma única sessão contando com a mesma identidade.

---

### User Story 2 - Acumular sessões rumo a metas e confirmar conclusão (Priority: P1)

Como usuário, somo várias sessões para atingir 30 minutos numa tarefa ou 3 horas semanais num hábito; ao
atingir o objetivo o cronômetro pausa e pede minha confirmação para concluir pelo fluxo existente.
**Why this priority**: transforma o tempo em progresso sem conceder moedas/XP automaticamente.
**Independent Test**: registrar 40 + 50 + 90 minutos num hábito semanal de 3 horas e confirmar.

**Acceptance Scenarios**:
1. **Given** um hábito com meta semanal de 3 horas, **When** somo sessões de 40, 50 e 90 minutos na mesma
   semana, **Then** o progresso da semana mostra 3 h e o objetivo é considerado atingido.
2. **Given** uma sessão de domingo 23:50 a segunda 00:20, **When** contabilizada, **Then** 10 minutos vão para a
   semana (e o dia) anteriores e 20 minutos para a nova semana (e o novo dia).
3. **Given** o objetivo atingido durante a contagem, **When** o tempo restante chega a zero, **Then** a contagem
   pausa exatamente no objetivo, há um alerta e uma confirmação de conclusão; nenhum tempo extra é creditado.
4. **Given** a confirmação, **When** aceito, **Then** o app usa o fluxo existente de conclusão (checklist,
   prévia financeira, registro de prática) e o direito é concedido uma única vez; recusar mantém pendente.
5. **Given** alertas repetidos, nova confirmação, troca de modo ou retomada da interface, **When** a conclusão
   já foi aplicada, **Then** moedas/XP não são concedidos novamente.

---

### User Story 3 - Continuar em segundo plano e recuperar após reinício (Priority: P1)

Como usuário, mantenho a contagem com a tela bloqueada ou o app em segundo plano, vejo uma notificação da
sessão com Pausar e Abrir, e após reiniciar o aparelho encontro o tempo salvo, pausado.
**Why this priority**: sem isto o cronômetro não serve para atividades reais (leitura, jogo).
**Independent Test**: iniciar, bloquear a tela, pausar pela notificação; reiniciar o aparelho durante outra sessão.

**Acceptance Scenarios**:
1. **Given** uma sessão contando, **When** bloqueio a tela ou saio do app, **Then** a contagem continua e uma
   notificação mostra a origem, tempo/progresso, Pausar e acesso ao app.
2. **Given** a notificação, **When** toco Pausar, **Then** a mesma sessão persistida é pausada, como no app.
3. **Given** a Activity recriada ou o processo encerrado pelo sistema, **When** reabro o app,
   **Then** o tempo não é zerado nem duplicado.
4. **Given** um reinício do aparelho durante a contagem, **When** o aparelho volta, **Then** a sessão fica pausada
   com o último tempo conhecido salvo; o período desligado não é creditado e o app explica a recuperação.
5. **Given** notificações bloqueadas, **When** inicio uma sessão, **Then** o app explica a limitação, continua
   oferecendo o controle no app e não alega que a notificação foi exibida.

---

### User Story 4 - Comprar e usar uma recompensa temporizada (Priority: P2)

Como usuário, compro uma recompensa com duração (ex.: 2 horas de jogo); a compra cobra moedas e inicia a
contagem imediatamente; posso pausar e retomar sem nova cobrança e, ao acabar, apenas recebo um aviso.
**Why this priority**: estende recompensas existentes; depende do cronômetro (US1/US3).
**Independent Test**: comprar com outra sessão ativa, cancelar; repetir confirmando; pausar, retomar, esgotar.

**Acceptance Scenarios**:
1. **Given** uma recompensa de 2 horas e saldo suficiente, **When** confirmo a compra, **Then** o custo é debitado
   uma vez, a compra é registrada como uso imediato e a contagem regressiva de 2 horas começa.
2. **Given** outro cronômetro contando, **When** escolho comprar, **Then** antes de cobrar o app pede confirmação
   para pausá-lo; cancelar não cobra, não registra compra e não pausa a sessão original.
3. **Given** uma confirmação repetida/retentativa ou falha entre cobrança e exibição, **When** o app se recupera,
   **Then** existe uma única compra, um único débito e uma única sessão ligada ao recibo.
4. **Given** uma sessão de recompensa pausada, **When** retomo, **Then** não há nova cobrança, devolução nem consumo
   adicional do limite por período; uma nova compra consome o limite normalmente.
5. **Given** o tempo comprado esgotado, **When** a contagem chega a zero, **Then** pausa no total comprado e
   alerta, sem bloquear apps, penalizar, comprar mais tempo ou reembolsar.
6. **Given** uma recompensa sem duração, **When** compro, **Then** o comportamento do ponto 6 é mantido.

---

### User Story 5 - Adicionar tempo esquecido e corrigir sessões (Priority: P2)

Como usuário, adiciono um intervalo esquecido ou corrijo uma sessão errada, com histórico de alterações e
efeitos coerentes no progresso e nas conclusões.
**Why this priority**: tempo real nem sempre é cronometrado; erros precisam de correção auditável.
**Independent Test**: adicionar ontem 20:00–20:30, corrigir para 20:00–20:10 e consultar o histórico.

**Acceptance Scenarios**:
1. **Given** um intervalo passado válido, **When** adiciono tempo manual, **Then** ele conta para a data/semana em que
   a atividade ocorreu e o histórico guarda o intervalo real e o momento da edição separadamente.
2. **Given** intervalo negativo, futuro, antes da criação da origem, em período inativo (hábito pausado/excluído)
   ou sobreposto a outro tempo creditado da mesma origem, **When** salvo, **Then** é rejeitado com explicação.
3. **Given** uma correção que reduz o total abaixo de um objetivo já concluído, **When** confirmo, **Then** o app
   mostra a consequência e aplica a semântica de correção existente (desfazer/reabrir), sem apagar histórico
   financeiro nem emitir reversões duplicadas; nova correção para cima não concede prêmio automaticamente.
4. **Given** uma sessão de recompensa, **When** corrijo seu registro, **Then** a compra não é revertida e não se
   cria tempo pago além do comprado.

### Edge Cases

- Mudança manual do relógio ou fuso durante a contagem não cria nem apaga tempo decorrido.
- Horário de verão: a alocação civil usa o fuso do aparelho no instante real de cada trecho.
- Sessão de origem que foi concluída, excluída, marcada como não feita ou pausada (hábito): a contagem pausa
  e o tempo salvo permanece no histórico.
- Origem excluída durante sessão pausada: a sessão fica somente leitura no histórico.
- Parada forçada pelo usuário ou restrição do fabricante interrompe a contagem; ao reabrir, o app só credita
  até o último ponto salvo conhecido e explica o limite.
- Objetivo já atingido ao iniciar: o app oferece confirmar a conclusão em vez de contar além do objetivo.
- Tarefa com meta de tempo mas concluída manualmente sem cronômetro: conclusão normal, sem exigir tempo.

## Requirements *(mandatory)*

### Escopo e restrições do Ethos

Tudo é local e offline; sem login, backend, telemetria ou sincronização. Incluído: cronômetro crescente e
regressivo; metas de duração para tarefa (por ocorrência) e hábito de prática (por dia ou semana civil
segunda–domingo); sessões, tempo manual e correções auditadas; notificação de sessão; recompensas com
duração. Excluído: metas mensais de duração, Pomodoro, pausas programadas, coaching, pontuação de foco,
detecção automática de atividade, monitoramento de uso de apps, bloqueio de apps, cronômetro avulso sem
origem, novas taxas de moeda por minuto, penalidades por tempo e inventário de tempo comprado não iniciado.
Permissões: reutilizar a de notificação já existente (pedida em contexto) e somente as declarações de
serviço exigidas pela plataforma para contagem visível em segundo plano. Dados pessoais (nomes de origem,
intervalos) não entram em logs. Interface em português, temas claro/escuro/sistema, controles acessíveis
com descrição, fonte ampliada e estados de carregamento/erro recuperável.

### Functional Requirements

- **FR-001**: Permitir iniciar, pausar e retomar uma sessão ligada a ocorrência de tarefa, hábito de prática ou
  compra de recompensa temporizada, com visão crescente e, quando houver objetivo, regressiva.
- **FR-002**: Garantir no máximo uma sessão contando no app; trocar exige confirmação e pausa a anterior sem descartar.
- **FR-003**: Derivar tempo decorrido de trechos persistidos e de fonte de tempo robusta, não de redesenho da UI;
  pausas, tempo antes do início e intervalos não contados nunca são creditados.
- **FR-004**: Permitir meta de duração opcional por tarefa e por hábito de prática (dia ou semana), sem
  reinterpretar metas existentes de dias distintos nem alterar resultados históricos.
- **FR-005**: Somar sessões e tempo manual por origem e período; alocar cada trecho à data e à semana civil em
  que ocorreu, dividindo em meia-noite e na virada de semana.
- **FR-006**: Ao atingir o objetivo, pausar exatamente no objetivo, alertar e pedir confirmação; a confirmação usa
  fluxos existentes de conclusão (tarefa com checklist/prévia financeira; hábito por registro de prática).
- **FR-007**: Nunca escrever moedas/XP a partir da contagem; conclusões repetidas não multiplicam direitos.
- **FR-008**: Manter contagem com tela bloqueada/segundo plano via notificação de sessão com origem, tempo,
  Pausar e Abrir; pausar pela notificação altera a mesma sessão persistida.
- **FR-009**: Preservar tempo em recriação da Activity e morte de processo; após reinício do aparelho, recuperar o
  último tempo salvo como pausado sem creditar o período desligado, e explicar a recuperação.
- **FR-010**: Mudanças de relógio/fuso/horário de verão não criam nem apagam trabalho decorrido.
- **FR-011**: Permitir adicionar tempo manual e corrigir/excluir trechos com auditoria (intervalo real, momento da
  edição, antes/depois); rejeitar intervalos inválidos, futuros, fora do período ativo ou sobrepostos.
- **FR-012**: Correções que alterem o atingimento de um objetivo confirmado mostram consequência e usam a
  semântica de correção existente, com vínculo estável e sem reversões duplicadas.
- **FR-013**: Permitir duração opcional em recompensas; compra temporizada debita, registra uso imediato e inicia
  a sessão na mesma operação idempotente vinculada ao recibo.
- **FR-014**: Se outra sessão estiver contando, pedir confirmação de pausa antes de cobrar; cancelar não cobra.
- **FR-015**: Sessão de recompensa pausa/retoma sem nova cobrança, devolução ou consumo de limite; ao esgotar,
  pausa no total comprado e apenas alerta.
- **FR-016**: Informar limitações de permissão/canal/dispositivo honestamente; o controle no app sempre funciona.
- **FR-017**: Mostrar controles de sessão em detalhes de tarefa/hábito, em Hoje e em recompensas, com acesso à
  sessão ativa/pausada, totais por período e histórico de sessões/alterações, sem nova destinação principal.
- **FR-018**: Persistir em armazenamento local com migração preservadora; não popular produção com sessões fictícias.
- **FR-019**: Verificar com relógio controlado, migração, consistência, UI, serviço/notificação e recuperação,
  relatando cobertura de SDK/dispositivo não executada.

### Key Entities

- **Timer session**: origem (tarefa/ocorrência, hábito, compra), modo, objetivo opcional, estado
  (contando/pausada/finalizada), identidade estável, tempo total salvo.
- **Session segment**: trecho real de atividade (início/fim em instante), origem manual ou cronometrada, ativo ou
  substituído por correção.
- **Duration goal**: objetivo opcional em minutos por ocorrência de tarefa ou por período (dia/semana) de hábito.
- **Time audit entry**: ação (iniciar, pausar, recuperar, adicionar, corrigir, excluir, confirmar), momento,
  intervalo real e antes/depois.
- **Timed reward**: duração opcional em recompensa e no instantâneo da compra; sessão vinculada ao recibo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Em 100% dos cenários de pausa/retomada, o tempo creditado difere do tempo real contado em menos de 1 s
  e o intervalo pausado contribui 0 s.
- **SC-002**: 40 + 50 + 90 minutos totalizam exatamente 3 h; domingo 23:50 → segunda 00:20 divide 10/20 minutos.
- **SC-003**: Repetir confirmação, alerta ou recuperação 3 vezes concede no máximo um direito por ocorrência/período.
- **SC-004**: Após reinício simulado, o tempo salvo é preservado e 0 s do período desligado é creditado.
- **SC-005**: Repetir 3 vezes a confirmação de uma compra temporizada gera 1 débito, 1 compra e 1 sessão.
- **SC-006**: Cancelar a transição de compra deixa saldo, compras e a sessão original 100% inalterados.
- **SC-007**: Migração preserva 100% de tarefas, hábitos, razão financeira, recompensas, compras, lembretes e temas.
- **SC-008**: Com notificações negadas, 100% dos controles de sessão continuam disponíveis no app.

## Assumptions

- Fonte: prompt salvo e `TODO/planejamento_ponto_8.md` (dez decisões aprovadas); sem transcript acessível.
- Estado verificado em 2026-11-07: features 001–007 implementadas; Room schema 5; manifesto já tem permissão de
  notificação, receptores de alarme/recuperação e canais de lembrete; nenhum serviço ou código de cronômetro.
- Suposição de integração mínima (não aprovada separadamente, a validar no plano): metas de duração de hábito
  aplicam-se a hábitos de prática com período dia ou semana; a confirmação registra uma prática na data de
  atingimento, usando a regra e a contabilidade existentes sem nova taxa.
- Suposição: sessões existem apenas ligadas a uma origem (tarefa, hábito de prática ou compra), pois cronômetro
  avulso não foi pedido.
- Precisão, pontos de salvamento, mecanismo de serviço e política de sobreposição serão definidos no plano
  conforme documentação oficial Android atual.
