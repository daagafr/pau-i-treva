package cat.mineuab.pauitreva.proves;

import cat.mineuab.pauitreva.Config;
import cat.mineuab.pauitreva.PauITreva;
import cat.mineuab.pauitreva.guerra.Batalles;
import cat.mineuab.pauitreva.guerra.Guerres;
import cat.mineuab.pauitreva.model.Chunk;
import cat.mineuab.pauitreva.model.Guerra;
import cat.mineuab.pauitreva.model.Pais;
import cat.mineuab.pauitreva.model.Registre;
import cat.mineuab.pauitreva.model.Resultat;
import cat.mineuab.pauitreva.model.Termes;
import cat.mineuab.pauitreva.model.TipusObjectiu;
import cat.mineuab.pauitreva.model.TipusRelacio;
import cat.mineuab.pauitreva.mon.Dades;
import cat.mineuab.pauitreva.opac.PontOpac;
import cat.mineuab.pauitreva.opac.ZonaOpac;
import cat.mineuab.pauitreva.web.ApiWeb;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;
import xaero.pac.common.server.config.ServerConfig;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;
import xaero.pac.common.server.player.config.api.v2.IPlayerConfigAPI;
import xaero.pac.common.server.player.config.api.v2.PlayerConfigOptions;

/**
 * Una guerra sencera dins d'un servidor de veritat amb OPAC: fundar dos països, reclamar, declarar,
 * obrir la finestra, capturar, matar, l'API i imposar la pau, mirant a OPAC cada pas.
 *
 * <p>{@code ./gradlew runProves}: el servidor fa les proves, escriu el resultat i s'atura.
 */
@GameTestHolder(PauITreva.ID)
@PrefixGameTestTemplate(false)
public final class ProvesGuerra {

    private ProvesGuerra() {
    }

