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

    /** El que surt d'OPAC per a cada país: el líder i els membres. */
    public record InfoPartit(String lider, List<Membre> membres) {
    }

    public record Membre(String nom, Rang rang) {
    }

    private Exportacio() {
    }

    /**
     * @param partits per a cada id de país, la seva party, o {@code null} si OPAC no la té
     */
    public static String estat(Registre r, Function<UUID, InfoPartit> partits, long ara) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("{\"versio\":1,\"generat\":").append(ara).append(",\"paisos\":[");
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
        sb.append("]}");
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
