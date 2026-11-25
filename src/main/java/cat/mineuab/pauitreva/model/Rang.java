package cat.mineuab.pauitreva.model;

import java.util.Locale;
import java.util.Optional;

/**
 * Rangs d'un país, de menys a més. Es corresponen amb els rangs d'OPAC:
 * MEMBER, CLAIMER, MODERATOR, ADMIN i el propietari de la party.
 * Cada país els pot posar nom (vegeu {@link Pais#nomRang}).
 */
public enum Rang {
    CIUTADA("Ciutadà/ana"),
    COLON("Colon"),
    OFICIAL("Oficial"),
    MINISTRE("Ministre/a"),
    LIDER("Líder");

    private final String nomPerDefecte;

    Rang(String nomPerDefecte) {
        this.nomPerDefecte = nomPerDefecte;
    }

    public String nomPerDefecte() {
        return nomPerDefecte;
    }

    /** La paraula que es fa servir a les ordres: «ciutada», «colon», «oficial», «ministre», «lider». */
    public String clau() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean almenys(Rang altre) {
        return ordinal() >= altre.ordinal();
    }

    public static Optional<Rang> perClau(String clau) {
        for (Rang r : values()) {
            if (r.clau().equals(clau.toLowerCase(Locale.ROOT))) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }
}
