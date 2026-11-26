package cat.mineuab.pauitreva.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Una guerra oberta entre dos països. Comença en preparació i passa a activa a {@link #inici()}; s'acaba
 * amb una pau ({@link Registre#ferPau}), i llavors surt del registre i queda al diari.
 *
 * <p>La puntuació va de -100 a 100: positiva vol dir que guanya l'atacant, negativa que guanya el defensor.
 * Per dins es compta en mil·lèsimes de punt ({@link #milpunts()}), perquè l'ocupació suma una mica cada minut.
 *
 * <p>Punts de captura: els chunks de l'objectiu (els pren l'atacant) i les dues capitals (la del defensor la
 * pren l'atacant i la de l'atacant, el defensor). El control de cada punt va de 0 a {@link #CONTROL_MAX}, i és
 * sempre el control de qui el vol prendre.
 */
public final class Guerra {

    public static final int PUNTS_MAX = 100;
    public static final int MIL = 1000;
    public static final int CONTROL_MAX = 10_000;

    private final UUID id;
    private final UUID atacant;
    private final UUID defensor;
    private final TipusObjectiu objectiu;
    private final Set<Chunk> chunks;
    private final long declarada;
    private final long inici;
    private final long limit;
    private final Chunk capitalAtacant;
    private final Chunk capitalDefensor;
    private int milpunts;
    private final Map<Chunk, Integer> control = new LinkedHashMap<>();
    private int controlCapitalDefensor;
    private int controlCapitalAtacant;
    private int baixesAtacant;
    private int baixesDefensor;
    /** Per a OPAC: la sub-config que tenia cada chunk de l'objectiu abans de la guerra, per tornar-la-hi. */
    private final Map<Chunk, String> subconfigs = new HashMap<>();

    // Comptadors de la finestra d'ara. No es desen: si el servidor es reinicia, tothom torna a tenir les vides.
    private final Map<UUID, Integer> morts = new HashMap<>();
    private final Map<UUID, Integer> baixesPerVictima = new HashMap<>();

    public Guerra(UUID id, UUID atacant, UUID defensor, TipusObjectiu objectiu, Set<Chunk> chunks,
                  long declarada, long inici, int punts) {
        this(id, atacant, defensor, objectiu, chunks, declarada, inici, 0, null, null, punts * MIL);
    }

    /** Per crear-la o carregar-la. {@code limit} 0 vol dir sense pau automàtica. */
    public Guerra(UUID id, UUID atacant, UUID defensor, TipusObjectiu objectiu, Set<Chunk> chunks,
                  long declarada, long inici, long limit, Chunk capitalAtacant, Chunk capitalDefensor, int milpunts) {
        this.id = id;
        this.atacant = atacant;
        this.defensor = defensor;
        this.objectiu = objectiu;
        this.chunks = Collections.unmodifiableSet(new LinkedHashSet<>(chunks));
        this.declarada = declarada;
        this.inici = inici;
        this.limit = limit;
        this.capitalAtacant = capitalAtacant;
        this.capitalDefensor = capitalDefensor;
        this.milpunts = limitaMil(milpunts);
        for (Chunk c : this.chunks) {
            control.put(c, 0);
        }
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

    /** Quan s'acaba sola amb la pau automàtica, o 0 si no s'acaba sola. */
    public long limit() {
        return limit;
    }

    /** La capital de l'atacant durant la guerra (la pot prendre el defensor), o {@code null}. */
    public Chunk capitalAtacant() {
        return capitalAtacant;
    }

    /** La capital del defensor durant la guerra (la pot prendre l'atacant), o {@code null}. */
    public Chunk capitalDefensor() {
        return capitalDefensor;
    }

    /** Punts sencers, arrodonits cap a zero. */
    public int punts() {
        return milpunts / MIL;
    }

    public int milpunts() {
        return milpunts;
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
        return atacant.equals(pais) ? punts() : -punts();
    }

    public int baixesAtacant() {
        return baixesAtacant;
    }

    public int baixesDefensor() {
        return baixesDefensor;
    }

    /** Control de l'atacant sobre un chunk de l'objectiu (0 si no n'és). */
    public int control(Chunk c) {
        return control.getOrDefault(c, 0);
    }

    public Map<Chunk, Integer> controls() {
        return Collections.unmodifiableMap(control);
    }

    public int controlCapitalDefensor() {
        return controlCapitalDefensor;
    }

    public int controlCapitalAtacant() {
        return controlCapitalAtacant;
    }

    /** Els chunks de l'objectiu que l'atacant controla del tot, per l'ordre de l'objectiu. */
    public List<Chunk> controlats() {
        return control.entrySet().stream().filter(e -> e.getValue() >= CONTROL_MAX).map(Map.Entry::getKey).toList();
    }

    /** Els chunks de l'objectiu, de més controlat a menys (i, si empaten, per l'ordre de l'objectiu). */
    public List<Chunk> perControl() {
        return control.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<Chunk, Integer> e) -> e.getValue()).reversed())
                .map(Map.Entry::getKey).toList();
    }

    public Map<Chunk, String> subconfigs() {
        return Collections.unmodifiableMap(subconfigs);
    }

    /** És un punt de captura d'aquesta guerra (un chunk de l'objectiu o una de les capitals)? */
    public boolean esPuntDeCaptura(Chunk c) {
        return control.containsKey(c) || c.equals(capitalDefensor) || c.equals(capitalAtacant);
    }

    /** El país que vol prendre aquest punt de captura (l'atacant, o el defensor a la capital de l'atacant). */
    public UUID quiPren(Chunk c) {
        return c.equals(capitalAtacant) && !control.containsKey(c) ? defensor : atacant;
    }

    /** El control d'un punt de captura, de qui el vol prendre. */
    public int controlDe(Chunk c) {
        if (control.containsKey(c)) {
            return control.get(c);
        }
        if (c.equals(capitalDefensor)) {
            return controlCapitalDefensor;
        }
        if (c.equals(capitalAtacant)) {
            return controlCapitalAtacant;
        }
        return 0;
    }

    public int mortsDe(UUID jugador) {
        return morts.getOrDefault(jugador, 0);
    }

    // ---------------------------------------------------------------- Canvis (només des del Registre)

    void sumar(UUID perAquest, int quants) {
        sumarMil(perAquest, quants * MIL);
    }

    void sumarMil(UUID perAquest, int mil) {
        milpunts = limitaMil(milpunts + (atacant.equals(perAquest) ? mil : -mil));
    }

    /** Canvia el control d'un punt de captura. Torna el control nou, o -1 si el chunk no n'és cap. */
    int moureControl(Chunk c, int delta) {
        if (control.containsKey(c)) {
            int nou = limitaControl(control.get(c) + delta);
            control.put(c, nou);
            return nou;
        }
        if (c.equals(capitalDefensor)) {
            controlCapitalDefensor = limitaControl(controlCapitalDefensor + delta);
            return controlCapitalDefensor;
        }
        if (c.equals(capitalAtacant)) {
            controlCapitalAtacant = limitaControl(controlCapitalAtacant + delta);
            return controlCapitalAtacant;
        }
        return -1;
    }

    void comptarBaixa(UUID paisAssassi) {
        if (atacant.equals(paisAssassi)) {
            baixesAtacant++;
        } else {
            baixesDefensor++;
        }
    }

    /** Apunta una baixa d'aquest jugador a la finestra. Torna quantes en porta. */
    int baixaDe(UUID victima) {
        return baixesPerVictima.merge(victima, 1, Integer::sum);
    }

    int mort(UUID jugador) {
        return morts.merge(jugador, 1, Integer::sum);
    }

    void novaFinestra() {
        morts.clear();
        baixesPerVictima.clear();
    }

    /** Per carregar les dades del món. */
    public void carregarEstat(Map<Chunk, Integer> controls, int capitalDefensor, int capitalAtacant,
                              int baixesAtacant, int baixesDefensor, Map<Chunk, String> subconfigs) {
        controls.forEach((c, v) -> {
            if (control.containsKey(c)) {
                control.put(c, limitaControl(v));
            }
        });
        this.controlCapitalDefensor = limitaControl(capitalDefensor);
        this.controlCapitalAtacant = limitaControl(capitalAtacant);
        this.baixesAtacant = baixesAtacant;
        this.baixesDefensor = baixesDefensor;
        this.subconfigs.putAll(subconfigs);
    }

    /** Per a OPAC: apunta la sub-config que tenia el chunk abans de la guerra (només el primer cop). */
    public void recordarSubconfig(Chunk c, String sub) {
        subconfigs.putIfAbsent(c, sub);
    }

    private static int limitaMil(int p) {
        return Math.max(-PUNTS_MAX * MIL, Math.min(PUNTS_MAX * MIL, p));
    }

    private static int limitaControl(int c) {
        return Math.max(0, Math.min(CONTROL_MAX, c));
    }
}
