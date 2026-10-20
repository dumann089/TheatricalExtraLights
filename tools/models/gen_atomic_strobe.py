"""Modele de l'Atomic Strobe : corps 14 x 8 x 4 avec la face LED (plaques RGB haut et bas, tube
strobe au milieu), poignee, lyre en U sur socle. Ecrit static / pan / tilt / whole.

Repere Blockbench (1/16 de bloc). La face est au sud (z = 11.5), texture atomic_face_base sur
toute la face : cadre 24 px, plaques et tube aux lignes mesurees dans la texture (voir
AtomicStrobeRenderer)."""
import json
import pathlib

OUT = pathlib.Path(__file__).resolve().parents[2] / "common/src/main/resources/assets/theatricalextralights/models/block/atomic_strobe"
SIDE = "theatricalextralights:block/atomic_strobe/atomic_side"
FACE = "theatricalextralights:block/atomic_strobe/atomic_face_base"


def box(frm, to, tex="#0", uv_scale=1.0, faces=None):
    """Element avec des UV par face en unites de bloc, tuilage de la texture 16 px."""
    x0, y0, z0 = frm
    x1, y1, z1 = to
    w, h, d = x1 - x0, y1 - y0, z1 - z0

    def uv(a, b):
        return [0, 0, min(16, a * uv_scale), min(16, b * uv_scale)]

    element = {"from": list(frm), "to": list(to), "faces": {
        "north": {"uv": uv(w, h), "texture": tex},
        "south": {"uv": uv(w, h), "texture": tex},
        "east": {"uv": uv(d, h), "texture": tex},
        "west": {"uv": uv(d, h), "texture": tex},
        "up": {"uv": uv(w, d), "texture": tex},
        "down": {"uv": uv(w, d), "texture": tex},
    }}
    if faces:
        element["faces"].update(faces)
    return element


# --- Socle et lyre (tournent avec le pan) ---------------------------------------------------
PAN = [
    box((1, 0, 7), (15, 0.5, 12)),                      # plaque de sol
    box((0.9, 0.5, 9), (1.4, 8, 10)),                   # bras gauche
    box((14.6, 0.5, 9), (15.1, 8, 10)),                 # bras droit
    box((0.4, 6.5, 9), (0.9, 7.5, 10)),                 # axe gauche
    box((15.1, 6.5, 9), (15.6, 7.5, 10)),               # axe droit
    box((6, 0.5, 8.5), (10, 1, 10.5)),                  # embase du pied
]

# --- Corps (tourne avec le tilt autour de (8, 7, 9.5)) --------------------------------------
BODY = box((1, 3, 7.5), (15, 11, 11.5), faces={
    "south": {"uv": [0, 0, 16, 16], "texture": "#1"},   # face LED, texture entiere
})
TILT = [
    BODY,
    box((2, 4, 7.3), (14, 10, 7.5)),                    # panneau arriere (grilles)
    box((5, 11, 9), (11, 11.6, 10)),                    # poignee
    box((4.6, 11, 9), (5, 11.6, 10)),
    box((11, 11, 9), (11.4, 11.6, 10)),
    box((1.4, 5.5, 8.8), (1.7, 8.5, 10.2)),             # flasques laterales
    box((14.3, 5.5, 8.8), (14.6, 8.5, 10.2)),
]

TEXTURES = {"0": SIDE, "1": FACE, "particle": FACE}


def write(name, elements, textures):
    (OUT / f"atomic_strobe_{name}.json").write_text(
        json.dumps({"credit": "Theatrical Extra Lights, tools/models/gen_atomic_strobe.py",
                    "texture_size": [16, 16], "textures": textures, "elements": elements}, indent=2) + "\n",
        encoding="utf-8")
    print("wrote", name, len(elements), "elements")


write("static", [], {"particle": FACE})
write("pan", PAN, {"0": SIDE, "particle": SIDE})
write("tilt", TILT, TEXTURES)
write("whole", PAN + TILT, TEXTURES)
