"""Sprites du brouillard CO2 : nuages grumeleux 64x64, bord irregulier, RGB blanc partout (pas de
frange sombre au filtrage lineaire). Ecrit co2_jet_0..7.png. Sans dependance : PNG ecrit a la main."""
import math, random, struct, zlib, pathlib

SIZE = 64
OUT = pathlib.Path(__file__).resolve().parents[2] / "common/src/main/resources/assets/theatricalextralights/textures/particle"

def value_noise(seed, cells):
    rnd = random.Random(seed)
    grid = [[rnd.random() for _ in range(cells)] for _ in range(cells)]
    def sample(u, v):
        x, y = u * cells, v * cells
        x0, y0 = int(math.floor(x)) % cells, int(math.floor(y)) % cells
        fx, fy = x - math.floor(x), y - math.floor(y)
        sx, sy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
        x1, y1 = (x0 + 1) % cells, (y0 + 1) % cells
        top = grid[y0][x0] * (1 - sx) + grid[y0][x1] * sx
        bottom = grid[y1][x0] * (1 - sx) + grid[y1][x1] * sx
        return top * (1 - sy) + bottom * sy
    return sample

def fbm(octaves, u, v):
    total, amp, norm, freq = 0.0, 1.0, 0.0, 1.0
    for sample in octaves:
        total += amp * sample((u * freq) % 1.0, (v * freq) % 1.0)
        norm += amp
        amp *= 0.5
        freq *= 2.0
    return total / norm

def clamp(v, lo=0.0, hi=1.0):
    return lo if v < lo else hi if v > hi else v

def png(path, rows):
    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xffffffff)
    raw = b"".join(b"\x00" + bytes(row) for row in rows)
    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0))
                     + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))

def sprite(index):
    seed = 7000 + index * 17
    rnd = random.Random(seed)
    octaves = [value_noise(seed + o, cells) for o, cells in enumerate((4, 8, 16, 32))]
    phases = [rnd.random() * math.tau for _ in range(3)]
    rows = []
    for py in range(SIZE):
        row = []
        for px in range(SIZE):
            dx, dy = (px + 0.5) / SIZE * 2 - 1, (py + 0.5) / SIZE * 2 - 1
            r = math.hypot(dx, dy)
            ang = math.atan2(dy, dx)
            wobble = 0.5 * math.sin(2 * ang + phases[0]) + 0.3 * math.sin(3 * ang + phases[1]) + 0.2 * math.sin(5 * ang + phases[2])
            boundary = 0.92 * (0.85 + 0.15 * (0.5 + 0.5 * wobble))
            # Plateau dense au centre, bord doux sur les 60 % exterieurs.
            edge = clamp((1.0 - r / boundary) / 0.5) ** 1.1
            n = fbm(octaves, (px + 0.5) / SIZE, (py + 0.5) / SIZE)
            lump = clamp(0.55 + 0.9 * (n - 0.5))
            # Contraste doux : les grumeaux se lisent en masse, pas en chou-fleur.
            a = clamp(edge * (0.62 + 0.38 * lump) * (0.80 + 0.40 * n))
            v = int(round(255 * (0.92 + 0.08 * n)))
            row += [v, v, 255, int(round(255 * a))]
        rows.append(row)
    return rows

for i in range(8):
    png(OUT / f"co2_jet_{i}.png", sprite(i))
    print("wrote", f"co2_jet_{i}.png")
