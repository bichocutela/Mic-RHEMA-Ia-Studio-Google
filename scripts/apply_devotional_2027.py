from pathlib import Path

screen = Path("app/src/main/java/com/aistudio/micrhema/DevotionalsScreen.kt")
text = screen.read_text(encoding="utf-8")

# O filtro só precisa sobreviver às recomposições desta tela; remember evita uma
# dependência/import desnecessário e mantém o comportamento previsvisível.
text = text.replace(
    "var newestFirst by rememberSaveable { mutableStateOf(true) }",
    "var newestFirst by remember { mutableStateOf(true) }"
)

# A tela vazia deve considerar também o calendário automático de 2027 já mesclado
# em availableDevotionals, e não apenas o estado remoto/cache do Firestore.
text = text.replace(
    "if (devotionalsState.isEmpty()) {",
    "if (availableDevotionals.isEmpty()) {",
    1
)

screen.write_text(text, encoding="utf-8")
print("Acabamento idempotente dos devocionais aplicado.")
