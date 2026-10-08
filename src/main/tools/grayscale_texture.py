#!/usr/bin/env python3
"""Convert a PNG texture to grayscale while preserving its alpha channel.

Usage:
  python grayscale_texture.py input.png output.png
  python grayscale_texture.py input.png --in-place

RGB is converted with Rec. 709 luminance so perceived brightness is preserved.
Transparent pixels remain transparent and the source dimensions are unchanged.
"""
from __future__ import annotations

import argparse
from pathlib import Path
from PIL import Image


def grayscale(image: Image.Image) -> Image.Image:
    src = image.convert("RGBA")
    out = Image.new("RGBA", src.size, (0, 0, 0, 0))
    src_px = src.load()
    out_px = out.load()
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = src_px[x, y]
            if a == 0:
                continue
            gray = round(0.2126 * r + 0.7152 * g + 0.0722 * b)
            out_px[x, y] = (gray, gray, gray, a)
    return out


def main() -> int:
    parser = argparse.ArgumentParser(description="Convert a PNG texture to grayscale while preserving alpha.")
    parser.add_argument("input", type=Path, help="Input PNG")
    parser.add_argument("output", nargs="?", type=Path, help="Output PNG")
    parser.add_argument("--in-place", action="store_true", help="Overwrite the input PNG")
    args = parser.parse_args()

    source = args.input.resolve()
    if not source.is_file():
        parser.error(f"Input does not exist: {source}")
    if source.suffix.lower() != ".png":
        parser.error("Input must be a PNG")
    if args.in_place and args.output is not None:
        parser.error("Choose either an output path or --in-place, not both")
    if not args.in_place and args.output is None:
        parser.error("Provide an output path or use --in-place")

    target = source if args.in_place else args.output.resolve()
    target.parent.mkdir(parents=True, exist_ok=True)
    with Image.open(source) as image:
        result = grayscale(image)
    result.save(target)
    print(target)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
