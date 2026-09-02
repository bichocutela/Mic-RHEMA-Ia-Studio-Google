from pathlib import Path
import re

for name in ["pt_aa.json", "pt_acf.json"]:
    path = Path("app/src/main/assets/bibles") / name
    raw = path.read_text(encoding="utf-8-sig")
    abbrevs = re.findall(r'"abbrev"\s*:\s*"([^"]+)"', raw)
    in_string = False
    escape = False
    square = 0
    curly = 0
    complete_books = []
    for i, ch in enumerate(raw):
        if in_string:
            if escape:
                escape = False
            elif ch == '\\':
                escape = True
            elif ch == '"':
                in_string = False
            continue
        if ch == '"':
            in_string = True
        elif ch == '[':
            square += 1
        elif ch == ']':
            square -= 1
        elif ch == '{':
            curly += 1
        elif ch == '}':
            curly -= 1
            if curly == 0 and square == 1:
                complete_books.append(i)
    print(f"{name}: chars={len(raw)} abbrevs={len(abbrevs)} completos={len(complete_books)} last_abbrevs={abbrevs[-6:]}")
    print(f"{name}: square={square} curly={curly} in_string={in_string} last_complete={complete_books[-1] if complete_books else None}")
    print(f"{name}: tail={raw[-350:]!r}")
