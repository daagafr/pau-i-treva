package cat.mineuab.pauitreva.model;

/** El chunk on hi ha la capital d'un país. {@code dimensio} és l'id de la dimensió («minecraft:overworld»). */
public record Capital(String dimensio, int chunkX, int chunkZ) {
}
