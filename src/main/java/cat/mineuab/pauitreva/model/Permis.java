package cat.mineuab.pauitreva.model;

import java.util.Locale;
import java.util.Optional;

/**
 * Què pot fer cada rang dins del país. Cada permís té un rang mínim per defecte, que el líder pot canviar
 * (menys {@link #DISSOLDRE}, que sempre és del líder).
 */
public enum Permis {
    /** Canviar el nom, la sigla, el color, el lema, la bandera, la capital i els noms dels rangs. */
    EDITAR("editar", Rang.MINISTRE),
    /** Proposar, acceptar i rebutjar aliances i pactes. */
    DIPLOMACIA("diplomacia", Rang.MINISTRE),
    /** Trencar una aliança o un pacte abans d'hora. */
    TRENCAR("trencar", Rang.LIDER),
    /** Dissoldre el país. Només el líder. */
    DISSOLDRE("dissoldre", Rang.LIDER);

    private final String clau;
    private final Rang perDefecte;

    Permis(String clau, Rang perDefecte) {
        this.clau = clau;
        this.perDefecte = perDefecte;
    }

    public String clau() {
        return clau;
    }

    public Rang perDefecte() {
        return perDefecte;
    }

    public boolean configurable() {
        return this != DISSOLDRE;
    }

    public static Optional<Permis> perClau(String clau) {
        for (Permis p : values()) {
            if (p.clau.equals(clau.toLowerCase(Locale.ROOT))) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }
}
