# Seletores de horário

Decisão de interface em 2026-11-11: substituir somente horários digitados por seletores de arrastar
com horas (00–23) e minutos (00–59), sem segundos. Durações continuam em minutos; seletores existentes
com relógio circular permanecem como estão.

Aplicado a lembretes (incluindo referência e adiamento), resumo diário, lembrete de acompanhamento e
início/fim do registro manual de tempo. Campos opcionais mantêm a ação de remover horário.
Contratos dos rascunhos, persistência e regras de agendamento permanecem iguais.

Verificação: assembleDebug, 155 testes JVM e lintDebug aprovados.
Verificação visual e TalkBack ainda pendentes; nenhum emulador ou dispositivo foi acionado.
