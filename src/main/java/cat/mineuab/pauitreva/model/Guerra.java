package cat.mineuab.pauitreva.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Una guerra oberta entre dos països. Comença en preparació i passa a activa a {@link #inici()}; s'acaba
 * amb una pau ({@link Registre#ferPau}), i llavors surt del registre i queda al diari.
 *
 * <p>La puntuació va de -100 a 100: positiva vol dir que guanya l'atacant, negativa que guanya el defensor.
 */
public final class Guerra {

    public static final int PUNTS_MAX = 100;

    private final UUID id;
    private final UUID atacant;
    private final UUID defensor;
    private final TipusObjectiu objectiu;
    private final Set<Chunk> chunks;
    private final long declarada;
    private final long inici;
    private int punts;

    public Guerra(UUID id, UUID atacant, UUID defensor, TipusObjectiu objectiu, Set<Chunk> chunks,
                  long declarada, long inici, int punts) {
        this.id = id;
        this.atacant = atacant;
        this.defensor = defensor;
        this.objectiu = objectiu;
        this.chunks = Collections.unmodifiableSet(new LinkedHashSet<>(chunks));
        this.declarada = declarada;
        this.inici = inici;
        this.punts = limita(punts);
    }

    public UUID id() {
        return id;
    }

    public UUID atacant() {
        return atacant;
    }

    public UUID defensor() {
        return defensor;
    }

    public TipusObjectiu objectiu() {
        return objectiu;
    }

    /** Els chunks en disputa (només en una conquesta). */
    public Set<Chunk> chunks() {
        return chunks;
    }

    public long declarada() {
        return declarada;
    }

    /** Quan s'acaba la preparació i es pot començar a lluitar (dins de les finestres). */
    public long inici() {
        return inici;
    }

    public int punts() {
        return punts;
    }

    public boolean activa(long ara) {
        return ara >= inici;
    }

    public boolean hiEs(UUID pais) {
        return atacant.equals(pais) || defensor.equals(pais);
    }

    public UUID enemic(UUID pais) {
        return atacant.equals(pais) ? defensor : atacant;
    }

    /** Els punts del país: els de l'atacant tal qual, els del defensor girats. */
    public int puntsDe(UUID pais) {
        return atacant.equals(pais) ? punts : -punts;
    }

    void sumar(UUID perAquest, int quants) {
        punts = limita(punts + (atacant.equals(perAquest) ? quants : -quants));
    }

    private static int limita(int p) {
        return Math.max(-PUNTS_MAX, Math.min(PUNTS_MAX, p));
    }
}
