# Cores primárias e cor personalizada

**Feature Branch**: `038-custom-item-color`

**Created**: 2026-11-20

**Status**: Draft

**Input**: User description: "da pra criarmos mais uma opção de cor (nos lugares que tem como pôr) pra abrir um popup de
escolha de cor com aquele círculo que tem todas as cores e opção pra pessoa colocar hexadecimal ou rgb?"
Decisões combinadas na conversa e no protótipo `prototypes/seletor-cor/index.html` (rodada 3, opção B escolhida),
registradas em `TODO/seletor_cor.md`.

## Contexto verificado

- Listas de tarefas e hábitos (bons e maus) têm uma cor gravada como número: a posição numa paleta fixa de 7 cores
  (Sálvia, Azul, Roxo, Laranja, Rosa, Cinza, Vermelho). As posições nunca foram reordenadas.
- A cor é escolhida em dois lugares: diálogo de criar/editar lista e folha Aparência do editor de hábito. Recompensas
  não têm cor.
- A cor aparece no ícone da lista no menu ☰, no título da página da lista, nos detalhes de tarefa e hábito, no
  cartão e nos detalhes do hábito, no calendário, nas estatísticas e na página de acompanhamento.
- O ícone sobre o fundo colorido é sempre branco; funciona porque as 7 cores são escuras.
- Mau hábito começa na cor Vermelho.
- As regras de lista e hábito recusam cor fora das 7 posições.
- O backup portátil (spec 037) copia os dados como estão, sem tratar a cor.
- O usuário já usa o app com a rotina real.

## User Scenarios & Testing *(mandatory)*

### US1 · Cores do app mais primárias, numa linha (P1)

As cores atuais parecem aleatórias. Quero cores primárias, e a linha de cores não pode quebrar quando entrar a opção
personalizada.

**Why this priority**: Muda o que todos os itens já mostram e prepara a linha para a cor personalizada.

**Independent Test**: Abrir o editor de uma lista e de um hábito existentes e conferir a linha e as cores no menu ☰.

**Acceptance Scenarios**:

1. **Given** o editor de lista ou a Aparência do hábito, **When** abro a escolha de cor, **Then** vejo numa linha só,
   em ordem, Vermelho, Laranja, Verde, Azul, Roxo e o círculo Personalizada, sem quebra em tela de 360 dp de largura
   com fonte padrão.
2. **Given** uma lista gravada em Sálvia, Azul, Roxo, Laranja ou Vermelho, **When** abro o app, **Then** ela aparece
   em Verde, Azul, Roxo, Laranja ou Vermelho novos, respectivamente, sem nenhum dado ser regravado.
3. **Given** um item gravado em Rosa ou Cinza, **When** o vejo em qualquer tela, **Then** ele mantém a cor antiga;
   **When** abro o editor, **Then** o círculo Personalizada aparece marcado com essa cor.
4. **Given** "Quebrar mau hábito", **When** o editor abre, **Then** a cor inicial é o Vermelho.

---

### US2 · Escolher uma cor personalizada (P1)

Quero escolher qualquer cor arrastando numa roda de cores ou digitando o código Hex ou RGB.

**Why this priority**: É o pedido principal.

**Independent Test**: Criar uma lista com a cor #1E7F86 digitada, salvar, reabrir o app e conferir a cor em todas as
telas onde a lista aparece.

**Acceptance Scenarios**:

1. **Given** a linha de cores, **When** toco em Personalizada, **Then** abre a folha "Cor personalizada" com prévia,
   roda de cores, controle de brilho, campos Hex | RGB, "Partir de" e os botões Cancelar e Usar cor.
2. **Given** a folha aberta, **When** arrasto ou toco na roda, **Then** o ângulo escolhe o tom, a distância do centro
   escolhe a intensidade e a prévia, o Hex e o RGB mudam juntos.
