"""Fa l'atles d'icones de la pantalla de Pau i Treva (16x16 cadascuna, en una fila).

    python3 eines/icones.py

Escriu src/main/resources/assets/pauitreva/textures/gui/icones.png i, per veure-les grans,
build/icones-x8.png. L'ordre de la llista NOMS és el que fa servir el client (Icones.java):
si n'afegeixes una, posa-la al final.
"""
from pathlib import Path

from PIL import Image, ImageDraw

NOMS = ["corona", "membres", "territori", "capital", "guerra", "pau", "alianca", "pacte",
        "treva", "baixes", "vides", "diari", "diplomacia", "punts", "objectiu", "escut"]

OR = (212, 168, 75, 255)
OR_CLAR = (240, 213, 138, 255)
OR_FOSC = (150, 110, 40, 255)
ACER = (190, 198, 208, 255)
ACER_FOSC = (110, 120, 134, 255)
FUSTA = (122, 82, 48, 255)
PERGAMI = (230, 211, 163, 255)
PERGAMI_FOSC = (180, 154, 99, 255)
VERMELL = (192, 57, 43, 255)
VERMELL_FOSC = (123, 31, 23, 255)
BLAU = (61, 111, 181, 255)
VERD = (79, 143, 58, 255)
BLANC = (242, 234, 216, 255)
PEDRA = (150, 146, 138, 255)
PEDRA_FOSCA = (100, 96, 90, 255)
CONTORN = (22, 16, 12, 255)


def corona(d):
    d.polygon([(2, 12), (2, 5), (5, 8), (8, 3), (11, 8), (14, 5), (14, 12)], fill=OR)
    d.rectangle([2, 11, 14, 13], fill=OR_FOSC)
    d.point([(8, 7), (5, 10), (11, 10)], fill=VERMELL)
    d.point([(8, 10)], fill=BLAU)
    d.point([(2, 5), (8, 3), (14, 5)], fill=OR_CLAR)


def membres(d):
    d.ellipse([9, 2, 13, 6], fill=PERGAMI_FOSC)
    d.rectangle([8, 8, 14, 13], fill=VERMELL_FOSC)
    d.ellipse([3, 3, 7, 7], fill=PERGAMI)
    d.rectangle([2, 9, 9, 14], fill=BLAU)


def territori(d):
    d.rectangle([1, 3, 14, 13], fill=PERGAMI)
    for x in (5, 9, 13):
        d.line([(x, 3), (x, 13)], fill=PERGAMI_FOSC)
    for y in (6, 10):
        d.line([(1, y), (14, y)], fill=PERGAMI_FOSC)
    d.rectangle([6, 7, 8, 9], fill=VERD)
    d.line([(10, 2), (10, 8)], fill=FUSTA)
    d.polygon([(11, 2), (14, 3), (11, 5)], fill=VERMELL)


def capital(d):
    d.rectangle([4, 5, 11, 14], fill=PEDRA)
    for x in (4, 7, 10):
        d.rectangle([x, 3, x + 1, 5], fill=PEDRA)
    d.rectangle([6, 10, 9, 14], fill=CONTORN)
    d.rectangle([7, 7, 8, 8], fill=CONTORN)
    d.line([(4, 13), (11, 13)], fill=PEDRA_FOSCA)
    d.line([(8, 0), (8, 3)], fill=FUSTA)
    d.polygon([(9, 0), (12, 1), (9, 2)], fill=VERMELL)


def guerra(d):
    d.line([(2, 2), (11, 11)], fill=ACER, width=2)
    d.line([(13, 2), (4, 11)], fill=ACER, width=2)
    d.line([(2, 2), (4, 4)], fill=BLANC)
    d.line([(13, 2), (11, 4)], fill=BLANC)
    d.line([(9, 13), (13, 9)], fill=OR)
    d.line([(2, 9), (6, 13)], fill=OR)
    d.point([(13, 13), (2, 13)], fill=FUSTA)


def pau(d):
    d.line([(3, 1), (3, 14)], fill=FUSTA)
    d.rectangle([4, 2, 13, 8], fill=BLANC)
    d.line([(4, 8), (13, 8)], fill=PERGAMI_FOSC)
    d.point([(3, 0)], fill=OR)


def alianca(d):
    d.ellipse([1, 4, 9, 12], outline=OR, width=2)
    d.ellipse([6, 4, 14, 12], outline=OR_CLAR, width=2)


def pacte(d):
    d.rectangle([3, 3, 12, 12], fill=PERGAMI)
    d.rectangle([2, 2, 13, 4], fill=PERGAMI_FOSC)
    d.rectangle([2, 11, 13, 13], fill=PERGAMI_FOSC)
    for y in (6, 8):
        d.line([(5, y), (10, y)], fill=FUSTA)
    d.ellipse([9, 9, 12, 12], fill=VERMELL)


