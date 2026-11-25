package cat.mineuab.pauitreva.model;

import java.util.Set;

/**
 * Els termes d'un tractat de pau, del punt de vista de qui guanya: quins chunks de l'objectiu se li cedeixen
 * i si el perdedor queda humiliat. Una pau blanca no té cap terme.
 *
 * <p>Cada terme costa punts de guerra ({@link #cost}). Qui guanya els pot imposar si té prou punts; si
 * tots dos països hi estan d'acord, es pot signar qualsevol cosa.
 */
public record Termes(Set<Chunk> chunks, boolean humiliacio) {

    public static final int COST_HUMILIACIO = 50;

    public static Termes blanca() {
        return new Termes(Set.of(), false);
    }

    public boolean esBlanca() {
        return chunks.isEmpty() && !humiliacio;
    }

    /**
     * Quants punts costen aquests termes en aquesta guerra: tots els chunks de l'objectiu junts en valen 100
     * (cadascun, la part que li toca, arrodonida cap amunt), i la humiliació 50.
     */
    public int cost(Guerra g) {
        int cost = humiliacio ? COST_HUMILIACIO : 0;
        if (!chunks.isEmpty()) {
            int total = Math.max(1, g.chunks().size());
            cost += (int) Math.ceil(chunks.size() * (double) Guerra.PUNTS_MAX / total);
        }
        return cost;
    }
}
