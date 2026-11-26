package cat.mineuab.pauitreva.model;

/**
 * Tots els números de la guerra, perquè es puguin canviar a la config sense tocar el codi. Les durades són
 * mil·lisegons; el control d'un chunk va de 0 a {@link Guerra#CONTROL_MAX} (centèsimes de percentatge), i
 * les velocitats de captura són unitats de control per segon.
 *
 * @param preparacio      des de la declaració fins que es pot lluitar
 * @param treva           després de la pau, temps sense poder tornar a declarar la guerra
 * @param durada          des de l'inici de la guerra (fi de la preparació) fins a la pau automàtica
 * @param humiliacio      quant temps queda humiliat qui perd amb aquest terme
 * @param maxChunks       màxim de chunks d'una conquesta
 * @param maxPercent      màxim de chunks d'una conquesta, en percentatge del territori del defensor
 * @param calFrontera     si l'objectiu ha de tocar el territori de l'atacant
 * @param guerresMax      quantes guerres alhora pot tenir un país (atacant o defensor)
 * @param capturaBase     velocitat de captura amb un sol atacant
 * @param capturaExtra    el que suma cada atacant de més
 * @param capturaMax      la velocitat màxima, per molts atacants que hi hagi (contra els que van en massa)
 * @param pesDefensor     el que fa recular cada defensor present
 * @param decadencia      el que recula un chunk a mig capturar quan no hi ha ningú (els capturats del tot no)
 * @param factorCapital   percentatge de la velocitat de captura a les capitals (50 = la meitat)
 * @param ocupacioPerHora punts per hora de finestra que guanya l'atacant si controla tot l'objectiu
 * @param capitalPerHora  punts per hora de finestra per tenir la capital enemiga
 * @param defensaPerHora  punts per hora de finestra per al defensor si l'atacant no controla res
 * @param puntsBaixa      punts per matar un enemic durant la finestra
 * @param baixesMaxVictima quantes vegades per finestra compta matar el mateix jugador
 * @param vides           morts per finestra a partir de les quals un jugador ja no captura
 */
public record ParametresGuerra(long preparacio, long treva, long durada, long humiliacio,
                               int maxChunks, int maxPercent, boolean calFrontera, int guerresMax,
                               int capturaBase, int capturaExtra, int capturaMax, int pesDefensor, int decadencia,
                               int factorCapital, int ocupacioPerHora, int capitalPerHora, int defensaPerHora,
                               int puntsBaixa, int baixesMaxVictima, int vides) {

    public static ParametresGuerra perDefecte() {
        return new ParametresGuerra(24 * Registre.HORA, 7 * Registre.DIA, 7 * Registre.DIA, 7 * Registre.DIA,
                20, 20, true, 2,
                40, 5, 60, 35, 10,
                50, 25, 40, 5,
                2, 3, 5);
    }

    /** Quants chunks pot tenir com a molt una conquesta contra un país amb {@code territori} chunks. */
    public int maxChunksContra(int territori) {
        int perPercent = (int) Math.floor(territori * maxPercent / 100.0);
        return Math.max(1, Math.min(maxChunks, perPercent));
    }

    /**
     * Com avança (positiu) o recula (negatiu) el control d'un punt de captura en un segon, segons qui hi ha.
     * Els atacants són els que el volen prendre; els defensors, els que el volen mantenir.
     */
    public int deltaCaptura(int atacants, int defensors, boolean capital) {
        int delta;
        if (atacants > 0) {
            delta = Math.min(capturaMax, capturaBase + (atacants - 1) * capturaExtra) - defensors * pesDefensor;
        } else if (defensors > 0) {
            delta = -defensors * pesDefensor;
        } else {
            return 0; // la decadència la decideix Guerra, que sap si el punt ja és capturat del tot
        }
        return capital ? delta * factorCapital / 100 : delta;
    }
}
