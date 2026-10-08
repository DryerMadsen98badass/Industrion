#!/usr/bin/env python3
"""Generate Industron casting artwork from the same material-part sprites.

For every configured castable part this one tool can generate:
  * mold.png        - cast mold cut from empty_mold using variant_1 silhouette
  * hot_overlay.png - hot-metal overlay for every available variant
  * terracotta PNG  - raw, pre-coloured terracotta item texture from variant_1

Existing output files are preserved by default. Use --force (or the individual
--force-* flags) to regenerate them.

The terracotta output is intentionally a finished raw texture: it is not a tint
mask and does not require material colouring in-game.
"""
from __future__ import annotations

import argparse
from dataclasses import dataclass
from pathlib import Path
from PIL import Image

DARK_RECESS = (86, 82, 77, 255)
NEIGHBORS_4 = ((1, 0), (-1, 0), (0, 1), (0, -1))
SOURCE_NAMES = ("base.png", "head.png", "handle.png", "saw.png", "wrench.png", "drill.png", "wire_cutter_base.png", "body.png", "chainsaw_body.png")

# Palette sampled from the existing Industron hot overlays, coolest -> hottest.
HOT_PALETTE = (
    (255, 40, 37, 24),
    (255, 42, 39, 39),
    (255, 94, 28, 86),
    (255, 41, 18, 98),
    (255, 119, 32, 178),
    (255, 163, 75, 203),
    (255, 186, 97, 204),
    (254, 225, 143, 230),
    (255, 246, 218, 255),
    (255, 255, 255, 255),
)

# Sampled from the existing raw terracotta textures. Source luminance is mapped
# onto this fixed brown ramp, preserving the original sprite's shading while
# keeping every terracotta part in the same colour family.
TERRACOTTA_DARK = (122, 75, 54)
TERRACOTTA_LIGHT = (175, 107, 77)
TERRACOTTA_LUMINANCE_LOW = 0.20
TERRACOTTA_LUMINANCE_HIGH = 0.90


@dataclass(frozen=True)
class PartTarget:
    collection: str
    folder: str
    variant1_source: str


