package cat.mineuab.pauitreva.ordres;

import cat.mineuab.pauitreva.Config;
import cat.mineuab.pauitreva.guerra.Guerres;
import cat.mineuab.pauitreva.model.Capital;
import cat.mineuab.pauitreva.model.Guerra;
import cat.mineuab.pauitreva.model.EstatRelacio;
import cat.mineuab.pauitreva.model.Esdeveniment;
import cat.mineuab.pauitreva.model.Pais;
import cat.mineuab.pauitreva.model.Permis;
import cat.mineuab.pauitreva.model.Proposta;
import cat.mineuab.pauitreva.model.Rang;
import cat.mineuab.pauitreva.model.Registre;
import cat.mineuab.pauitreva.model.Resultat;
import cat.mineuab.pauitreva.model.TipusRelacio;
import cat.mineuab.pauitreva.model.Validacio;
import cat.mineuab.pauitreva.mon.Dades;
import cat.mineuab.pauitreva.opac.PontOpac;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import xaero.pac.common.parties.party.member.api.IPartyMemberAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

/**
 * Ordres {@code /nacio} i {@code /diplomacia}. Més endavant, la pantalla del joc (tecla N) farà el mateix
 * per sota, i les ordres quedaran com a drecera.
 */
public final class Ordres {

    static final DynamicCommandExceptionType ERROR =
            new DynamicCommandExceptionType(m -> Component.literal(String.valueOf(m)));

    /** Colors per als països nous, fins que el líder en triï un. */
    private static final int[] PALETA = {
            0xC0392B, 0x2980B9, 0x27AE60, 0x8E44AD, 0xD35400, 0x16A085, 0xF1C40F, 0x2C3E50, 0xE84393, 0x6D4C41,
    };

    static final SuggestionProvider<CommandSourceStack> SIGLES = (c, b) -> SharedSuggestionProvider.suggest(
            Dades.registre(c.getSource().getServer()).paisos().stream().map(Pais::sigla), b);
    private static final SuggestionProvider<CommandSourceStack> BANDERES = (c, b) -> SharedSuggestionProvider.suggest(
            cat.mineuab.pauitreva.banderes.Banderes.totes().stream().map(cat.mineuab.pauitreva.banderes.Banderes.Bandera::id), b);
    private static final SuggestionProvider<CommandSourceStack> RANGS = (c, b) -> SharedSuggestionProvider.suggest(
            Arrays.stream(Rang.values()).map(Rang::clau), b);
    private static final SuggestionProvider<CommandSourceStack> PERMISOS = (c, b) -> SharedSuggestionProvider.suggest(
            Arrays.stream(Permis.values()).filter(Permis::configurable).map(Permis::clau), b);

    private Ordres() {
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("nacio")
                .executes(c -> info(c, null))
                .then(Commands.literal("info")
                        .executes(c -> info(c, null))
                        .then(Commands.argument("sigla", StringArgumentType.word()).suggests(SIGLES)
                                .executes(c -> info(c, StringArgumentType.getString(c, "sigla")))))
                .then(Commands.literal("membres")
                        .executes(c -> membres(c, null))
                        .then(Commands.argument("sigla", StringArgumentType.word()).suggests(SIGLES)
                                .executes(c -> membres(c, StringArgumentType.getString(c, "sigla")))))
                .then(Commands.literal("llista").executes(Ordres::llista))
                .then(Commands.literal("diari").executes(Ordres::diari))
                .then(Commands.literal("funda")
                        .then(Commands.argument("sigla", StringArgumentType.word())
                                .then(Commands.argument("nom", StringArgumentType.greedyString())
                                        .executes(Ordres::funda))))
                .then(Commands.literal("nom")
                        .then(Commands.argument("nom", StringArgumentType.greedyString()).executes(Ordres::nom)))
                .then(Commands.literal("sigla")
                        .then(Commands.argument("sigla", StringArgumentType.word()).executes(Ordres::sigla)))
                .then(Commands.literal("color")
                        .then(Commands.argument("color", StringArgumentType.greedyString()).executes(Ordres::color)))
                .then(Commands.literal("lema")
                        .executes(c -> lema(c, ""))
                        .then(Commands.argument("lema", StringArgumentType.greedyString())
                                .executes(c -> lema(c, StringArgumentType.getString(c, "lema")))))
                .then(Commands.literal("bandera")
                        .executes(c -> bandera(c, ""))
                        .then(Commands.argument("bandera", StringArgumentType.greedyString()).suggests(BANDERES)
                                .executes(c -> bandera(c, StringArgumentType.getString(c, "bandera")))))
                .then(Commands.literal("capital").executes(Ordres::capital))
                .then(Commands.literal("rang")
                        .then(Commands.argument("rang", StringArgumentType.word()).suggests(RANGS)
                                .executes(c -> rang(c, ""))
                                .then(Commands.argument("nom", StringArgumentType.greedyString())
                                        .executes(c -> rang(c, StringArgumentType.getString(c, "nom"))))))
                .then(Commands.literal("permis")
                        .then(Commands.argument("permis", StringArgumentType.word()).suggests(PERMISOS)
                                .then(Commands.argument("rang", StringArgumentType.word()).suggests(RANGS)
                                        .executes(Ordres::permis))))
                .then(Commands.literal("dissol")
                        .then(Commands.literal("confirmo").executes(Ordres::dissol)))
                .then(Commands.literal("admin")
                        .requires(s -> s.hasPermission(3))
                        .then(Commands.literal("migra")
                                .then(Commands.literal("prova").executes(c -> migrar(c, true)))
                                .then(Commands.literal("fes")
                                        .then(Commands.literal("confirmo").executes(c -> migrar(c, false)))))));

