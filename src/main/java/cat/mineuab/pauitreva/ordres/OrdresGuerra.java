package cat.mineuab.pauitreva.ordres;

import cat.mineuab.pauitreva.Config;
import cat.mineuab.pauitreva.guerra.Batalles;
import cat.mineuab.pauitreva.guerra.Guerres;
import cat.mineuab.pauitreva.model.Chunk;
import cat.mineuab.pauitreva.model.FinestresGuerra;
import cat.mineuab.pauitreva.model.Guerra;
import cat.mineuab.pauitreva.model.Pais;
import cat.mineuab.pauitreva.model.ParametresGuerra;
import cat.mineuab.pauitreva.model.Permis;
import cat.mineuab.pauitreva.model.Registre;
import cat.mineuab.pauitreva.model.Resultat;
import cat.mineuab.pauitreva.model.Termes;
import cat.mineuab.pauitreva.model.Territori;
import cat.mineuab.pauitreva.model.TipusObjectiu;
import cat.mineuab.pauitreva.mon.Dades;
import cat.mineuab.pauitreva.opac.PontOpac;
import cat.mineuab.pauitreva.opac.ZonaOpac;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

/**
 * Ordre {@code /guerra}: declarar (amb un esborrany de l'objectiu), mirar com va, signar la pau i rendir-se.
 * Les ordres d'admin ({@code /guerra admin}) serveixen per provar-ho i per arreglar coses.
 */
public final class OrdresGuerra {

    private static final SuggestionProvider<CommandSourceStack> OBJECTIUS = (c, b) -> SharedSuggestionProvider.suggest(
            Arrays.stream(TipusObjectiu.values()).map(TipusObjectiu::clau), b);
    private static final SuggestionProvider<CommandSourceStack> TERMES = (c, b) -> SharedSuggestionProvider.suggest(
            List.of("blanca", "guanyem objectiu", "guanyem controlats", "guanyem humiliacio",
                    "guanyem objectiu humiliacio", "guanyeu objectiu", "guanyeu controlats", "guanyeu humiliacio"), b);

    /** L'esborrany de l'objectiu de conquesta de cada país: contra qui i quins chunks. No es desa. */
    private record Esborrany(UUID contra, Set<Chunk> chunks) {
    }

    private static final Map<UUID, Esborrany> ESBORRANYS = new HashMap<>();