def treva(d):
    d.rectangle([3, 1, 12, 2], fill=FUSTA)
    d.rectangle([3, 13, 12, 14], fill=FUSTA)
    d.polygon([(4, 3), (11, 3), (8, 8), (7, 8)], fill=PERGAMI)
    d.polygon([(7, 8), (8, 8), (11, 12), (4, 12)], fill=PERGAMI)
    d.polygon([(6, 10), (9, 10), (10, 12), (5, 12)], fill=OR)
    d.point([(7, 8), (8, 9)], fill=OR)


def baixes(d):
    d.ellipse([3, 2, 12, 10], fill=BLANC)
    d.rectangle([5, 9, 10, 13], fill=BLANC)
    d.rectangle([5, 5, 6, 7], fill=CONTORN)
    d.rectangle([9, 5, 10, 7], fill=CONTORN)
    d.point([(7, 9), (8, 9)], fill=CONTORN)
    d.line([(6, 12), (9, 12)], fill=PEDRA_FOSCA)


def vides(d):
    d.ellipse([1, 2, 8, 9], fill=VERMELL)
    d.ellipse([7, 2, 14, 9], fill=VERMELL)
    d.polygon([(1, 6), (14, 6), (8, 14), (7, 14)], fill=VERMELL)
    d.point([(4, 4), (3, 5)], fill=BLANC)


def diari(d):
    d.polygon([(1, 3), (7, 4), (7, 14), (1, 13)], fill=PERGAMI)
    d.polygon([(8, 4), (14, 3), (14, 13), (8, 14)], fill=PERGAMI_FOSC)
    for y in (6, 8, 10):
        d.line([(2, y), (6, y + 1)], fill=FUSTA)
    d.line([(10, 1), (13, 9)], fill=BLANC)


def diplomacia(d):
    d.line([(7, 2), (7, 13)], fill=OR)
    d.line([(2, 4), (13, 4)], fill=OR)
    d.rectangle([5, 13, 10, 14], fill=OR_FOSC)
    d.polygon([(0, 9), (5, 9), (3, 11), (2, 11)], fill=OR_CLAR)
    d.polygon([(10, 9), (15, 9), (13, 11), (12, 11)], fill=OR_CLAR)
    d.line([(2, 4), (1, 9)], fill=OR_FOSC)
    d.line([(2, 4), (4, 9)], fill=OR_FOSC)
    d.line([(13, 4), (11, 9)], fill=OR_FOSC)
    d.line([(13, 4), (14, 9)], fill=OR_FOSC)


def punts(d):
    d.polygon([(8, 1), (10, 6), (15, 6), (11, 9), (13, 14), (8, 11), (3, 14), (5, 9), (1, 6), (6, 6)], fill=OR)
    d.polygon([(8, 3), (9, 6), (8, 9), (7, 6)], fill=OR_CLAR)


def objectiu(d):
    d.ellipse([1, 1, 14, 14], fill=BLANC)
    d.ellipse([3, 3, 12, 12], fill=VERMELL)
    d.ellipse([5, 5, 10, 10], fill=BLANC)
    d.ellipse([7, 7, 8, 8], fill=VERMELL)


def escut(d):
    d.polygon([(2, 1), (13, 1), (13, 8), (8, 14), (7, 14), (2, 8)], fill=OR)
    d.polygon([(4, 3), (11, 3), (11, 8), (8, 12), (7, 12), (4, 8)], fill=VERMELL)
    d.line([(7, 3), (7, 11)], fill=OR_CLAR)
    d.line([(8, 3), (8, 11)], fill=OR_CLAR)


DIBUIXOS = [corona, membres, territori, capital, guerra, pau, alianca, pacte,
            treva, baixes, vides, diari, diplomacia, punts, objectiu, escut]


def contorn(icona):
    """Un contorn fosc d'un píxel al voltant de tot el que no és transparent (com les icones de Paradox)."""
    px = icona.load()
    w, h = icona.size
    nou = icona.copy()
    pn = nou.load()
    for y in range(h):
        for x in range(w):
            if px[x, y][3] != 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and px[nx, ny][3] != 0:
                    pn[x, y] = CONTORN
                    break
    return nou


def main():
    arrel = Path(__file__).resolve().parent.parent
    atles = Image.new("RGBA", (16 * len(NOMS), 16), (0, 0, 0, 0))
    for i, dibuix in enumerate(DIBUIXOS):
        icona = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        dibuix(ImageDraw.Draw(icona))
        atles.paste(contorn(icona), (16 * i, 0))
    sortida = arrel / "src/main/resources/assets/pauitreva/textures/gui/icones.png"
    atles.save(sortida)
    gran = arrel / "build/icones-x8.png"
    gran.parent.mkdir(exist_ok=True)
    fons = Image.new("RGBA", atles.size, (42, 36, 30, 255))
    fons.alpha_composite(atles)
    fons.resize((atles.width * 8, atles.height * 8), Image.NEAREST).save(gran)
    print(sortida, len(NOMS), "icones")


if __name__ == "__main__":
    main()