        d.register(Commands.literal("diplomacia")
                .executes(Ordres::relacions)
                .then(Commands.literal("alianca")
                        .then(Commands.argument("sigla", StringArgumentType.word()).suggests(SIGLES)
                                .executes(c -> proposar(c, TipusRelacio.ALIANCA, 0))))
                .then(Commands.literal("pacte")
                        .then(Commands.argument("sigla", StringArgumentType.word()).suggests(SIGLES)
                                .then(Commands.argument("dies", IntegerArgumentType.integer(1, 365))
                                        .executes(c -> proposar(c, TipusRelacio.PACTE,
                                                IntegerArgumentType.getInteger(c, "dies"))))))
                .then(Commands.literal("accepta")
                        .then(Commands.argument("sigla", StringArgumentType.word()).suggests(SIGLES)
                                .executes(Ordres::accepta)))
                .then(Commands.literal("rebutja")
                        .then(Commands.argument("sigla", StringArgumentType.word()).suggests(SIGLES)
                                .executes(Ordres::rebutja)))
                .then(Commands.literal("trenca")
                        .then(Commands.argument("sigla", StringArgumentType.word()).suggests(SIGLES)
                                .executes(Ordres::trenca))));
    }

    // ---------------------------------------------------------------- Qui executa l'ordre

    /** El jugador que executa l'ordre, la seva party, el seu país (si en té) i el seu rang. */
    record Jo(ServerPlayer jugador, MinecraftServer server, Registre registre, IServerPartyAPI partit,
                      Pais pais, Rang rang) {
    }

    static Jo jo(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer jugador = c.getSource().getPlayerOrException();
        MinecraftServer server = c.getSource().getServer();
        Registre r = Dades.registre(server);
        IServerPartyAPI partit = PontOpac.partitDe(server, jugador.getUUID());
        Pais pais = partit == null ? null : r.pais(partit.getId()).orElse(null);
        Rang rang = partit == null ? null : PontOpac.rang(partit, jugador.getUUID());
        return new Jo(jugador, server, r, partit, pais, rang);
    }

    /** Com {@link #jo}, però cal tenir país i el permís que es demana. */
    static Jo jo(CommandContext<CommandSourceStack> c, Permis permis) throws CommandSyntaxException {
        Jo jo = jo(c);
        if (jo.pais == null) {
            throw ERROR.create("No ets de cap país. Funda'n un amb /nacio funda <sigla> <nom>, "
                    + "o entra a la party d'OPAC d'un país que ja existeixi.");
        }
        if (!jo.pais.pot(jo.rang, permis)) {
            Rang cal = jo.pais.rangPer(permis);
            throw ERROR.create("Per fer això cal ser " + jo.pais.nomRang(cal) + (cal == Rang.LIDER ? "." : " o més."));
        }
        return jo;
    }

    static Pais altrePais(Registre r, CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        String sigla = StringArgumentType.getString(c, "sigla");
        return r.perSigla(sigla).orElseThrow(() -> ERROR.create("No hi ha cap país amb la sigla " + sigla + "."));
    }

    static int resposta(CommandContext<CommandSourceStack> c, Resultat r) throws CommandSyntaxException {
        if (!r.bo()) {
            throw ERROR.create(Missatges.resultat(r));
        }
        c.getSource().sendSuccess(() -> Missatges.bo(Missatges.resultat(r)), false);
        return 1;
    }

    static long ara() {
        return System.currentTimeMillis();
    }

    // ---------------------------------------------------------------- /nacio

    private static int info(CommandContext<CommandSourceStack> c, String sigla) throws CommandSyntaxException {
        MinecraftServer server = c.getSource().getServer();
        Registre r = Dades.registre(server);
        Pais p;
        boolean meu;
        if (sigla == null) {
            Jo jo = jo(c);
            if (jo.pais == null) {
                throw ERROR.create("No ets de cap país. Mira'ls tots amb /nacio llista, o funda'n un amb "
                        + "/nacio funda <sigla> <nom>.");
            }
            p = jo.pais;
            meu = true;
        } else {
            p = r.perSigla(sigla).orElseThrow(() -> ERROR.create("No hi ha cap país amb la sigla " + sigla + "."));
            ServerPlayer jugador = c.getSource().getPlayer();
            meu = jugador != null && p.id().equals(idPartit(server, jugador.getUUID()));
        }

        IServerPartyAPI partit = PontOpac.partit(server, p.id());
        CommandSourceStack s = c.getSource();
        s.sendSuccess(() -> Missatges.titol("━━ ").append(Missatges.pais(p)).append(Missatges.titol(" ━━")), false);
        if (!p.lema().isEmpty()) {
            s.sendSuccess(() -> Component.literal("«" + p.lema() + "»").withStyle(ChatFormatting.ITALIC), false);
        }
        String lider = partit == null ? "?" : PontOpac.nomLider(partit);
        int membres = partit == null ? 0 : partit.getMemberCount();
        s.sendSuccess(() -> Missatges.gris(p.nomRang(Rang.LIDER) + ": ").append(Component.literal(lider))
                .append(Missatges.gris(" · " + membres + (membres == 1 ? " membre" : " membres")
                        + " · fundat el " + Missatges.dia(p.fundat()))), false);
        String capital = p.capital() == null ? "sense capital"
                : "chunk " + p.capital().chunkX() + ", " + p.capital().chunkZ() + " (" + p.capital().dimensio() + ")";
        s.sendSuccess(() -> Missatges.gris("Capital: " + capital
                + (p.bandera().isEmpty() ? "" : " · Bandera: " + p.bandera())), false);

        if (p.humiliat(ara())) {
            s.sendSuccess(() -> Component.literal("Humiliat per una guerra perduda fins al " + Missatges.data(p.humiliatFins()) + ".")
                    .withStyle(ChatFormatting.RED), false);
        }
        Map<UUID, EstatRelacio> rel = r.relacionsDe(p.id());
        if (rel.isEmpty()) {
            s.sendSuccess(() -> Missatges.gris("Sense aliances ni pactes."), false);
        } else {
            for (Map.Entry<UUID, EstatRelacio> e : rel.entrySet()) {
                r.pais(e.getKey()).ifPresent(altre -> s.sendSuccess(() -> Missatges.gris("• ")
                        .append(Component.literal(capitalitza(e.getValue().tipus().nom()) + " amb "))
                        .append(Missatges.pais(altre))
                        .append(Missatges.gris(e.getValue().fins() > 0
                                ? " (fins al " + Missatges.data(e.getValue().fins()) + ")" : "")), false));
            }
        }
        if (meu) {
            propostesPendents(s, r, p);
        }
        return 1;
    }

    private static int membres(CommandContext<CommandSourceStack> c, String sigla) throws CommandSyntaxException {
        MinecraftServer server = c.getSource().getServer();
        Registre r = Dades.registre(server);
        Pais p;
        if (sigla == null) {
            Jo jo = jo(c);
            if (jo.pais == null) {
                throw ERROR.create("No ets de cap país.");
            }
            p = jo.pais;
        } else {
            p = r.perSigla(sigla).orElseThrow(() -> ERROR.create("No hi ha cap país amb la sigla " + sigla + "."));
        }
        IServerPartyAPI partit = PontOpac.partit(server, p.id());
        if (partit == null) {
            throw ERROR.create("La party d'aquest país ja no existeix.");
        }
        c.getSource().sendSuccess(() -> Missatges.titol("Membres de ").append(Missatges.pais(p)), false);
        for (IPartyMemberAPI m : PontOpac.membres(partit)) {
            Rang rang = PontOpac.rang(partit, m.getUUID());
            c.getSource().sendSuccess(() -> Missatges.gris("• " + (rang == null ? "" : p.nomRang(rang)) + ": ")
                    .append(Component.literal(m.getUsername())), false);
        }
        return 1;
    }

    private static int llista(CommandContext<CommandSourceStack> c) {
        MinecraftServer server = c.getSource().getServer();
        Registre r = Dades.registre(server);
        if (r.paisos().isEmpty()) {
            c.getSource().sendSuccess(() -> Missatges.gris("Encara no hi ha cap país. Funda'n un amb "
                    + "/nacio funda <sigla> <nom>."), false);
            return 0;
        }
        c.getSource().sendSuccess(() -> Missatges.titol("Països (" + r.paisos().size() + ")"), false);
        for (Pais p : r.paisos()) {
            IServerPartyAPI partit = PontOpac.partit(server, p.id());
            int membres = partit == null ? 0 : partit.getMemberCount();
            c.getSource().sendSuccess(() -> Missatges.gris("• ").append(Missatges.pais(p))
                    .append(Missatges.gris(" · " + membres + (membres == 1 ? " membre" : " membres"))), false);
        }
        return r.paisos().size();
    }

    private static int diari(CommandContext<CommandSourceStack> c) {
        Registre r = Dades.registre(c.getSource().getServer());
        List<Esdeveniment> darrers = r.darrers(10);
        if (darrers.isEmpty()) {
            c.getSource().sendSuccess(() -> Missatges.gris("El diari diplomàtic és buit."), false);
            return 0;
        }
        c.getSource().sendSuccess(() -> Missatges.titol("Diari diplomàtic"), false);
        for (int i = darrers.size() - 1; i >= 0; i--) {
            Esdeveniment e = darrers.get(i);
            c.getSource().sendSuccess(() -> Missatges.gris(Missatges.data(e.quan()) + " ")
                    .append(Component.literal(e.text())), false);
        }
        return darrers.size();
    }

    private static int funda(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Jo jo = jo(c);
        String sigla = Validacio.normalitzarSigla(StringArgumentType.getString(c, "sigla"));
        String nom = Validacio.normalitzarText(StringArgumentType.getString(c, "nom"));
        if (jo.pais != null) {
            throw ERROR.create("Ja ets de " + Registre.etiqueta(jo.pais) + ".");
        }
        // Abans de crear cap party, que el nom i la sigla valguin.
        if (!Validacio.siglaValida(sigla)) {
            throw ERROR.create(Missatges.resultat(Resultat.SIGLA_INVALIDA));
        }
        if (!Validacio.nomValid(nom)) {
            throw ERROR.create(Missatges.resultat(Resultat.NOM_INVALID));
        }
        if (jo.registre.perSigla(sigla).isPresent()) {
            throw ERROR.create(Missatges.resultat(Resultat.SIGLA_OCUPADA));
        }
        if (jo.registre.perNom(nom).isPresent()) {
            throw ERROR.create(Missatges.resultat(Resultat.NOM_OCUPAT));
        }

        IServerPartyAPI partit = jo.partit;
        if (partit == null) {
            partit = PontOpac.crearPartit(jo.jugador);
            if (partit == null) {
                throw ERROR.create("OPAC no ha deixat crear la party del país.");
            }
        } else if (jo.rang != Rang.LIDER) {
            throw ERROR.create("Ets d'una party d'OPAC i no n'ets el propietari: només el propietari hi pot fundar "
                    + "un país.");
        }

        int color = PALETA[Math.floorMod(partit.getId().hashCode(), PALETA.length)];
        Resultat res = jo.registre.fundar(partit.getId(), nom, sigla, color, ara());
        if (!res.bo()) {
            throw ERROR.create(Missatges.resultat(res));
        }
        Pais p = jo.registre.pais(partit.getId()).orElseThrow();
        PontOpac.aplicarIdentitat(jo.server, p);
        Missatges.anunciar(jo.server, "S'ha fundat " + Registre.etiqueta(p) + ", amb " + jo.jugador.getGameProfile().getName()
                + " de líder.");
        c.getSource().sendSuccess(() -> Missatges.bo("País fundat. Ara pots posar-li color (/nacio color #RRGGBB), "
                + "lema, bandera i capital. Per convidar gent, fes servir les parties d'OPAC."), false);
        return 1;
    }

    private static int nom(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Jo jo = jo(c, Permis.EDITAR);
        Resultat r = jo.registre.canviarNom(jo.pais.id(), StringArgumentType.getString(c, "nom"), ara());
        if (r.bo()) {
            PontOpac.aplicarIdentitat(jo.server, jo.pais);
            Missatges.anunciar(jo.server, jo.registre.darrers(1).get(0).text());
        }
        return resposta(c, r);
    }

    private static int sigla(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Jo jo = jo(c, Permis.EDITAR);
        Resultat r = jo.registre.canviarSigla(jo.pais.id(), StringArgumentType.getString(c, "sigla"), ara());
        if (r.bo()) {
            Missatges.anunciar(jo.server, jo.registre.darrers(1).get(0).text());
        }
        return resposta(c, r);
    }

    private static int color(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Jo jo = jo(c, Permis.EDITAR);
        OptionalInt color = Validacio.color(StringArgumentType.getString(c, "color"));
        if (color.isEmpty()) {
            throw ERROR.create("El color s'escriu en hexadecimal, com #C0392B.");
        }
        Resultat r = jo.registre.canviarColor(jo.pais.id(), color.getAsInt());
        if (r.bo()) {
            PontOpac.aplicarIdentitat(jo.server, jo.pais);
        }
        return resposta(c, r);
    }

    private static int lema(CommandContext<CommandSourceStack> c, String lema) throws CommandSyntaxException {
        Jo jo = jo(c, Permis.EDITAR);
        return resposta(c, jo.registre.canviarLema(jo.pais.id(), lema));
    }

    private static int bandera(CommandContext<CommandSourceStack> c, String bandera) throws CommandSyntaxException {
        Jo jo = jo(c, Permis.EDITAR);
        if (!bandera.isBlank() && !cat.mineuab.pauitreva.banderes.Banderes.existeix(bandera.trim())) {
            throw ERROR.create("No hi ha cap bandera " + bandera.trim() + ". Tria-la a la pantalla del país (tecla N) "
                    + "o escriu /nacio bandera i mira els suggeriments.");
        }
        Resultat r = jo.registre.canviarBandera(jo.pais.id(), bandera);
        if (r == Resultat.NOM_INVALID) {
            throw ERROR.create("La bandera és l'id d'una pintura, com mineuab:catalunya (les del datapack "
                    + "mineuab-banderes o una de pujada amb Betsy Ross).");
        }
        return resposta(c, r);
    }

    private static int capital(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Jo jo = jo(c, Permis.EDITAR);
        ChunkPos chunk = jo.jugador.chunkPosition();
        ResourceLocation dimensio = jo.jugador.level().dimension().location();
        if (!PontOpac.chunkDelPais(jo.server, jo.partit, dimensio, chunk.x, chunk.z)) {
            throw ERROR.create("La capital ha de ser en un chunk reclamat pel país. Aquest no ho és.");
        }
        return resposta(c, jo.registre.canviarCapital(jo.pais.id(), new Capital(dimensio.toString(), chunk.x, chunk.z)));
    }

    private static int rang(CommandContext<CommandSourceStack> c, String nom) throws CommandSyntaxException {
        Jo jo = jo(c, Permis.EDITAR);
        String clau = StringArgumentType.getString(c, "rang");
        Rang rang = Rang.perClau(clau).orElseThrow(() -> ERROR.create(
                "Els rangs són: ciutada, colon, oficial, ministre i lider."));
        Resultat r = jo.registre.canviarNomRang(jo.pais.id(), rang, nom);
        if (r == Resultat.NOM_INVALID) {
            throw ERROR.create("El nom del rang ha de tenir fins a " + Validacio.NOM_RANG_MAX
                    + " caràcters, només lletres, xifres, espais i signes senzills.");
        }
        return resposta(c, r);
    }

    private static int permis(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Jo jo = jo(c, Permis.DISSOLDRE); // només el líder
        Permis permis = Permis.perClau(StringArgumentType.getString(c, "permis"))
                .filter(Permis::configurable)
                .orElseThrow(() -> ERROR.create("Els permisos són: editar, diplomacia i trencar."));
        Rang rang = Rang.perClau(StringArgumentType.getString(c, "rang")).orElseThrow(() -> ERROR.create(
                "Els rangs són: ciutada, colon, oficial, ministre i lider."));
        return resposta(c, jo.registre.canviarPermis(jo.pais.id(), permis, rang));
    }

    private static int dissol(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Jo jo = jo(c, Permis.DISSOLDRE);
        for (Map.Entry<UUID, EstatRelacio> e : jo.registre.relacionsDe(jo.pais.id()).entrySet()) {
            if (e.getValue().tipus() == TipusRelacio.ALIANCA) {
                PontOpac.desaliar(jo.server, jo.pais.id(), e.getKey());
            }
        }
        for (Guerra g : jo.registre.guerresDe(jo.pais.id())) {
            Guerres.acabarEnBlanc(jo.server, g);
        }
        Resultat r = jo.registre.dissoldre(jo.pais.id(), "", ara());
        if (r.bo()) {
            Missatges.anunciar(jo.server, jo.registre.darrers(1).get(0).text());
        }
        c.getSource().sendSuccess(() -> Missatges.gris("La party d'OPAC i els claims es queden com estaven."), false);
        return resposta(c, r);
    }

    /**
     * Passa els claims (i els chunks forçats) de cada membre de cada país al líder, perquè siguin del país
     * (amb {@code partyOwnedClaims} d'OPAC). Amb {@code prova}, només compta i diu si caben als límits d'ara.
     */
    private static int migrar(CommandContext<CommandSourceStack> c, boolean prova) throws CommandSyntaxException {
        MinecraftServer server = c.getSource().getServer();
        Registre r = Dades.registre(server);
        if (!r.guerres().isEmpty()) {
            throw ERROR.create("Hi ha guerres obertes. Acaba-les abans (/guerra admin acaba), perquè la zona en disputa "
                    + "no es barregi amb la migració.");
        }
        CommandSourceStack s = c.getSource();
        s.sendSuccess(() -> titol(prova ? "Migració dels claims (prova: no es toca res)" : "Migració dels claims"), false);
        if (!PontOpac.claimsDePartit()) {
            s.sendSuccess(() -> Component.literal("Atenció: OPAC encara no té partyOwnedClaims = true. Sense això, els "
                    + "claims passats al líder són del líder, no del país.").withStyle(ChatFormatting.RED), false);
        }
        int totalChunks = 0;
        int totalForcats = 0;
        var claims = PontOpac.api(server).getServerClaimsManager();
        for (Pais p : r.paisos()) {
            IServerPartyAPI partit = PontOpac.partit(server, p.id());
            if (partit == null) {
                continue;
            }
            UUID lider = partit.getOwner().getUUID();
            int chunks = 0;
            int forcats = 0;
            for (IPartyMemberAPI m : PontOpac.membres(partit)) {
                int[] n = PontOpac.traspassarClaims(server, m.getUUID(), lider, prova);
                chunks += n[0];
                forcats += n[1];
            }
            int liderChunks = claims.hasPlayerInfo(lider) ? claims.getPlayerInfo(lider).getClaimCount() : 0;
            int liderForcats = claims.hasPlayerInfo(lider) ? claims.getPlayerInfo(lider).getForceloadCount() : 0;
            int totalPais = prova ? liderChunks + chunks : liderChunks;
            int forcatsPais = prova ? liderForcats + forcats : liderForcats;
            int limit = claims.getPlayerFullClaimLimit(lider);
            int limitForcats = claims.getPlayerFullForceloadLimit(lider);
            int fc = chunks;
            int ff = forcats;
            s.sendSuccess(() -> Missatges.pais(p).append(gris(": " + fc + " chunks (" + ff + " forçats) dels membres "
                    + (prova ? "passarien" : "han passat") + " al líder. En total, " + totalPais + " de " + limit
                    + " chunks i " + forcatsPais + " de " + limitForcats + " forçats que deixa OPAC ara.")), false);
            if (totalPais > limit || forcatsPais > limitForcats) {
                s.sendSuccess(() -> Component.literal("   Passa del límit: puja claimBonusPerPartyMember i "
                        + "forceloadBonusPerPartyMember a la config d'OPAC.").withStyle(ChatFormatting.RED), false);
            }
            totalChunks += chunks;
            totalForcats += forcats;
        }
        int tc = totalChunks;
        int tf = totalForcats;
        s.sendSuccess(() -> bo("Total: " + tc + " chunks, " + tf + " forçats." + (prova
                ? " Per fer-ho de veritat, fes una còpia del món i després /nacio admin migra fes confirmo." : "")), true);
        return totalChunks;
    }

    private static MutableComponent titol(String text) {
        return Missatges.titol(text);
    }

    private static MutableComponent gris(String text) {
        return Missatges.gris(text);
    }

    private static MutableComponent bo(String text) {
        return Missatges.bo(text);
    }

    // ---------------------------------------------------------------- /diplomacia

    private static int relacions(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Jo jo = jo(c);
        if (jo.pais == null) {
            throw ERROR.create("No ets de cap país.");
        }
        CommandSourceStack s = c.getSource();
        s.sendSuccess(() -> Missatges.titol("Diplomàcia de ").append(Missatges.pais(jo.pais)), false);
        Map<UUID, EstatRelacio> rel = jo.registre.relacionsDe(jo.pais.id());
        if (rel.isEmpty()) {
            s.sendSuccess(() -> Missatges.gris("Sense aliances ni pactes. Proposa'n amb /diplomacia alianca <sigla> "
                    + "o /diplomacia pacte <sigla> <dies>."), false);
        }
        for (Map.Entry<UUID, EstatRelacio> e : rel.entrySet()) {
            jo.registre.pais(e.getKey()).ifPresent(altre -> {
                MutableComponent linia = Missatges.gris("• ")
                        .append(Component.literal(capitalitza(e.getValue().tipus().nom()) + " amb "))
                        .append(Missatges.pais(altre))
                        .append(Missatges.gris(e.getValue().fins() > 0
                                ? " (fins al " + Missatges.data(e.getValue().fins()) + ") " : " "));
                if (e.getValue().tipus() == TipusRelacio.ALIANCA || e.getValue().tipus() == TipusRelacio.PACTE) {
                    linia.append(Missatges.boto("Trenca", "/diplomacia trenca " + altre.sigla(), ChatFormatting.RED));
                }
                s.sendSuccess(() -> linia, false);
            });
        }
        propostesPendents(s, jo.registre, jo.pais);
        return 1;
    }

    private static void propostesPendents(CommandSourceStack s, Registre r, Pais p) {
        for (Proposta pr : r.propostesRebudes(p.id())) {
            r.pais(pr.de()).ifPresent(de -> s.sendSuccess(() -> Missatges.gris("Proposta de ")
                    .append(Missatges.pais(de))
                    .append(Missatges.gris(": " + descriu(pr) + " "))
                    .append(Missatges.boto("Accepta", "/diplomacia accepta " + de.sigla(), ChatFormatting.GREEN))
                    .append(Component.literal(" "))
                    .append(Missatges.boto("Rebutja", "/diplomacia rebutja " + de.sigla(), ChatFormatting.RED)), false));
        }
        for (Proposta pr : r.propostesFetes(p.id())) {
            r.pais(pr.a()).ifPresent(a -> s.sendSuccess(() -> Missatges.gris("Esperant resposta de ")
                    .append(Missatges.pais(a))
                    .append(Missatges.gris(": " + descriu(pr) + " (fins al " + Missatges.data(pr.caduca()) + ")")), false));
        }
    }

    private static int proposar(CommandContext<CommandSourceStack> c, TipusRelacio tipus, int dies)
            throws CommandSyntaxException {
        Jo jo = jo(c, Permis.DIPLOMACIA);
        Pais altre = altrePais(jo.registre, c);
        if (tipus == TipusRelacio.PACTE && dies > Config.PACTE_DIES_MAX.get()) {
            throw ERROR.create("Un pacte pot durar com a molt " + Config.PACTE_DIES_MAX.get() + " dies.");
        }
        long caducitat = Config.CADUCITAT_PROPOSTES_HORES.get() * Registre.HORA;
        Resultat r = jo.registre.proposar(jo.pais.id(), altre.id(), tipus, dies, ara(), caducitat);
        if (r == Resultat.PROPOSADA) {
            Proposta pr = jo.registre.propostesFetes(jo.pais.id()).stream()
                    .filter(x -> x.a().equals(altre.id()) && x.tipus() == tipus).findFirst().orElseThrow();
            avisarPais(jo.server, altre, Missatges.pais(jo.pais)
                    .append(Component.literal(" us proposa " + descriu(pr) + " ").withStyle(ChatFormatting.YELLOW))
                    .append(Missatges.boto("Accepta", "/diplomacia accepta " + jo.pais.sigla(), ChatFormatting.GREEN))
                    .append(Component.literal(" "))
                    .append(Missatges.boto("Rebutja", "/diplomacia rebutja " + jo.pais.sigla(), ChatFormatting.RED)));
        } else if (r == Resultat.ACCEPTADA) {
            establert(jo, altre);
        }
        return resposta(c, r);
    }

    private static int accepta(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Jo jo = jo(c, Permis.DIPLOMACIA);
        Pais altre = altrePais(jo.registre, c);
        Resultat r = jo.registre.acceptar(jo.pais.id(), altre.id(), ara());
        if (r == Resultat.ACCEPTADA) {
            establert(jo, altre);
        }
        return resposta(c, r);
    }

    /** Després d'acceptar: aliats a OPAC si és una aliança, i l'avís per a tothom. */
    private static void establert(Jo jo, Pais altre) {
        if (jo.registre.relacio(jo.pais.id(), altre.id()) == TipusRelacio.ALIANCA) {
            PontOpac.aliar(jo.server, jo.pais.id(), altre.id());
        }
        Missatges.anunciar(jo.server, jo.registre.darrers(1).get(0).text());
    }

    private static int rebutja(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Jo jo = jo(c, Permis.DIPLOMACIA);
        Pais altre = altrePais(jo.registre, c);
        Resultat r = jo.registre.rebutjar(jo.pais.id(), altre.id());
        if (r.bo()) {
            avisarPais(jo.server, altre, Missatges.pais(jo.pais)
                    .append(Component.literal(" ha rebutjat la vostra proposta.").withStyle(ChatFormatting.YELLOW)));
        }
        return resposta(c, r);
    }

    private static int trenca(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Jo jo = jo(c, Permis.TRENCAR);
        Pais altre = altrePais(jo.registre, c);
        Resultat r = jo.registre.trencar(jo.pais.id(), altre.id(), ara());
        if (r == Resultat.TRENCADA_ALIANCA) {
            PontOpac.desaliar(jo.server, jo.pais.id(), altre.id());
        }
        if (r.bo()) {
            Missatges.anunciar(jo.server, jo.registre.darrers(1).get(0).text());
        }
        return resposta(c, r);
    }

    // ---------------------------------------------------------------- Ajudes

    private static UUID idPartit(MinecraftServer server, UUID jugador) {
        IServerPartyAPI partit = PontOpac.partitDe(server, jugador);
        return partit == null ? null : partit.getId();
    }

    private static void avisarPais(MinecraftServer server, Pais pais, Component missatge) {
        IServerPartyAPI partit = PontOpac.partit(server, pais.id());
        if (partit != null) {
            partit.getOnlineMemberStream().forEach(j -> j.sendSystemMessage(missatge));
        }
    }

    private static String descriu(Proposta pr) {
        return pr.tipus() == TipusRelacio.ALIANCA ? "una aliança"
                : "un pacte de no agressió de " + pr.dies() + (pr.dies() == 1 ? " dia" : " dies");
    }

    static String capitalitza(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