# Folder is both the mold output folder and the parent of variant_* directories.
# variant1_source is explicit because some tool parts intentionally use semantic
# names such as head.png, handle.png, saw.png and wrench.png instead of base.png.
TARGETS = (
    PartTarget("material_sets", "machine_parts/mixing_blade", "machine_parts/mixing_blade/variant_1/base.png"),
    PartTarget("material_sets", "machine_parts/impeller", "machine_parts/impeller/variant_1/base.png"),
    PartTarget("material_sets", "machine_parts/crank", "machine_parts/crank/variant_1/base.png"),
    PartTarget("material_sets", "machine_parts/connecting_rod", "machine_parts/connecting_rod/variant_1/base.png"),
    PartTarget("material_sets", "machine_parts/press_head", "machine_parts/press_head/variant_1/base.png"),
    PartTarget("material_sets", "machine_parts/guide_rail", "machine_parts/guide_rail/variant_1/base.png"),
    PartTarget("material_sets", "machine_parts/chuck_jaw", "machine_parts/chuck_jaw/variant_1/base.png"),
    PartTarget("material_sets", "machine_parts/cutting_insert", "machine_parts/cutting_insert/variant_1/base.png"),
    PartTarget("material_sets", "machine_parts/crushing_segment", "machine_parts/crushing_segment/variant_1/base.png"),
    PartTarget("material_sets", "machine_parts/cylinder", "machine_parts/cylinder/variant_1/base.png"),
    PartTarget("material_sets", "machine_parts/piston", "machine_parts/piston/variant_1/base.png"),
    PartTarget("material_sets", "machine_parts/piston_ring", "machine_parts/piston_ring/variant_1/base.png"),
    PartTarget("material_sets", "machine_parts/valve_body", "machine_parts/valve_body/variant_1/base.png"),
    PartTarget("material_sets", "machine_parts/valve_stem", "machine_parts/valve_stem/variant_1/base.png"),
    PartTarget("material_sets", "machine_parts/valve_seat", "machine_parts/valve_seat/variant_1/base.png"),
    PartTarget("material_sets", "spring/normal", "spring/normal/variant_1/base.png"),
    PartTarget("material_sets", "machine_parts/flange", "machine_parts/flange/variant_1/base.png"),
    PartTarget("material_sets", "machine_parts/extrusion_die", "machine_parts/extrusion_die/variant_1/base.png"),
    PartTarget("tool", "shears/head", "shears/head/variant_1/base.png"),
    PartTarget("tool", "shears/handle", "shears/handle/variant_1/base.png"),
    PartTarget("tool", "crossbow/limbs", "crossbow/limbs/variant_1/base.png"),
    PartTarget("tool", "crossbow/trigger", "crossbow/trigger/variant_1/base.png"),
    PartTarget("tool", "fishing_rod/hook", "fishing_rod/hook/variant_1/base.png"),
    PartTarget("tool", "shield/body", "shield/body/variant_1/base.png"),
    PartTarget("tool", "shield/handle", "shield/handle/variant_1/base.png"),
    PartTarget("tool", "armour/helmet", "armour/helmet/variant_1/base.png"),
    PartTarget("tool", "armour/chestplate", "armour/chestplate/variant_1/base.png"),
    PartTarget("tool", "armour/leggings", "armour/leggings/variant_1/base.png"),
    PartTarget("tool", "armour/boots", "armour/boots/variant_1/base.png"),
    PartTarget("tool", "snap_ring_pliers/head", "snap_ring_pliers/head/variant_1/base.png"),
    PartTarget("tool", "snap_ring_pliers/handle", "snap_ring_pliers/handle/variant_1/base.png"),
    PartTarget("tool", "bearing_press/head", "bearing_press/head/variant_1/base.png"),
    PartTarget("tool", "bearing_press/handle", "bearing_press/handle/variant_1/base.png"),
    PartTarget("tool", "clamp/head", "clamp/head/variant_1/base.png"),
    PartTarget("tool", "clamp/handle", "clamp/handle/variant_1/base.png"),
    PartTarget("tool", "crimping_tool/head", "crimping_tool/head/variant_1/base.png"),
    PartTarget("tool", "crimping_tool/handle", "crimping_tool/handle/variant_1/base.png"),
    PartTarget("tool", "gear_cutter/head", "gear_cutter/head/variant_1/base.png"),
    PartTarget("tool", "gear_cutter/handle", "gear_cutter/handle/variant_1/base.png"),

    PartTarget("material_sets", "ball/tiny", "ball/tiny/variant_1/base.png"),
    PartTarget("material_sets", "ball/large", "ball/large/variant_1/base.png"),
    PartTarget("material_sets", "ball/huge", "ball/huge/variant_1/base.png"),
    PartTarget("material_sets", "gear/tiny", "gear/tiny/variant_1/base.png"),
    PartTarget("material_sets", "gear/huge", "gear/huge/variant_1/base.png"),
    PartTarget("material_sets", "ring/tiny", "ring/tiny/variant_1/base.png"),
    PartTarget("material_sets", "ring/huge", "ring/huge/variant_1/base.png"),
    PartTarget("material_sets", "rivet/tiny", "rivet/tiny/variant_1/base.png"),
    PartTarget("material_sets", "rivet/small", "rivet/small/variant_1/base.png"),
    PartTarget("material_sets", "rivet/large", "rivet/large/variant_1/base.png"),
    PartTarget("material_sets", "rivet/huge", "rivet/huge/variant_1/base.png"),
    PartTarget("material_sets", "rotor/tiny", "rotor/tiny/variant_1/base.png"),
    PartTarget("material_sets", "rotor/small", "rotor/small/variant_1/base.png"),
    PartTarget("material_sets", "rotor/normal", "rotor/normal/variant_1/base.png"),
    PartTarget("material_sets", "rotor/huge", "rotor/huge/variant_1/base.png"),
    PartTarget("material_sets", "plates/plate/large", "plates/plate/large/variant_1/base.png"),
    PartTarget("material_sets", "rod/very_short", "rod/very_short/variant_1/base.png"),
    PartTarget("material_sets", "rod/short", "rod/short/variant_1/base.png"),
    PartTarget("material_sets", "rod/normal", "rod/normal/variant_1/base.png"),
    PartTarget("material_sets", "ball/small", "ball/small/variant_1/base.png"),
    PartTarget("material_sets", "ball/normal", "ball/normal/variant_1/base.png"),
    PartTarget("material_sets", "gear/small", "gear/small/variant_1/base.png"),
    PartTarget("material_sets", "gear/normal", "gear/normal/variant_1/base.png"),
    PartTarget("material_sets", "gear/large", "gear/large/variant_1/base.png"),
    PartTarget("material_sets", "rivet/normal", "rivet/normal/variant_1/base.png"),
    PartTarget("material_sets", "ingots/ingot", "ingots/ingot/variant_1/base.png"),
    PartTarget("material_sets", "nuggets/nugget", "nuggets/nugget/variant_1/base.png"),
    PartTarget("material_sets", "plates/plate/normal", "plates/plate/normal/variant_1/base.png"),
    PartTarget("material_sets", "ring/small", "ring/small/variant_1/base.png"),
    PartTarget("material_sets", "ring/normal", "ring/normal/variant_1/base.png"),
    PartTarget("material_sets", "ring/large", "ring/large/variant_1/base.png"),
    PartTarget("material_sets", "rod/long", "rod/long/variant_1/base.png"),
    PartTarget("material_sets", "rod/very_long", "rod/very_long/variant_1/base.png"),
    PartTarget("material_sets", "rotor/large", "rotor/large/variant_1/base.png"),
    PartTarget("material_sets", "drill", "drill/variant_1/base.png"),
    PartTarget("tool", "axe/head", "axe/head/variant_1/head.png"),
    PartTarget("material_sets", "buzz_saw", "buzz_saw/variant_1/base.png"),
    PartTarget("tool", "chainsaw/head", "chainsaw/head/variant_1/base.png"),
    PartTarget("tool", "chisel/head", "chisel/head/variant_1/head.png"),
    PartTarget("tool", "crowbar/head", "crowbar/head/variant_1/head.png"),
    PartTarget("tool", "drill/head", "drill/head/variant_1/drill.png"),
    PartTarget("tool", "file/head", "file/head/variant_1/head.png"),
    PartTarget("tool", "hammer/head", "hammer/head/variant_1/head.png"),
    PartTarget("tool", "hoe/head", "hoe/head/variant_1/head.png"),
    PartTarget("tool", "pickaxe/head", "pickaxe/head/variant_1/head.png"),
    PartTarget("tool", "screwdriver/head", "screwdriver/head/variant_1/base.png"),
    PartTarget("tool", "shovel/head", "shovel/head/variant_1/head.png"),
    PartTarget("tool", "wirecutter/head", "wirecutter/head/variant_1/base.png"),
    PartTarget("tool", "wrench", "wrench/variant_1/wrench.png"),
    PartTarget("tool", "sword/head", "sword/head/variant_1/head.png"),
    PartTarget("tool", "knife/head", "knife/head/variant_1/head.png"),
    PartTarget("tool", "saw/head", "saw/head/variant_1/saw.png"),
    PartTarget("tool", "saw/handle", "saw/handle/variant_1/handle.png"),
    PartTarget("tool", "wirecutter/body", "wirecutter/body/variant_1/wire_cutter_base.png"),
    PartTarget("tool", "tool_handle", "tool_handle/variant_1/handle.png"),
)