    /** Un jugador de mentida en mode supervivència, connectat al servidor (OPAC el veu com a connectat). */
    static ServerPlayer jugador(GameTestHelper h, String nom) {
        ServerLevel nivell = h.getLevel();
        CommonListenerCookie galeta = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), nom), false);
        ServerPlayer j = new ServerPlayer(nivell.getServer(), nivell, galeta.gameProfile(), galeta.clientInformation());
        Connection connexio = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connexio);
        nivell.getServer().getPlayerList().placeNewPlayer(connexio, j, galeta);
        j.setGameMode(GameType.SURVIVAL);
        return j;
    }

    /** Si OPAC no deixa que {@code qui} ataqui {@code a} (tots dos on són ara). */
    static boolean protegeixAtac(OpenPACServerAPI opac, ServerPlayer qui, ServerPlayer a) {
        return opac.getChunkProtection().onEntityInteraction(null, qui, a, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
                true, false);
    }

    /** Si OPAC no deixa que {@code qui} trenqui el bloc de {@code pos}. */
    static boolean protegeixBloc(OpenPACServerAPI opac, ServerPlayer qui, BlockPos pos) {
        return opac.getChunkProtection().onBlockInteraction(qui, InteractionHand.MAIN_HAND, ItemStack.EMPTY,
                qui.serverLevel(), pos, Direction.UP, true, false);
    }

    /** Posa una pedra, hi fa petar una explosió de TNT al costat i diu si la pedra ha sobreviscut. */
    static boolean sobreviuExplosio(ServerLevel nivell, BlockPos pos) {
        nivell.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        nivell.explode(null, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 2.5f, Level.ExplosionInteraction.TNT);
        return nivell.getBlockState(pos).is(Blocks.STONE);
    }

    static BlockPos centre(int chunkX, int chunkZ) {
        return new BlockPos(chunkX * 16 + 8, -59, chunkZ * 16 + 8);
    }

    static void anar(ServerPlayer j, ServerLevel nivell, BlockPos pos) {
        j.teleportTo(nivell, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
    }

    static void cert(GameTestHelper h, boolean condicio, String que) {
        if (!condicio) {
            h.fail(que);
        }
    }

    @GameTest(template = "buit", timeoutTicks = 1200)
    public static void guerraSencera(GameTestHelper h) {
        ServerLevel nivell = h.getLevel();
        MinecraftServer server = nivell.getServer();
        OpenPACServerAPI opac = OpenPACServerAPI.get(server);
        IServerClaimsManagerAPI claims = opac.getServerClaimsManager();
        ResourceLocation dim = nivell.dimension().location();
        String d = dim.toString();
        Registre r = Dades.registre(server);
        long ara = System.currentTimeMillis();

        // Perquè el mod pugui obrir el PvP, com caldrà fer al servidor de veritat.
        List<String> op = new ArrayList<>(ServerConfig.CONFIG.opConfigurablePlayerConfigOptions.get());
        op.add("claims.protection.exceptions.playersByPlayers");
        ServerConfig.CONFIG.opConfigurablePlayerConfigOptions.set(op);
        // Captura ràpida per no haver d'esperar quatre minuts.
        Config.CAPTURA_BASE.set(5000);
        Config.CAPTURA_MAX.set(5000);
        // El defensor només té 3 chunks: amb el 20 % no se'n podria demanar més d'un.
        Config.MAX_PERCENT.set(100);

        ServerPlayer a = jugador(h, "Atacant");
        ServerPlayer b = jugador(h, "Defensor");
        IServerPartyAPI pa = opac.getPartyManager().createPartyForOwner(a);
        IServerPartyAPI pb = opac.getPartyManager().createPartyForOwner(b);
        cert(h, pa != null && pb != null, "OPAC no ha creat les parties");
        cert(h, r.fundar(pa.getId(), "Atacants", "ATA", 0x2255CC, ara) == Resultat.FET, "fundar ATA");
        cert(h, r.fundar(pb.getId(), "Defensors", "DEF", 0xCC2222, ara) == Resultat.FET, "fundar DEF");
        Pais ata = r.pais(pa.getId()).orElseThrow();
        Pais def = r.pais(pb.getId()).orElseThrow();
        PontOpac.aplicarIdentitat(server, ata);
        IPlayerConfigAPI configA = opac.getPlayerConfigManager().getLoadedConfig(a.getUUID());
        IPlayerConfigAPI configB = opac.getPlayerConfigManager().getLoadedConfig(b.getUUID());
        cert(h, "Atacants".equals(configA.getRaw(PlayerConfigOptions.CLAIMS_NAME)), "el nom del país no és als claims");

        // Territori en fila: ATA té x+10 i x+11; DEF, x+12 a x+14.
        ChunkPos base = new ChunkPos(h.absolutePos(BlockPos.ZERO));
        int x0 = base.x + 10;
        int z = base.z;
        claims.claim(dim, a.getUUID(), -1, x0, z, false);
        claims.claim(dim, a.getUUID(), -1, x0 + 1, z, false);
        claims.claim(dim, b.getUUID(), -1, x0 + 2, z, true); // un de forçat, que ho ha de continuar sent
        claims.claim(dim, b.getUUID(), -1, x0 + 3, z, false);
        claims.claim(dim, b.getUUID(), -1, x0 + 4, z, false);
        Chunk c2 = new Chunk(d, x0 + 2, z);
        Chunk c3 = new Chunk(d, x0 + 3, z);

        // Un objectiu que no toca la frontera no val.
        try {
            Guerres.declarar(server, ata, def, TipusObjectiu.CONQUESTA, Set.of(new Chunk(d, x0 + 4, z)));
            h.fail("s'ha pogut declarar una conquesta que no toca la frontera");
        } catch (Guerres.Error e) {
            cert(h, r.guerres().isEmpty(), "la declaració dolenta ha deixat una guerra");
        }

        Guerra g;
        try {
            g = Guerres.declarar(server, ata, def, TipusObjectiu.CONQUESTA, Set.of(c2, c3));
        } catch (Guerres.Error e) {
            h.fail("no s'ha pogut declarar: " + e.getMessage());
            return;
        }
        cert(h, r.relacio(ata.id(), def.id()) == TipusRelacio.GUERRA, "la relació no és de guerra");
        cert(h, g.capitalAtacant() != null && g.capitalDefensor() != null, "falten les capitals");

        // Zona en disputa: sub-config de guerra protegida (la finestra encara no és oberta).
        cert(h, configB.subConfigExists(ZonaOpac.sub(g)), "no s'ha creat la sub-config de guerra");
        IPlayerConfigAPI sub = configB.getSubConfig(ZonaOpac.sub(g));
        IPlayerChunkClaimAPI claim2 = claims.get(dim, x0 + 2, z);
        cert(h, claim2.getSubConfigIndex() == sub.getSubIndex(), "el chunk no és a la sub-config de guerra");
        cert(h, claim2.isForceloadable(), "el chunk forçat ha deixat de ser-ho");
        cert(h, Boolean.TRUE.equals(sub.getEffective(PlayerConfigOptions.PROTECT_CLAIMED_CHUNKS)), "la zona no és protegida");
        cert(h, "main".equals(g.subconfigs().get(c2)), "no s'ha recordat la sub-config d'abans");

        // Com al Create: fora de la guerra, als claims no hi ha PvP.
        cert(h, Boolean.FALSE.equals(configA.getEffective(PlayerConfigOptions.CLAIM_EXCEPTION_PLAYERS_BY_PLAYERS)),
                "abans de la finestra ja hi ha PvP (la config per defecte d'OPAC no és la del Create?)");

        // No es pot desreclamar un chunk en disputa.
        claims.tryToUnclaim(dim, b.getUUID(), x0 + 3, z, x0 + 3, z, false);
        cert(h, claims.get(dim, x0 + 3, z) != null, "s'ha pogut desreclamar un chunk en disputa");

        // Abans de la finestra, tot protegit: cadascú a casa seva, sense PvP ni res a trencar ni a explotar.
        BlockPos casaA = centre(x0 + 1, z);
        BlockPos disputa = centre(x0 + 2, z).offset(3, 0, 0);
        BlockPos casaB = centre(x0 + 4, z);
        anar(a, nivell, casaA);
        anar(b, nivell, casaB);
        cert(h, protegeixAtac(opac, a, b), "hi ha PvP abans de la finestra");
        cert(h, protegeixBloc(opac, a, disputa), "es pot trencar la zona en disputa abans de la finestra");
        cert(h, sobreviuExplosio(nivell, disputa), "una explosió trenca la zona en disputa abans de la finestra");

        // Comença la guerra i s'obre la finestra.
        r.acabarPreparacio(g.id(), System.currentTimeMillis());
        Batalles.forcarFinestra(true);
        int px = (x0 + 2) * 16 + 8;
        int pz = z * 16 + 8;

        h.startSequence()
                .thenExecuteAfter(30, () -> {
                    cert(h, Batalles.finestraOberta(), "la finestra no s'ha obert");
                    cert(h, Boolean.FALSE.equals(sub.getEffective(PlayerConfigOptions.PROTECT_CLAIMED_CHUNKS)),
                            "la zona no s'ha obert amb la finestra");
                    cert(h, Boolean.TRUE.equals(configA.getEffective(PlayerConfigOptions.CLAIM_EXCEPTION_PLAYERS_BY_PLAYERS)),
                            "el PvP de l'atacant no s'ha obert");
                    cert(h, Boolean.TRUE.equals(configB.getEffective(PlayerConfigOptions.CLAIM_EXCEPTION_PLAYERS_BY_PLAYERS)),
                            "el PvP del defensor no s'ha obert");
                    // Amb la finestra oberta: PvP entre tots dos, la zona en disputa oberta i la resta protegida.
                    cert(h, !protegeixAtac(opac, a, b), "OPAC encara no deixa atacar el defensor");
                    cert(h, !protegeixAtac(opac, b, a), "OPAC encara no deixa atacar l'atacant");
                    cert(h, !protegeixBloc(opac, a, disputa), "no es pot trencar la zona en disputa amb la finestra oberta");
                    cert(h, protegeixBloc(opac, a, casaB.offset(2, 0, 0)), "es pot trencar fora de l'objectiu");
                    cert(h, !sobreviuExplosio(nivell, disputa), "les explosions no trenquen la zona en disputa");
                    cert(h, sobreviuExplosio(nivell, casaB.offset(2, 0, 0)), "les explosions trenquen fora de l'objectiu");
                    a.teleportTo(nivell, px, -59, pz, 0, 0);
                })
                .thenExecuteAfter(80, () -> {
                    cert(h, g.control(c2) == Guerra.CONTROL_MAX, "l'atacant no ha capturat el chunk (control "
                            + g.control(c2) + ")");
                    cert(h, g.control(c3) == 0, "s'ha capturat un chunk on no hi havia ningú");
                    // Una baixa: el defensor mor a mans de l'atacant.
                    int abans = g.milpunts();
                    b.die(nivell.damageSources().playerAttack(a));
                    cert(h, g.baixesAtacant() == 1, "no s'ha comptat la baixa");
                    cert(h, g.milpunts() > abans, "la baixa no ha donat punts");
                    cert(h, g.mortsDe(b.getUUID()) == 1, "no s'ha comptat la vida perduda");
                })
                .thenExecuteAfter(5, () -> {
                    ApiWeb.actualitzar(server);
                    String json = llegir("http://127.0.0.1:" + Config.API_PORT.get() + "/estat.json");
                    cert(h, json != null && json.contains("\"guerres\":[{") && json.contains("\"atacant\":\"ATA\""),
                            "l'API no ensenya la guerra: " + json);
                    cert(h, json.contains("\"control\":100"), "l'API no ensenya el control");
                })
                .thenExecuteAfter(5, () -> {
                    r.sumarPunts(g.id(), ata.id(), 100);
                    Termes termes = Registre.termes(g, ata.id(), Registre.QuinsChunks.OBJECTIU, false);
                    Resultat res = Guerres.signarPau(server, g, ata.id(), termes, true);
                    cert(h, res == Resultat.PAU_SIGNADA, "no s'ha pogut imposar la pau: " + res);
                    cert(h, r.relacio(ata.id(), def.id()) == TipusRelacio.TREVA, "després de la pau no hi ha treva");
                    IPlayerChunkClaimAPI nou2 = claims.get(dim, x0 + 2, z);
                    IPlayerChunkClaimAPI nou3 = claims.get(dim, x0 + 3, z);
                    cert(h, nou2 != null && nou2.getPlayerId().equals(a.getUUID()) && nou2.getSubConfigIndex() == -1,
                            "el chunk cedit no és del líder guanyador");
                    cert(h, nou3 != null && nou3.getPlayerId().equals(a.getUUID()), "el segon chunk cedit no hi és");
                    cert(h, nou2.isForceloadable(), "el chunk cedit ha perdut el forçat");
                    IPlayerChunkClaimAPI queda = claims.get(dim, x0 + 4, z);
                    cert(h, queda != null && queda.getPlayerId().equals(b.getUUID()) && queda.getSubConfigIndex() == -1,
                            "el chunk que no era de l'objectiu ha canviat");
                    cert(h, Boolean.TRUE.equals(sub.getEffective(PlayerConfigOptions.PROTECT_CLAIMED_CHUNKS)),
                            "la sub-config de guerra no ha tornat a protegir");
                })
                .thenExecuteAfter(30, () -> {
                    cert(h, !Boolean.TRUE.equals(configA.getRaw(PlayerConfigOptions.CLAIM_EXCEPTION_PLAYERS_BY_PLAYERS)),
                            "el PvP no s'ha tancat després de la pau");
                    cert(h, Dades.dades(server).pvpObert().isEmpty(), "queden configs amb el PvP obert");
                    anar(a, nivell, casaA);
                    anar(b, nivell, casaB);
                    cert(h, protegeixAtac(opac, a, b), "després de la pau encara hi ha PvP");
                    // El chunk conquerit ara és de l'atacant: el defensor ja no hi pot trencar res.
                    cert(h, protegeixBloc(opac, b, disputa), "el defensor pot trencar el chunk que ha perdut");
                    Batalles.forcarFinestra(null);
                    Config.CAPTURA_BASE.set(40);
                    Config.CAPTURA_MAX.set(60);
                    Config.MAX_PERCENT.set(20);
                })
                .thenSucceed();
    }

    /** Defensor que es rendeix en una guerra d'humiliació: queda humiliat i la zona (que no n'hi ha) no peta. */
    @GameTest(template = "buit", timeoutTicks = 200)
    public static void rendicioIHumiliacio(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        OpenPACServerAPI opac = OpenPACServerAPI.get(server);
        Registre r = Dades.registre(server);
        long ara = System.currentTimeMillis();
        ServerPlayer a = jugador(h, "Rei");
        ServerPlayer b = jugador(h, "Duc");
        IServerPartyAPI pa = opac.getPartyManager().createPartyForOwner(a);
        IServerPartyAPI pb = opac.getPartyManager().createPartyForOwner(b);
        r.fundar(pa.getId(), "Regne", "REG", 0x00AA00, ara);
        r.fundar(pb.getId(), "Ducat", "DUC", 0xAAAA00, ara);
        Pais reg = r.pais(pa.getId()).orElseThrow();
        Pais duc = r.pais(pb.getId()).orElseThrow();
        Guerra g;
        try {
            g = Guerres.declarar(server, reg, duc, TipusObjectiu.HUMILIACIO, Set.of());
        } catch (Guerres.Error e) {
            h.fail("no s'ha pogut declarar: " + e.getMessage());
            return;
        }
        cert(h, g.capitalAtacant() == null, "un país sense territori no pot tenir capital");
        Resultat res = Guerres.rendir(server, duc.id(), g);
        cert(h, res == Resultat.PAU_SIGNADA, "la rendició no ha signat la pau: " + res);
        cert(h, duc.humiliat(System.currentTimeMillis()), "el que es rendeix no queda humiliat");
        cert(h, !reg.humiliat(System.currentTimeMillis()), "el guanyador ha quedat humiliat");
        cert(h, Objects.equals(r.relacio(reg.id(), duc.id()), TipusRelacio.TREVA), "no hi ha treva");
        h.succeed();
    }

    static String llegir(String url) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> resposta = client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            return resposta.statusCode() == 200 ? resposta.body() : "HTTP " + resposta.statusCode();
        } catch (Exception e) {
            return e.toString();
        }
    }
}
