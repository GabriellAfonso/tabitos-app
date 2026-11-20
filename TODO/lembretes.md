# Lembretes — pop-up e editor

Estado: opção A escolhida e aplicada em 2026-11-19 (atalhos criam direto; escopo padrão "Só esta").
Protótipo: `prototypes/lembretes/index.html`.

## Como está (ReminderContent.kt)
- Dois AlertDialogs empilhados; lembretes como linhas de texto + botões Editar/Excluir do mesmo peso.
- Editor com tipos em botões de texto, data digitada em AAAA-MM-DD e antecedência em minutos (contraria spec 013).
- Não mostra quando o aviso vai tocar.

## Opções no protótipo
- A: folha inferior com lista + atalhos (Na hora/30 min/1 h/1 dia antes); editor em folha que cresce com resumo do aviso.
- B: página cheia com linhas rótulo → valor (como Prazo opção C); editor em tela cheia com seletores inline.
- C: folha única com linha do tempo até o prazo; edição em cartão sanfona.

## Pendências
- Quais atalhos e se criam direto ou abrem o editor.
- Escopo padrão da série: "Só esta" (atual) ou "Esta e próximas".
- Fora do protótipo: diálogo Adiar da notificação (data digitada), lembretes de acompanhamento, ligar/desligar sem excluir.
