# Roteiro de verificação da feature 038

## JVM (rotina)

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.tabitos.app.core.colors.*" --tests "com.tabitos.app.core.ui.components.*" --tests "com.tabitos.app.features.habits.*" --tests "com.tabitos.app.features.tasks.*"
```

Esperado: codificação/validação, contraste, HSV/Hex/RGB, transições da folha, regras de lista/hábito com cor
personalizada e cor inicial do mau hábito aprovados.

## Manual (celular, com autorização)

1. Abrir uma lista existente: tom novo correspondente (Sálvia → Verde etc.); Rosa/Cinza inalterados.
2. Editar lista: linha com 6 círculos sem quebra em 360 dp.
3. Personalizada → digitar `1E7F86` → Usar cor → Salvar; conferir menu ☰, título da lista e detalhes; reabrir o app.
4. Hábito → Aparência → Personalizada → arrastar na roda e mudar o brilho; Hex/RGB acompanham.
5. Digitar `F6D743`: prévia mostra a cor nos dois temas, sem aviso; ícone do cartão escuro.
6. Hex inválido (`12ZZ`): campo em erro, Usar cor desativado.
7. Quebrar mau hábito: começa em Vermelho.
8. TalkBack: círculos falam nomes; Personalizada fala o código; brilho ajustável.
9. Fonte 200%: folha rola até Usar cor.
