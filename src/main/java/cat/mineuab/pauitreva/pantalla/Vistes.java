package cat.mineuab.pauitreva.pantalla;

import cat.mineuab.pauitreva.Config;
import cat.mineuab.pauitreva.guerra.Batalles;
import cat.mineuab.pauitreva.model.EstatRelacio;
import cat.mineuab.pauitreva.model.Esdeveniment;
import cat.mineuab.pauitreva.model.FinestresGuerra;
import cat.mineuab.pauitreva.model.Guerra;
import cat.mineuab.pauitreva.model.Pais;
import cat.mineuab.pauitreva.model.ParametresGuerra;
import cat.mineuab.pauitreva.model.Permis;
import cat.mineuab.pauitreva.model.Proposta;
import cat.mineuab.pauitreva.model.Rang;
import cat.mineuab.pauitreva.model.Registre;
import cat.mineuab.pauitreva.model.Termes;
import cat.mineuab.pauitreva.model.TipusObjectiu;
import cat.mineuab.pauitreva.model.TipusRelacio;
import cat.mineuab.pauitreva.model.Validacio;
import cat.mineuab.pauitreva.mon.Dades;
import cat.mineuab.pauitreva.opac.PontOpac;
import cat.mineuab.pauitreva.opac.ZonaOpac;
import cat.mineuab.pauitreva.ordres.Missatges;
import cat.mineuab.pauitreva.ordres.OrdresGuerra;
import cat.mineuab.pauitreva.pantalla.Pantalla.Boto;
import cat.mineuab.pauitreva.pantalla.Pantalla.Entrada;
import cat.mineuab.pauitreva.pantalla.Pantalla.Fila;
import cat.mineuab.pauitreva.pantalla.Pantalla.Peca;
import cat.mineuab.pauitreva.pantalla.Pantalla.Pestanya;
import cat.mineuab.pauitreva.pantalla.Pantalla.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import xaero.pac.common.parties.party.member.api.IPartyMemberAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

/**
 * Fa la pantalla del país per a un jugador: les pestanyes País, Membres, Diplomàcia, Guerres i Diari, i
 * algunes de confirmació que no surten a dalt. Tot el que hi ha es pot fer també amb ordres, i cada botó fa
 * l'ordre corresponent (així les regles i els permisos són els mateixos).
 */
public final class Vistes {

    public static final String PAIS = "pais";
    public static final String MEMBRES = "membres";
    public static final String DIPLOMACIA = "diplomacia";
    public static final String GUERRES = "guerres";
    public static final String DIARI = "diari";

    /** Colors per triar d'un clic (els mateixos que fa servir el mod en fundar). */
    private static final int[] COLORS = {
            0xC0392B, 0xD35400, 0xF1C40F, 0x27AE60, 0x16A085, 0x2980B9, 0x8E44AD, 0xE84393, 0x6D4C41, 0x2C3E50,
    };

    private Vistes() {
    }

    /** El que necessiten totes les pestanyes: qui mira i de quin país és. */
    private record Qui(ServerPlayer jugador, MinecraftServer server, Registre r, IServerPartyAPI partit, Pais pais,
                       Rang rang) {

        boolean pot(Permis p) {
            return pais != null && pais.pot(rang, p);
        }
    }

    public static Pantalla construir(ServerPlayer jugador, String pestanya, List<Component> avisos) {
        MinecraftServer server = jugador.server;
        Registre r = Dades.registre(server);
        IServerPartyAPI partit = PontOpac.partitDe(server, jugador.getUUID());
        Pais pais = partit == null ? null : r.pais(partit.getId()).orElse(null);
        Rang rang = partit == null ? null : PontOpac.rang(partit, jugador.getUUID());
        Qui q = new Qui(jugador, server, r, partit, pais, rang);

        List<Fila> f = new ArrayList<>();
        String activa = pestanya;
        if (pestanya.startsWith("declara:")) {
            declarar(q, f, pestanya.substring("declara:".length()));
            activa = GUERRES;
        } else if (pestanya.startsWith("rendir:")) {
            rendir(q, f, pestanya.substring("rendir:".length()));
            activa = GUERRES;
        } else {
            switch (pestanya) {
                case MEMBRES -> membres(q, f);
                case DIPLOMACIA -> diplomacia(q, f);
                case GUERRES -> guerres(q, f);
                case DIARI -> diari(q, f);
                case "dissol" -> {
                    dissoldre(q, f);
                    activa = PAIS;
                }
                default -> {
                    pais(q, f);
                    activa = PAIS;
                }
            }
        }
        List<Pestanya> pestanyes = List.of(
                new Pestanya(PAIS, Component.literal("País"), "escut"),
                new Pestanya(MEMBRES, Component.literal("Membres"), "membres"),
                new Pestanya(DIPLOMACIA, Component.literal("Diplomàcia"), "diplomacia"),
                new Pestanya(GUERRES, Component.literal("Guerres" + (r.guerres().isEmpty() ? "" : " (" + r.guerres().size() + ")")),
                        "guerra"),
                new Pestanya(DIARI, Component.literal("Diari"), "diari"));
        return new Pantalla(capcalera(q), pestanyes, activa, avisos, f);
    }