    private OrdresGuerra() {
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("guerra")
                .executes(OrdresGuerra::resum)
                .then(Commands.literal("llista").executes(OrdresGuerra::llista))
                .then(Commands.literal("finestres").executes(OrdresGuerra::finestres))
                .then(Commands.literal("estat")
                        .executes(c -> estat(c, null))
                        .then(Commands.argument("sigla", StringArgumentType.word()).suggests(Ordres.SIGLES)
                                .executes(c -> estat(c, StringArgumentType.getString(c, "sigla")))))
                .then(Commands.literal("objectiu")
                        .executes(OrdresGuerra::mostrar)
                        .then(Commands.literal("mostra").executes(OrdresGuerra::mostrar))
                        .then(Commands.literal("afegeix").executes(c -> afegir(c, 0)))
                        .then(Commands.literal("radi")
                                .then(Commands.argument("radi", IntegerArgumentType.integer(0, 6))
                                        .executes(c -> afegir(c, IntegerArgumentType.getInteger(c, "radi")))))
                        .then(Commands.literal("treu").executes(OrdresGuerra::treure))
                        .then(Commands.literal("buida").executes(OrdresGuerra::buidar)))
                .then(Commands.literal("declara")
                        .then(Commands.argument("sigla", StringArgumentType.word()).suggests(Ordres.SIGLES)
                                .then(Commands.argument("objectiu", StringArgumentType.word()).suggests(OBJECTIUS)
                                        .executes(OrdresGuerra::declarar))))
                .then(Commands.literal("pau")
                        .then(Commands.argument("sigla", StringArgumentType.word()).suggests(Ordres.SIGLES)
                                .executes(OrdresGuerra::veurePau)
                                .then(Commands.literal("proposa")
                                        .then(Commands.argument("termes", StringArgumentType.greedyString()).suggests(TERMES)
                                                .executes(c -> proposar(c, false))))
                                .then(Commands.literal("imposa")
                                        .then(Commands.argument("termes", StringArgumentType.greedyString()).suggests(TERMES)
                                                .executes(c -> proposar(c, true))))
                                .then(Commands.literal("accepta")
                                        .executes(OrdresGuerra::veurePau)
                                        .then(Commands.argument("creada", LongArgumentType.longArg())
                                                .executes(OrdresGuerra::acceptar)))
                                .then(Commands.literal("rebutja").executes(OrdresGuerra::rebutjar))))
                .then(Commands.literal("rendeix")
                        .then(Commands.argument("sigla", StringArgumentType.word()).suggests(Ordres.SIGLES)
                                .executes(c -> {
                                    throw Ordres.ERROR.create("Rendir-se vol dir perdre la guerra. Si n'estàs segur, "
                                            + "escriu /guerra rendeix " + StringArgumentType.getString(c, "sigla") + " confirmo");
                                })
                                .then(Commands.literal("confirmo").executes(OrdresGuerra::rendir))))
                .then(Commands.literal("admin")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("diagnosi").executes(OrdresGuerra::diagnosi))
                        .then(Commands.literal("finestra")
                                .then(Commands.literal("obre").executes(c -> forcarFinestra(c, Boolean.TRUE)))
                                .then(Commands.literal("tanca").executes(c -> forcarFinestra(c, Boolean.FALSE)))
                                .then(Commands.literal("auto").executes(c -> forcarFinestra(c, null))))
                        .then(Commands.literal("comenca")
                                .then(Commands.argument("a", StringArgumentType.word()).suggests(Ordres.SIGLES)
                                        .then(Commands.argument("b", StringArgumentType.word()).suggests(Ordres.SIGLES)
                                                .executes(OrdresGuerra::adminComenca))))
                        .then(Commands.literal("punts")
                                .then(Commands.argument("a", StringArgumentType.word()).suggests(Ordres.SIGLES)
                                        .then(Commands.argument("b", StringArgumentType.word()).suggests(Ordres.SIGLES)
                                                .then(Commands.argument("punts", IntegerArgumentType.integer(-200, 200))
                                                        .executes(OrdresGuerra::adminPunts)))))
                        .then(Commands.literal("acaba")
                                .then(Commands.argument("a", StringArgumentType.word()).suggests(Ordres.SIGLES)
                                        .then(Commands.argument("b", StringArgumentType.word()).suggests(Ordres.SIGLES)
                                                .executes(OrdresGuerra::adminAcaba))))));
    }

    // ---------------------------------------------------------------- Consultes

    private static int resum(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Ordres.Jo jo = Ordres.jo(c);
        if (jo.pais() == null) {
            return llista(c);
        }
        List<Guerra> meves = jo.registre().guerresDe(jo.pais().id());
        CommandSourceStack s = c.getSource();
        if (meves.isEmpty()) {
            s.sendSuccess(() -> Missatges.gris("El teu país no és en guerra. ").append(
                    Missatges.boto("Totes les guerres", "/guerra llista", ChatFormatting.GOLD)).append(" ").append(
                    Missatges.boto("Finestres", "/guerra finestres", ChatFormatting.GOLD)), false);
            return 0;
        }
        for (Guerra g : meves) {
            s.sendSuccess(() -> liniaGuerra(jo.registre(), g, jo.pais().id()), false);
        }
        return meves.size();
    }

    private static int llista(CommandContext<CommandSourceStack> c) {
        Registre r = Dades.registre(c.getSource().getServer());
        if (r.guerres().isEmpty()) {
            c.getSource().sendSuccess(() -> Missatges.gris("Ara no hi ha cap guerra. Que duri."), false);
            return 0;
        }
        c.getSource().sendSuccess(() -> Missatges.titol("Guerres (" + r.guerres().size() + ")"), false);
        for (Guerra g : r.guerres()) {
            c.getSource().sendSuccess(() -> liniaGuerra(r, g, null), false);
        }
        return r.guerres().size();
    }

    /** «[CAT] Catalunya ⚔ [OCC] Occitània · conquesta · +12 · activa [Estat]», del punt de vista de {@code meu}. */
    private static MutableComponent liniaGuerra(Registre r, Guerra g, UUID meu) {
        long ara = Ordres.ara();
        MutableComponent m = Missatges.gris("• ");
        r.pais(g.atacant()).ifPresent(p -> m.append(Missatges.pais(p)));
        m.append(Component.literal(" ⚔ ").withStyle(ChatFormatting.RED));
        r.pais(g.defensor()).ifPresent(p -> m.append(Missatges.pais(p)));
        int punts = meu == null ? g.punts() : g.puntsDe(meu);
        m.append(Missatges.gris(" · " + g.objectiu().nom() + " · " + (punts > 0 ? "+" : "") + punts + " punts"
                + (meu == null ? " (atacant)" : "") + " · "
                + (g.activa(ara) ? "activa" : "preparació fins al " + Missatges.data(g.inici())) + " "));
        UUID altre = meu == null ? g.defensor() : g.enemic(meu);
        r.pais(altre).ifPresent(p -> m.append(Missatges.boto("Estat", "/guerra estat " + p.sigla(), ChatFormatting.GOLD)));
        return m;
    }

    private static int finestres(CommandContext<CommandSourceStack> c) {
        long ara = Ordres.ara();
        FinestresGuerra f = Config.finestres();
        CommandSourceStack s = c.getSource();
        s.sendSuccess(() -> Missatges.titol("Finestres de guerra"), false);
        s.sendSuccess(() -> Missatges.gris("De dilluns a divendres, de " + f.iniciFeiner() + " a " + f.fiFeiner()
                + "; dissabte i diumenge, de " + f.iniciCapDeSetmana() + " a " + f.fiCapDeSetmana() + " (" + f.zona() + ")."), false);
        if (Batalles.finestraOberta()) {
            long tanca = f.tancament(ara);
            s.sendSuccess(() -> Component.literal("Ara és oberta" + (tanca > 0 ? " fins al " + Missatges.data(tanca) : "")
                    + ": als chunks en disputa s'hi pot trencar i explotar, i hi ha PvP al territori dels països en guerra.")
                    .withStyle(ChatFormatting.RED), false);
        } else {
            s.sendSuccess(() -> Missatges.gris("Ara és tancada: tot està protegit. S'obre el "
                    + Missatges.data(f.properaObertura(ara)) + "."), false);
        }
        return 1;
    }

    private static int estat(CommandContext<CommandSourceStack> c, String sigla) throws CommandSyntaxException {
        MinecraftServer server = c.getSource().getServer();
        Registre r = Dades.registre(server);
        ServerPlayer jugador = c.getSource().getPlayer();
        IServerPartyAPI partit = jugador == null ? null : PontOpac.partitDe(server, jugador.getUUID());
        UUID meu = partit != null && r.pais(partit.getId()).isPresent() ? partit.getId() : null;
        Guerra g;
        if (sigla == null) {
            List<Guerra> meves = meu == null ? List.of() : r.guerresDe(meu);
            if (meves.size() != 1) {
                throw Ordres.ERROR.create(meves.isEmpty() ? "No ets en cap guerra. Mira-les totes amb /guerra llista."
                        : "Ets en més d'una guerra: digues amb quin país, /guerra estat <sigla>.");
            }
            g = meves.get(0);
        } else {
            Pais altre = r.perSigla(sigla).orElseThrow(() -> Ordres.ERROR.create("No hi ha cap país amb la sigla " + sigla + "."));
            g = (meu != null ? r.guerraEntre(meu, altre.id()) : java.util.Optional.<Guerra>empty())
                    .or(() -> r.guerresDe(altre.id()).stream().findFirst())
                    .orElseThrow(() -> Ordres.ERROR.create(Registre.etiqueta(altre) + " no és en cap guerra."));
        }
        mostrarEstat(c.getSource(), r, g, meu);
        return 1;
    }

    private static void mostrarEstat(CommandSourceStack s, Registre r, Guerra g, UUID meu) {
        long ara = Ordres.ara();
        FinestresGuerra f = Config.finestres();
        ParametresGuerra p = Config.parametres();
        Pais a = r.pais(g.atacant()).orElse(null);
        Pais d = r.pais(g.defensor()).orElse(null);
        if (a == null || d == null) {
            return;
        }
        s.sendSuccess(() -> Missatges.titol("━━ ").append(Missatges.pais(a))
                .append(Component.literal(" ⚔ ").withStyle(ChatFormatting.RED)).append(Missatges.pais(d))
                .append(Missatges.titol(" ━━")), false);
        String objectiu = g.objectiu() == TipusObjectiu.CONQUESTA
                ? "conquesta de " + g.chunks().size() + (g.chunks().size() == 1 ? " chunk" : " chunks")
                : g.objectiu().nom();
        s.sendSuccess(() -> Missatges.gris("Objectiu: " + objectiu + " · declarada el " + Missatges.data(g.declarada())), false);
        String fase;
        if (!g.activa(ara)) {
            fase = "Preparació fins al " + Missatges.data(g.inici()) + ". Primera batalla: "
                    + Missatges.data(f.properaObertura(g.inici())) + ".";
        } else if (Batalles.finestraOberta()) {
            long tanca = f.tancament(ara);
            fase = "Batalla en curs" + (tanca > 0 ? " fins al " + Missatges.data(tanca) : "") + ".";
        } else {
            fase = "Activa. Propera batalla: " + Missatges.data(f.properaObertura(ara)) + ".";
        }
        s.sendSuccess(() -> Component.literal(fase).withStyle(ChatFormatting.YELLOW), false);
        if (g.limit() > 0) {
            s.sendSuccess(() -> Missatges.gris("Si ningú no signa la pau, s'acaba sola el " + Missatges.data(g.limit())
                    + " (guanya qui tingui més punts)."), false);
        }
        int punts = g.punts();
        Pais guanya = punts >= 0 ? a : d;
        s.sendSuccess(() -> Missatges.gris("Punts: ").append(punts == 0 ? Component.literal("empat")
                : Component.literal(Math.abs(punts) + " a favor de ").append(Missatges.pais(guanya)))
                .append(Missatges.gris(" · baixes: " + a.sigla() + " " + g.baixesAtacant() + ", " + d.sigla() + " "
                        + g.baixesDefensor())), false);
        if (!g.chunks().isEmpty()) {
            String chunks = g.perControl().stream().limit(12)
                    .map(ch -> ch.x() + "," + ch.z() + " " + g.control(ch) * 100 / Guerra.CONTROL_MAX + "%")
                    .collect(Collectors.joining(" · "));
            s.sendSuccess(() -> Missatges.gris("Chunks (" + g.controlats().size() + " de " + g.chunks().size()
                    + " presos): " + chunks + (g.chunks().size() > 12 ? " ..." : "")), false);
        }
        if (g.capitalDefensor() != null) {
            s.sendSuccess(() -> Missatges.gris("Capital de " + d.sigla() + ": " + g.capitalDefensor().x() + ","
                    + g.capitalDefensor().z() + " (" + g.controlCapitalDefensor() * 100 / Guerra.CONTROL_MAX + "% presa)"), false);
        }
        if (g.capitalAtacant() != null) {
            s.sendSuccess(() -> Missatges.gris("Capital de " + a.sigla() + ": " + g.capitalAtacant().x() + ","
                    + g.capitalAtacant().z() + " (" + g.controlCapitalAtacant() * 100 / Guerra.CONTROL_MAX + "% presa)"), false);
        }
        if (meu != null && g.hiEs(meu)) {
            int vides = Math.max(0, p.vides() - (s.getPlayer() == null ? 0 : g.mortsDe(s.getPlayer().getUUID())));
            s.sendSuccess(() -> Missatges.gris("Et queden " + vides + " de " + p.vides() + " vides en aquesta batalla."), false);
            Pais enemic = g.enemic(meu).equals(a.id()) ? a : d;
            mostrarPau(s, r, g, meu, enemic);
            s.sendSuccess(() -> Missatges.boto("Proposa pau blanca", "/guerra pau " + enemic.sigla() + " proposa blanca",
                    ChatFormatting.GREEN).append(" ").append(Missatges.boto("Rendeix-te", "/guerra rendeix " + enemic.sigla(),
                    ChatFormatting.RED)), false);
        }
    }

    private static void mostrarPau(CommandSourceStack s, Registre r, Guerra g, UUID meu, Pais enemic) {
        r.propostaPau(g.id()).ifPresent(pr -> {
            String termes = descriu(r, g, pr.guanyador(), pr.termes());
            if (pr.de().equals(meu)) {
                s.sendSuccess(() -> Missatges.gris("Heu proposat la pau: " + termes + ". Esperant resposta. ")
                        .append(Missatges.boto("Retira-la", "/guerra pau " + enemic.sigla() + " rebutja", ChatFormatting.RED)), false);
            } else {
                s.sendSuccess(() -> Missatges.pais(enemic).append(Component.literal(" proposa la pau: " + termes + " ")
                        .withStyle(ChatFormatting.YELLOW))
                        .append(Missatges.boto("Accepta", "/guerra pau " + enemic.sigla() + " accepta " + pr.creada(),
                                ChatFormatting.GREEN))
                        .append(" ")
                        .append(Missatges.boto("Rebutja", "/guerra pau " + enemic.sigla() + " rebutja", ChatFormatting.RED)), false);
            }
        });
    }

    private static String descriu(Registre r, Guerra g, UUID guanyador, Termes t) {
        if (t.esBlanca()) {
            return "pau blanca";
        }
        String qui = r.pais(guanyador).map(Pais::sigla).orElse("?");
        String perd = r.pais(g.enemic(guanyador)).map(Pais::sigla).orElse("?");
        List<String> parts = new ArrayList<>();
        if (!t.chunks().isEmpty()) {
            parts.add(perd + " cedeix " + t.chunks().size() + (t.chunks().size() == 1 ? " chunk" : " chunks") + " a " + qui);
        }
        if (t.humiliacio()) {
            parts.add(perd + " queda humiliat");
        }
        return String.join(" i ", parts) + " (" + t.cost(g) + " punts)";
    }

    // ---------------------------------------------------------------- Objectiu

    private static int mostrar(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Ordres.Jo jo = Ordres.jo(c, Permis.GUERRA);
        Esborrany e = ESBORRANYS.get(jo.pais().id());
        if (e == null || e.chunks().isEmpty()) {
            c.getSource().sendSuccess(() -> Missatges.gris("L'objectiu és buit. Posa't en un chunk del país que vols "
                    + "atacar i fes /guerra objectiu afegeix (o /guerra objectiu radi <r>)."), false);
            return 0;
        }
        Pais contra = jo.registre().pais(e.contra()).orElse(null);
        IServerPartyAPI pd = contra == null ? null : PontOpac.partit(jo.server(), contra.id());
        int max = pd == null ? 0 : Config.parametres().maxChunksContra(ZonaOpac.territori(jo.server(), pd).size());
        String llista = e.chunks().stream().map(ch -> ch.x() + "," + ch.z()).collect(Collectors.joining(" · "));
        c.getSource().sendSuccess(() -> Missatges.gris("Objectiu contra ")
                .append(contra == null ? Component.literal("?") : Missatges.pais(contra))
                .append(Missatges.gris(": " + e.chunks().size() + " de " + max + " chunks possibles (" + llista + ") "))
                .append(contra == null ? Component.empty() : Missatges.boto("Declara la guerra",
                        "/guerra declara " + contra.sigla() + " conquesta", ChatFormatting.RED)), false);
        dibuixar(jo.jugador(), e.chunks());
        return e.chunks().size();
    }

    private static int afegir(CommandContext<CommandSourceStack> c, int radi) throws CommandSyntaxException {
        Ordres.Jo jo = Ordres.jo(c, Permis.GUERRA);
        Chunk aqui = aqui(jo.jugador());
        UUID contra = ZonaOpac.paisDelChunk(jo.server(), aqui);
        if (contra == null || jo.registre().pais(contra).isEmpty()) {
            throw Ordres.ERROR.create("Aquest chunk no és de cap país.");
        }
        if (contra.equals(jo.pais().id())) {
            throw Ordres.ERROR.create("Aquest chunk és del teu país.");
        }
        Esborrany e = ESBORRANYS.get(jo.pais().id());
        if (e != null && !e.contra().equals(contra) && !e.chunks().isEmpty()) {
            throw Ordres.ERROR.create("L'objectiu que prepares és contra un altre país. Buida'l primer amb /guerra objectiu buida.");
        }
        if (e == null || !e.contra().equals(contra)) {
            e = new Esborrany(contra, new LinkedHashSet<>());
            ESBORRANYS.put(jo.pais().id(), e);
        }
        IServerPartyAPI pd = PontOpac.partit(jo.server(), contra);
        Set<Chunk> seus = pd == null ? Set.of() : ZonaOpac.territori(jo.server(), pd);
        int abans = e.chunks().size();
        if (radi == 0) {
            e.chunks().add(aqui);
        } else {
            e.chunks().addAll(Territori.voltant(aqui, radi, seus));
        }
        int afegits = e.chunks().size() - abans;
        c.getSource().sendSuccess(() -> Missatges.bo(afegits == 0 ? "Aquests chunks ja hi eren."
                : "Afegits " + afegits + (afegits == 1 ? " chunk" : " chunks") + " a l'objectiu."), false);
        return mostrar(c);
    }

    private static int treure(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Ordres.Jo jo = Ordres.jo(c, Permis.GUERRA);
        Esborrany e = ESBORRANYS.get(jo.pais().id());
        if (e == null || !e.chunks().remove(aqui(jo.jugador()))) {
            throw Ordres.ERROR.create("Aquest chunk no és a l'objectiu.");
        }
        c.getSource().sendSuccess(() -> Missatges.bo("Tret de l'objectiu."), false);
        return mostrar(c);
    }

    private static int buidar(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Ordres.Jo jo = Ordres.jo(c, Permis.GUERRA);
        ESBORRANYS.remove(jo.pais().id());
        c.getSource().sendSuccess(() -> Missatges.bo("Objectiu buidat."), false);
        return 1;
    }

    private static Chunk aqui(ServerPlayer j) {
        return new Chunk(j.level().dimension().location().toString(), j.chunkPosition().x, j.chunkPosition().z);
    }

    /** Dibuixa amb partícules la vora dels chunks de l'objectiu que són a la dimensió del jugador. */
    private static void dibuixar(ServerPlayer j, Set<Chunk> chunks) {
        ServerLevel nivell = j.serverLevel();
        String dim = nivell.dimension().location().toString();
        double y = j.getY() + 0.5;
        for (Chunk ch : chunks) {
            if (!ch.dimensio().equals(dim)) {
                continue;
            }
            for (Chunk v : Territori.veins(ch)) {
                if (chunks.contains(v)) {
                    continue;
                }
                double x0 = ch.x() * 16;
                double z0 = ch.z() * 16;
                for (int i = 0; i <= 16; i += 2) {
                    double x;
                    double z;
                    if (v.x() != ch.x()) {
                        x = v.x() > ch.x() ? x0 + 16 : x0;
                        z = z0 + i;
                    } else {
                        x = x0 + i;
                        z = v.z() > ch.z() ? z0 + 16 : z0;
                    }
                    nivell.sendParticles(j, ParticleTypes.FLAME, true, x, y, z, 1, 0, 0.3, 0, 0);
                }
            }
        }
    }

    // ---------------------------------------------------------------- Declarar

    private static int declarar(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Ordres.Jo jo = Ordres.jo(c, Permis.GUERRA);
        Pais altre = Ordres.altrePais(jo.registre(), c);
        String clau = StringArgumentType.getString(c, "objectiu").toLowerCase(Locale.ROOT).replace("ó", "o");
        TipusObjectiu objectiu = TipusObjectiu.perClau(clau).orElseThrow(() -> Ordres.ERROR.create(
                "Els objectius són: conquesta, humiliacio i reparacions."));
        Set<Chunk> chunks = Set.of();
        if (objectiu == TipusObjectiu.CONQUESTA) {
            Esborrany e = ESBORRANYS.get(jo.pais().id());
            if (e == null || e.chunks().isEmpty() || !e.contra().equals(altre.id())) {
                throw Ordres.ERROR.create("Primer prepara l'objectiu: posa't als chunks de " + Registre.etiqueta(altre)
                        + " que vols i fes /guerra objectiu afegeix (o /guerra objectiu radi <r>).");
            }
            chunks = Set.copyOf(e.chunks());
        }
        try {
            Guerres.declarar(jo.server(), jo.pais(), altre, objectiu, chunks);
        } catch (Guerres.Error err) {
            throw Ordres.ERROR.create(err.getMessage());
        }
        ESBORRANYS.remove(jo.pais().id());
        return Ordres.resposta(c, Resultat.DECLARADA);
    }

    // ---------------------------------------------------------------- Pau

    private static Guerra guerraAmb(Ordres.Jo jo, Pais altre) throws CommandSyntaxException {
        return jo.registre().guerraEntre(jo.pais().id(), altre.id()).orElseThrow(() -> Ordres.ERROR.create(
                "No sou en guerra amb " + Registre.etiqueta(altre) + "."));
    }

    private static int veurePau(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Ordres.Jo jo = Ordres.jo(c, Permis.PAU);
        Pais altre = Ordres.altrePais(jo.registre(), c);
        Guerra g = guerraAmb(jo, altre);
        if (jo.registre().propostaPau(g.id()).isEmpty()) {
            c.getSource().sendSuccess(() -> Missatges.gris("No hi ha cap proposta de pau. Proposa'n una amb /guerra pau "
                    + altre.sigla() + " proposa <termes>: blanca, guanyem [objectiu|controlats] [humiliacio] o "
                    + "guanyeu [objectiu|controlats] [humiliacio]."), false);
            return 0;
        }
        mostrarPau(c.getSource(), jo.registre(), g, jo.pais().id(), altre);
        return 1;
    }

    private static int proposar(CommandContext<CommandSourceStack> c, boolean imposar) throws CommandSyntaxException {
        Ordres.Jo jo = Ordres.jo(c, Permis.PAU);
        Pais altre = Ordres.altrePais(jo.registre(), c);
        Guerra g = guerraAmb(jo, altre);
        String[] paraules = StringArgumentType.getString(c, "termes").toLowerCase(Locale.ROOT).replace("ó", "o")
                .trim().split("\\s+");
        UUID guanyador;
        Registre.QuinsChunks quins = Registre.QuinsChunks.CAP;
        boolean humiliacio = false;
        switch (paraules[0]) {
            case "blanca" -> guanyador = g.atacant();
            case "guanyem" -> guanyador = jo.pais().id();
            case "guanyeu" -> guanyador = altre.id();
            default -> throw Ordres.ERROR.create("Els termes comencen per blanca, guanyem o guanyeu.");
        }
        if (!paraules[0].equals("blanca")) {
            for (int i = 1; i < paraules.length; i++) {
                switch (paraules[i]) {
                    case "objectiu" -> quins = Registre.QuinsChunks.OBJECTIU;
                    case "controlats" -> quins = Registre.QuinsChunks.CONTROLATS;
                    case "humiliacio" -> humiliacio = true;
                    default -> throw Ordres.ERROR.create("No entenc «" + paraules[i] + "»: després de guanyem o guanyeu "
                            + "hi pot anar objectiu o controlats, i humiliacio.");
                }
            }
            if (quins != Registre.QuinsChunks.CAP && !guanyador.equals(g.atacant())) {
                throw Ordres.ERROR.create("Els chunks de l'objectiu només els pot guanyar l'atacant.");
            }
            if (quins == Registre.QuinsChunks.CAP && !humiliacio) {
                throw Ordres.ERROR.create("Uns termes sense chunks ni humiliació són una pau blanca: escriu blanca.");
            }
        }
        Termes termes = Registre.termes(g, guanyador, quins, humiliacio);
        if (quins == Registre.QuinsChunks.CONTROLATS && termes.chunks().isEmpty() && !humiliacio) {
            throw Ordres.ERROR.create("L'atacant encara no controla del tot cap chunk de l'objectiu.");
        }
        if (imposar) {
            if (!guanyador.equals(jo.pais().id()) || termes.esBlanca()) {
                throw Ordres.ERROR.create("Només es poden imposar termes a favor teu (guanyem ...).");
            }
            Resultat r = Guerres.signarPau(jo.server(), g, guanyador, termes, true);
            if (r == Resultat.PUNTS_INSUFICIENTS) {
                throw Ordres.ERROR.create("Aquests termes costen " + termes.cost(g) + " punts i en teniu "
                        + g.puntsDe(jo.pais().id()) + ". Proposa'ls (l'altre els pot acceptar) o lluiteu més.");
            }
            return Ordres.resposta(c, r);
        }
        Resultat r = jo.registre().proposarPau(jo.pais().id(), g.id(), guanyador, termes, Ordres.ara());
        if (r == Resultat.PAU_PROPOSADA) {
            String text = descriu(jo.registre(), g, guanyador, termes);
            long creada = jo.registre().propostaPau(g.id()).orElseThrow().creada();
            Avisos.xat(Avisos.jugadors(jo.server(), altre.id()), Missatges.pais(jo.pais())
                    .append(Component.literal(" us proposa la pau: " + text + " ").withStyle(ChatFormatting.YELLOW))
                    .append(Missatges.boto("Accepta", "/guerra pau " + jo.pais().sigla() + " accepta " + creada, ChatFormatting.GREEN))
                    .append(" ")
                    .append(Missatges.boto("Rebutja", "/guerra pau " + jo.pais().sigla() + " rebutja", ChatFormatting.RED)));
            Avisos.jugadors(jo.server(), altre.id()).forEach(j -> Avisos.so(j, Avisos.So.DIPLOMACIA));
            if (termes.cost(g) <= g.puntsDe(jo.pais().id()) && guanyador.equals(jo.pais().id()) && !termes.esBlanca()) {
                c.getSource().sendSuccess(() -> Missatges.gris("Teniu prou punts per imposar-los: ")
                        .append(Missatges.boto("Imposa", "/guerra pau " + altre.sigla() + " imposa "
                                + StringArgumentType.getString(c, "termes"), ChatFormatting.RED)), false);
            }
        }
        return Ordres.resposta(c, r);
    }

    private static int acceptar(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Ordres.Jo jo = Ordres.jo(c, Permis.PAU);
        Pais altre = Ordres.altrePais(jo.registre(), c);
        Guerra g = guerraAmb(jo, altre);
        Resultat r = Guerres.acceptarPau(jo.server(), jo.pais().id(), g, LongArgumentType.getLong(c, "creada"));
        if (r == Resultat.NO_HI_HA_PROPOSTA) {
            throw Ordres.ERROR.create("Aquesta proposta ja no hi és o ha canviat. Mira la d'ara amb /guerra pau " + altre.sigla() + ".");
        }
        return Ordres.resposta(c, r);
    }

    private static int rebutjar(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Ordres.Jo jo = Ordres.jo(c, Permis.PAU);
        Pais altre = Ordres.altrePais(jo.registre(), c);
        Guerra g = guerraAmb(jo, altre);
        Resultat r = jo.registre().retirarPau(jo.pais().id(), g.id());
        if (r.bo()) {
            Avisos.xat(Avisos.jugadors(jo.server(), altre.id(), jo.pais().id()), Missatges.gris("La proposta de pau entre ")
                    .append(Missatges.pais(jo.pais())).append(Missatges.gris(" i ")).append(Missatges.pais(altre))
                    .append(Missatges.gris(" ja no hi és.")));
        }
        return Ordres.resposta(c, r);
    }

    private static int rendir(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Ordres.Jo jo = Ordres.jo(c, Permis.PAU);
        Pais altre = Ordres.altrePais(jo.registre(), c);
        Guerra g = guerraAmb(jo, altre);
        return Ordres.resposta(c, Guerres.rendir(jo.server(), jo.pais().id(), g));
    }

    // ---------------------------------------------------------------- Admin

    private static int diagnosi(CommandContext<CommandSourceStack> c) {
        for (String linia : Batalles.diagnosi(c.getSource().getServer())) {
            c.getSource().sendSuccess(() -> Missatges.gris(linia), false);
        }
        return 1;
    }

    private static int forcarFinestra(CommandContext<CommandSourceStack> c, Boolean oberta) {
        Batalles.forcarFinestra(oberta);
        c.getSource().sendSuccess(() -> Missatges.bo(oberta == null ? "La finestra torna a seguir l'horari."
                : oberta ? "Finestra oberta a mà (fins que facis /guerra admin finestra auto)."
                : "Finestra tancada a mà (fins que facis /guerra admin finestra auto)."), true);
        return 1;
    }

    private static Guerra guerraAdmin(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Registre r = Dades.registre(c.getSource().getServer());
        String sa = StringArgumentType.getString(c, "a");
        String sb = StringArgumentType.getString(c, "b");
        Pais a = r.perSigla(sa).orElseThrow(() -> Ordres.ERROR.create("No hi ha cap país " + sa + "."));
        Pais b = r.perSigla(sb).orElseThrow(() -> Ordres.ERROR.create("No hi ha cap país " + sb + "."));
        return r.guerraEntre(a.id(), b.id()).orElseThrow(() -> Ordres.ERROR.create("No són en guerra."));
    }

    private static int adminComenca(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Guerra g = guerraAdmin(c);
        Dades.registre(c.getSource().getServer()).acabarPreparacio(g.id(), Ordres.ara());
        c.getSource().sendSuccess(() -> Missatges.bo("S'ha acabat la preparació: la guerra ja és activa."), true);
        return 1;
    }

    private static int adminPunts(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Guerra g = guerraAdmin(c);
        Registre r = Dades.registre(c.getSource().getServer());
        Pais a = r.perSigla(StringArgumentType.getString(c, "a")).orElseThrow();
        r.sumarPunts(g.id(), a.id(), IntegerArgumentType.getInteger(c, "punts"));
        c.getSource().sendSuccess(() -> Missatges.bo("Ara " + a.sigla() + " té " + g.puntsDe(a.id()) + " punts."), true);
        return 1;
    }

    private static int adminAcaba(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Guerra g = guerraAdmin(c);
        return Ordres.resposta(c, Guerres.acabarEnBlanc(c.getSource().getServer(), g));
    }
}
