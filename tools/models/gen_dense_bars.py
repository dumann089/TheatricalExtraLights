"""Modeles des barres denses : la tete de la barre d'origine, lentilles remplacees par un fond sombre
et une petite LED par pixel, centree la ou PixelBarRenderer dessine le pixel (voir les Strip des
renderers Dense*). Ecrit <dossier>/<nom>_dense_tilt.json et <nom>_dense_whole.json."""
import copy
import json
import pathlib

BLOCK = pathlib.Path(__file__).resolve().parents[2] / "common/src/main/resources/assets/theatricalextralights/models/block"


def face_all(uv, tex="#0"):
    return {k: {"uv": uv, "texture": tex} for k in ("north", "east", "south", "west", "up", "down")}


def load(rel):
    return json.loads((BLOCK / rel).read_text(encoding="utf-8"))


def write(rel, model):
    (BLOCK / rel).write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")
    print("wrote", rel, len(model["elements"]), "elements")


def replace(model, drop, add):
    """Retire les elements dont (from, to) est dans drop, ajoute add."""
    keep = [e for e in model["elements"] if (tuple(e["from"]), tuple(e["to"])) not in drop]
    removed = len(model["elements"]) - len(keep)
    out = copy.deepcopy(model)
    out["elements"] = keep + add
    out.pop("groups", None)
    return out, removed


def build(folder, base, drop_fn, add, expect):
    for part in ("tilt", "whole"):
        src = load(f"{folder}/{base}_{part}.json")
        drop = {(tuple(e["from"]), tuple(e["to"])) for e in src["elements"] if drop_fn(e)}
        out, removed = replace(src, drop, add)
        assert removed == expect, (folder, part, removed)
        write(f"{folder}/{base}_dense_{part}.json", out)


# ── Dense RGB Bar : 46 LED sur x -14..30, hauteur 3, devant z 6.4375 ───────────────────────────
BODY_LB = [0, 4, 4, 11]      # gris du corps (new_texture1)
LENS_LB = [6, 2.5, 10.5, 7]  # lentille
rgb_add = [{"from": [-14, 5.9375, 6.6875], "to": [30, 9.9375, 8.9375], "faces": face_all(BODY_LB)}]
pitch = 44 / 46
for i in range(46):
    c = -14 + (i + 0.5) * pitch
    rgb_add.append({"from": [round(c - 0.36, 4), 7.5775, 6.4375], "to": [round(c + 0.36, 4), 8.2975, 6.6875],
                    "faces": face_all(LENS_LB)})
build("ledbar", "ledbar",
      lambda e: e["from"][1] == 5.9375 and e["to"][1] == 9.9375 and (
          (e["from"][2] == 6.4375 and e["to"][2] == 6.6875) or (e["from"][2] == 5.9375 and e["to"][2] == 8.9375)),
      rgb_add, 17)

# ── Dense Vertical RGB Bar : 46 LED sur y -15..31, largeur 3, devant z 13 ──────────────────────
BODY_VL = [0, 0, 2, 1]       # corps sombre (moving_vl2c_body)
LENS_VL = [7, 1, 11, 5]
v_add = []
for i in range(46):
    c = -15 + i + 0.5
    v_add.append({"from": [7.64, round(c - 0.36, 4), 12.8], "to": [8.36, round(c + 0.36, 4), 13],
                  "faces": face_all(LENS_VL)})


def vertical_body(model):
    for e in model["elements"]:
        if e["from"] == [6, -15, 13] and e["to"] == [10, 31, 16]:
            e["faces"]["north"] = {"uv": BODY_VL, "texture": "#0"}
            return 1
    return 0


for part in ("tilt", "whole"):
    m = copy.deepcopy(load(f"barvertical/barvertical_{part}.json"))
    assert vertical_body(m) == 1, part
    m["elements"] += v_add
    m.pop("groups", None)
    write(f"barvertical/barvertical_dense_{part}.json", m)

# ── Dense Moving RGB Bar : 24 LED sur x -4.1..19.9, hauteur 2.5, devant z 5.25 ─────────────────
LENS_MB = [6, 0, 12, 6]


def moving_add(x0, x1):
    """Fond sombre et une LED par 1/16 sur x0..x1 (la tete : -4.1..19.9, 24 LED)."""
    add = [{"from": [x0, 13.25, 5.5], "to": [x1, 16.25, 10.75], "faces": face_all(BODY_VL)}]
    for i in range(round(x1 - x0)):
        c = x0 + i + 0.5
        add.append({"from": [round(c - 0.36, 4), 14.39, 5.25], "to": [round(c + 0.36, 4), 15.11, 5.5],
                    "faces": face_all(LENS_MB)})
    return add


def is_cell(e):
    return e["from"][1] == 13.25 and e["to"][1] == 16.25 and e["from"][2] == 5.25 and e["to"][2] == 10.75


for part in ("tilt", "whole"):
    src = load(f"movingbar/movingbar_{part}.json")
    cells = [e for e in src["elements"] if is_cell(e)]
    x0 = min(e["from"][0] for e in cells)
    x1 = max(e["to"][0] for e in cells)
    drop = {(tuple(e["from"]), tuple(e["to"])) for e in cells}
    out, _ = replace(src, drop, moving_add(x0, x1))
    write(f"movingbar/movingbar_dense_{part}.json", out)
