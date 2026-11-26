package cat.mineuab.pauitreva;

import cat.mineuab.pauitreva.model.FinestresGuerra;
import cat.mineuab.pauitreva.model.ParametresGuerra;
import cat.mineuab.pauitreva.model.Registre;
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

    // ---------------------------------------------------------------- Tags

    static {
        B.comment("El tag del país al xat (prefix de LuckPerms, si hi és) i damunt del cap (placeholder %pauitreva-tag%",
                "del TAB, si hi és).").push("tags");
    }

    public static final ModConfigSpec.BooleanValue TAG_LUCKPERMS = B
            .comment("Posar el tag del país com a prefix de LuckPerms (per al xat i la llista del TAB).")
            .define("luckperms", true);

    public static final ModConfigSpec.ConfigValue<String> TAG_FORMAT = B
            .comment("El tag. %color% és el color del país en hexadecimal (RRGGBB) i %sigla%, la sigla.")
            .define("format", "#%color%&l[%sigla%]&r ");

    public static final ModConfigSpec.IntValue TAG_PRIORITAT = B
            .comment("Prioritat del prefix a LuckPerms. Ha de ser més baixa que la de les universitats (9) i que cap",
                    "altre prefix que facin servir els jugadors: els prefixos d'aquesta prioritat amb només el context",
                    "server=<servidor> són del mod.")
            .defineInRange("prioritat", 5, 1, 1000);

    public static final ModConfigSpec.ConfigValue<String> TAG_SERVIDOR = B
            .comment("El context server= del prefix: el nom del servidor a la config de LuckPerms. Al MineUAB no hi surt.")
            .define("servidor", "create");

    static {
        B.pop();
    }

    // ---------------------------------------------------------------- Guerra

    static {
        B.comment("La guerra. Les hores són en hora local de la zona que es diu aquí.").push("guerra");
    }

    public static final ModConfigSpec.ConfigValue<String> FINESTRA_FEINERS = B
            .comment("Quan es pot lluitar de dilluns a divendres, com a «18:00-23:00».")
            .define("finestraFeiners", "18:00-23:00");

    public static final ModConfigSpec.ConfigValue<String> FINESTRA_CAP_DE_SETMANA = B
            .comment("Quan es pot lluitar dissabte i diumenge.")
            .define("finestraCapDeSetmana", "12:00-23:00");

    public static final ModConfigSpec.ConfigValue<String> ZONA = B
            .comment("Zona horària de les finestres.")
            .define("zona", "Europe/Madrid");

    public static final ModConfigSpec.IntValue PREPARACIO_HORES = B
            .comment("Hores des de la declaració fins que es pot lluitar.")
            .defineInRange("preparacioHores", 24, 0, 24 * 14);

    public static final ModConfigSpec.IntValue DURADA_DIES = B
            .comment("Dies des de l'inici de la guerra fins a la pau automàtica (guanya qui té més punts). 0: no s'acaba sola.")
            .defineInRange("duradaDies", 7, 0, 365);

    public static final ModConfigSpec.IntValue TREVA_DIES = B
            .comment("Dies de treva després de cada pau.")
            .defineInRange("trevaDies", 7, 0, 365);

    public static final ModConfigSpec.IntValue HUMILIACIO_DIES = B
            .comment("Dies que queda humiliat qui perd amb aquest terme.")
            .defineInRange("humiliacioDies", 7, 0, 365);

    public static final ModConfigSpec.IntValue MAX_CHUNKS = B
            .comment("Màxim de chunks d'una conquesta.")
            .defineInRange("maxChunks", 20, 1, 1000);

    public static final ModConfigSpec.IntValue MAX_PERCENT = B
            .comment("Màxim de chunks d'una conquesta, en percentatge del territori del defensor (sempre almenys 1).")
            .defineInRange("maxPercent", 20, 1, 100);

    public static final ModConfigSpec.BooleanValue CAL_FRONTERA = B
            .comment("Si l'objectiu d'una conquesta ha de tocar el territori de l'atacant.")
            .define("calFrontera", true);

    public static final ModConfigSpec.IntValue GUERRES_MAX = B
            .comment("Quantes guerres alhora pot tenir un país, d'atacant o de defensor.")
            .defineInRange("guerresMax", 2, 1, 20);

    public static final ModConfigSpec.BooleanValue OBRIR_ZONA = B
            .comment("Durant la finestra, els dos països en guerra poden trencar, posar i obrir coses als chunks de",
                    "l'objectiu, i hi entren les explosions (de qualsevol). Per a la resta, continuen protegits. Cal que",
                    "«claims.protection.exceptions.blocksByExplosions» sigui a opConfigurablePlayerConfigOptions d'OPAC.",
                    "Si és false, la guerra només dona punts.")
            .define("obrirZona", true);

    public static final ModConfigSpec.BooleanValue OBRIR_PVP = B
            .comment("Durant la finestra, PvP entre els enemics d'una guerra (només entre ells) al territori dels seus dos",
                    "països. Ni companys, ni neutrals, ni al territori d'un altre país.")
            .define("obrirPvp", true);

    public static final ModConfigSpec.IntValue AFK_MINUTS = B
            .comment("Minuts sense moure's a partir dels quals un jugador no captura.")
            .defineInRange("afkMinuts", 5, 1, 120);

    public static final ModConfigSpec.BooleanValue BRILLAR = B
            .comment("Fer brillar (Glowing) els jugadors d'un punt de captura disputat, perquè ningú no s'hi amagui.")
            .define("brillar", true);

    static {
        B.pop();
        B.comment("Captura per presència. El control d'un chunk va de 0 a 10000; les velocitats són per segon.").push("captura");
    }

    public static final ModConfigSpec.IntValue CAPTURA_BASE = B.defineInRange("base", 40, 0, 10000);
    public static final ModConfigSpec.IntValue CAPTURA_EXTRA = B
            .comment("El que suma cada atacant de més.").defineInRange("extra", 5, 0, 10000);
    public static final ModConfigSpec.IntValue CAPTURA_MAX = B
            .comment("Màxim per molts atacants que hi hagi.").defineInRange("max", 60, 0, 10000);
    public static final ModConfigSpec.IntValue PES_DEFENSOR = B
            .comment("El que fa recular cada defensor present.").defineInRange("pesDefensor", 35, 0, 10000);
    public static final ModConfigSpec.IntValue DECADENCIA = B
            .comment("El que recula un chunk a mig capturar si no hi ha ningú (un de capturat del tot no recula).")
            .defineInRange("decadencia", 10, 0, 10000);
    public static final ModConfigSpec.IntValue FACTOR_CAPITAL = B
            .comment("Percentatge de la velocitat a les capitals.").defineInRange("factorCapital", 50, 1, 1000);

    static {
        B.pop();
        B.comment("Punts de guerra (de -100 a 100). Amb prou punts es pot imposar la pau.").push("punts");
    }

    public static final ModConfigSpec.IntValue OCUPACIO_PER_HORA = B
            .comment("Punts per hora de finestra per a l'atacant si controla tot l'objectiu (proporcional).")
            .defineInRange("ocupacioPerHora", 25, 0, 100);
    public static final ModConfigSpec.IntValue CAPITAL_PER_HORA = B
            .comment("Punts per hora de finestra per tenir la capital enemiga.")
            .defineInRange("capitalPerHora", 40, 0, 100);
    public static final ModConfigSpec.IntValue DEFENSA_PER_HORA = B
            .comment("Punts per hora de finestra per al defensor mentre l'atacant no controla res.")
            .defineInRange("defensaPerHora", 5, 0, 100);
    public static final ModConfigSpec.IntValue PUNTS_BAIXA = B
            .comment("Punts per matar un enemic durant la finestra.").defineInRange("puntsBaixa", 2, 0, 100);
    public static final ModConfigSpec.IntValue BAIXES_MAX_VICTIMA = B
            .comment("Quantes vegades per finestra compta matar el mateix jugador.")
            .defineInRange("baixesMaxVictima", 3, 0, 100);
    public static final ModConfigSpec.IntValue VIDES = B
            .comment("Morts per finestra a partir de les quals un jugador ja no captura.")
            .defineInRange("vides", 5, 1, 100);

    static {
        B.pop();
    }

    public static final ModConfigSpec SPEC = B.build();

    private Config() {
    }

    /** Els paràmetres de la guerra de la config, o els de per defecte si la config encara no s'ha carregat. */
    public static ParametresGuerra parametres() {
        try {
            return new ParametresGuerra(PREPARACIO_HORES.get() * Registre.HORA, TREVA_DIES.get() * Registre.DIA,
                    DURADA_DIES.get() * Registre.DIA, HUMILIACIO_DIES.get() * Registre.DIA,
                    MAX_CHUNKS.get(), MAX_PERCENT.get(), CAL_FRONTERA.get(), GUERRES_MAX.get(),
                    CAPTURA_BASE.get(), CAPTURA_EXTRA.get(), CAPTURA_MAX.get(), PES_DEFENSOR.get(), DECADENCIA.get(),
                    FACTOR_CAPITAL.get(), OCUPACIO_PER_HORA.get(), CAPITAL_PER_HORA.get(), DEFENSA_PER_HORA.get(),
                    PUNTS_BAIXA.get(), BAIXES_MAX_VICTIMA.get(), VIDES.get());
        } catch (IllegalStateException e) {
            return ParametresGuerra.perDefecte();
        }
    }

    private static FinestresGuerra finestresDesades;
    private static String finestresText = "";

    /** Les finestres de la config. Si estan mal escrites, les de per defecte (i un avís al registre, un sol cop). */
    public static FinestresGuerra finestres() {
        String f;
        String c;
        String z;
        try {
            f = FINESTRA_FEINERS.get();
            c = FINESTRA_CAP_DE_SETMANA.get();
            z = ZONA.get();
        } catch (IllegalStateException e) {
            return FinestresGuerra.perDefecte();
        }
        String text = f + "|" + c + "|" + z;
        if (!text.equals(finestresText)) {
            finestresText = text;
            finestresDesades = FinestresGuerra.llegir(f, c, z).orElseGet(() -> {
                PauITreva.LOG.warn("Pau i Treva: les finestres de guerra de la config no es poden llegir ({}); "
                        + "faig servir les de per defecte", text);
                return FinestresGuerra.perDefecte();
            });
        }
        return finestresDesades;
    }

    /** Llegeix un valor de la config, o el de per defecte si encara no s'ha carregat. */
    public static <T> T o(ModConfigSpec.ConfigValue<T> valor, T perDefecte) {
        try {
            return valor.get();
        } catch (IllegalStateException e) {
            return perDefecte;
        }
    }
}
