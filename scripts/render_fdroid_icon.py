from pathlib import Path

import cairosvg


def main() -> int:
    svg = Path("docs/icon.svg")
    out = Path("fdroid/repo/icons/icon.png")
    out.parent.mkdir(parents=True, exist_ok=True)
    cairosvg.svg2png(url=str(svg), write_to=str(out), output_width=512, output_height=512)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
