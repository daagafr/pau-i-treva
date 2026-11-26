package cat.mineuab.pauitreva.model;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Regles de geometria dels chunks: si un objectiu de conquesta val, quina és la capital d'un país que no
 * n'ha triat cap i quins chunks hi ha al voltant d'un punt. Sense res de Minecraft, perquè es pugui provar sol.
 */
public final class Territori {

    /** Per què un objectiu de conquesta no val. */
    public enum Error {
        BUIT,
        MASSA_CHUNKS,
        NO_ES_DEL_DEFENSOR,
        DIMENSIONS_BARREJADES,
        NO_CONNECTAT,
        NO_TOCA_FRONTERA
    }

    private Territori() {
    }

    /**
     * Comprova un objectiu de conquesta.
     *
     * @param objectiu     els chunks que vol l'atacant
     * @param delDefensor  tot el territori del defensor
     * @param delAtacant   tot el territori de l'atacant
     * @param max          quants chunks pot tenir com a molt
     * @param calFrontera  si almenys un chunk ha de tocar (pels costats) un chunk de l'atacant
     * @return l'error, o {@code null} si l'objectiu val
     */
    public static Error validar(Set<Chunk> objectiu, Set<Chunk> delDefensor, Set<Chunk> delAtacant, int max,
                                boolean calFrontera) {
        if (objectiu.isEmpty()) {
            return Error.BUIT;
        }
        if (objectiu.size() > max) {
            return Error.MASSA_CHUNKS;
        }
        if (!delDefensor.containsAll(objectiu)) {
            return Error.NO_ES_DEL_DEFENSOR;
        }
        String dim = objectiu.iterator().next().dimensio();
        if (objectiu.stream().anyMatch(c -> !c.dimensio().equals(dim))) {
            return Error.DIMENSIONS_BARREJADES;
        }
        if (!connectat(objectiu)) {
            return Error.NO_CONNECTAT;
        }
        if (calFrontera && objectiu.stream().noneMatch(c -> veins(c).stream().anyMatch(delAtacant::contains))) {
            return Error.NO_TOCA_FRONTERA;
        }
        return null;
    }

    /** Els quatre chunks del costat. */
    public static Set<Chunk> veins(Chunk c) {
        return Set.of(new Chunk(c.dimensio(), c.x() + 1, c.z()), new Chunk(c.dimensio(), c.x() - 1, c.z()),
                new Chunk(c.dimensio(), c.x(), c.z() + 1), new Chunk(c.dimensio(), c.x(), c.z() - 1));
    }

    /** Si tots els chunks formen una sola zona, tocant-se pels costats. */
    public static boolean connectat(Set<Chunk> chunks) {
        if (chunks.isEmpty()) {
            return true;
        }
        Set<Chunk> vistos = new HashSet<>();
        Deque<Chunk> pendents = new ArrayDeque<>();
        Chunk primer = chunks.iterator().next();
        pendents.add(primer);
        vistos.add(primer);
        while (!pendents.isEmpty()) {
            for (Chunk v : veins(pendents.poll())) {
                if (chunks.contains(v) && vistos.add(v)) {
                    pendents.add(v);
                }
            }
        }
        return vistos.size() == chunks.size();
    }

    /**
     * Els chunks d'un territori que són a {@code radi} chunks o menys del centre (en quadrat), de més a prop a
     * més lluny.
     */
    public static Set<Chunk> voltant(Chunk centre, int radi, Collection<Chunk> territori) {
        Set<Chunk> r = new LinkedHashSet<>();
        territori.stream()
                .filter(c -> c.dimensio().equals(centre.dimensio())
                        && Math.abs(c.x() - centre.x()) <= radi && Math.abs(c.z() - centre.z()) <= radi)
                .sorted(Comparator.comparingInt((Chunk c) -> distancia2(c, centre)).thenComparingInt(Chunk::x)
                        .thenComparingInt(Chunk::z))
                .forEach(r::add);
        return r;
    }

    /**
     * La capital d'un país que no n'ha triat cap: a la dimensió on té més chunks (si empaten, la superfície),
     * el chunk del país més a prop del centre del seu territori. {@code null} si no té territori.
     */
    public static Chunk capitalPerDefecte(Collection<Chunk> territori) {
        if (territori.isEmpty()) {
            return null;
        }
        Map<String, Integer> perDim = new HashMap<>();
        territori.forEach(c -> perDim.merge(c.dimensio(), 1, Integer::sum));
        String dim = perDim.entrySet().stream()
                .max(Comparator.comparingInt((Map.Entry<String, Integer> e) -> e.getValue())
                        .thenComparing(e -> e.getKey().equals("minecraft:overworld")))
                .orElseThrow().getKey();
        double sx = 0;
        double sz = 0;
        int n = 0;
        for (Chunk c : territori) {
            if (c.dimensio().equals(dim)) {
                sx += c.x();
                sz += c.z();
                n++;
            }
        }
        double cx = sx / n;
        double cz = sz / n;
        return territori.stream().filter(c -> c.dimensio().equals(dim))
                .min(Comparator.comparingDouble((Chunk c) -> (c.x() - cx) * (c.x() - cx) + (c.z() - cz) * (c.z() - cz))
                        .thenComparingInt(Chunk::x).thenComparingInt(Chunk::z))
                .orElseThrow();
    }

    private static int distancia2(Chunk a, Chunk b) {
        int dx = a.x() - b.x();
        int dz = a.z() - b.z();
        return dx * dx + dz * dz;
    }
}
