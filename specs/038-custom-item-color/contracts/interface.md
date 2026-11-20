# Contrato de interface da feature 038

## Linha de cores (`ColorPicker(selected: Int, enabled, onSelect: (Int) -> Unit)`)

- Rótulo "Cor"; `selectableGroup`; 6 alvos de 48 dp em uma linha: Vermelho, Laranja, Verde, Azul, Roxo,
  Personalizada.
- Círculo da paleta: nome falado, papel RadioButton, ✓ na cor de ícone sobre a cor quando selecionado.
- Personalizada sem cor (item numa das 5): gradiente de tons com ícone de paleta; fala "Escolher cor personalizada".
- Personalizada com cor (cor personalizada, Rosa ou Cinza): preenchido com a cor, ✓ e lápis; selecionado; fala
  "Cor personalizada, #RRGGBB" (Rosa/Cinza: o nome da cor). Tocar abre a folha.

## Folha "Cor personalizada"

Ordem: título; prévia em dois cartões (fundo claro e escuro) com o ícone do item sobre a cor e a cor como tom de
ícone; roda; brilho; segmentado Hex | RGB; campo(s); erro; "Partir de" com as 5
cores; Cancelar e Usar cor.

- Roda: toque/arraste define tom e intensidade; marcador na posição atual; descrição "Roda de cores".
- Brilho: `Slider` 0–100% com rótulo "Brilho".
- Hex: aceita `#`, 3 ou 6 dígitos, maiúsculas/minúsculas; exibe 6 dígitos em maiúsculas quando a cor muda por outro
  controle. Erro: "Use 6 dígitos de 0–9 e A–F, por exemplo 1E7F86."
- RGB: 3 campos numéricos R, G, B; erro: "Cada valor vai de 0 a 255."
- Inválido: Usar cor desativado; roda e prévia ficam na última cor válida.
- Sem aviso de contraste (removido a pedido do usuário).
- Cancelar, deslizar para fechar ou tocar fora: não muda o item. Usar cor: `onSelect(encode(Custom(rgb)))`.
