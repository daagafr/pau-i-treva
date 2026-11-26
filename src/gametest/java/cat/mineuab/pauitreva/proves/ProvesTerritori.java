package cat.mineuab.pauitreva.proves;

import static cat.mineuab.pauitreva.proves.ProvesGuerra.cert;
import static cat.mineuab.pauitreva.proves.ProvesGuerra.jugador;

import cat.mineuab.pauitreva.Manteniment;
import cat.mineuab.pauitreva.PauITreva;
import cat.mineuab.pauitreva.model.Pais;
import cat.mineuab.pauitreva.model.Registre;
import cat.mineuab.pauitreva.mon.Dades;
import cat.mineuab.pauitreva.opac.PontOpac;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.parties.party.member.PartyMemberRank;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;
import xaero.pac.common.server.config.ServerConfig;
import xaero.pac.common.server.parties.party.ServerParty;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;
import xaero.pac.common.server.player.config.api.v2.PlayerConfigOptions;

/** El territori del país amb claims de party: la migració dels claims dels membres i el canvi de líder. */
@GameTestHolder(PauITreva.ID)
@PrefixGameTestTemplate(false)
public final class ProvesTerritori {

    private ProvesTerritori() {
    }

    /** En una tanda a part: les proves d'una tanda van alhora, i la migració no es fa amb guerres obertes. */
    @GameTest(template = "buit", timeoutTicks = 200, batch = "territori")
    public static void migracioICanviDeLider(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        OpenPACServerAPI opac = OpenPACServerAPI.get(server);
        IServerClaimsManagerAPI claims = opac.getServerClaimsManager();
        ResourceLocation dim = h.getLevel().dimension().location();
        Registre r = Dades.registre(server);
        // El servidor de proves arrenca amb partyOwnedClaims = true (opac/openpartiesandclaims-server.toml).
        cert(h, ServerConfig.CONFIG.partyOwnedClaims.get(), "la config d'OPAC de proves no té partyOwnedClaims");
        try {
            ServerPlayer lider = jugador(h, "Reina");
            ServerPlayer membre = jugador(h, "Hereu");
            IServerPartyAPI partit = opac.getPartyManager().createPartyForOwner(lider);
            partit.addMember(membre.getUUID(), PartyMemberRank.ADMIN, "Hereu");
            r.fundar(partit.getId(), "Corona", "COR", 0x8800AA, System.currentTimeMillis());
            Pais p = r.pais(partit.getId()).orElseThrow();

            ChunkPos base = new ChunkPos(h.absolutePos(BlockPos.ZERO));
            int x0 = base.x - 20;
            int z = base.z;
            claims.claim(dim, lider.getUUID(), -1, x0, z, false);
            claims.claim(dim, lider.getUUID(), -1, x0 + 1, z, true);
            claims.claim(dim, membre.getUUID(), -1, x0 + 2, z, true);

            // La migració passa el chunk del membre al líder, i el chunk forçat continua forçat.
            int fets = server.getCommands().getDispatcher().execute("nacio admin migra fes confirmo",
                    server.createCommandSourceStack());
            cert(h, fets == 1, "la migració hauria d'haver passat 1 chunk i n'ha passat " + fets);
            IPlayerChunkClaimAPI migrat = claims.get(dim, x0 + 2, z);
            cert(h, migrat.getPlayerId().equals(lider.getUUID()) && migrat.isForceloadable(), "el chunk del membre no és del líder");

            // Primera passada del manteniment: apunta el líder i li posa el nom del país.
            Manteniment.fer(server);
            cert(h, lider.getUUID().equals(p.lider()), "no s'ha apuntat el líder");
            cert(h, "Corona".equals(opac.getPlayerConfigManager().getLoadedConfig(lider.getUUID())
                    .getRaw(PlayerConfigOptions.CLAIMS_NAME)), "el líder no porta el nom del país");

            // Canvi de líder a OPAC, i el manteniment ho arregla: tot el territori passa al nou líder.
            cert(h, ((ServerParty) partit).changeOwner(membre.getUUID(), "Hereu"), "OPAC no ha canviat el propietari");
            Manteniment.fer(server);
            UUID nou = membre.getUUID();
            for (int i = 0; i < 3; i++) {
                IPlayerChunkClaimAPI c = claims.get(dim, x0 + i, z);
                cert(h, c != null && c.getPlayerId().equals(nou), "el chunk " + i + " no ha passat al nou líder");
            }
            cert(h, claims.get(dim, x0 + 1, z).isForceloadable(), "el chunk forçat ha deixat de ser-ho en canviar de líder");
            cert(h, nou.equals(p.lider()), "no s'ha apuntat el nou líder");
            cert(h, "".equals(opac.getPlayerConfigManager().getLoadedConfig(lider.getUUID())
                    .getRaw(PlayerConfigOptions.CLAIMS_NAME)), "el líder vell encara porta el nom del país");
            cert(h, "Corona".equals(opac.getPlayerConfigManager().getLoadedConfig(nou)
                    .getRaw(PlayerConfigOptions.CLAIMS_NAME)), "el nou líder no porta el nom del país");
            cert(h, r.darrers(1).get(0).text().contains("nou líder"), "no és al diari");
            cert(h, PontOpac.claimsDePartit(), "claimsDePartit no veu la config d'OPAC");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            h.fail("l'ordre de migració ha fallat: " + e.getMessage());
        }
        h.succeed();
    }
}
