package cat.mineuab.pauitreva.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * El JSON públic que llegeix la web ({@code /estat.json}): països amb membres i títols, aliances, pactes i
 * treves, i el diari. Sense res de Minecraft, perquè es pugui provar sol.
 *
 * <p>No hi ha propostes pendents (són cosa de cada país) ni posicions de jugadors.
 */
public final class Exportacio {

    public static final int MAX_DIARI = 100;

    /**
     * El que surt d'OPAC per a cada país: el líder, els membres i el territori (per a cada dimensió, els
     * chunks reclamats com a parells {x, z}).
     */
    public record InfoPartit(String lider, List<Membre> membres, Map<String, List<int[]>> territori) {

        public InfoPartit(String lider, List<Membre> membres) {
            this(lider, membres, Map.of());
        }
    }

    public record Membre(String nom, Rang rang) {
    }

    /** Les finestres de guerra tal com surten a l'API: si ara és oberta, quan es tanca i quan s'obre la propera. */
    public record Finestres(boolean oberta, long tancament, long properaObertura, String feiners, String capDeSetmana,
                            String zona) {

        public static Finestres de(FinestresGuerra f, boolean oberta, long ara) {
            return new Finestres(oberta, oberta ? f.tancament(ara) : -1, f.properaObertura(ara),
                    f.iniciFeiner() + "-" + f.fiFeiner(), f.iniciCapDeSetmana() + "-" + f.fiCapDeSetmana(),
                    f.zona().getId());
        }
    }

    private Exportacio() {
    }

    /**
     * @param partits per a cada id de país, la seva party, o {@code null} si OPAC no la té
     */
    public static String estat(Registre r, Function<UUID, InfoPartit> partits, long ara) {
        FinestresGuerra f = FinestresGuerra.perDefecte();
        return estat(r, partits, ara, Finestres.de(f, f.oberta(ara), ara));
    }