def find_source(variant_dir: Path) -> Path | None:
    for name in SOURCE_NAMES:
        path = variant_dir / name
        if path.is_file():
            return path
    return None


def generate_mold(empty: Image.Image, item: Image.Image) -> Image.Image:
    if empty.size != item.size:
        raise ValueError(f"Texture size mismatch: empty={empty.size}, item={item.size}")

    empty = empty.convert("RGBA")
    item = item.convert("RGBA")
    out = empty.copy()
    width, height = empty.size

    overlap = {
        (x, y)
        for y in range(height)
        for x in range(width)
        if empty.getpixel((x, y))[3] > 0 and item.getpixel((x, y))[3] > 0
    }

    for x, y in overlap:
        boundary = any((x + dx, y + dy) not in overlap for dx, dy in NEIGHBORS_4)
        # The dark recess is one pixel INSIDE the item-shaped cavity.
        out.putpixel((x, y), DARK_RECESS if boundary else (0, 0, 0, 0))

    return out


def visible_bbox(image: Image.Image) -> tuple[int, int, int, int] | None:
    return image.getchannel("A").getbbox()


def heat_index(x: int, y: int, bbox: tuple[int, int, int, int], luminance: float) -> int:
    left, top, right, bottom = bbox
    width = max(1, right - left - 1)
    height = max(1, bottom - top - 1)
    nx = (x - left) / width
    ny = (y - top) / height
    diagonal = (nx + (1.0 - ny)) * 0.5
    detail = (luminance - 0.5) * 0.18
    jitter_table = (-0.055, 0.0, 0.035, -0.02, 0.05, -0.035, 0.015)
    jitter = jitter_table[(x * 3 + y * 5) % len(jitter_table)]
    heat = max(0.0, min(1.0, diagonal + detail + jitter))
    return max(0, min(9, round(heat * 9)))


def generate_hot_overlay(source: Image.Image) -> Image.Image:
    source = source.convert("RGBA")
    bbox = visible_bbox(source)
    overlay = Image.new("RGBA", source.size, (0, 0, 0, 0))
    if bbox is None:
        return overlay

    src_px = source.load()
    out_px = overlay.load()
    for y in range(source.height):
        for x in range(source.width):
            r, g, b, a = src_px[x, y]
            if a == 0:
                continue
            lum = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0
            pr, pg, pb, pa = HOT_PALETTE[heat_index(x, y, bbox, lum)]
            out_px[x, y] = (pr, pg, pb, round(pa * (a / 255.0)))
    return overlay


def _lerp(a: int, b: int, t: float) -> int:
    return round(a + (b - a) * t)


