# Verificação · Lembrete de revisão

19/11/2026 · [Spec](spec.md) · [Plano](plan.md) · [Tarefas](tasks.md)

## Executado

`.\gradlew.bat :app:testDebugUnitTest --tests "com.ethos.app.core.reminders.*" --tests "com.ethos.app.features.reminders.*"`

27 testes JVM passaram (15 em ReminderRulesTest, 5 de remarcação, 4 de lembretes padrão e 3 de apresentação).
Os casos de revisão cobrem tarefas abertas, concluídas, vencidas e com prazo em período; prática marcada, desmarcada,
pausada e fora dos dias selecionados; maus hábitos; horário alterado, alarme atrasado, duplicado ou de outro dia.
O compilador também sinalizou uma condição redundante no cartão de cronômetro em EthosApp.kt.

## Código e dados

A preferência existente alimenta o seletor, a reserva da data usa uma atualização condicional e os schemas exportados
permanecem na versão 7. O toque usa o mesmo endereço da notificação anterior e solicita Hoje com o filtro Tudo.

## A validar no Android

Foram escritos testes instrumentados para texto e fonte ampliada, preferência persistida e reserva única,
flags/prazo da notificação e navegação pelo toque a partir de Configurações.
Testes instrumentados, aparelho físico, emulador, build do APK e lint não foram executados.
A remoção do painel à meia-noite e o comportamento por fabricante permanecem sem validação em aparelho.
O Android pode atrasar alarmes aproximados. No Android 14 ou superior, a pessoa pode dispensar o aviso por gesto.