    /**
     * @param partits   per a cada id de país, la seva party, o {@code null} si OPAC no la té
     * @param finestres les finestres de guerra d'ara
     */
    public static String estat(Registre r, Function<UUID, InfoPartit> partits, long ara, Finestres finestres) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("{\"versio\":2,\"generat\":").append(ara).append(",\"finestres\":");
        finestres(sb, finestres);
        sb.append(",\"paisos\":[");
        boolean primer = true;
        for (Pais p : r.paisos()) {
            if (!primer) {
                sb.append(',');
            }
            primer = false;
            pais(sb, p, partits.apply(p.id()));
        }
        sb.append("],\"relacions\":[");
        primer = true;
        for (Map.Entry<Parella, EstatRelacio> e : r.totesLesRelacions().entrySet()) {
            Pais a = r.pais(e.getKey().a()).orElse(null);
            Pais b = r.pais(e.getKey().b()).orElse(null);
            if (a == null || b == null) {
                continue;
            }
            if (!primer) {
                sb.append(',');
            }
            primer = false;
            EstatRelacio estat = e.getValue();
            sb.append("{\"a\":");
            text(sb, a.sigla());
            sb.append(",\"b\":");
            text(sb, b.sigla());
            sb.append(",\"tipus\":");
            text(sb, estat.tipus().name().toLowerCase(java.util.Locale.ROOT));
            sb.append(",\"des\":").append(estat.des());
            sb.append(",\"fins\":").append(estat.fins() > 0 ? Long.toString(estat.fins()) : "null");
            sb.append('}');
        }
        sb.append("],\"guerres\":[");
        primer = true;
        for (Guerra g : r.guerres()) {
            Pais a = r.pais(g.atacant()).orElse(null);
            Pais d = r.pais(g.defensor()).orElse(null);
            if (a == null || d == null) {
                continue;
            }
            if (!primer) {
                sb.append(',');
            }
            primer = false;
            guerra(sb, g, a, d, ara);
        }
        sb.append("],\"diari\":[");
        primer = true;
        for (Esdeveniment e : r.darrers(MAX_DIARI)) {
            if (!primer) {
                sb.append(',');
            }
            primer = false;
            sb.append("{\"quan\":").append(e.quan()).append(",\"tipus\":");
            text(sb, e.tipus());
            sb.append(",\"text\":");
            text(sb, e.text());
            sb.append('}');
        }
        sb.append("]}");
        return sb.toString();
    }

    private static void pais(StringBuilder sb, Pais p, InfoPartit partit) {
        sb.append("{\"id\":");
        text(sb, p.id().toString());
        sb.append(",\"nom\":");
        text(sb, p.nom());
        sb.append(",\"sigla\":");
        text(sb, p.sigla());
        sb.append(",\"color\":");
        text(sb, Validacio.colorText(p.color()));
        sb.append(",\"lema\":");
        text(sb, p.lema());
        sb.append(",\"bandera\":");
        if (p.bandera().isEmpty()) {
            sb.append("null");
        } else {
            text(sb, p.bandera());
        }
        sb.append(",\"fundat\":").append(p.fundat());
        sb.append(",\"humiliatFins\":").append(p.humiliatFins() > 0 ? Long.toString(p.humiliatFins()) : "null");
        sb.append(",\"capital\":");
        if (p.capital() == null) {
            sb.append("null");
        } else {
            sb.append("{\"dimensio\":");
            text(sb, p.capital().dimensio());
            sb.append(",\"chunkX\":").append(p.capital().chunkX());
            sb.append(",\"chunkZ\":").append(p.capital().chunkZ()).append('}');
        }
        sb.append(",\"lider\":");
        if (partit == null) {
            sb.append("null");
        } else {
            text(sb, partit.lider());
        }
        sb.append(",\"membres\":[");
        if (partit != null) {
            boolean primer = true;
            for (Membre m : partit.membres()) {
                if (!primer) {
                    sb.append(',');
                }
                primer = false;
                sb.append("{\"nom\":");
                text(sb, m.nom());
                sb.append(",\"rang\":");
                text(sb, m.rang().clau());
                sb.append(",\"titol\":");
                text(sb, p.nomRang(m.rang()));
                sb.append('}');
            }
        }
        sb.append("],\"territori\":{");
        if (partit != null) {
            boolean primeraDim = true;
            for (Map.Entry<String, List<int[]>> dim : partit.territori().entrySet()) {
                if (!primeraDim) {
                    sb.append(',');
                }
                primeraDim = false;
                text(sb, dim.getKey());
                sb.append(":[");
                boolean primer = true;
                for (int[] chunk : dim.getValue()) {
                    if (!primer) {
                        sb.append(',');
                    }
                    primer = false;
                    sb.append('[').append(chunk[0]).append(',').append(chunk[1]).append(']');
                }
                sb.append(']');
            }
        }
        sb.append("}}");
    }

    private static void finestres(StringBuilder sb, Finestres f) {
        sb.append("{\"oberta\":").append(f.oberta());
        sb.append(",\"tancament\":").append(f.tancament() > 0 ? Long.toString(f.tancament()) : "null");
        sb.append(",\"properaObertura\":").append(f.properaObertura());
        sb.append(",\"feiners\":");
        text(sb, f.feiners());
        sb.append(",\"capDeSetmana\":");
        text(sb, f.capDeSetmana());
        sb.append(",\"zona\":");
        text(sb, f.zona());
        sb.append('}');
    }

    /** Una guerra. Els punts són del punt de vista de l'atacant; el control, en percentatge. */
    private static void guerra(StringBuilder sb, Guerra g, Pais a, Pais d, long ara) {
        sb.append("{\"id\":");
        text(sb, g.id().toString());
        sb.append(",\"atacant\":");
        text(sb, a.sigla());
        sb.append(",\"defensor\":");
        text(sb, d.sigla());
        sb.append(",\"objectiu\":");
        text(sb, g.objectiu().clau());
        sb.append(",\"declarada\":").append(g.declarada());
        sb.append(",\"inici\":").append(g.inici());
        sb.append(",\"limit\":").append(g.limit() > 0 ? Long.toString(g.limit()) : "null");
        sb.append(",\"activa\":").append(g.activa(ara));
        sb.append(",\"punts\":").append(g.punts());
        sb.append(",\"baixes\":{\"atacant\":").append(g.baixesAtacant())
                .append(",\"defensor\":").append(g.baixesDefensor()).append('}');
        sb.append(",\"chunks\":[");
        boolean primer = true;
        for (Map.Entry<Chunk, Integer> e : g.controls().entrySet()) {
            if (!primer) {
                sb.append(',');
            }
            primer = false;
            chunk(sb, e.getKey(), e.getValue());
        }
        sb.append("],\"capitals\":{\"atacant\":");
        if (g.capitalAtacant() == null) {
            sb.append("null");
        } else {
            chunk(sb, g.capitalAtacant(), g.controlCapitalAtacant());
        }
        sb.append(",\"defensor\":");
        if (g.capitalDefensor() == null) {
            sb.append("null");
        } else {
            chunk(sb, g.capitalDefensor(), g.controlCapitalDefensor());
        }
        sb.append("}}");
    }

    private static void chunk(StringBuilder sb, Chunk c, int control) {
        sb.append("{\"dimensio\":");
        text(sb, c.dimensio());
        sb.append(",\"x\":").append(c.x()).append(",\"z\":").append(c.z());
        sb.append(",\"control\":").append(control * 100 / Guerra.CONTROL_MAX).append('}');
    }

    /** Escriu una cadena JSON, amb les cometes i els caràcters especials escapats. */
    static void text(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '<' -> sb.append("\\u003c"); // per si algú l'enganxa dins d'un HTML
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }
}
