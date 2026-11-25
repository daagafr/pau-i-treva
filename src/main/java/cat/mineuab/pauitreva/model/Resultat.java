package cat.mineuab.pauitreva.model;

/** Com ha anat una operació del {@link Registre}. Els textos per al jugador són a {@code Missatges}. */
public enum Resultat {
    FET,
    PROPOSADA,
    ACCEPTADA,
    TRENCADA_ALIANCA,
    TRENCAT_PACTE,

    JA_EXISTEIX,
    NO_EXISTEIX,
    NOM_INVALID,
    SIGLA_INVALIDA,
    NOM_OCUPAT,
    SIGLA_OCUPADA,
    MATEIX_PAIS,
    TIPUS_INVALID,
    DIES_INVALIDS,
    JA_PROPOSAT,
    JA_HO_SOU,
    EN_GUERRA,
    EN_TREVA,
    NO_HI_HA_PROPOSTA,
    NO_HI_HA_RES;

    public boolean bo() {
        return this == FET || this == PROPOSADA || this == ACCEPTADA
                || this == TRENCADA_ALIANCA || this == TRENCAT_PACTE;
    }
}
