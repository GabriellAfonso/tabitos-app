# Decisões técnicas da feature 038

## Gravação da cor personalizada

- Decision: mesmo campo `Int`; 0–6 = paleta, ARGB com alfa `0xFF` (negativo) = personalizada.
- Rationale: sem migração nem mudança do backup; os valores nunca colidem (posições ≥ 0, personalizadas < 0).
- Alternatives considered: nova coluna `customColor` (exigiria migração 7→8 e mudança de backup sem ganho);
  gravar tudo como ARGB (regravaria todos os itens e perderia o vínculo com a paleta, que agora muda de tom).

## Paleta nova no lugar das posições

- Decision: posições 0, 1, 2, 3 e 6 passam a desenhar Verde, Azul, Roxo, Laranja e Vermelho primários; 4 e 5
  mantêm Rosa e Cinza e saem da linha.
- Rationale: o usuário aceitou que seus itens mudem de tom; manter as posições evita regravar dados.
- Alternatives considered: acrescentar 5 posições novas (itens antigos ficariam todos como "personalizada").

## Roda de cores

- Decision: Canvas com `Brush.sweepGradient` de tons + `Brush.radialGradient` de branco para transparente,
  escurecido pelo brilho; toque e arraste por `pointerInput`; brilho em `Slider` Material 3.
- Rationale: sem dependência; `Slider` já é acessível pelo TalkBack.
- Alternatives considered: biblioteca de color picker (dependência nova sem necessidade); anel + quadrado (opção C,
  rejeitada no protótipo).

## Contraste

- Decision: razão de contraste WCAG (luminância relativa). Ícone claro (#FFFFFF) ou escuro (#1E1A23) pelo maior
  contraste. Aviso de contraste < 3:1 removido a pedido do usuário (2026-11-20); a folha só mostra a prévia nos
  fundos claro (#FBF8FC) e escuro (#171419).

## Estado da folha

- Decision: estado efêmero (`h, s, v`, modo Hex/RGB e textos digitados) guardado por `rememberSaveable` na folha;
  transições em funções puras; só "Usar cor" chama `onSelect(stored)`, que altera o rascunho já mantido pelo
  ViewModel do editor.
- Rationale: a edição em andamento não é resultado crítico; cancelar descarta por definição. Evita levar estado de
  gesto ao ViewModel.

## Nomes falados

- Decision: paleta mantém nomes por posição; personalizada é "Cor personalizada, #RRGGBB"; sem cor, "Escolher cor
  personalizada".
