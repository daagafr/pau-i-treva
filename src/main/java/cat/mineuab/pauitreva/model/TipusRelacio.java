package cat.mineuab.pauitreva.model;

/** Relació entre dos països. Sense cap relació apuntada, són {@link #NEUTRAL}. */
public enum TipusRelacio {
    NEUTRAL("neutrals"),
    ALIANCA("aliança"),
    PACTE("pacte de no agressió"),
    TREVA("treva"),
    GUERRA("guerra");

    private final String nom;

    TipusRelacio(String nom) {
        this.nom = nom;
    }

    public String nom() {
        return nom;
    }
}
