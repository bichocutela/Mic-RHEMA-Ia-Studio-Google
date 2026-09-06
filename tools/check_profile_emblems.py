#!/usr/bin/env python3
"""Read-only quality gate for the bundled profile frames (requires Pillow).

Run from any directory: python3 tools/check_profile_emblems.py
Decode the entire file: reading its dimensions alone misses damaged WebP data.
This check does not repair, crop, resize, or rewrite any image.
"""

from pathlib import Path
import argparse
import hashlib
import json
import re
import struct
import sys

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "app/src/main/res/drawable-nodpi"
CATALOG = ROOT / "app/src/main/java/com/aistudio/micrhema/BadgeCatalog.kt"
MIN_EDGE = 1024
MANIFEST = ROOT / "docs/profile-emblem-art.json"
ANDROID_RENDERER = ROOT / "app/src/main/java/com/aistudio/micrhema/BadgeFrame.kt"
PWA_RENDERER = ROOT / "pwa/client/src/components/BiblicalBadgeAvatar.tsx"
BADGES = {
    8: ("semente_da_fe", "Semente da Fé"),
    9: ("caminho_da_promessa", "Caminho da Promessa"),
    10: ("escudo_da_fe", "Escudo da Fé"),
    11: ("aguas_vivas", "Águas Vivas"),
    12: ("videira_verdadeira", "Videira Verdadeira"),
    13: ("luz_do_mundo", "Luz do Mundo"),
    14: ("armadura_de_deus", "Armadura de Deus"),
    15: ("leao_de_juda", "Leão de Judá"),
    16: ("chama_do_espirito", "Chama do Espírito"),
    17: ("coroa_da_vida", "Coroa da Vida"),
    18: ("asas_da_promessa", "Asas da Promessa"),
    19: ("tabernaculo", "Tabernáculo"),
    20: ("arca_da_alianca", "Arca da Aliança"),
    21: ("nova_jerusalem", "Nova Jerusalém"),
    22: ("gloria_eterna", "Glória Eterna"),
}


def inspect_frame(path: Path) -> list[str]:
    errors = []
    if not path.is_file():
        return ["arquivo ausente"]
    raw = path.read_bytes()
    if len(raw) < 12 or raw[:4] != b"RIFF" or raw[8:12] != b"WEBP":
        errors.append("cabeçalho WebP inválido")
    else:
        expected_size = struct.unpack_from("<I", raw, 4)[0] + 8
        if expected_size != len(raw):
            errors.append(f"RIFF declara {expected_size} bytes, arquivo tem {len(raw)}")
    try:
        with Image.open(path) as frame:
            frame.load()
            width, height = frame.size
            if width != height:
                errors.append(f"imagem não quadrada: {width}x{height}")
            if (width, height) != (MIN_EDGE, MIN_EDGE):
                errors.append(f"resolução {width}x{height}; padrão {MIN_EDGE}x{MIN_EDGE}")
            if "A" not in frame.getbands():
                return errors + ["sem canal alpha; cobre o avatar"]
            alpha = frame.getchannel("A")
            bounds = alpha.getbbox()
            if bounds is None:
                return errors + ["moldura inteiramente transparente"]
            margin = max(1, round(min(width, height) * 0.03))
            left, top, right, bottom = bounds
            if min(left, top, width - right, height - bottom) < margin:
                errors.append("margem transparente menor que 3%; revisar recorte")
            # Check only the central part of the opening. Ornaments may overlap
            # the circular portrait rim, but must never cover the portrait center.
            center = alpha.crop((width * 2 // 5, height * 2 // 5,
                                 width * 3 // 5, height * 3 // 5))
            if center.getextrema()[1] != 0:
                errors.append("centro da moldura não é totalmente transparente")
    except (OSError, ValueError) as exc:
        errors.append(f"falha ao decodificar imagem completa: {exc}")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--level", type=int, action="append", choices=range(8, 23),
                        help="Verificar somente o nível informado antes do commit individual")
    args = parser.parse_args()
    selected = args.level or list(BADGES)
    catalog = CATALOG.read_text(encoding="utf-8")
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    android = ANDROID_RENDERER.read_text(encoding="utf-8")
    pwa = PWA_RENDERER.read_text(encoding="utf-8")
    failures = 0
    for level in selected:
        badge_id, name = BADGES[level]
        path = RESOURCES / f"profile_emblem_level_{level:02}.webp"
        errors = inspect_frame(path)
        art = next((a for a in manifest["frames"] if a["level"] == level), None)
        if art is None:
            errors.append("arte ainda não revisada no manifesto")
        else:
            if art["id"] != badge_id or art["name"] != name:
                errors.append("arte revisada não corresponde ao ID/nome esperado")
            if path.is_file() and hashlib.sha256(path.read_bytes()).hexdigest() != art["sha256"]:
                errors.append("conteúdo diverge da arte revisada (SHA-256)")
            fraction = str(art["avatarFraction"])
            if not re.search(rf"\b{level}\s*->\s*{re.escape(fraction)}f\b", android):
                errors.append("encaixe Android diverge da arte revisada")
            if not re.search(rf"\b{level}:\s*{re.escape(fraction)}\s*,", pwa):
                errors.append("encaixe PWA diverge da arte revisada")
        pattern = (
            rf'BiblicalBadge\("{re.escape(badge_id)}",\s*"{re.escape(name)}",'
            rf'\s*"[^"\n]*",\s*BadgeCategory\.LEVEL,\s*{level},'
            rf'\s*BadgeFrameStyle\.PROFILE_EMBLEM\b'
        )
        if len(re.findall(pattern, catalog)) != 1:
            errors.append("associação ID/nome/nível diverge do contrato persistido")
        if errors:
            failures += 1
            print(f"FALHA {level:02} — {name}: " + "; ".join(errors))
        else:
            print(f"OK {level:02} — {name}")
    print(f"\n{len(selected) - failures}/{len(selected)} molduras selecionadas aprovadas.")
    print("A correspondência visual do desenho ao nome exige revisão humana.")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
