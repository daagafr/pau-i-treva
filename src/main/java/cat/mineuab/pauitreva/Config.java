package cat.mineuab.pauitreva;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Config del servidor: {@code world/serverconfig/pauitreva-server.toml}. */
public final class Config {

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue CADUCITAT_PROPOSTES_HORES = B
            .comment("Hores que val una proposta d'aliança o de pacte si ningú no la respon.")
            .defineInRange("caducitatPropostesHores", 48, 1, 24 * 14);

    public static final ModConfigSpec.IntValue PACTE_DIES_MAX = B
            .comment("Màxim de dies que pot durar un pacte de no agressió.")
            .defineInRange("pacteDiesMax", 30, 1, 365);

    public static final ModConfigSpec.IntValue SEGONS_MANTENIMENT = B
            .comment("Cada quants segons es fan caducar propostes, pactes i treves, i es torna a posar el nom i el color",
                    "dels països a OPAC.")
            .defineInRange("segonsManteniment", 30, 5, 3600);

    public static final ModConfigSpec SPEC = B.build();

    private Config() {
    }
}