    /** A dalt de tot: l'escut i el nom del país, el lema i uns xips amb el més important. */
    private static Pantalla.Capcalera capcalera(Qui q) {
        if (q.pais == null) {
            return new Pantalla.Capcalera(0x8C6F35, "", Component.literal("Pau i Treva"),
                    Component.literal("No ets de cap país").withStyle(s -> s.withColor(GRIS)),
                    List.of(icona("escut", q.r.paisos().size() + " països", "Països que hi ha al servidor"),
                            icona("guerra", q.r.guerres().size() + " guerres", "Guerres obertes ara")));
        }
        Pais p = q.pais;
        long ara = System.currentTimeMillis();
        List<Peca> xips = new ArrayList<>();
        xips.add(icona("corona", q.partit == null ? "?" : PontOpac.nomLider(q.partit), p.nomRang(Rang.LIDER)));
        xips.add(icona("membres", String.valueOf(membres(q.server, p)), "Membres"));
        int chunks = q.partit == null ? 0 : ZonaOpac.territori(q.server, q.partit).size();
        xips.add(icona("territori", String.valueOf(chunks), "Chunks de territori"));
        xips.add(icona("capital", p.capital() == null ? "—" : p.capital().chunkX() + ", " + p.capital().chunkZ(),
                p.capital() == null ? "Sense capital: a la guerra, la del mig del territori" : "Capital (chunk)"));
        int guerres = q.r.guerresDe(p.id()).size();
        if (guerres > 0) {
            xips.add(new Pantalla.Icona("guerra", Component.literal(String.valueOf(guerres)).withStyle(s -> s.withColor(to(ChatFormatting.RED))),
                    Component.literal("Guerres obertes")));
        }
        if (p.humiliat(ara)) {
            xips.add(new Pantalla.Icona("baixes", Component.literal("Humiliat").withStyle(s -> s.withColor(to(ChatFormatting.RED))),
                    Component.literal("Fins al " + Missatges.data(p.humiliatFins()))));
        }
        Component subtitol = p.lema().isEmpty()
                ? Component.literal("Fundat el " + Missatges.dia(p.fundat())).withStyle(s -> s.withColor(GRIS))
                : Component.literal("«" + p.lema() + "»").withStyle(s -> s.withColor(GRIS).withItalic(true));
        return new Pantalla.Capcalera(p.color(), p.sigla(), Component.literal(p.nom()), subtitol, xips);
    }

    // ---------------------------------------------------------------- Peces

    private static Text text(Component c) {
        return new Text(c);
    }

    private static Text text(String s) {
        return new Text(Component.literal(s));
    }

    /** Un gris càlid, que lliga amb el fons de la pantalla. */
    static final int GRIS = 0xA8997C;

    private static Text gris(String s) {
        return new Text(Component.literal(s).withStyle(st -> st.withColor(GRIS)));
    }

    private static Fila titol(String s) {
        return titol(s, "");
    }

    private static Fila titol(String s, String icona) {
        return Fila.de(new Pantalla.Seccio(Component.literal(s), icona));
    }

    private static Pantalla.Icona icona(String icona, String text, String ajuda) {
        return new Pantalla.Icona(icona, Component.literal(text), Component.literal(ajuda));
    }

    private static Pantalla.Escut escut(Pais p) {
        return new Pantalla.Escut(p.color(), p.sigla(), Component.literal(Registre.etiqueta(p)));
    }

    /** El país amb el seu escut, per a les llistes. */
    private static List<Peca> ambEscut(Pais p) {
        return new ArrayList<>(List.of(escut(p), text(Missatges.pais(p))));
    }

    private static Boto boto(String text, String accio) {
        return new Boto(Component.literal(text), accio, Component.empty());
    }

    private static Boto boto(String text, String accio, ChatFormatting color, String ajuda) {
        return new Boto(Component.literal(text).withStyle(s -> s.withColor(to(color))), accio, Component.literal(ajuda));
    }

    /** Els colors de Minecraft, passats a tons que lliguen amb la fusta i l'or de la pantalla. */
    private static int to(ChatFormatting c) {
        return switch (c) {
            case GREEN -> 0x8FC86A;
            case AQUA -> 0x7FBCCB;
            case RED -> 0xE5705A;
            case YELLOW, GOLD -> 0xE8C977;
            case GRAY -> GRIS;
            default -> c.getColor() == null ? 0xE8DCC0 : c.getColor();
        };
    }

    private static Boto desactivat(String text, String perque) {
        return new Boto(Component.literal(text), "", Component.literal(perque));
    }

