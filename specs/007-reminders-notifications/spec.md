# Feature Specification: Lembretes e notificações locais

**Feature Branch**: existing working branch; no branch creation requested
**Created**: 2026-11-07
**Status**: Ready for planning
**Input**: `TODO/specify_lembretes_notificacoes_app.md`; current user authorizes specify → plan → tasks → implement.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Configurar lembretes opcionais (Priority: P1)

Como usuário, escolho vários lembretes por tarefa ou hábito sem mudar os prazos ou as metas.
**Why this priority**: configura o valor central sem alertas não solicitados.
**Independent Test**: salvar, reabrir, editar e remover dois lembretes de cada tipo.

**Acceptance Scenarios**:
1. **Given** uma tarefa sem lembretes, **When** configuro horário escolhido ou antecedência,
   **Then** os lembretes persistem e nenhum prazo é criado ou alterado implicitamente.
2. **Given** uma tarefa semanal sem dia fixo, **When** escolho dias e horários,
   **Then** os avisos pertencem ao período correto sem impor dia de conclusão.
3. **Given** uma série, **When** altero lembretes desta ocorrência ou desta e futuras,
   **Then** a configuração respeita o escopo, âncoras, limite e fim da série existentes.
4. **Given** um hábito flexível, **When** escolho dias e horários de aviso,
   **Then** não são criadas obrigações diárias nem registros de prática.

### User Story 2 - Agir com segurança e adiar (Priority: P1)

Como usuário, abro o item, adio seu aviso ou concluo uma tarefa preservando as confirmações existentes.
**Why this priority**: evita alterações incorretas de dados e cobranças duplicadas.
**Independent Test**: concluir tarefa com checklist incompleto e adiar um hábito sem gerar registro.

**Acceptance Scenarios**:
1. **Given** uma notificação de tarefa, **When** uso Concluir, **Then** entro no fluxo existente,
   com confirmação de itens pendentes e consequências financeiras quando aplicáveis.
2. **Given** um hábito, **When** recebo aviso, **Then** somente Abrir e Adiar estão disponíveis.
3. **Given** um aviso válido, **When** adio por 10 minutos, 30 minutos, 1 hora ou horário futuro,
   **Then** substituo o adiamento anterior sem mudar prazo, meta, moeda ou XP.
4. **Given** notificação antiga ou ação repetida, **When** acionada, **Then** o estado é revalidado,
   sem conclusão dupla, resultado falso ou erro não tratado; item removido tem retorno honesto.

### User Story 3 - Receber apenas avisos relevantes (Priority: P1)

Como usuário, não recebo avisos obsoletos e entendo quando o dispositivo impede a entrega.
**Why this priority**: confiança e controle sobre interrupções.
**Independent Test**: concluir/pausar a origem, revogar permissão e recuperar após reinício.

**Acceptance Scenarios**:
1. **Given** tarefa concluída, excluída ou não será feita, **When** reconciliada,
   **Then** seus avisos pendentes e adiados são cancelados sem parar outras ocorrências.
2. **Given** hábito pausado/excluído ou prática com meta atingida, **When** reconciliado,
   **Then** não recebe avisos nesse intervalo/período; futuras práticas elegíveis retomam.
3. **Given** hábito de evitar/limitar acima do limite, **When** chega o horário,
   **Then** pode continuar recebendo avisos para abrir seus detalhes reais.
4. **Given** indisponibilidade ou permissão negada, **When** reabro ou recupero o app,
   **Then** avisos perdidos ainda relevantes são consolidados uma vez, sem rajada individual.
5. **Given** mudança de data, fuso, relógio, reinício ou atualização, **When** reconciliada,
   **Then** futuros avisos são recalculados sem alterar datas civis, histórico ou contabilidade.
6. **Given** alertas bloqueados, **When** configuro lembretes, **Then** vejo estado honesto e caminho
   para configurações; tarefas e hábitos seguem utilizáveis e nenhuma entrega é alegada.

### User Story 4 - Resumo diário escolhido (Priority: P2)

Como usuário, habilito opcionalmente um resumo de Hoje e pendências de revisão no horário escolhido.
**Why this priority**: oferece visão compacta sem configurar alertas por item.
**Independent Test**: habilitar, mudar horário, conferir contagens reais e desabilitar.

**Acceptance Scenarios**:
1. **Given** instalação existente, **When** abro ajustes, **Then** resumo começa desabilitado.
2. **Given** resumo habilitado com horário, **When** entregue, **Then** mostra contagens reais de Hoje
   e revisão e abre o app; dia vazio tem apresentação compacta sem dados fictícios.
