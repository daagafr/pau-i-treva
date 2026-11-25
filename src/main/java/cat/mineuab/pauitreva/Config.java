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

    public static final ModConfigSpec.BooleanValue API_ACTIVA = B
            .comment("Obre l'API JSON de només lectura per a la web (/estat.json), només a 127.0.0.1.",
                    "Es llegeix en arrencar el servidor.")
            .define("apiActiva", true);

    public static final ModConfigSpec.IntValue API_PORT = B
            .comment("Port de l'API web. Credits fa servir el 25580 i Perfil el 25581.")
            .defineInRange("apiPort", 25582, 1024, 65535);

    public static final ModConfigSpec SPEC = B.build();

    private Config() {
    }
}
