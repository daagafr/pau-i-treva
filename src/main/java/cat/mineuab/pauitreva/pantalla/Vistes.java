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
        Component titol = pais == null ? Component.literal("Pau i Treva") : Missatges.pais(pais);
        List<Pestanya> pestanyes = List.of(
                new Pestanya(PAIS, Component.literal("País")),
                new Pestanya(MEMBRES, Component.literal("Membres")),
                new Pestanya(DIPLOMACIA, Component.literal("Diplomàcia")),
                new Pestanya(GUERRES, Component.literal(r.guerres().isEmpty() ? "Guerres" : "Guerres ⚔")),
                new Pestanya(DIARI, Component.literal("Diari")));
        return new Pantalla(titol, pestanyes, activa, avisos, f);
    }

    // ---------------------------------------------------------------- Peces

    private static Text text(Component c) {
        return new Text(c);
    }

    private static Text text(String s) {
        return new Text(Component.literal(s));
    }

    private static Text gris(String s) {
        return new Text(Component.literal(s).withStyle(ChatFormatting.GRAY));
    }

    private static Fila titol(String s) {
        return Fila.de(new Text(Component.literal(s).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
    }

    private static Boto boto(String text, String accio) {
        return new Boto(Component.literal(text), accio, Component.empty());
    }

    private static Boto boto(String text, String accio, ChatFormatting color, String ajuda) {
        return new Boto(Component.literal(text).withStyle(color), accio, Component.literal(ajuda));
    }

    private static Boto desactivat(String text, String perque) {
        return new Boto(Component.literal(text), "", Component.literal(perque));
    }

    private static Entrada entrada(String id, String valor, int ample, int max) {
        return new Entrada(id, valor, ample, max, Component.empty());
    }

    private static Fila buida() {
        return Fila.de(text(""));
    }

    // ---------------------------------------------------------------- País

    private static void pais(Qui q, List<Fila> f) {
        if (q.pais == null) {
            f.add(titol("No ets de cap país"));
            if (q.partit != null && q.rang != Rang.LIDER) {
                f.add(Fila.de(gris("Ets d'una party d'OPAC i no n'ets el propietari: només el propietari en pot fer un país.")));
            } else {
                f.add(Fila.de(gris("Funda'n un: la sigla té de 2 a 4 lletres o xifres, i el nom, de 3 a 32 caràcters."
                        + (q.partit != null ? " La teva party d'OPAC passarà a ser el país." : ""))));
                f.add(Fila.de(text("Sigla"), entrada("sigla", "", 44, 4), text("Nom"), entrada("nom", "", 150, 32),
                        boto("Funda el país", "/nacio funda {sigla} {nom}", ChatFormatting.GREEN, "")));
            }
            f.add(Fila.de(gris("O entra a la party d'OPAC d'un país que ja existeixi (que et convidin amb /openpac-parties).")));
            f.add(buida());
            f.add(titol("Països (" + q.r.paisos().size() + ")"));
            for (Pais p : q.r.paisos()) {
                f.add(Fila.de(text(Missatges.pais(p)), gris(" · " + membres(q.server, p) + " membres")));
            }
            return;
        }
        Pais p = q.pais;
        long ara = System.currentTimeMillis();
        if (!p.lema().isEmpty()) {
            f.add(Fila.de(text(Component.literal("«" + p.lema() + "»").withStyle(ChatFormatting.ITALIC))));
        }
        String lider = q.partit == null ? "?" : PontOpac.nomLider(q.partit);
        f.add(Fila.de(gris(p.nomRang(Rang.LIDER) + ": "), text(lider),
                gris(" · " + membres(q.server, p) + " membres · fundat el " + Missatges.dia(p.fundat()))));
        f.add(Fila.de(gris("El teu rang: "), text(q.rang == null ? "?" : p.nomRang(q.rang))));
        String capital = p.capital() == null ? "sense capital (a la guerra, la del mig del territori)"
                : "chunk " + p.capital().chunkX() + ", " + p.capital().chunkZ() + " (" + p.capital().dimensio() + ")";
        f.add(Fila.de(gris("Capital: " + capital)));
        f.add(Fila.de(gris("Bandera: " + (p.bandera().isEmpty() ? "cap" : p.bandera()))));
        if (p.humiliat(ara)) {
            f.add(Fila.de(text(Component.literal("Humiliat per una guerra perduda fins al " + Missatges.data(p.humiliatFins()))
                    .withStyle(ChatFormatting.RED))));
        }
        List<Guerra> guerres = q.r.guerresDe(p.id());
        if (!guerres.isEmpty()) {
            f.add(Fila.de(text(Component.literal("En guerra amb " + guerres.size()
                    + (guerres.size() == 1 ? " país " : " països ")).withStyle(ChatFormatting.RED)),
                    boto("Mira-ho", "pestanya:" + GUERRES)));
        }
        f.add(buida());
        if (!q.pot(Permis.EDITAR)) {
            f.add(Fila.de(gris("Per canviar les dades del país cal ser " + p.nomRang(p.rangPer(Permis.EDITAR)) + " o més.")));
            return;
        }
        f.add(titol("Canviar les dades"));
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
            f.add(buida());
            f.add(Fila.de(boto("Dissol el país…", "pestanya:dissol", ChatFormatting.RED,
                    "La party d'OPAC i els claims es queden.")));
        }
    }

    private static void dissoldre(Qui q, List<Fila> f) {
        if (q.pais == null || q.rang != Rang.LIDER) {
            pais(q, f);
            return;
        }
        f.add(titol("Dissoldre " + Registre.etiqueta(q.pais) + "?"));
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
        f.add(titol("Membres (" + q.partit.getMemberCount() + ")"));
        for (IPartyMemberAPI m : PontOpac.membres(q.partit)) {
            Rang rang = PontOpac.rang(q.partit, m.getUUID());
            boolean connectat = q.server.getPlayerList().getPlayer(m.getUUID()) != null;
            f.add(Fila.de(text(Component.literal(connectat ? "● " : "○ ")
                            .withStyle(connectat ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY)),
                    text(m.getUsername()), gris(" · " + (rang == null ? "" : p.nomRang(rang)))));
        }
        f.add(Fila.de(gris("Per convidar, expulsar o canviar el rang de algú: /openpac-parties (ho fa OPAC).")));
        f.add(buida());
        if (q.pot(Permis.EDITAR)) {
            f.add(titol("Noms dels rangs"));
            for (Rang rang : Rang.values()) {
                String id = "rang_" + rang.clau();
                f.add(Fila.de(gris(rang.nomPerDefecte()), entrada(id, p.nomRang(rang), 140, Validacio.NOM_RANG_MAX),
                        boto("Desa", "/nacio rang " + rang.clau() + " {" + id + "}"),
                        boto("Per defecte", "/nacio rang " + rang.clau())));
            }
        }
        f.add(buida());
        f.add(titol("Qui pot fer què"));
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
            f.add(titol("Relacions"));
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
                fila.add(text(majuscula(estat.tipus().nom()) + " amb "));
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
                    List<Peca> fila = new ArrayList<>(List.of(text(Missatges.pais(de)),
                            text(Component.literal(" us proposa " + descriu(pr)).withStyle(ChatFormatting.YELLOW))));
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
            f.add(buida());
        }
        f.add(titol("Països"));
        for (Pais altre : q.r.paisos()) {
            if (p != null && altre.id().equals(p.id())) {
                continue;
            }
            List<Peca> fila = new ArrayList<>();
            fila.add(text(Missatges.pais(altre)));
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

    private static String descriu(Proposta pr) {
        return pr.tipus() == TipusRelacio.ALIANCA ? "una aliança"
                : "un pacte de no agressió de " + pr.dies() + (pr.dies() == 1 ? " dia" : " dies");
    }

    // ---------------------------------------------------------------- Guerres

    private static void guerres(Qui q, List<Fila> f) {
        long ara = System.currentTimeMillis();
        FinestresGuerra fin = Config.finestres();
        f.add(Fila.de(gris("Es lluita de dilluns a divendres de " + fin.iniciFeiner() + " a " + fin.fiFeiner()
                + " i el cap de setmana de " + fin.iniciCapDeSetmana() + " a " + fin.fiCapDeSetmana() + ". "),
                Batalles.finestraOberta()
                        ? text(Component.literal("Ara és oberta.").withStyle(ChatFormatting.RED))
                        : gris("Propera: " + Missatges.data(fin.properaObertura(ara)) + ".")));
        Pais p = q.pais;
        if (p != null) {
            for (Guerra g : q.r.guerresDe(p.id())) {
                f.add(buida());
                guerra(q, f, g, ara);
            }
            OrdresGuerra.esborrany(p.id()).ifPresent(e -> {
                f.add(buida());
                Pais contra = q.r.pais(e.contra()).orElse(null);
                f.add(titol("Objectiu que prepareu"));
                f.add(Fila.de(gris(e.chunks().size() + (e.chunks().size() == 1 ? " chunk de " : " chunks de ")),
                        text(contra == null ? Component.literal("?") : Missatges.pais(contra)),
                        boto("Mostra'l", "/guerra objectiu mostra"), boto("Buida", "/guerra objectiu buida")));
                if (contra != null) {
                    f.add(Fila.de(boto("Declara la guerra…", "pestanya:declara:" + contra.sigla(), ChatFormatting.RED, "")));
                }
            });
            if (q.pot(Permis.GUERRA)) {
                f.add(buida());
                f.add(titol("Preparar una conquesta"));
                f.add(Fila.de(gris("Posa't als chunks del país que vols i afegeix-los a l'objectiu (es veuen amb flames):")));
                f.add(Fila.de(boto("Aquest chunk", "/guerra objectiu afegeix"), boto("Radi 1", "/guerra objectiu radi 1"),
                        boto("Radi 2", "/guerra objectiu radi 2"), boto("Treu aquest", "/guerra objectiu treu")));
            }
        }
        List<Guerra> altres = q.r.guerres().stream().filter(g -> p == null || !g.hiEs(p.id())).toList();
        if (!altres.isEmpty()) {
            f.add(buida());
            f.add(titol("Altres guerres"));
            for (Guerra g : altres) {
                f.add(Fila.de(text(Missatges.pais(q.r.pais(g.atacant()).orElseThrow())),
                        text(Component.literal(" ⚔ ").withStyle(ChatFormatting.RED)),
                        text(Missatges.pais(q.r.pais(g.defensor()).orElseThrow())),
                        gris(" · " + g.objectiu().nom() + " · " + (g.punts() > 0 ? "+" : "") + g.punts())));
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
        f.add(Fila.de(text(Component.literal("⚔ ").withStyle(ChatFormatting.RED)), text(Missatges.pais(enemic)),
                gris(ataquem ? " · l'ataquem · " : " · ens ataca · "), gris(objectiu(g))));
        int punts = g.puntsDe(p.id());
        String fase = !g.activa(ara) ? "Preparació fins al " + Missatges.data(g.inici())
                : Batalles.finestraOberta() ? "Batalla en curs" : "Activa";
        f.add(Fila.de(gris(fase + " · "),
                text(Component.literal((punts > 0 ? "+" : "") + punts + " punts").withStyle(punts > 0 ? ChatFormatting.GREEN
                        : punts < 0 ? ChatFormatting.RED : ChatFormatting.YELLOW)),
                gris(" · baixes " + (ataquem ? g.baixesAtacant() + "/" + g.baixesDefensor() : g.baixesDefensor() + "/"
                        + g.baixesAtacant()) + (g.limit() > 0 ? " · s'acaba sola el " + Missatges.data(g.limit()) : ""))));
        if (!g.chunks().isEmpty()) {
            f.add(Fila.de(gris("Objectiu: " + g.controlats().size() + " de " + g.chunks().size() + " chunks presos"
                    + capitals(g))));
        } else if (g.capitalDefensor() != null || g.capitalAtacant() != null) {
            f.add(Fila.de(gris(capitals(g).replaceFirst("^ · ", ""))));
        }
        ParametresGuerra par = Config.parametres();
        f.add(Fila.de(gris("Et queden " + Math.max(0, par.vides() - g.mortsDe(q.jugador.getUUID())) + " de " + par.vides()
                + " vides en aquesta batalla."), boto("Detall al xat", "/guerra estat " + enemic.sigla())));
        Registre.PropostaPau pr = q.r.propostaPau(g.id()).orElse(null);
        if (pr != null) {
            String termes = termes(q.r, g, pr.guanyador(), pr.termes());
            if (pr.de().equals(p.id())) {
                f.add(Fila.de(gris("Heu proposat: " + termes + ". "),
                        q.pot(Permis.PAU) ? boto("Retira-la", "/guerra pau " + enemic.sigla() + " rebutja") : gris("")));
            } else {
                List<Peca> fila = new ArrayList<>(List.of(text(Component.literal("Proposen la pau: " + termes + " ")
                        .withStyle(ChatFormatting.YELLOW))));
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

    private static String capitals(Guerra g) {
        StringBuilder sb = new StringBuilder();
        if (g.capitalDefensor() != null && !g.chunks().contains(g.capitalDefensor())) {
            sb.append(" · capital del defensor ").append(g.controlCapitalDefensor() * 100 / Guerra.CONTROL_MAX).append("% presa");
        }
        if (g.capitalAtacant() != null) {
            sb.append(" · capital de l'atacant ").append(g.controlCapitalAtacant() * 100 / Guerra.CONTROL_MAX).append("% presa");
        }
        return sb.toString();
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
        f.add(titol("Declarar la guerra a " + Registre.etiqueta(contra)));
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
        f.add(titol("Rendir-se davant de " + Registre.etiqueta(enemic) + "?"));
        f.add(Fila.de(gris("Perdeu la guerra: " + termes(q.r, g, enemic.id(), t) + ".")));
        f.add(Fila.de(boto("Sí, ens rendim", "/guerra rendeix " + sigla + " confirmo", ChatFormatting.RED, ""),
                boto("No", "pestanya:" + GUERRES)));
    }

    // ---------------------------------------------------------------- Diari

    private static void diari(Qui q, List<Fila> f) {
        List<Esdeveniment> darrers = q.r.darrers(25);
        f.add(titol("Diari diplomàtic"));
        if (darrers.isEmpty()) {
            f.add(Fila.de(gris("Encara és buit.")));
        }
        for (Esdeveniment e : darrers) {
            f.add(Fila.de(gris(Missatges.data(e.quan()) + "  "), text(e.text())));
        }
    }

    private static String majuscula(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @SuppressWarnings("unused")
    private static MutableComponent pais(Pais p) {
        return Missatges.pais(p);
    }
}
