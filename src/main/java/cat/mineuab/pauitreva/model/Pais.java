package cat.mineuab.pauitreva.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Un país. El seu id és el de la party d'OPAC que hi ha al darrere: els membres, els rangs i els aliats
 * d'OPAC són els del país. Aquí només hi ha el que OPAC no té.
 *
 * <p>Els canvis es fan des del {@link Registre}, que comprova les regles i apunta que hi ha hagut canvis.
 */
public final class Pais {

    private final UUID id;
    private final long fundat;
    private String nom;
    private String sigla;
    private int color;
    private String lema = "";
    private String bandera = "";
    private Capital capital;
    private long humiliatFins;
    private final EnumMap<Rang, String> nomsRangs = new EnumMap<>(Rang.class);
    private final EnumMap<Permis, Rang> permisos = new EnumMap<>(Permis.class);

    public Pais(UUID id, String nom, String sigla, int color, long fundat) {
        this.id = Objects.requireNonNull(id);
        this.nom = Objects.requireNonNull(nom);
        this.sigla = Objects.requireNonNull(sigla);
        this.color = color & 0xFFFFFF;
        this.fundat = fundat;
    }

    public UUID id() {
        return id;
    }

    public long fundat() {
        return fundat;
    }

    public String nom() {
        return nom;
    }

    public String sigla() {
        return sigla;
    }

    public int color() {
        return color;
    }

    public String lema() {
        return lema;
    }

    public String bandera() {
        return bandera;
    }

    /** Pot ser {@code null}. */
    public Capital capital() {
        return capital;
    }

    /** Fins quan queda humiliat per una guerra perduda (0 si mai no ho ha estat). */
    public long humiliatFins() {
        return humiliatFins;
    }

    public boolean humiliat(long ara) {
        return ara < humiliatFins;
    }

    /** El nom que el país dona a aquest rang, o el de per defecte. */
    public String nomRang(Rang rang) {
        return nomsRangs.getOrDefault(rang, rang.nomPerDefecte());
    }

    public Map<Rang, String> nomsRangsPropis() {
        return Collections.unmodifiableMap(nomsRangs);
    }

    /** El rang mínim que cal per a aquest permís en aquest país. */
    public Rang rangPer(Permis permis) {
        if (!permis.configurable()) {
            return permis.perDefecte();
        }
        return permisos.getOrDefault(permis, permis.perDefecte());
    }

    public Map<Permis, Rang> permisosPropis() {
        return Collections.unmodifiableMap(permisos);
    }

    public boolean pot(Rang rang, Permis permis) {
        return rang != null && rang.almenys(rangPer(permis));
    }

    // Només el Registre (i la càrrega de dades) toquen els camps.

    void posarNom(String nom) {
        this.nom = nom;
    }

    void posarSigla(String sigla) {
        this.sigla = sigla;
    }

    void posarColor(int color) {
        this.color = color & 0xFFFFFF;
    }

    void posarLema(String lema) {
        this.lema = lema;
    }

    void posarBandera(String bandera) {
        this.bandera = bandera;
    }

    void posarHumiliatFins(long fins) {
        this.humiliatFins = Math.max(this.humiliatFins, fins);
    }

    void posarCapital(Capital capital) {
        this.capital = capital;
    }

    void posarNomRang(Rang rang, String nom) {
        if (nom == null || nom.isEmpty() || nom.equals(rang.nomPerDefecte())) {
            nomsRangs.remove(rang);
        } else {
            nomsRangs.put(rang, nom);
        }
    }

    void posarPermis(Permis permis, Rang rang) {
        if (rang == null || rang == permis.perDefecte()) {
            permisos.remove(permis);
        } else {
            permisos.put(permis, rang);
        }
    }
}