    private static Entrada entrada(String id, String valor, int ample, int max) {
        return new Entrada(id, valor, ample, max, Component.empty());
    }


    // ---------------------------------------------------------------- País

    private static void pais(Qui q, List<Fila> f) {
        if (q.pais == null) {
            f.add(titol("No ets de cap país", "escut"));
            if (q.partit != null && q.rang != Rang.LIDER) {
                f.add(Fila.de(gris("Ets d'una party d'OPAC i no n'ets el propietari: només el propietari en pot fer un país.")));
            } else {
                f.add(Fila.de(gris("Funda'n un: la sigla té de 2 a 4 lletres o xifres, i el nom, de 3 a 32 caràcters."
                        + (q.partit != null ? " La teva party d'OPAC passarà a ser el país." : ""))));
                f.add(Fila.de(text("Sigla"), entrada("sigla", "", 44, 4), text("Nom"), entrada("nom", "", 150, 32),
                        boto("Funda el país", "/nacio funda {sigla} {nom}", ChatFormatting.GREEN, "")));
            }
            f.add(Fila.de(gris("O entra a la party d'OPAC d'un país que ja existeixi (que et convidin amb /openpac-parties).")));
            f.add(titol("Països (" + q.r.paisos().size() + ")", "escut"));
            for (Pais p : q.r.paisos()) {
                List<Peca> fila = ambEscut(p);
                fila.add(gris(" · " + membres(q.server, p) + " membres"));
                f.add(new Fila(fila));
            }
            return;
        }
        Pais p = q.pais;
        f.add(titol("El país", "escut"));
        f.add(Fila.de(icona("corona", "El teu rang: " + (q.rang == null ? "?" : p.nomRang(q.rang)), "Ho decideix OPAC"),
                icona("diari", "Fundat el " + Missatges.dia(p.fundat()), "")));
        String capital = p.capital() == null ? "Sense capital (a la guerra, la del mig del territori)"
                : "Capital al chunk " + p.capital().chunkX() + ", " + p.capital().chunkZ();
        f.add(Fila.de(icona("capital", capital, p.capital() == null ? "" : p.capital().dimensio()),
                icona("pau", p.bandera().isEmpty() ? "Sense bandera" : "Bandera: " + p.bandera(), "")));
        List<Guerra> guerres = q.r.guerresDe(p.id());
        if (!guerres.isEmpty()) {
            f.add(Fila.de(new Pantalla.Icona("guerra", Component.literal("En guerra amb " + guerres.size()
                    + (guerres.size() == 1 ? " país" : " països")).withStyle(s -> s.withColor(to(ChatFormatting.RED))), Component.empty()),
                    boto("Mira-ho", "pestanya:" + GUERRES)));
        }
        if (!q.pot(Permis.EDITAR)) {
            f.add(Fila.de(gris("Per canviar les dades del país cal ser " + p.nomRang(p.rangPer(Permis.EDITAR)) + " o més.")));
            return;
        }
        f.add(titol("Canviar les dades", "pacte"));
        f.add(Fila.de(text("Nom"), entrada("nom", p.nom(), 160, Validacio.NOM_MAX), boto("Desa", "/nacio nom {nom}")));
        f.add(Fila.de(text("Sigla"), entrada("sigla", p.sigla(), 44, 4), boto("Desa", "/nacio sigla {sigla}")));
        List<Peca> colors = new ArrayList<>();
        colors.add(text("Color"));
        colors.add(entrada("color", Validacio.colorText(p.color()), 64, 7));
        colors.add(boto("Desa", "/nacio color {color}"));
        for (int c : COLORS) {
            colors.add(new Boto(Component.literal("■").withStyle(s -> s.withColor(TextColor.fromRgb(c))),
                    "/nacio color " + Validacio.colorText(c), Component.literal(Validacio.colorText(c))));
        }
        f.add(new Fila(colors));
        f.add(Fila.de(text("Lema"), entrada("lema", p.lema(), 220, Validacio.LEMA_MAX), boto("Desa", "/nacio lema {lema}")));
        f.add(Fila.de(text("Bandera"), entrada("bandera", p.bandera(), 160, 64), boto("Desa", "/nacio bandera {bandera}")));
        f.add(Fila.de(boto("Posa la capital on soc", "/nacio capital", ChatFormatting.YELLOW,
                "El chunk on ets ara, que ha de ser del país.")));
        if (q.rang == Rang.LIDER) {
            f.add(Fila.de(boto("Dissol el país…", "pestanya:dissol", ChatFormatting.RED,
                    "La party d'OPAC i els claims es queden.")));
        }
    }

    private static void dissoldre(Qui q, List<Fila> f) {
        if (q.pais == null || q.rang != Rang.LIDER) {
            pais(q, f);
            return;
        }
        f.add(titol("Dissoldre " + Registre.etiqueta(q.pais) + "?", "baixes"));
        f.add(Fila.de(gris("El país desapareix, amb les aliances i els pactes. Les guerres s'acaben en blanc. "
                + "La party d'OPAC i els claims es queden com estan.")));
        f.add(Fila.de(boto("Sí, dissol-lo", "/nacio dissol confirmo", ChatFormatting.RED, ""),
                boto("No", "pestanya:" + PAIS)));
    }

