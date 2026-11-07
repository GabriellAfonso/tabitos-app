# Ponto 6 — recompensas

Design consolidado. Prompt salvo em `TODO/specify_recompensas_app.md` em 2026-11-07.
Este registro e o prompt não executam Spec Kit nem autorizam implementação.

## Decisões aprovadas

- Cadastro com nome, descrição opcional, ícone e custo em moedas.
- Comprar/resgatar exige confirmação explícita antes do desconto.
- Compra desconta as moedas e registra uso imediato; não há estoque de recompensas resgatadas,
  estado A utilizar ou confirmação de uso posterior nesta entrega.
- Não permitir cancelamento, devolução ou estorno de uma compra confirmada pelo usuário.
  A confirmação Tem certeza? é a proteção contra engano. Falhas técnicas/repetição de requisição
  não autorizam cobrar duplicado nem debitar sem registrar a compra.
- Limites de resgate são por recompensa, não globais. Ex.: um lanche específico no máximo
  cinco vezes por mês. Prever períodos diário, semanal ou mensal conforme a proposta aprovada.
- Edição/exclusão afeta disponibilidade e compras futuras, preservando resgates antigos no
  histórico com seus valores originais.
- Herdar a regra da carteira: compra exige saldo suficiente e não gera dívida; desconto e
  registro precisam ser consistentes. Não criar moedas manualmente para permitir compra.
- Cronômetro de recompensas pertence ao specify de tempo; não implementar agora nem presumir
  estoque de tempo a consumir, pois a compra foi definida como uso imediato.

## Detalhes finais aprovados

- Custo inteiro maior que zero; limite opcional. Sem limite configurado, permitir compras
  enquanto houver saldo suficiente, sem restrição de quantidade.
- Limites por dia civil, semana de segunda a domingo ou mês do calendário. O novo período
  reinicia a contagem de disponibilidade, sem apagar compras históricas.
- Confirmação da compra mostra preço, saldo restante e quantidade de compras ainda permitidas
  quando houver limite. Não mostrar um contador fictício para recompensas sem limite.
- Alterações de limite consideram compras já realizadas no período correspondente. Se o novo
  limite ficar abaixo da quantidade comprada, bloquear novas compras até o próximo período,
  sem penalidade ou alteração retroativa dos resgates anteriores.
- Permitir reordenar recompensas na loja.
- Excluir pede confirmação, remove a recompensa da loja e preserva as compras no histórico.

## Estado do planejamento

As seis decisões iniciais e as quatro propostas finais foram aprovadas pelo usuário.
Prompt consolidado. Não reabrir essas escolhas como dúvidas de produto;
detalhes técnicos devem ser resolvidos no plano dentro do escopo acordado.
