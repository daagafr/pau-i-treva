package cat.mineuab.pauitreva.model;

import java.util.Locale;
import java.util.Optional;

/** Per què es declara una guerra (casus belli). */
public enum TipusObjectiu {
    /** Una zona de chunks de l'enemic. Qui guanya se la queda, amb tot el que hi ha construït. */
    CONQUESTA("conquesta"),
    /** No canvia el territori: el perdedor queda humiliat una temporada. */
    HUMILIACIO("humiliació"),
    /** El perdedor paga. Com, encara està per decidir (Numismatics o ítems). */
    REPARACIONS("reparacions");

    private final String nom;

    TipusObjectiu(String nom) {
        this.nom = nom;
    }

    public String nom() {
        return nom;
    }

    public String clau() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<TipusObjectiu> perClau(String clau) {
        for (TipusObjectiu t : values()) {
            if (t.clau().equals(clau.toLowerCase(Locale.ROOT))) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }
}