    private static int membres(MinecraftServer server, Pais p) {
        IServerPartyAPI partit = PontOpac.partit(server, p.id());
        return partit == null ? 0 : partit.getMemberCount();
    }

    // ---------------------------------------------------------------- Membres

    private static void membres(Qui q, List<Fila> f) {
        if (q.pais == null || q.partit == null) {
            f.add(Fila.de(gris("No ets de cap país.")));
            return;
        }
        Pais p = q.pais;
        f.add(titol("Membres (" + q.partit.getMemberCount() + ")", "membres"));
        for (IPartyMemberAPI m : PontOpac.membres(q.partit)) {
            Rang rang = PontOpac.rang(q.partit, m.getUUID());
            boolean connectat = q.server.getPlayerList().getPlayer(m.getUUID()) != null;
            String icona = rang == Rang.LIDER ? "corona" : rang != null && rang.almenys(Rang.OFICIAL) ? "escut" : "membres";
            f.add(Fila.de(new Pantalla.Icona(icona, Component.literal(m.getUsername()),
                            Component.literal(connectat ? "Connectat" : "No connectat")),
                    gris(" " + (rang == null ? "" : p.nomRang(rang)) + "  "),
                    text(Component.literal(connectat ? "● connectat" : "○").withStyle(st -> st.withColor(connectat
                            ? to(ChatFormatting.GREEN) : 0x6E6252)))));
        }
        f.add(Fila.de(gris("Per convidar, expulsar o canviar el rang de algú: /openpac-parties (ho fa OPAC).")));
        if (q.pot(Permis.EDITAR)) {
            f.add(titol("Noms dels rangs", "corona"));
            for (Rang rang : Rang.values()) {
                String id = "rang_" + rang.clau();
                f.add(Fila.de(gris(rang.nomPerDefecte()), entrada(id, p.nomRang(rang), 140, Validacio.NOM_RANG_MAX),
                        boto("Desa", "/nacio rang " + rang.clau() + " {" + id + "}"),
                        boto("Per defecte", "/nacio rang " + rang.clau())));
            }
        }
        f.add(titol("Qui pot fer què", "pacte"));
        for (Permis permis : Permis.values()) {
            if (!permis.configurable()) {
                continue;
            }
            List<Peca> fila = new ArrayList<>();
            fila.add(gris(descriu(permis) + ": "));
            Rang ara = p.rangPer(permis);
            for (Rang rang : Rang.values()) {
                if (rang == ara) {
                    fila.add(text(Component.literal("[" + p.nomRang(rang) + "]").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)));
                } else if (q.rang == Rang.LIDER) {
                    fila.add(boto(p.nomRang(rang), "/nacio permis " + permis.clau() + " " + rang.clau()));
                }
            }
            f.add(new Fila(fila));
        }
        if (q.rang != Rang.LIDER) {
            f.add(Fila.de(gris("Només el líder canvia els permisos.")));
        }
    }

    private static String descriu(Permis p) {
        return switch (p) {
            case EDITAR -> "Canviar les dades";
            case DIPLOMACIA -> "Aliances i pactes";
            case TRENCAR -> "Trencar-los";
            case GUERRA -> "Declarar la guerra";
            case PAU -> "Pau i rendició";
            case DISSOLDRE -> "Dissoldre";
        };
    }

    // ---------------------------------------------------------------- Diplomàcia

    private static void diplomacia(Qui q, List<Fila> f) {
        Pais p = q.pais;
        if (p != null) {
            f.add(titol("Relacions", "diplomacia"));
            Map<UUID, EstatRelacio> rel = q.r.relacionsDe(p.id());
            if (rel.isEmpty()) {
                f.add(Fila.de(gris("Cap aliança, pacte, treva ni guerra.")));
            }
            for (Map.Entry<UUID, EstatRelacio> e : rel.entrySet()) {
                Pais altre = q.r.pais(e.getKey()).orElse(null);
                if (altre == null) {
                    continue;
                }
                EstatRelacio estat = e.getValue();
                List<Peca> fila = new ArrayList<>();
                fila.add(new Pantalla.Icona(iconaRelacio(estat.tipus()), Component.literal(majuscula(estat.tipus().nom())
                        + " amb "), Component.empty()));
                fila.add(escut(altre));
                fila.add(text(Missatges.pais(altre)));
                if (estat.fins() > 0) {
                    fila.add(gris(" fins al " + Missatges.data(estat.fins())));
                }
                if ((estat.tipus() == TipusRelacio.ALIANCA || estat.tipus() == TipusRelacio.PACTE)) {
                    fila.add(q.pot(Permis.TRENCAR)
                            ? boto("Trenca", "/diplomacia trenca " + altre.sigla(), ChatFormatting.RED,
                            estat.tipus() == TipusRelacio.PACTE ? "Trencar un pacte abans d'hora queda al diari." : "")
                            : desactivat("Trenca", "Cal ser " + p.nomRang(p.rangPer(Permis.TRENCAR))));
                }
                if (estat.tipus() == TipusRelacio.GUERRA) {
                    fila.add(boto("Mira-la", "pestanya:" + GUERRES));
                }
                f.add(new Fila(fila));
            }
            for (Proposta pr : q.r.propostesRebudes(p.id())) {
                q.r.pais(pr.de()).ifPresent(de -> {
                    List<Peca> fila = new ArrayList<>(List.of(escut(de), text(Missatges.pais(de)),
                            text(Component.literal(" us proposa " + descriu(pr)).withStyle(s -> s.withColor(to(ChatFormatting.YELLOW))))));
                    if (q.pot(Permis.DIPLOMACIA)) {
                        fila.add(boto("Accepta", "/diplomacia accepta " + de.sigla(), ChatFormatting.GREEN, ""));
                        fila.add(boto("Rebutja", "/diplomacia rebutja " + de.sigla(), ChatFormatting.RED, ""));
                    }
                    f.add(new Fila(fila));
                });
            }
            for (Proposta pr : q.r.propostesFetes(p.id())) {
                q.r.pais(pr.a()).ifPresent(a -> f.add(Fila.de(gris("Esperant que "), text(Missatges.pais(a)),
                        gris(" respongui: " + descriu(pr)))));
            }
        }
        f.add(titol("Països", "escut"));
        for (Pais altre : q.r.paisos()) {
            if (p != null && altre.id().equals(p.id())) {
                continue;
            }
            List<Peca> fila = ambEscut(altre);
            int n = membres(q.server, altre);
            fila.add(gris(" · " + n + (n == 1 ? " membre" : " membres")));
            if (p != null) {
                TipusRelacio rel = q.r.relacio(p.id(), altre.id());
                if (q.pot(Permis.DIPLOMACIA) && rel != TipusRelacio.GUERRA && rel != TipusRelacio.ALIANCA) {
                    fila.add(boto("Aliança", "/diplomacia alianca " + altre.sigla(), ChatFormatting.GREEN, "Proposa una aliança."));
                    if (rel == TipusRelacio.NEUTRAL) {
                        fila.add(boto("Pacte 7 d", "/diplomacia pacte " + altre.sigla() + " 7", ChatFormatting.AQUA,
                                "Proposa un pacte de no agressió de 7 dies."));
                        fila.add(boto("30 d", "/diplomacia pacte " + altre.sigla() + " 30", ChatFormatting.AQUA,
                                "Proposa un pacte de no agressió de 30 dies."));
                    }
                }
                if (q.pot(Permis.GUERRA) && rel == TipusRelacio.NEUTRAL) {
                    fila.add(boto("Guerra…", "pestanya:declara:" + altre.sigla(), ChatFormatting.RED, ""));
                }
            }
            f.add(new Fila(fila));
        }
        if (q.r.paisos().size() <= (p == null ? 0 : 1)) {
            f.add(Fila.de(gris("Encara no hi ha cap altre país.")));
        }
    }

    private static String iconaRelacio(TipusRelacio t) {
        return switch (t) {
            case ALIANCA -> "alianca";
            case PACTE -> "pacte";
            case TREVA -> "treva";
            case GUERRA -> "guerra";
            case NEUTRAL -> "pau";
        };
    }

    private static String descriu(Proposta pr) {
        return pr.tipus() == TipusRelacio.ALIANCA ? "una aliança"
                : "un pacte de no agressió de " + pr.dies() + (pr.dies() == 1 ? " dia" : " dies");
    }

    // ---------------------------------------------------------------- Guerres

    private static void guerres(Qui q, List<Fila> f) {
        long ara = System.currentTimeMillis();
        FinestresGuerra fin = Config.finestres();
        f.add(Fila.de(new Pantalla.Icona("treva", Batalles.finestraOberta()
                        ? Component.literal("Ara es lluita").withStyle(s -> s.withColor(to(ChatFormatting.RED)))
                        : Component.literal("Propera batalla: " + Missatges.data(fin.properaObertura(ara))),
                Component.literal("De dilluns a divendres de " + fin.iniciFeiner() + " a " + fin.fiFeiner()
                        + "; el cap de setmana de " + fin.iniciCapDeSetmana() + " a " + fin.fiCapDeSetmana()))));
        Pais p = q.pais;
        if (p != null) {
            for (Guerra g : q.r.guerresDe(p.id())) {
                guerra(q, f, g, ara);
            }
            OrdresGuerra.esborrany(p.id()).ifPresent(e -> {
                Pais contra = q.r.pais(e.contra()).orElse(null);
                f.add(titol("Objectiu que prepareu", "objectiu"));
                f.add(Fila.de(gris(e.chunks().size() + (e.chunks().size() == 1 ? " chunk de " : " chunks de ")),
                        text(contra == null ? Component.literal("?") : Missatges.pais(contra)),
                        boto("Mostra'l", "/guerra objectiu mostra"), boto("Buida", "/guerra objectiu buida")));
                if (contra != null) {
                    f.add(Fila.de(boto("Declara la guerra…", "pestanya:declara:" + contra.sigla(), ChatFormatting.RED, "")));
                }
            });
            if (q.pot(Permis.GUERRA)) {
                f.add(titol("Preparar una conquesta", "objectiu"));
                f.add(Fila.de(gris("Posa't als chunks del país que vols i afegeix-los a l'objectiu (es veuen amb flames):")));
                f.add(Fila.de(boto("Aquest chunk", "/guerra objectiu afegeix"), boto("Radi 1", "/guerra objectiu radi 1"),
                        boto("Radi 2", "/guerra objectiu radi 2"), boto("Treu aquest", "/guerra objectiu treu")));
            }
        }
        List<Guerra> altres = q.r.guerres().stream().filter(g -> p == null || !g.hiEs(p.id())).toList();
        if (!altres.isEmpty()) {
            f.add(titol("Altres guerres", "guerra"));
            for (Guerra g : altres) {
                Pais a = q.r.pais(g.atacant()).orElseThrow();
                Pais d = q.r.pais(g.defensor()).orElseThrow();
                f.add(Fila.de(escut(a), barraGuerra(g, a, d, 120), escut(d), gris(" " + g.objectiu().nom())));
            }
        }
        if (p == null && q.r.guerres().isEmpty()) {
            f.add(Fila.de(gris("Ara no hi ha cap guerra.")));
        }
    }

    private static void guerra(Qui q, List<Fila> f, Guerra g, long ara) {
        Pais p = q.pais;
        Pais enemic = q.r.pais(g.enemic(p.id())).orElse(null);
        if (enemic == null) {
            return;
        }
        boolean ataquem = g.atacant().equals(p.id());
        f.add(titol("Guerra amb " + Registre.etiqueta(enemic) + " · " + (ataquem ? "l'ataquem" : "ens ataca") + " · "
                + objectiu(g), "guerra"));
        // La puntuació, com a Europa Universalis: el nostre color a l'esquerra, el de l'enemic a la dreta.
        f.add(Fila.de(escut(p), barraGuerra(g, p, enemic, 230), escut(enemic)));
        String fase = !g.activa(ara) ? "Preparació fins al " + Missatges.data(g.inici())
                : Batalles.finestraOberta() ? "Batalla en curs" : "Activa";
        ParametresGuerra par = Config.parametres();
        int nostres = ataquem ? g.baixesAtacant() : g.baixesDefensor();
        int seves = ataquem ? g.baixesDefensor() : g.baixesAtacant();
        f.add(Fila.de(icona("treva", fase, g.limit() > 0 ? "Si ningú no signa la pau, s'acaba sola el "
                        + Missatges.data(g.limit()) : ""),
                icona("baixes", nostres + " / " + seves, "Baixes que hem fet / que ens han fet"),
                icona("vides", Math.max(0, par.vides() - g.mortsDe(q.jugador.getUUID())) + "/" + par.vides(),
                        "Les teves vides en aquesta batalla"),
                boto("Detall al xat", "/guerra estat " + enemic.sigla())));
        // El control de cada punt de captura.
        List<Peca> punts = new ArrayList<>();
        for (cat.mineuab.pauitreva.model.Chunk c : g.perControl()) {
            if (punts.size() >= 8) {
                break;
            }
            punts.add(barraControl(c.x() + ", " + c.z(), 64, g.control(c), ataquem ? p : enemic, ataquem ? enemic : p,
                    "Chunk de l'objectiu " + c.x() + ", " + c.z()));
        }
        if (g.capitalDefensor() != null && !g.chunks().contains(g.capitalDefensor())) {
            Pais def = ataquem ? enemic : p;
            punts.add(barraControl("Capital " + def.sigla(), 96, g.controlCapitalDefensor(), ataquem ? p : enemic, def,
                    "La capital del defensor: si l'atacant la pren, guanya punts cada minut"));
        }
        if (g.capitalAtacant() != null) {
            Pais at = ataquem ? p : enemic;
            punts.add(barraControl("Capital " + at.sigla(), 96, g.controlCapitalAtacant(), ataquem ? enemic : p, at,
                    "La capital de l'atacant: si el defensor la pren, guanya punts cada minut"));
        }
        if (!punts.isEmpty()) {
            f.add(new Fila(punts));
        }
        Registre.PropostaPau pr = q.r.propostaPau(g.id()).orElse(null);
        if (pr != null) {
            String termes = termes(q.r, g, pr.guanyador(), pr.termes());
            if (pr.de().equals(p.id())) {
                f.add(Fila.de(gris("Heu proposat: " + termes + ". "),
                        q.pot(Permis.PAU) ? boto("Retira-la", "/guerra pau " + enemic.sigla() + " rebutja") : gris("")));
            } else {
                List<Peca> fila = new ArrayList<>(List.of(text(Component.literal("Proposen la pau: " + termes + " ")
                        .withStyle(s -> s.withColor(to(ChatFormatting.YELLOW))))));
                if (q.pot(Permis.PAU)) {
                    fila.add(boto("Accepta", "/guerra pau " + enemic.sigla() + " accepta " + pr.creada(), ChatFormatting.GREEN, ""));
                    fila.add(boto("Rebutja", "/guerra pau " + enemic.sigla() + " rebutja", ChatFormatting.RED, ""));
                }
                f.add(new Fila(fila));
            }
        }
        if (!q.pot(Permis.PAU)) {
            f.add(Fila.de(gris("La pau la signa qui sigui " + p.nomRang(p.rangPer(Permis.PAU)) + " o més.")));
            return;
        }
        List<Peca> pau = new ArrayList<>();
        pau.add(gris("Pau: "));
        pau.add(boto("Blanca", "/guerra pau " + enemic.sigla() + " proposa blanca", ChatFormatting.WHITE, "Sense canvis."));
        if (ataquem) {
            if (!g.chunks().isEmpty()) {
                pau.add(botoTermes(g, p, enemic, "Objectiu", "guanyem objectiu", Registre.termes(g, p.id(),
                        Registre.QuinsChunks.OBJECTIU, false)));
                if (!g.controlats().isEmpty()) {
                    pau.add(botoTermes(g, p, enemic, "Els presos", "guanyem controlats", Registre.termes(g, p.id(),
                            Registre.QuinsChunks.CONTROLATS, false)));
                }
            }
            pau.add(botoTermes(g, p, enemic, "Humiliació", "guanyem humiliacio", new Termes(java.util.Set.of(), true)));
        } else {
            pau.add(botoTermes(g, p, enemic, "Humiliar-los", "guanyem humiliacio", new Termes(java.util.Set.of(), true)));
            if (!g.chunks().isEmpty()) {
                pau.add(boto("Cedir-los l'objectiu", "/guerra pau " + enemic.sigla() + " proposa guanyeu objectiu",
                        ChatFormatting.GRAY, "Proposeu donar-los els chunks de l'objectiu."));
            }
        }
        f.add(new Fila(pau));
        f.add(Fila.de(boto("Rendeix-te…", "pestanya:rendir:" + enemic.sigla(), ChatFormatting.RED, "")));
    }

    /** La barra de la puntuació d'una guerra, del punt de vista de {@code esquerra}. */
    private static Pantalla.Barra barraGuerra(Guerra g, Pais esquerra, Pais dreta, int ample) {
        int punts = g.puntsDe(esquerra.id());
        float valor = (punts + Guerra.PUNTS_MAX) / (2f * Guerra.PUNTS_MAX);
        return new Pantalla.Barra(valor, esquerra.color(), dreta.color(),
                Component.literal((punts > 0 ? "+" : "") + punts), ample,
                Component.literal("Punts de guerra de " + Registre.etiqueta(esquerra) + ": " + punts
                        + ". Amb prou punts es pot imposar la pau."));
    }

    /** Una barra petita amb el control d'un punt de captura: el color de qui el pren omple des de l'esquerra. */
    private static Pantalla.Barra barraControl(String text, int ample, int control, Pais pren, Pais te, String ajuda) {
        return new Pantalla.Barra(control / (float) Guerra.CONTROL_MAX, pren.color(), te.color(),
                Component.literal(text), ample, Component.literal(ajuda + ": " + control * 100 / Guerra.CONTROL_MAX + "% pres"));
    }

    /** Un botó per proposar uns termes a favor nostre; si tenim prou punts, els imposa. */
    private static Boto botoTermes(Guerra g, Pais p, Pais enemic, String text, String paraules, Termes t) {
        int cost = t.cost(g);
        boolean imposa = g.puntsDe(p.id()) >= cost;
        return boto(text + " (" + cost + ")", "/guerra pau " + enemic.sigla() + (imposa ? " imposa " : " proposa ") + paraules,
                imposa ? ChatFormatting.GOLD : ChatFormatting.YELLOW,
                imposa ? "Teniu prou punts: s'imposa sense que l'altre hagi d'acceptar." : "Costa " + cost
                        + " punts: el proposeu i l'altre l'ha d'acceptar.");
    }

    private static String objectiu(Guerra g) {
        return g.objectiu() == TipusObjectiu.CONQUESTA
                ? "conquesta de " + g.chunks().size() + (g.chunks().size() == 1 ? " chunk" : " chunks")
                : g.objectiu().nom();
    }


    private static String termes(Registre r, Guerra g, UUID guanyador, Termes t) {
        if (t.esBlanca()) {
            return "pau blanca";
        }
        String qui = r.pais(guanyador).map(Pais::sigla).orElse("?");
        String perd = r.pais(g.enemic(guanyador)).map(Pais::sigla).orElse("?");
        List<String> parts = new ArrayList<>();
        if (!t.chunks().isEmpty()) {
            parts.add(perd + " cedeix " + t.chunks().size() + (t.chunks().size() == 1 ? " chunk" : " chunks") + " a " + qui);
        }
        if (t.humiliacio()) {
            parts.add(perd + " queda humiliat");
        }
        return String.join(" i ", parts);
    }

    private static void declarar(Qui q, List<Fila> f, String sigla) {
        Pais contra = q.r.perSigla(sigla).orElse(null);
        if (q.pais == null || contra == null || !q.pot(Permis.GUERRA)) {
            guerres(q, f);
            return;
        }
        ParametresGuerra par = Config.parametres();
        f.add(titol("Declarar la guerra a " + Registre.etiqueta(contra), "guerra"));
        f.add(Fila.de(gris("Després hi ha " + par.preparacio() / Registre.HORA + " h de preparació; només es lluita dins de "
                + "les finestres. Si ningú no signa la pau, al cap de " + par.durada() / Registre.DIA
                + " dies guanya qui tingui més punts.")));
        var esb = OrdresGuerra.esborrany(q.pais.id()).filter(e -> e.contra().equals(contra.id()));
        if (esb.isPresent()) {
            f.add(Fila.de(boto("Conquesta de " + esb.get().chunks().size() + " chunks", "/guerra declara " + sigla + " conquesta",
                    ChatFormatting.RED, "Els chunks que heu preparat a l'objectiu.")));
        } else {
            f.add(Fila.de(desactivat("Conquesta", "Primer prepareu l'objectiu als seus chunks."),
                    gris(" Primer, prepareu l'objectiu des de la pestanya Guerres.")));
        }
        f.add(Fila.de(boto("Humiliació", "/guerra declara " + sigla + " humiliacio", ChatFormatting.RED,
                "Si guanyeu, queden humiliats " + par.humiliacio() / Registre.DIA + " dies.")));
        f.add(Fila.de(boto("Tornar", "pestanya:" + DIPLOMACIA)));
    }

    private static void rendir(Qui q, List<Fila> f, String sigla) {
        Pais enemic = q.r.perSigla(sigla).orElse(null);
        Guerra g = q.pais == null || enemic == null ? null : q.r.guerraEntre(q.pais.id(), enemic.id()).orElse(null);
        if (g == null || !q.pot(Permis.PAU)) {
            guerres(q, f);
            return;
        }
        Termes t = Registre.termesRendicio(g, q.pais.id());
        f.add(titol("Rendir-se davant de " + Registre.etiqueta(enemic) + "?", "pau"));
        f.add(Fila.de(gris("Perdeu la guerra: " + termes(q.r, g, enemic.id(), t) + ".")));
        f.add(Fila.de(boto("Sí, ens rendim", "/guerra rendeix " + sigla + " confirmo", ChatFormatting.RED, ""),
                boto("No", "pestanya:" + GUERRES)));
    }

    // ---------------------------------------------------------------- Diari

    private static void diari(Qui q, List<Fila> f) {
        List<Esdeveniment> darrers = q.r.darrers(25);
        f.add(titol("Diari diplomàtic", "diari"));
        if (darrers.isEmpty()) {
            f.add(Fila.de(gris("Encara és buit.")));
        }
        for (Esdeveniment e : darrers) {
            f.add(Fila.de(new Pantalla.Icona(iconaDiari(e.tipus()), Component.literal(Missatges.data(e.quan()))
                    .withStyle(st -> st.withColor(GRIS)), Component.empty())));
            f.add(Fila.de(text("    " + e.text())));
        }
    }

    private static String iconaDiari(String tipus) {
        return switch (tipus) {
            case Esdeveniment.GUERRA, Esdeveniment.GUERRA_COMENCA -> "guerra";
            case Esdeveniment.PAU, Esdeveniment.LIMIT -> "pau";
            case Esdeveniment.ALIANCA, Esdeveniment.ALIANCA_TRENCADA -> "alianca";
            case Esdeveniment.PACTE, Esdeveniment.PACTE_TRENCAT, Esdeveniment.PACTE_ACABAT -> "pacte";
            case Esdeveniment.TREVA_ACABADA -> "treva";
            case Esdeveniment.CAPTURA -> "capital";
            case Esdeveniment.LIDER -> "corona";
            case Esdeveniment.DISSOLUCIO -> "baixes";
            default -> "escut";
        };
    }

    private static String majuscula(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @SuppressWarnings("unused")
    private static MutableComponent pais(Pais p) {
        return Missatges.pais(p);
    }
}
