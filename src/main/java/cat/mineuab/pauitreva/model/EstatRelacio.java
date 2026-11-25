package cat.mineuab.pauitreva.model;

/**
 * Una relació en vigor entre dos països.
 *
 * @param des  quan va començar (mil·lisegons des de 1970)
 * @param fins quan s'acaba; 0 vol dir que no s'acaba sola (per exemple, una aliança)
 */
public record EstatRelacio(TipusRelacio tipus, long des, long fins) {

    public boolean caducada(long ara) {
        return fins > 0 && ara >= fins;
    }
}