def generate_terracotta(source: Image.Image) -> Image.Image:
    """Create the finished raw terracotta texture from the item sprite.

    This deliberately outputs actual brown RGBA pixels. It is NOT a grayscale
    mask and is not intended to receive an in-game material tint.
    """
    source = source.convert("RGBA")
    output = Image.new("RGBA", source.size, (0, 0, 0, 0))
    src_px = source.load()
    out_px = output.load()

    span = TERRACOTTA_LUMINANCE_HIGH - TERRACOTTA_LUMINANCE_LOW
    for y in range(source.height):
        for x in range(source.width):
            r, g, b, a = src_px[x, y]
            if a == 0:
                continue
            lum = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0
            t = 0.0 if span <= 0 else (lum - TERRACOTTA_LUMINANCE_LOW) / span
            t = max(0.0, min(1.0, t))
            tr = _lerp(TERRACOTTA_DARK[0], TERRACOTTA_LIGHT[0], t)
            tg = _lerp(TERRACOTTA_DARK[1], TERRACOTTA_LIGHT[1], t)
            tb = _lerp(TERRACOTTA_DARK[2], TERRACOTTA_LIGHT[2], t)
            out_px[x, y] = (tr, tg, tb, a)
    return output


def resolve_root(argument: str | None) -> Path:
    """Resolve the textures/item root. Accept old material_sets paths for compatibility."""
    if argument:
        requested = Path(argument).resolve()
        return requested.parent if requested.name == "material_sets" else requested
    script_default = (
        Path(__file__).resolve().parent.parent
        / "resources/assets/industron/textures/item"
    )
    return script_default


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate Industron mold, hot-overlay and terracotta casting textures.")
    parser.add_argument("root", nargs="?", default=None, help="Path to textures/item (normally auto-detected)")
    parser.add_argument("--force", action="store_true", help="Regenerate every output type")
    parser.add_argument("--force-mold", action="store_true", help="Regenerate mold.png files")
    parser.add_argument("--force-hot", action="store_true", help="Regenerate hot_overlay.png files")
    parser.add_argument("--force-terracotta", action="store_true", help="Regenerate terracotta textures")
    parser.add_argument("--dry-run", action="store_true", help="Print work without writing files")
    args = parser.parse_args()

    root = resolve_root(args.root)
    if not root.is_dir():
        parser.error(f"textures/item directory not found: {root}")

    empty_path = root / "material_sets/empty_mold/variant_1/base.png"
    if not empty_path.is_file():
        raise FileNotFoundError(empty_path)
    empty = Image.open(empty_path).convert("RGBA")

    force_mold = args.force or args.force_mold
    force_hot = args.force or args.force_hot
    force_terracotta = args.force or args.force_terracotta

    counts = {"mold": 0, "hot": 0, "terracotta": 0, "missing": 0}

    for target in TARGETS:
        collection_root = root / target.collection
        variant1_path = collection_root / target.variant1_source
        if not variant1_path.is_file():
            print(f"SKIP missing source: {target.collection}/{target.variant1_source}")
            counts["missing"] += 1
            continue

        variant1 = Image.open(variant1_path).convert("RGBA")
        part_dir = collection_root / target.folder

        mold_path = part_dir / "mold.png"
        if force_mold or not mold_path.exists():
            print(f"{'WOULD ' if args.dry_run else ''}GENERATE mold: {mold_path.relative_to(root)}")
            if not args.dry_run:
                mold_path.parent.mkdir(parents=True, exist_ok=True)
                generate_mold(empty, variant1).save(mold_path)
            counts["mold"] += 1

        terracotta_path = part_dir / "terracotta.png"
        if force_terracotta or not terracotta_path.exists():
            print(f"{'WOULD ' if args.dry_run else ''}GENERATE terracotta: {terracotta_path.relative_to(root)}")
            if not args.dry_run:
                terracotta_path.parent.mkdir(parents=True, exist_ok=True)
                generate_terracotta(variant1).save(terracotta_path)
            counts["terracotta"] += 1

        for variant_dir in sorted(part_dir.glob("variant_*")):
            if not variant_dir.is_dir():
                continue
            source_path = find_source(variant_dir)
            if source_path is None:
                continue
            hot_path = variant_dir / "hot_overlay.png"
            if not force_hot and hot_path.exists():
                continue
            print(f"{'WOULD ' if args.dry_run else ''}GENERATE hot: {hot_path.relative_to(root)}")
            if not args.dry_run:
                generate_hot_overlay(Image.open(source_path)).save(hot_path)
            counts["hot"] += 1

    action = "would generate" if args.dry_run else "generated"
    print(
        f"done: {action} molds={counts['mold']}, hot_overlays={counts['hot']}, "
        f"terracotta={counts['terracotta']}; missing_variant1_sources={counts['missing']}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
