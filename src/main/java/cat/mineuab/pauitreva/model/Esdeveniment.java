package cat.mineuab.pauitreva.model;

/**
 * Una entrada del diari diplomàtic. {@code tipus} és una clau fixa (per filtrar a la web i donar títols més
 * endavant) i {@code text} és el que es llegeix, ja en català.
 */
public record Esdeveniment(long quan, String tipus, String text) {

    public static final String FUNDACIO = "fundacio";
    public static final String DISSOLUCIO = "dissolucio";
    public static final String NOM = "nom";
    public static final String ALIANCA = "alianca";
    public static final String ALIANCA_TRENCADA = "alianca_trencada";
    public static final String PACTE = "pacte";
    public static final String PACTE_TRENCAT = "pacte_trencat";
    public static final String PACTE_ACABAT = "pacte_acabat";
    public static final String TREVA_ACABADA = "treva_acabada";
    public static final String GUERRA = "guerra";
    public static final String PAU = "pau";
}
