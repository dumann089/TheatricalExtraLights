#!/usr/bin/env python3
"""Inject FixtureMountTransform.apply as first line of preparePoseStack."""
import re
import glob
import os

DIR = os.path.normpath(
    os.path.join(
        os.path.dirname(__file__),
        "..",
        "common",
        "src",
        "main",
        "java",
        "com",
        "github",
        "dumann089",
        "theatricalextralights",
        "client",
        "blockentities",
    )
)
IMPORT = "import com.github.dumann089.theatricalextralights.util.FixtureMountTransform;"
APPLY = "        FixtureMountTransform.apply(poseStack, blockEntity);"

count = 0
for path in sorted(glob.glob(os.path.join(DIR, "*Renderer.java"))):
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    if "void preparePoseStack" not in content or "FixtureMountTransform.apply" in content:
        continue
    if IMPORT not in content:
        m = re.search(r"(import com\.github\.dumann089\.theatricalextralights\.[^\n]+;\n)", content)
        if m:
            content = content[: m.end()] + IMPORT + "\n" + content[m.end() :]
        else:
            content = re.sub(r"(package [^\n]+;\n\n)", r"\1" + IMPORT + "\n", content, count=1)

    new_content, n = re.subn(
        r"(public void preparePoseStack\([^{]+\)\s*\{\s*\n)",
        r"\1" + APPLY + "\n",
        content,
        count=1,
    )
    if new_content != content:
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(new_content)
        count += 1

print(f"Updated {count} renderer files")
