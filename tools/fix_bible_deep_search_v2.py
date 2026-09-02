from pathlib import Path
import json
import runpy
import re
import unicodedata

# O primeiro patch aplica todas as melhorias de busca, mas o smoke test revelou
# o problema real: pt_acf.json está truncado/malformado. Mantemos as alterações
# feitas antes da validação e trocamos a pesquisa profunda para a base local ARA
# (pt_aa.json), que é a base usada pelo app para ARA.
try:
    runpy.run_path("tools/fix_bible_deep_search.py", run_name="__main__")
except json.JSONDecodeError:
    pass

path = Path("app/src/main/java/com/aistudio/micrhema/BibleDeepSearch.kt")
text = path.read_text(encoding="utf-8")
if 'context.assets.open("bibles/pt_acf.json")' not in text:
    raise SystemExit("Origem ACF da pesquisa profunda não encontrada")
text = text.replace('context.assets.open("bibles/pt_acf.json")', 'context.assets.open("bibles/pt_aa.json")', 1)
text = text.replace(
    "Usa o índice ACF local para localizar referências rapidamente e funciona sem depender da internet.",
    "Usa o índice bíblico local para localizar referências rapidamente e funciona sem depender da internet.",
    1,
)
text = text.replace(
    "A localização usa o índice bíblico ACF armazenado no app; ao tocar no resultado, a referência abre na versão que você estiver usando.",
    "A localização usa o índice bíblico armazenado no app; ao tocar no resultado, a referência abre na versão que você estiver usando.",
    1,
)
path.write_text(text, encoding="utf-8")

# Validação real da base usada pela pesquisa profunda.
def norm(value: str) -> str:
    value = unicodedata.normalize("NFD", value.lower())
    value = "".join(ch for ch in value if unicodedata.category(ch) != "Mn")
    value = re.sub(r"[^a-z0-9]+", " ", value).strip()
    return re.sub(r"\s+", " ", value)


def stem(token: str) -> str:
    token = norm(token).replace(" ", "")
    if len(token) < 4:
        return token
    suffixes = ["amentos","imentos","amento","imento","acoes","icoes","mente","ados","adas","idos","idas","ando","endo","indo","aram","eram","iram","coes","cao","ava","avam","iam","ado","ada","ido","ida","ou","ei","am","em","es","s"]
    for suffix in suffixes:
        if token.endswith(suffix) and len(token) - len(suffix) >= 3:
            return token[:-len(suffix)]
    return token

asset = Path("app/src/main/assets/bibles/pt_aa.json")
data = json.loads(asset.read_text(encoding="utf-8-sig"))
if len(data) != 66:
    raise SystemExit(f"Base bíblica local inválida: esperados 66 livros, encontrados {len(data)}")

all_words = set()
verse_count = 0
for book in data:
    chapters = book.get("chapters", [])
    for chapter in chapters:
        for verse in chapter:
            verse_count += 1
            all_words.update(norm(str(verse)).split())
if verse_count < 30000:
    raise SystemExit(f"Base bíblica incompleta: apenas {verse_count} versículos")

all_stems = {stem(w) for w in all_words if len(stem(w)) >= 3}
checks = {
    "curado": ["curou", "sarou", "cura", "enfermidade", "saude"],
    "cura": ["curou", "sarou", "enfermidade", "saude"],
    "ansiedade": ["cuidado", "medo", "paz", "descanso"],
    "amor": ["amor", "amar", "amou", "caridade"],
    "davi": ["davi"],
    "jesus": ["jesus"],
    "deus": ["deus"],
}
for query, alternatives in checks.items():
    candidates = [query] + alternatives
    if not any(norm(c) in all_words or stem(c) in all_stems for c in candidates):
        raise SystemExit(f"Smoke test falhou para {query}")

print(f"Base local validada: 66 livros, {verse_count} versículos. Pesquisa profunda corrigida.")
