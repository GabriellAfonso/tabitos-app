# Pesquisa · decisões da 032

1. A validação em TaskRules já aceita ir de sábado para quinta. O ponto que ainda perde a direção é a gravação
   de POSTPONED em toda mudança. Comparar a data solicitada com o prazo lido na transação. Usar um tipo próprio
   para a correção de uma antecipação permite distinguir os dois desfazeres em históricos novos.
2. O seletor já implementa limite inferior e exclusão. Uma seleção inicial permitida e atalhos calculados com o
   relógio da tela resolvem os casos hoje/amanhã. Usar o formato pt-BR da spec 013.
3. O deslocamento dos avisos usa dias civis com sinal. O coordenador pode recuperar uma entrega recém-calculada
   no passado. Usar o instante persistido da antecipação para filtrar esses momentos, inclusive depois de reiniciar.
   Filtrar tudo antes do momento da consulta impediria a recuperação legítima de outros avisos.
4. O resumo já conta apenas POSTPONED. Manter esse contrato preserva o significado histórico de registros antigos
   sem reclassificar a rotina do usuário.
