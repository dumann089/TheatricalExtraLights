"""
Generate an 'Atomic 3000'-style front face texture matching the reference photo.

Photo characteristics (left to right of the image, top to bottom):
  - Thick textured black plastic frame around the edge, with visible rounded
    corners and small mounting bumps at each corner.
  - Two large LED panels (top + bottom), each filled with a dense grid of
    silvery-blue off-state SMD RGB LEDs separated by a crisp black grid.
  - Between the two panels: a horizontal white-LED bar made of TWO parallel
    strips of tiny vertical warm-white emitters, with a black gap between
    them and a black gap from the panels above/below.

Output: 256x128 RGBA (2:1 ratio matching the body face proportions closely).
"""
import os
import random
from PIL import Image, ImageDraw

random.seed(7)

W, H = 512, 256

img = Image.new('RGBA', (W, H), (0, 0, 0, 255))
draw = ImageDraw.Draw(img)

# ---- Frame ----
FRAME = 24          # outer thick frame
INNER_FRAME = 4     # thin black border inside frame, around the panels

# Frame body: matte black plastic, very slightly lighter than pure black
draw.rectangle([0, 0, W - 1, H - 1], fill=(12, 12, 12, 255))

# Rounded-corner darkening — make the four corners look chamfered
for cx, cy in [(0, 0), (W - 1, 0), (0, H - 1), (W - 1, H - 1)]:
    sx = -1 if cx > 0 else 1
    sy = -1 if cy > 0 else 1
    for d in range(6):
        draw.point((cx, cy + sy * d), fill=(0, 0, 0, 255))
        draw.point((cx + sx * d, cy), fill=(0, 0, 0, 255))
    # tiny mounting bump highlight
    bx = cx + sx * 8
    by = cy + sy * 8
    draw.rectangle([bx - 2, by - 2, bx + 2, by + 2], fill=(28, 28, 28, 255))
    draw.point((bx, by), fill=(55, 55, 55, 255))

# Faint inner-frame edge highlight (subtle bevel against the panels)
draw.rectangle([FRAME - 1, FRAME - 1, W - FRAME, H - FRAME],
               outline=(40, 40, 40, 255))

# Inner panel area (where the LEDs and bar live)
ix0, iy0 = FRAME, FRAME
ix1, iy1 = W - FRAME - 1, H - FRAME - 1

# Vertical layout inside the inner area
inner_h = iy1 - iy0 + 1
# Bar takes ~14% of inner height, with small black gaps above/below
bar_h = max(8, int(inner_h * 0.14))
gap = 2
remaining = inner_h - bar_h - 2 * gap
top_h = remaining // 2
bot_h = remaining - top_h

top_y0 = iy0
top_y1 = top_y0 + top_h - 1
bar_y0 = top_y1 + gap + 1
bar_y1 = bar_y0 + bar_h - 1
bot_y0 = bar_y1 + gap + 1
bot_y1 = bot_y0 + bot_h - 1

# Pure black background behind LED panels (PCB color)
draw.rectangle([ix0, top_y0, ix1, top_y1], fill=(0, 0, 0, 255))
draw.rectangle([ix0, bot_y0, ix1, bot_y1], fill=(0, 0, 0, 255))


def draw_led_panel(x0, y0, x1, y1, cols, rows):
    """Dense grid of off-state RGB SMD LEDs separated by a crisp 1px black grid."""
    pw = x1 - x0 + 1
    ph = y1 - y0 + 1
    cw = pw / cols
    ch = ph / rows
    for ci in range(cols):
        for ri in range(rows):
            # Cell rectangle in float pixel-space
            fx0 = x0 + ci * cw
            fy0 = y0 + ri * ch
            fx1 = x0 + (ci + 1) * cw
            fy1 = y0 + (ri + 1) * ch
            # LED body, leaving a 1px black grid line
            lx0 = int(fx0) + 1
            ly0 = int(fy0) + 1
            lx1 = int(fx1) - 1
            ly1 = int(fy1) - 1
            if lx1 < lx0 or ly1 < ly0:
                # cell too small for an interior — skip and rely on black
                continue
            # Off-state look: silvery cool-grey with slight blueish cast,
            # as the SMD silicone reads under flash photography.
            base = random.randint(155, 185)
            r = base - random.randint(0, 10)
            g = base
            b = base + random.randint(10, 30)
            b = min(b, 230)
            draw.rectangle([lx0, ly0, lx1, ly1], fill=(r, g, b, 255))
            # specular highlight in top-left
            if lx1 > lx0 and ly1 > ly0:
                draw.point((lx0, ly0), fill=(230, 235, 245, 255))
            # darker bottom-right corner pixel
            if lx1 > lx0 and ly1 > ly0:
                draw.point((lx1, ly1), fill=(40, 45, 55, 255))


# Counted ~32 columns × 12 rows visible in the reference photo
PANEL_COLS = 36
PANEL_ROWS = 13
draw_led_panel(ix0, top_y0, ix1, top_y1, PANEL_COLS, PANEL_ROWS)
draw_led_panel(ix0, bot_y0, ix1, bot_y1, PANEL_COLS, PANEL_ROWS)


def draw_white_bar(x0, y0, x1, y1, segments=130):
    """White-LED bar: two parallel strips of tiny vertical warm-white emitters."""
    pw = x1 - x0 + 1
    ph = y1 - y0 + 1
    # PCB strip backdrop (very dark)
    draw.rectangle([x0, y0, x1, y1], fill=(6, 6, 6, 255))
    # Two strips with a black gap between them
    strip_h = (ph - 2) // 2  # gap of 2 in the middle
    strip1 = (y0, y0 + strip_h - 1)
    strip2 = (y1 - strip_h + 1, y1)
    # thin reflector lines top of each strip (silvery edge)
    for sy0, sy1 in (strip1, strip2):
        draw.line([(x0, sy0), (x1, sy0)], fill=(60, 60, 65, 255))
        draw.line([(x0, sy1), (x1, sy1)], fill=(45, 45, 50, 255))
    sw = pw / segments
    for i in range(segments):
        sx0 = int(x0 + i * sw + 0.6)
        sx1 = int(x0 + (i + 1) * sw - 0.6)
        if sx1 < sx0:
            sx1 = sx0
        for sy0, sy1 in (strip1, strip2):
            # warm-white vertical segment, with 1px black gap top/bottom
            draw.rectangle([sx0, sy0 + 1, sx1, sy1 - 1],
                           fill=(255, 248, 220, 255))


draw_white_bar(ix0, bar_y0, ix1, bar_y1)

# Subtle dark lines flanking the white bar (panel/bar boundaries)
draw.line([(ix0, bar_y0 - 1), (ix1, bar_y0 - 1)], fill=(0, 0, 0, 255))
draw.line([(ix0, bar_y1 + 1), (ix1, bar_y1 + 1)], fill=(0, 0, 0, 255))

out_dir = os.path.join(
    os.path.dirname(__file__),
    '..',
    'common', 'src', 'main', 'resources',
    'assets', 'theatricalextralights', 'textures', 'block', 'atomic_strobe',
)
out_dir = os.path.abspath(out_dir)
out_path = os.path.join(out_dir, 'atomic_face_base.png')
img.save(out_path)
print(f"Wrote {out_path} ({W}x{H})")
