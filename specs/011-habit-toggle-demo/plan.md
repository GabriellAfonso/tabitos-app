# Plano

Alterar HabitsViewModel e conteúdo existente. Manter Room/contabilidade/schema 7.
Contrato DemoRepository em core/demo e implementação Room reutilizam repositórios
reais em uma transação. Histórico usa relógio controlado, sem saldo artificial.
Configurações conectam ViewModel a conteúdo Compose; entrada somente debug.
Coordenador de lembretes serializa reset e cancelamento com reagendamento.

Constitution Check: spec antes do código; offline/Room/transação; UI → ViewModel →
contrato; textos em recursos; testes de estorno/repetição/rollback. Sem dependências
ou migrações novas. Pedido explícito autoriza aplicar e instalar, sem commits/push.