3. **Given** a folha aberta, **When** mudo o brilho, **Then** a cor escurece ou clareia e os campos acompanham.
4. **Given** o modo Hex, **When** digito 6 dígitos hexadecimais (com ou sem "#") ou 3 dígitos curtos, **Then** a
   roda e a prévia vão para essa cor. **When** o texto é inválido, **Then** o campo fica marcado como erro e Usar cor
   fica desativado até corrigir.
5. **Given** o modo RGB, **When** digito R, G e B de 0 a 255, **Then** a cor muda; valor fora disso ou vazio marca
   erro e desativa Usar cor.
6. **Given** "Partir de", **When** toco numa das 5 cores do app, **Then** a roda e os campos vão para essa cor.
7. **Given** uma cor escolhida, **When** toco Usar cor, **Then** a folha fecha, o círculo Personalizada mostra a cor
   com um lápis e o item usa essa cor ao salvar o editor. **When** toco Cancelar ou fora da folha, **Then** nada muda.
8. **Given** um item com cor personalizada, **When** toco numa das 5 cores do app, **Then** a personalizada é
   descartada; **When** toco de novo em Personalizada, **Then** a folha abre já na cor atual do item.

---

### US3 · Ver a cor nos dois temas (P2)

Uma cor muito clara ou muito escura pode sumir no tema claro ou no escuro, já que a cor é a mesma nos dois.

**Why this priority**: Ajuda a escolher uma cor legível; não bloqueia o uso.

**Independent Test**: Escolher #F6D743 e #1B1B3A e conferir a prévia e o ícone do cartão.

**Acceptance Scenarios**:

1. **Given** a folha aberta, **When** a cor muda, **Then** a prévia mostra o ícone do item sobre a cor e a cor como
   tom de ícone sobre o fundo do tema claro e do tema escuro.
2. **Given** qualquer cor (do app ou personalizada), **When** o ícone é desenhado sobre ela, **Then** o ícone é claro
   ou escuro, o que tiver mais contraste com a cor.

### Edge Cases

