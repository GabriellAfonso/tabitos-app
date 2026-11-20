# Aviso de atualização do app

**Feature 039 · 20/11/2026**
**Status:** Draft
Origem: [prompt de specify](../../TODO/specify_atualizacao_app.md). Decisões tomadas em conversa no Claude Code em
2026-11-20.

## User Scenarios & Testing

### US1 · Saber que há versão nova ao abrir o app (P1)

Quem abre o app e tem uma versão mais nova disponível na loja vê o pedido de atualização da própria loja depois que
o Hoje aparece. Aceitar inicia o download em segundo plano, e o app continua usável. Recusar não bloqueia nem fecha
nada.

**Teste independente:** publicar uma versão maior no teste interno da loja, abrir a versão anterior e aceitar o
pedido. O Hoje continua utilizável durante o download.

1. **Dado** que há versão nova, **quando** o app abre, **então** o pedido aparece depois que o Hoje está desenhado.
2. **Dado** o pedido aberto, **quando** a pessoa recusa, **então** o app segue normal e não pergunta de novo nesta
   abertura.
3. **Dado** que não há versão nova ou a loja está indisponível, **quando** o app abre, **então** nada aparece.

### US2 · Instalar com um toque quando o download termina (P1)

Ao terminar o download, aparece uma faixa fixa "Atualização pronta" com a ação "Reiniciar". Tocar instala a nova
versão e reabre o app. "Agora não" fecha a faixa até a próxima abertura.

**Teste independente:** com um download concluído, tocar em "Reiniciar". O app reabre na versão nova com a rotina
preservada.

1. **Dado** um download concluído, **então** a faixa aparece sobre qualquer página, sem cobrir a navegação principal.
2. **Dado** a faixa, **quando** a pessoa toca "Agora não", **então** ela some até a próxima abertura.
3. **Dado** um download concluído numa abertura anterior e não instalado, **quando** o app abre, **então** a faixa
   aparece direto, sem novo pedido nem novo download.

### US3 · Não perder o cronômetro ao atualizar (P2)

A instalação reinicia o app. Se o cronômetro estiver contando, a instalação pausaria a contagem. Por isso, enquanto
houver cronômetro contando, a faixa diz "Atualização pronta · Disponível quando o cronômetro parar" e não oferece
"Reiniciar". Quando o cronômetro pausa ou para, "Reiniciar" volta.

1. **Dado** um download concluído e um cronômetro contando, **então** a faixa não oferece "Reiniciar".
2. **Dado** essa situação, **quando** o cronômetro pausa ou é concluído, **então** "Reiniciar" aparece sem precisar
   reabrir o app.

### Edge Cases

- App instalado fora da loja, build debug ou aparelho sem os serviços da loja: a checagem falha em silêncio.
- Sem internet: nenhuma faixa nem erro. A próxima abertura tenta de novo.
- Download falha ou é cancelado: nada é mostrado. A próxima abertura volta a oferecer.
- Página recriada (rotação, tema): a faixa e o estado do download continuam iguais.
- Voltar ao app (do segundo plano para o primeiro) não conta como nova abertura. Só uma abertura nova do processo
  pergunta de novo.
- Cronômetro de recompensa ou de tarefa/hábito contando: os dois tipos bloqueiam "Reiniciar" do mesmo jeito.

## Requirements

### Escopo e restrições

- **Escopo:** pedido flexível da loja, faixa de pronto, bloqueio por cronômetro e frequência por abertura.
- **Fora do escopo:** atualização forçada ou imediata (inclusive para versões de alta prioridade), versão mínima,
  configuração remota, tela de novidades, linha de versão ou "Procurar atualização" em Configurações, distribuição
  fora da loja.
- **Offline:** as funções do app não dependem disso. Sem conexão, nada aparece.
- **Dados:** nada é gravado em Room nem em DataStore. Rotina, schema e preferências ficam inalterados.
- **Permissões:** nenhuma nova. **Dados pessoais:** N/A. Os logs não guardam conteúdo do usuário.
- **Sem** backend, login ou sincronização.

### Functional Requirements

- **FR-001.** Em cada abertura nova do app, depois que o Hoje estiver pronto, consultar a loja. Se houver versão
  nova e a atualização flexível for permitida, mostrar o pedido de atualização da loja.
- **FR-002.** Nunca bloquear o uso nem encerrar o app por recusa, falha ou versão desatualizada.
- **FR-003.** Durante o download, o app continua inteiramente usável. Não é obrigatório mostrar o progresso.
- **FR-004.** Com o download concluído, mostrar a faixa "Atualização pronta" com "Reiniciar" e "Agora não". A faixa
  usa as cores do tema (claro/escuro), fica fixa até uma ação e é lida pelo TalkBack. Os botões têm alvo de toque de
  48 dp e a faixa aceita fonte ampliada.
- **FR-005.** "Reiniciar" instala a atualização baixada.
- **FR-006.** Enquanto houver cronômetro contando, a faixa mostra "Disponível quando o cronômetro parar" no lugar
  de "Reiniciar". Ela reage sozinha quando o cronômetro pausa ou para.
- **FR-007.** Recusar o pedido ou tocar "Agora não" suprime novos pedidos e a faixa só nesta abertura, sem gravar
  nada. A próxima abertura oferece de novo.
- **FR-008.** Um download concluído e ainda não instalado leva direto à faixa na abertura seguinte.
- **FR-009.** Falhas ou indisponibilidade da loja não mostram erro nem atrasam o Hoje.
- **FR-010.** Os textos ficam em português, em recursos.

### Key Entities

- **Estado da atualização:** nenhum / baixando / pronta / pronta com cronômetro contando / dispensada nesta abertura.
  Existe só em memória.

## Success Criteria

- **SC-001.** O tempo até o Hoje aparecer não piora de forma mensurável com a checagem (medição "Fully drawn",
  spec027).
- **SC-002.** Com versão nova, no máximo um pedido por abertura. Zero pedidos quando não há versão nova.
- **SC-003.** Com download concluído, um toque basta para instalar e voltar ao app na versão nova, com a rotina
  intacta.
- **SC-004.** Zero cronômetros pausados por instalação iniciada pela faixa.
- **SC-005.** Zero fechamentos ou bloqueios do app causados por esta feature.

## Assumptions

- O app é distribuído pela loja do Google. A validação real só é possível pelo teste interno da loja. Nos testes
  locais, usa-se um falso.
- O pedido de consentimento e a instalação são telas do próprio Android/loja. O app controla apenas a faixa.
- A loja pode instalar sozinha um download concluído quando o app está em segundo plano. Isso é aceitável.
- A faixa fica sobre a página atual, acima da área inferior, no mesmo lugar das mensagens existentes.
- Instalar reinicia o processo, e um cronômetro pausado é preservado como já acontece (spec008).
