package cat.mineuab.pauitreva.model;

import java.util.UUID;

/**
 * Una proposta d'aliança o de pacte que encara no s'ha respost.
 *
 * @param de     el país que la fa
 * @param a      el país que la rep
 * @param dies   per a un pacte, quants dies dura; per a una aliança, 0
 * @param caduca quan deixa de valer si ningú no la respon
 */
public record Proposta(UUID de, UUID a, TipusRelacio tipus, int dies, long creada, long caduca) {

    public boolean caducada(long ara) {
        return ara >= caduca;
    }
}