- Cor personalizada idêntica a uma das 5 do app: fica como personalizada; não vira a posição da paleta.
- Cor preta (#000000) ou branca (#FFFFFF): permitidas; o ícone escolhe claro ou escuro.
- Na roda, brilho zero: todos os pontos são preto; mudar o brilho de novo recupera tom e intensidade anteriores.
- Hex com letras minúsculas é aceito e exibido em maiúsculas.
- Girar a tela ou fonte em 200% com a folha aberta: a folha rola; a cor em edição não se perde ao rolar.
- Backup de um app sem esta versão restaurado nesta versão: as cores antigas valem como descrito em US1. Backup desta
  versão com cor personalizada restaurado numa versão antiga não é suportado (a regra antiga recusa a cor).
- Editor salvo sem tocar na cor: a cor gravada não muda (inclusive Rosa/Cinza).

## Requirements *(mandatory)*

### Escopo e restrições do Tabitos

- Escopo: paleta do app, linha de cores, folha "Cor personalizada" e desenho da cor nas telas existentes, para listas
  e hábitos.
- Exclusões: cor em recompensas, tarefas ou acompanhamentos; cores recentes ou favoritas; cores diferentes por tema;
  ajuste automático de cores com pouco contraste; nomes aproximados de cor.
- Offline e persistência: a cor continua no mesmo dado local já gravado; não há mudança de estrutura de dados nem
  migração; falha ao salvar segue o tratamento de erro atual do editor.
- Permissões: N/A. Dados pessoais: a cor faz parte da rotina do usuário e não vai para logs.
- Acessibilidade: ver FR-012 a FR-014.

### Functional Requirements

- **FR-001**: A paleta do app MUST passar a desenhar as posições existentes assim: Sálvia → Verde #2E8B3E, Azul →
  Azul #1F6FD6, Roxo → Roxo #9254DE, Laranja → Laranja #DB6400, Vermelho → Vermelho #D93025. Rosa (#A04D68) e Cinza
  (#657078) mantêm o tom antigo.
- **FR-002**: A linha de cores MUST mostrar, nesta ordem, Vermelho, Laranja, Verde, Azul, Roxo e Personalizada, numa
  única linha nos editores de lista e de hábito (bom e mau).
- **FR-003**: Rosa e Cinza MUST continuar válidas para itens que já as usam e não aparecer na linha; nesses itens o
  círculo Personalizada aparece marcado com a cor do item.
- **FR-004**: Um item MUST guardar exatamente uma cor: uma posição da paleta ou uma cor personalizada opaca (RGB).
- **FR-005**: A gravação MUST ser compatível com os dados já gravados: posições existentes continuam significando a
  mesma entrada da paleta, sem regravar itens.
- **FR-006**: As regras de lista e hábito MUST aceitar posições da paleta e cores personalizadas opacas e recusar
  qualquer outro valor.
- **FR-007**: A folha "Cor personalizada" MUST oferecer roda de cores (ângulo = tom, distância do centro =
  intensidade), controle de brilho, campos Hex ou RGB alternados por botão segmentado, "Partir de" com as 5 cores da
  linha, prévia nos dois temas, Cancelar e Usar cor.
- **FR-008**: Roda, brilho, Hex e RGB MUST ficar sincronizados; entrada inválida MUST marcar o campo e desativar Usar
  cor sem alterar a última cor válida.
- **FR-009**: A folha MUST abrir na cor atual do item (personalizada ou da paleta).
- **FR-010**: Escolher uma cor da linha MUST descartar a cor personalizada do item; Cancelar MUST manter a cor anterior.
- **FR-011**: A cor MUST ser a mesma nos temas claro e escuro; o ícone sobre a cor MUST ser claro ou escuro pelo maior
  contraste, em todas as telas que desenham o ícone sobre a cor.
- **FR-012**: O círculo Personalizada MUST ser anunciado como "Escolher cor personalizada" sem cor e como "Cor
  personalizada, #RRGGBB" com cor; os círculos da paleta mantêm nome falado e estado selecionado.
- **FR-013**: A roda MUST ter alternativa acessível: os campos Hex/RGB e "Partir de" permitem escolher qualquer cor
  sem arrastar; o controle de brilho é ajustável pelo leitor de tela.
- **FR-014**: A folha MUST rolar com fontes ampliadas e manter alvos de toque de pelo menos 48 dp.
- **FR-015**: A folha não mostra aviso de contraste (removido a pedido do usuário em 2026-11-20); a prévia nos dois
  temas basta.
- **FR-016**: Mau hábito MUST continuar começando em Vermelho.

### Key Entities

- **Cor do item**: ou uma entrada da paleta do app (com nome falado e tom fixo) ou uma cor personalizada RGB opaca.
  Pertence a uma lista ou a um hábito.
- **Paleta do app**: 7 entradas em posições fixas (5 visíveis na linha, 2 só para itens antigos).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Em tela de 360 dp com fonte padrão, a linha de cores ocupa uma linha só nos dois editores.
- **SC-002**: Uma cor digitada em Hex é aplicada ao item com no máximo 3 toques depois de abrir o editor (Personalizada,
  digitar, Usar cor) e salvar.
- **SC-003**: 100% das listas e hábitos existentes continuam abrindo após a atualização; nenhum dado é regravado.
- **SC-004**: Toda cor personalizada salva reaparece idêntica (mesmo #RRGGBB) depois de fechar e abrir o app e após
  backup e restauração na mesma versão.
- **SC-005**: A prévia mostra a cor sobre os fundos claro e escuro para 100% das cores escolhidas.

## Assumptions

- Laranja foi escolhido no lugar de Amarelo como quinta cor (Amarelo tem contraste 1,7:1 no tema claro).
- Cor com pouco contraste não gera aviso nem ajuste automático; a prévia mostra os dois temas.
- O leitor de tela anuncia o código hex da cor personalizada, não um nome aproximado.
- A roda é desenhada pelo próprio app, sem biblioteca nova.
- A prévia usa os fundos principais dos temas claro e escuro do app.