3. **Given** resumo alterado/desabilitado, **When** reabro, **Then** preferência persiste e aviso antigo
   é substituído/cancelado, sem alterar dados de domínio.

### Edge Cases

- Horário passado é rejeitado; horário civil ausente/ambíguo tem política técnica determinística.
- Remoção de lista, edição de recorrência, restauração e correção de prática reconciliam os avisos.
- Morte de processo entre persistência e agendamento não duplica avisos após reconciliação.
- Canal bloqueado, permissão revogada, parada forçada e restrições de fabricante limitam entrega.
- Dispensar aviso não remove definição, não gera falha e não muda saldo.

## Requirements *(mandatory)*

### Escopo e restrições do Ethos

Tudo é local e offline. Excluem-se cronômetros/sessões, backend, login, push, telemetria,
novas regras financeiras, sons próprios, quiet hours, tela cheia e bypass de restrições.
Preservar a interface atual, temas e dados existentes. Textos em português, controles acessíveis
com fonte ampliada, estado de carregamento/erro recuperável e privacidade na tela bloqueada.
Permissões são contextuais; dados pessoais não entram em logs.

### Functional Requirements

- **FR-001**: Permitir criar, editar e remover múltiplos lembretes opcionais por item.
- **FR-002**: Tarefas aceitam horário explícito, antecedência com referência explícita e agenda semanal
  quando não têm dia fixo; nenhuma configuração muda o prazo implicitamente.
- **FR-003**: Hábitos aceitam horários/dias independentes da meta; séries respeitam escopo e ocorrência.
- **FR-004**: Persistir definições, adiamentos e estado suficiente para recuperação idempotente.
- **FR-005**: Entregar somente após revalidar origem/período, com ações apropriadas ao tipo.
- **FR-006**: Concluir reutiliza fluxo de tarefa/checklist/contabilidade; Abrir aponta ao item ou fallback.
- **FR-007**: Adiar oferece 10/30/60 minutos ou futuro escolhido, substituindo o aviso da mesma ocorrência.
- **FR-008**: Reconciliar alterações de origem, listas, série, conclusão, status, prática e preferências,
  cancelando somente avisos afetados; correções retomam apenas horários futuros relevantes.
- **FR-009**: Resumo diário inicia desligado, exige horário escolhido e usa Hoje/revisão reais.
- **FR-010**: Recuperação consolida avisos perdidos relevantes sem repetir backlog nem cobrar ações.
- **FR-011**: Recuperar após reinício, atualização e mudanças de tempo preservando regras civis existentes.
- **FR-012**: Informar capacidade/bloqueios, solicitar acesso somente em contexto e respeitar canais.
- **FR-013**: Ações repetidas/antigas e componentes externos não podem provocar mutações indevidas.
- **FR-014**: Preservar histórico e dados; falha de gravação não pode parecer sucesso.
- **FR-015**: Verificar regras com tempo controlado, migração, ações/UI, entrega real disponível e regressões;
  registrar limites do ambiente e distinguir compilação de entrega observada.

### Key Entities

- **Reminder definition**: origem, escopo, modo, referência, antecedência, horário e dias escolhidos.
- **Reminder delivery**: identidade da definição/ocorrência/período, instante, adiamento e estado.
- **Summary preference**: habilitação e horário civil escolhido.
- **Recovery batch**: conjunto consolidado de avisos perdidos elegíveis já apresentado.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Criar dois lembretes, editar um e remover outro preserva 100% dos prazos/metas existentes.
- **SC-002**: Os quatro modos de adiar substituem o aviso anterior sem nenhuma alteração financeira.
- **SC-003**: Em todos os cenários de origem resolvida/inativa, zero avisos obsoletos são apresentados.
- **SC-004**: Repetir três vezes a recuperação do mesmo backlog produz no máximo um resumo consolidado.
- **SC-005**: Todas as ações de concluir preservam checklist e confirmação financeira, sem prêmios duplicados.
- **SC-006**: Resumo confere exatamente com Hoje/revisão nos cenários vazio, tarefas, hábitos e misto.
- **SC-007**: Negar acesso mantém 100% das operações centrais disponíveis com bloqueio explicado.

## Assumptions

- Fonte: prompt e decisões aprovadas em `TODO/planejamento_ponto_7.md`; não há transcript acessível.
- Recompensas estão implementadas em 006; preservar schemas 1–4 e os fluxos reais existentes.
- Entrega é sujeita ao sistema/dispositivo; o plano documentará precisão e políticas de tempo sem prometer
  garantias impossíveis. Sem nova questão de produto pendente.
