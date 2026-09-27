from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter


ROOT = Path(__file__).resolve().parents[1]
SCALE = 4
SIZE = 192 * SCALE


def gradient(size, top_left, bottom_right):
    image = Image.new("RGBA", (size, size))
    pixels = image.load()
    for y in range(size):
        for x in range(size):
            t = (0.62 * x + 0.38 * y) / (size - 1)
            pixels[x, y] = tuple(round(a + (b - a) * t) for a, b in zip(top_left, bottom_right)) + (255,)
    return image


def ellipse(draw, box, fill, outline=None, width=1):
    draw.ellipse(tuple(round(value * SCALE) for value in box), fill=fill, outline=outline, width=width * SCALE)


def make_icon():
    s = SIZE
    canvas = Image.new("RGBA", (s, s), (0, 0, 0, 0))

    # Glossy blue enamel base with a soft offset shadow and a bright upper edge.
    shadow = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    sd.rounded_rectangle((30, 34, s - 22, s - 18), radius=38 * SCALE, fill=(0, 18, 45, 115))
    shadow = shadow.filter(ImageFilter.GaussianBlur(7 * SCALE))
    canvas.alpha_composite(shadow)
    base = gradient(s, (63, 177, 250), (7, 55, 111))
    mask = Image.new("L", (s, s), 0)
    ImageDraw.Draw(mask).rounded_rectangle((8, 8, s - 8, s - 8), radius=36 * SCALE, fill=255)
    canvas.alpha_composite(Image.composite(base, Image.new("RGBA", (s, s)), mask))
    d = ImageDraw.Draw(canvas)
    d.rounded_rectangle((11, 11, s - 11, s - 11), radius=34 * SCALE, outline=(171, 226, 255, 210), width=2 * SCALE)
    d.rounded_rectangle((17, 17, s - 17, s - 17), radius=30 * SCALE, outline=(0, 55, 111, 120), width=2 * SCALE)

    # Broad translucent reflected highlight across the top of the enamel.
    gloss = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    gd = ImageDraw.Draw(gloss)
    gd.rounded_rectangle((17, 17, s - 17, int(s * .48)), radius=30 * SCALE, fill=(255, 255, 255, 67))
    gloss_mask = Image.new("L", (s, s), 0)
    ImageDraw.Draw(gloss_mask).rounded_rectangle((17, 17, s - 17, s - 17), radius=30 * SCALE, fill=255)
    gloss.putalpha(Image.composite(gloss.getchannel("A"), Image.new("L", (s, s), 0), gloss_mask))
    canvas.alpha_composite(gloss)

    # Raised record platter and ivory disc.
    platter_shadow = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    ellipse(ImageDraw.Draw(platter_shadow), (25, 27, 168, 170), (0, 20, 52, 130))
    platter_shadow = platter_shadow.filter(ImageFilter.GaussianBlur(3 * SCALE))
    canvas.alpha_composite(platter_shadow)
    d = ImageDraw.Draw(canvas)
    ellipse(d, (22, 21, 166, 165), (10, 48, 89, 255), (179, 225, 251, 255), 3)
    ellipse(d, (27, 26, 161, 160), (232, 241, 247, 255), (255, 255, 255, 255), 3)
    ellipse(d, (32, 31, 156, 155), (255, 255, 255, 255), (183, 200, 216, 255), 1)
    ellipse(d, (37, 36, 151, 150), None, (213, 222, 231, 255), 1)
    ellipse(d, (42, 41, 146, 145), None, (244, 248, 252, 255), 1)
    ellipse(d, (47, 46, 141, 140), None, (211, 222, 232, 255), 1)

    # Beveled cobalt center label.
    label_shadow = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    ellipse(ImageDraw.Draw(label_shadow), (59, 62, 132, 135), (0, 18, 50, 105))
    label_shadow = label_shadow.filter(ImageFilter.GaussianBlur(2 * SCALE))
    canvas.alpha_composite(label_shadow)
    d = ImageDraw.Draw(canvas)
    ellipse(d, (58, 57, 131, 130), (214, 230, 245, 255), (255, 255, 255, 255), 2)
    label = gradient(s, (56, 156, 239), (4, 54, 133))
    lm = Image.new("L", (s, s), 0)
    ImageDraw.Draw(lm).ellipse((61 * SCALE, 60 * SCALE, 128 * SCALE, 127 * SCALE), fill=255)
    canvas.alpha_composite(Image.composite(label, Image.new("RGBA", (s, s)), lm))
    d = ImageDraw.Draw(canvas)
    ellipse(d, (62, 61, 127, 126), None, (130, 204, 255, 230), 1)

    # Raised white eighth note with a subtle blue cast shadow.
    note = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    nd = ImageDraw.Draw(note)
    nd.ellipse((78 * SCALE, 96 * SCALE, 98 * SCALE, 110 * SCALE), fill=(0, 31, 84, 100))
    nd.rounded_rectangle((97 * SCALE, 72 * SCALE, 102 * SCALE, 101 * SCALE), radius=2 * SCALE, fill=(0, 31, 84, 100))
    nd.polygon([(100 * SCALE, 73 * SCALE), (115 * SCALE, 78 * SCALE), (117 * SCALE, 85 * SCALE), (103 * SCALE, 81 * SCALE)], fill=(0, 31, 84, 100))
    note = note.filter(ImageFilter.GaussianBlur(1 * SCALE))
    canvas.alpha_composite(note)
    d = ImageDraw.Draw(canvas)
    d.ellipse((75 * SCALE, 92 * SCALE, 95 * SCALE, 106 * SCALE), fill=(255, 255, 255, 255))
    d.rounded_rectangle((94 * SCALE, 68 * SCALE, 99 * SCALE, 97 * SCALE), radius=2 * SCALE, fill=(255, 255, 255, 255))
    d.polygon([(97 * SCALE, 69 * SCALE), (114 * SCALE, 74 * SCALE), (116 * SCALE, 81 * SCALE), (100 * SCALE, 77 * SCALE)], fill=(255, 255, 255, 255))
    d.line((95 * SCALE, 70 * SCALE, 95 * SCALE, 94 * SCALE), fill=(255, 255, 255, 255), width=1 * SCALE)

    # Small specular glint on the disc rim.
    d.arc((26 * SCALE, 25 * SCALE, 162 * SCALE, 161 * SCALE), 202, 278, fill=(255, 255, 255, 210), width=2 * SCALE)

    return canvas.resize((192, 192), Image.Resampling.LANCZOS)


def main():
    icon = make_icon()
    densities = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for density, pixels in densities.items():
        destination = ROOT / "app" / "src" / "legacy" / "res" / f"mipmap-{density}" / "ic_launcher_classic.png"
        destination.parent.mkdir(parents=True, exist_ok=True)
        icon.resize((pixels, pixels), Image.Resampling.LANCZOS).save(destination, optimize=True)
        print(destination)


if __name__ == "__main__":
    main()
