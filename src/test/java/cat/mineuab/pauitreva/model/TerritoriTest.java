package cat.mineuab.pauitreva.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TerritoriTest {

    private static final String OW = "minecraft:overworld";
    private static final String NETHER = "minecraft:the_nether";

    private static Chunk c(int x, int z) {
        return new Chunk(OW, x, z);
    }

    /** Un quadrat de costat n que comença a (x, z). */
    private static Set<Chunk> quadrat(int x, int z, int n) {
        Set<Chunk> s = new HashSet<>();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                s.add(c(x + i, z + j));
            }
        }
        return s;
    }

    @Test
    void objectiuQueToca() {
        Set<Chunk> atacant = quadrat(0, 0, 3);   // x 0..2
        Set<Chunk> defensor = quadrat(3, 0, 3);  // x 3..5, toca per x=2|3
        assertNull(Territori.validar(Set.of(c(3, 0), c(4, 0)), defensor, atacant, 20, true));
        assertEquals(Territori.Error.NO_TOCA_FRONTERA, Territori.validar(Set.of(c(5, 0)), defensor, atacant, 20, true));
        assertNull(Territori.validar(Set.of(c(5, 0)), defensor, atacant, 20, false));
    }

    @Test
    void errors() {
        Set<Chunk> atacant = quadrat(0, 0, 3);
        Set<Chunk> defensor = quadrat(3, 0, 3);
        assertEquals(Territori.Error.BUIT, Territori.validar(Set.of(), defensor, atacant, 20, true));
        assertEquals(Territori.Error.MASSA_CHUNKS, Territori.validar(Set.of(c(3, 0), c(3, 1)), defensor, atacant, 1, true));
        assertEquals(Territori.Error.NO_ES_DEL_DEFENSOR, Territori.validar(Set.of(c(2, 0)), defensor, atacant, 20, true));
        assertEquals(Territori.Error.NO_CONNECTAT, Territori.validar(Set.of(c(3, 0), c(3, 2)), defensor, atacant, 20, true));
        Set<Chunk> ambNether = new HashSet<>(defensor);
        ambNether.add(new Chunk(NETHER, 3, 1));
        assertEquals(Territori.Error.DIMENSIONS_BARREJADES,
                Territori.validar(Set.of(c(3, 0), new Chunk(NETHER, 3, 1)), ambNether, atacant, 20, true));
    }

    @Test
    void enDiagonalNoEstanConnectats() {
        assertTrue(Territori.connectat(Set.of(c(0, 0), c(0, 1), c(1, 1))));
        assertEquals(false, Territori.connectat(Set.of(c(0, 0), c(1, 1))));
    }

    @Test
    void capitalPerDefecteAlMig() {
        assertEquals(c(4, 4), Territori.capitalPerDefecte(quadrat(3, 3, 3)));
        assertNull(Territori.capitalPerDefecte(Set.of()));
        // Si té més chunks al Nether, la capital hi és.
        Set<Chunk> t = new HashSet<>(Set.of(c(0, 0), new Chunk(NETHER, 5, 5), new Chunk(NETHER, 5, 6),
                new Chunk(NETHER, 5, 7)));
        assertEquals(new Chunk(NETHER, 5, 6), Territori.capitalPerDefecte(t));
    }

    @Test
    void voltantDeMesAPropAMesLluny() {
        List<Chunk> v = List.copyOf(Territori.voltant(c(4, 4), 1, quadrat(0, 0, 10)));
        assertEquals(9, v.size());
        assertEquals(c(4, 4), v.get(0));
        assertTrue(Territori.voltant(c(40, 40), 2, quadrat(0, 0, 10)).isEmpty());
    }

    @Test
    void maximDeChunksSegonsElTerritori() {
        ParametresGuerra p = ParametresGuerra.perDefecte();
        assertEquals(1, p.maxChunksContra(3));    // sempre almenys un
        assertEquals(10, p.maxChunksContra(50));  // el 20 %
        assertEquals(20, p.maxChunksContra(500)); // fins al màxim
    }
}
