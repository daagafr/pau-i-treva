"""Fa l'atles d'icones de la pantalla de Pau i Treva: 32x32 cadascuna, en una fila.

    python3 eines/icones.py

El client les dibuixa en 16x16 unitats de GUI: a l'escala de GUI 2 (la més habitual) es veuen
píxel a píxel. Escriu src/main/resources/assets/pauitreva/textures/gui/icones.png i, per veure-les
grans, build/icones-x4.png. L'ordre de NOMS és el que fa servir el client (Estil.java): si
n'afegeixes una, posa-la al final.

Cada icona és una pila de capes (una forma i un material). L'ombrejat es fa sol: cada material té
cinc tons, la llum ve de dalt a l'esquerra (vores clares a dalt i a l'esquerra, fosques a baix i a
la dreta, i un degradat suau de dalt a baix), i al final hi ha un contorn fosc al voltant de tot.
"""
from pathlib import Path

from PIL import Image, ImageDraw

M = 32  # mida d'una icona

NOMS = ["corona", "membres", "territori", "capital", "guerra", "pau", "alianca", "pacte",
        "treva", "baixes", "vides", "diari", "diplomacia", "punts", "objectiu", "escut"]


def hexa(s):
    return tuple(int(s[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


# Gammes de cinc tons, de fosc a clar.
OR = [hexa(c) for c in ("#5a3d12", "#8f6420", "#c4922f", "#e8bf55", "#fbe7a1")]
ACER = [hexa(c) for c in ("#2e3540", "#56606e", "#8c97a5", "#c3ccd6", "#f2f5f8")]
VERMELL = [hexa(c) for c in ("#4a120d", "#7d1f16", "#b3362a", "#d9574a", "#f08a7c")]
BLAU = [hexa(c) for c in ("#14284a", "#234780", "#3b6ab8", "#6a95d8", "#a9c6ef")]
VERD = [hexa(c) for c in ("#1d3a15", "#2f5a22", "#4f8f3a", "#78b85a", "#aee08e")]
PERGAMI = [hexa(c) for c in ("#6e5530", "#a68a58", "#d2b98a", "#ebd9ae", "#fbf1d8")]
FUSTA = [hexa(c) for c in ("#2e1d0e", "#4f3319", "#74502a", "#97703f", "#b89262")]
OS = [hexa(c) for c in ("#7d7466", "#a89f8f", "#d2cbbd", "#ece7dc", "#ffffff")]
PEDRA = [hexa(c) for c in ("#3d3a35", "#5f5b54", "#85807a", "#aaa59d", "#cfcac2")]
PELL = [hexa(c) for c in ("#6b4428", "#9c6a43", "#c99468", "#e6b88e", "#f6d9b8")]
VIDRE = [hexa(c) for c in ("#4d5d66", "#7d929c", "#a9bcc4", "#cfdde2", "#eef6f8")]
FOSC = [hexa("#1c140d")] * 5          # detalls plans: forats, línies de text...
TINTA = [hexa("#4f3319")] * 5
CONTORN = hexa("#140e09")


class Icona:
    """Una pila de capes: cadascuna és una màscara (dibuixada amb PIL) i un material."""

    def __init__(self):
        self.capes = []

    def capa(self, material, dibuix, pla=False):
        mascara = Image.new("L", (M, M), 0)
        dibuix(ImageDraw.Draw(mascara))
        self.capes.append((mascara, material, pla))
        return self

    def pintar(self):
        # De qui és cada píxel (la capa de més amunt guanya).
        amo = [[-1] * M for _ in range(M)]
        for i, (mascara, _, _) in enumerate(self.capes):
            px = mascara.load()
            for y in range(M):
                for x in range(M):
                    if px[x, y] > 127:
                        amo[y][x] = i
        # Caixa de cada capa, per al degradat vertical.
        caixes = {}
        for y in range(M):
            for x in range(M):
                i = amo[y][x]
                if i >= 0:
                    a, b = caixes.get(i, (y, y))
                    caixes[i] = (min(a, y), max(b, y))
        img = Image.new("RGBA", (M, M), (0, 0, 0, 0))
        out = img.load()

        def de(x, y, i):
            return 0 <= x < M and 0 <= y < M and amo[y][x] == i

        for y in range(M):
            for x in range(M):
                i = amo[y][x]
                if i < 0:
                    continue
                _, material, pla = self.capes[i]
                if pla:
                    out[x, y] = material[2]
                    continue
                dalt, baix = caixes[i]
                rel = (y - dalt) / max(1, baix - dalt)
                t = 3 if rel < 0.3 else 2 if rel < 0.75 else 1
                llum = not de(x, y - 1, i) or not de(x - 1, y, i)
                ombra = not de(x, y + 1, i) or not de(x + 1, y, i)
                if llum and not ombra:
                    t += 1
                    if not de(x - 1, y - 1, i) and not de(x, y - 1, i) and not de(x - 1, y, i):
                        t = 4
                elif ombra and not llum:
                    t -= 1
                out[x, y] = material[max(0, min(4, t))]
        # Contorn fosc al voltant de tot (també en diagonal, perquè quedi tancat).
        res = img.copy()
        r = res.load()
        for y in range(M):
            for x in range(M):
                if out[x, y][3]:
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < M and 0 <= ny < M and out[nx, ny][3]:
                        r[x, y] = CONTORN
                        break
        return res


def mirall(punts):
    return [(M - 1 - x, y) for x, y in punts]


# ---------------------------------------------------------------- Les icones

def corona():
    ic = Icona()
    ic.capa(OR, lambda d: d.polygon([(4, 23), (4, 9), (10, 16), (15, 6), (16, 6), (21, 16), (27, 9), (27, 23)], fill=255))
    ic.capa(OR, lambda d: d.rectangle([3, 21, 28, 27], fill=255))
    for bola in ((2, 6, 6, 10), (13, 2, 18, 7), (25, 6, 29, 10)):
        ic.capa(OR, lambda d, b=bola: d.ellipse(b, fill=255))
    ic.capa(VERMELL, lambda d: d.ellipse([13, 21, 18, 26], fill=255))
    ic.capa(BLAU, lambda d: d.ellipse([6, 22, 9, 25], fill=255))
    ic.capa(BLAU, lambda d: d.ellipse([22, 22, 25, 25], fill=255))
    ic.capa(VERD, lambda d: d.ellipse([14, 13, 17, 16], fill=255))
    return ic


def membres():
    ic = Icona()
    ic.capa(VERMELL, lambda d: d.rounded_rectangle([15, 15, 29, 30], radius=5, fill=255))
    ic.capa(PELL, lambda d: d.ellipse([17, 4, 26, 13], fill=255))
    ic.capa(FUSTA, lambda d: d.chord([17, 3, 26, 11], 180, 360, fill=255))
    ic.capa(BLAU, lambda d: d.rounded_rectangle([2, 18, 18, 31], radius=6, fill=255))
    ic.capa(PELL, lambda d: d.ellipse([5, 7, 15, 17], fill=255))
    ic.capa(OR, lambda d: d.chord([5, 6, 15, 14], 180, 360, fill=255))
    ic.capa(OR, lambda d: d.rectangle([9, 22, 11, 27], fill=255))
    return ic


def territori():
    ic = Icona()
    ic.capa(PERGAMI, lambda d: d.polygon([(2, 8), (10, 5), (20, 9), (29, 6), (29, 26), (20, 29), (10, 25), (2, 28)], fill=255))
    ic.capa(VERD, lambda d: d.polygon([(5, 13), (9, 10), (14, 12), (16, 18), (11, 22), (6, 20)], fill=255))
    ic.capa(BLAU, lambda d: d.polygon([(20, 18), (26, 15), (27, 22), (22, 24)], fill=255))
    ic.capa(PERGAMI, lambda d: d.line([(10, 5), (10, 25)], fill=255), pla=True)
    ic.capa(PERGAMI, lambda d: d.line([(20, 9), (20, 29)], fill=255), pla=True)
    for p in ((13, 23), (16, 21), (19, 19), (22, 17)):
        ic.capa(VERMELL, lambda d, p=p: d.point([p], fill=255), pla=True)
    ic.capa(FUSTA, lambda d: d.rectangle([23, 2, 24, 13], fill=255))
    ic.capa(VERMELL, lambda d: d.polygon([(25, 2), (31, 4), (25, 7)], fill=255))
    return ic


def capital():
    ic = Icona()
    ic.capa(PEDRA, lambda d: d.rectangle([3, 15, 9, 30], fill=255))
    ic.capa(PEDRA, lambda d: d.rectangle([22, 15, 28, 30], fill=255))
    for x in (3, 7, 22, 26):
        ic.capa(PEDRA, lambda d, x=x: d.rectangle([x, 12, x + 2, 15], fill=255))
    ic.capa(PEDRA, lambda d: d.rectangle([9, 9, 22, 30], fill=255))
    for x in (9, 14, 19):
        ic.capa(PEDRA, lambda d, x=x: d.rectangle([x, 6, x + 3, 9], fill=255))
    ic.capa(FUSTA, lambda d: d.chord([12, 20, 19, 27], 180, 360, fill=255))
    ic.capa(FUSTA, lambda d: d.rectangle([12, 24, 19, 30], fill=255))
    ic.capa(FOSC, lambda d: d.rectangle([15, 13, 16, 16], fill=255), pla=True)
    ic.capa(FOSC, lambda d: d.rectangle([5, 19, 6, 21], fill=255), pla=True)
    ic.capa(FOSC, lambda d: d.rectangle([24, 19, 25, 21], fill=255), pla=True)
    ic.capa(FUSTA, lambda d: d.rectangle([15, 0, 16, 6], fill=255))
    ic.capa(VERMELL, lambda d: d.polygon([(17, 0), (24, 2), (17, 5)], fill=255))
    return ic


def espasa(ic, inverteix):
    def p(pts):
        return mirall(pts) if inverteix else pts
    # Fulla de la punta (dalt) fins a la guarda; es creuen al mig de la icona.
    ic.capa(ACER, lambda d: d.polygon(p([(1, 1), (5, 2), (24, 21), (21, 24), (2, 5)]), fill=255))
    ic.capa(ACER, lambda d: d.line(p([(4, 4), (21, 21)]), fill=255), pla=True)  # el nervi de la fulla
    ic.capa(OR, lambda d: d.line(p([(18, 28), (28, 18)]), fill=255, width=3))
    ic.capa(FUSTA, lambda d: d.line(p([(24, 24), (27, 27)]), fill=255, width=3))
    ic.capa(OR, lambda d: d.ellipse([26, 26, 30, 30] if not inverteix else [M - 1 - 30, 26, M - 1 - 26, 30], fill=255))


def guerra():
    ic = Icona()
    espasa(ic, True)
    espasa(ic, False)
    return ic


def pau():
    ic = Icona()
    ic.capa(OS, lambda d: d.polygon([(8, 4), (14, 6), (20, 3), (29, 5), (29, 19), (20, 17), (14, 20), (8, 18)], fill=255))
    ic.capa(FUSTA, lambda d: d.rectangle([5, 4, 7, 31], fill=255))
    ic.capa(OR, lambda d: d.ellipse([4, 0, 8, 4], fill=255))
    ic.capa(VERD, lambda d: d.line([(13, 12), (22, 10)], fill=255), pla=True)
    for f in ((15, 9, 17, 11), (18, 12, 20, 14), (20, 8, 22, 10)):
        ic.capa(VERD, lambda d, f=f: d.ellipse(f, fill=255))
    return ic


def alianca():
    ic = Icona()
    ic.capa(OR, lambda d: d.ellipse([2, 7, 19, 24], outline=255, width=4))
    ic.capa(OR, lambda d: d.ellipse([12, 7, 29, 24], outline=255, width=4))
    # L'anella de l'esquerra passa per sobre a la creu de dalt: entrellaçades.
    ic.capa(OR, lambda d: d.arc([2, 7, 19, 24], 300, 345, fill=255, width=4))
    return ic


def pacte():
    ic = Icona()
    ic.capa(PERGAMI, lambda d: d.rectangle([7, 5, 25, 27], fill=255))
    ic.capa(PERGAMI, lambda d: d.rounded_rectangle([4, 2, 28, 7], radius=2, fill=255))
    ic.capa(PERGAMI, lambda d: d.rounded_rectangle([4, 25, 28, 30], radius=2, fill=255))
    for y in (10, 13, 16, 19):
        ic.capa(TINTA, lambda d, y=y: d.line([(10, y), (22 if y < 19 else 15, y)], fill=255), pla=True)
    ic.capa(VERMELL, lambda d: d.polygon([(19, 24), (21, 31), (23, 27), (25, 31), (26, 24)], fill=255))
    ic.capa(VERMELL, lambda d: d.ellipse([17, 17, 27, 27], fill=255))
    ic.capa(VERMELL, lambda d: d.ellipse([20, 20, 24, 24], outline=255), pla=True)
    return ic


def treva():
    ic = Icona()
    ic.capa(VIDRE, lambda d: d.polygon([(9, 6), (23, 6), (17, 15), (15, 15)], fill=255))
    ic.capa(VIDRE, lambda d: d.polygon([(15, 17), (17, 17), (23, 26), (9, 26)], fill=255))
    ic.capa(VIDRE, lambda d: d.rectangle([15, 14, 16, 17], fill=255))
    ic.capa(OR, lambda d: d.polygon([(12, 9), (20, 9), (16, 14)], fill=255))
    ic.capa(OR, lambda d: d.line([(16, 14), (16, 22)], fill=255), pla=True)
    ic.capa(OR, lambda d: d.polygon([(10, 26), (22, 26), (18, 22), (14, 22)], fill=255))
    ic.capa(FUSTA, lambda d: d.rectangle([6, 6, 7, 26], fill=255))
    ic.capa(FUSTA, lambda d: d.rectangle([24, 6, 25, 26], fill=255))
    ic.capa(FUSTA, lambda d: d.rounded_rectangle([4, 2, 27, 6], radius=1, fill=255))
    ic.capa(FUSTA, lambda d: d.rounded_rectangle([4, 26, 27, 30], radius=1, fill=255))
    return ic


def baixes():
    ic = Icona()
    ic.capa(OS, lambda d: d.line([(3, 26), (28, 31)], fill=255, width=3))
    ic.capa(OS, lambda d: d.line([(3, 31), (28, 26)], fill=255, width=3))
    ic.capa(OS, lambda d: d.ellipse([6, 2, 25, 22], fill=255))
    ic.capa(OS, lambda d: d.rounded_rectangle([10, 17, 21, 27], radius=2, fill=255))
    ic.capa(FOSC, lambda d: d.ellipse([9, 10, 14, 16], fill=255), pla=True)
    ic.capa(FOSC, lambda d: d.ellipse([17, 10, 22, 16], fill=255), pla=True)
    ic.capa(FOSC, lambda d: d.polygon([(15, 17), (16, 17), (17, 20), (14, 20)], fill=255), pla=True)
    for x in (12, 15, 18):
        ic.capa(OS, lambda d, x=x: d.line([(x, 23), (x, 26)], fill=255), pla=True)
    return ic


def vides():
    ic = Icona()
    ic.capa(VERMELL, lambda d: (d.ellipse([2, 4, 17, 19], fill=255), d.ellipse([14, 4, 29, 19], fill=255),
                                d.polygon([(3, 14), (28, 14), (16, 29), (15, 29)], fill=255)))
    ic.capa(VERMELL, lambda d: d.ellipse([6, 7, 10, 11], fill=255))  # brillantor
    return ic


def diari():
    ic = Icona()
    ic.capa(VERMELL, lambda d: d.polygon([(1, 9), (15, 11), (30, 9), (30, 29), (15, 31), (1, 29)], fill=255))
    ic.capa(PERGAMI, lambda d: d.polygon([(2, 7), (15, 9), (15, 28), (2, 26)], fill=255))
    ic.capa(PERGAMI, lambda d: d.polygon([(16, 9), (29, 7), (29, 26), (16, 28)], fill=255))
    for y in (13, 16, 19, 22):
        ic.capa(TINTA, lambda d, y=y: d.line([(4, y), (13, y + 1)], fill=255), pla=True)
    ic.capa(OS, lambda d: d.polygon([(30, 0), (24, 9), (21, 17), (22, 18), (26, 12), (31, 3)], fill=255))
    ic.capa(FOSC, lambda d: d.line([(21, 17), (19, 21)], fill=255), pla=True)
    return ic


def diplomacia():
    ic = Icona()
    ic.capa(OR, lambda d: d.line([(4, 9), (9, 19)], fill=255))
    ic.capa(OR, lambda d: d.line([(4, 9), (0, 19)], fill=255))
    ic.capa(OR, lambda d: d.line([(27, 9), (22, 19)], fill=255))
    ic.capa(OR, lambda d: d.line([(27, 9), (31, 19)], fill=255))
    ic.capa(OR, lambda d: d.rectangle([14, 5, 17, 27], fill=255))
    ic.capa(OR, lambda d: d.rounded_rectangle([2, 7, 29, 10], radius=1, fill=255))
    ic.capa(OR, lambda d: d.ellipse([13, 1, 18, 6], fill=255))
    ic.capa(OR, lambda d: d.pieslice([0, 14, 10, 24], 0, 180, fill=255))
    ic.capa(OR, lambda d: d.pieslice([21, 14, 31, 24], 0, 180, fill=255))
    ic.capa(OR, lambda d: d.rounded_rectangle([9, 26, 22, 30], radius=1, fill=255))
    return ic


def punts():
    ic = Icona()
    ic.capa(OR, lambda d: d.polygon([(16, 1), (20, 11), (31, 12), (22, 19), (25, 30), (16, 24), (7, 30), (10, 19),
                                     (1, 12), (12, 11)], fill=255))
    ic.capa(OR, lambda d: d.polygon([(16, 6), (18, 13), (16, 19), (14, 13)], fill=255))
    return ic


def objectiu():
    ic = Icona()
    ic.capa(OS, lambda d: d.ellipse([1, 4, 26, 29], fill=255))
    ic.capa(VERMELL, lambda d: d.ellipse([5, 8, 22, 25], fill=255))
    ic.capa(OS, lambda d: d.ellipse([9, 12, 18, 21], fill=255))
    ic.capa(VERMELL, lambda d: d.ellipse([12, 15, 15, 18], fill=255))
    ic.capa(FUSTA, lambda d: d.line([(14, 17), (27, 4)], fill=255, width=2))
    ic.capa(OS, lambda d: d.polygon([(24, 3), (28, 0), (31, 3), (27, 7)], fill=255))
    ic.capa(VERMELL, lambda d: d.line([(26, 2), (29, 5)], fill=255), pla=True)
    return ic


def escut():
    ic = Icona()
    forma = [(3, 2), (28, 2), (28, 16), (16, 30), (15, 30), (3, 16)]
    ic.capa(OR, lambda d: d.polygon(forma, fill=255))
    ic.capa(VERMELL, lambda d: d.polygon([(6, 5), (25, 5), (25, 16), (16, 26), (15, 26), (6, 16)], fill=255))
    ic.capa(OR, lambda d: d.rectangle([14, 5, 17, 25], fill=255))
    ic.capa(OR, lambda d: d.rectangle([6, 11, 25, 13], fill=255))
    return ic


DIBUIXOS = [corona, membres, territori, capital, guerra, pau, alianca, pacte,
            treva, baixes, vides, diari, diplomacia, punts, objectiu, escut]


def main():
    arrel = Path(__file__).resolve().parent.parent
    atles = Image.new("RGBA", (M * len(NOMS), M), (0, 0, 0, 0))
    for i, fer in enumerate(DIBUIXOS):
        atles.paste(fer().pintar(), (M * i, 0))
    sortida = arrel / "src/main/resources/assets/pauitreva/textures/gui/icones.png"
    atles.save(sortida)
    gran = arrel / "build/icones-x4.png"
    gran.parent.mkdir(exist_ok=True)
    fons = Image.new("RGBA", atles.size, (42, 36, 30, 255))
    fons.alpha_composite(atles)
    fons.resize((atles.width * 4, atles.height * 4), Image.NEAREST).save(gran)
    print(sortida, len(NOMS), "icones de", M, "px")


if __name__ == "__main__":
    main()
