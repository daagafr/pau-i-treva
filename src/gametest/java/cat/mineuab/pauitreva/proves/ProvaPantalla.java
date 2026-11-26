package cat.mineuab.pauitreva.proves;

import cat.mineuab.pauitreva.PauITreva;
import cat.mineuab.pauitreva.client.ClientPauITreva;
import cat.mineuab.pauitreva.guerra.Guerres;
import cat.mineuab.pauitreva.model.Chunk;
import cat.mineuab.pauitreva.model.Guerra;
import cat.mineuab.pauitreva.model.ParametresGuerra;
import cat.mineuab.pauitreva.model.Rang;
import cat.mineuab.pauitreva.model.Registre;
import cat.mineuab.pauitreva.model.Termes;
import cat.mineuab.pauitreva.model.TipusObjectiu;
import cat.mineuab.pauitreva.model.TipusRelacio;
import cat.mineuab.pauitreva.mon.Dades;
import cat.mineuab.pauitreva.opac.PontOpac;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import xaero.pac.common.parties.party.member.PartyMemberRank;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

/**
 * Prova de la pantalla en un client de veritat (sense ningú davant): amb {@code -Dpauitreva.provaClient=true},
 * en entrar al món prepara tres països i una guerra al servidor integrat, obre la pantalla, fa una captura de
 * cada pestanya ({@code screenshots/}) i tanca el joc.
 */
@EventBusSubscriber(modid = PauITreva.ID, value = Dist.CLIENT)
public final class ProvaPantalla {

    private static final List<String> PASSOS = List.of("obrir", "pestanya:membres", "pestanya:diplomacia",
            "pestanya:guerres", "pestanya:diari", "pestanya:declara:ARA", "pestanya:rendir:OCC",
            "pestanya:diplomacia", "/diplomacia accepta ARA", "/nacio sigla X");
    private static int ticks;
    private static int pas = -1;

    private ProvaPantalla() {
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post e) {
        if (!Boolean.getBoolean("pauitreva.provaClient")) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getSingleplayerServer() == null) {
            return;
        }
        ticks++;
        if (ticks == 60) {
            MinecraftServer server = mc.getSingleplayerServer();
            UUID jo = mc.player.getUUID();
            server.execute(() -> preparar(server, server.getPlayerList().getPlayer(jo)));
        }
        if (ticks < 120 || (ticks - 120) % 50 != 0) {
            return;
        }
        if (pas >= 0) {
            Screenshot.grab(mc.gameDirectory, String.format("%02d-%s.png", pas, PASSOS.get(pas).replaceAll("[^a-zA-Z0-9_-]+", "_")),
                    mc.getMainRenderTarget(), m -> PauITreva.LOG.info("Captura: {}", m.getString()));
        }
        pas++;
        if (pas >= PASSOS.size()) {
            mc.stop();
            return;
        }
        // Sempre amb obrir(): si alguna cosa ha tancat la pantalla, es torna a obrir.
        ClientPauITreva.obrir(PASSOS.get(pas));
    }

    /** Al servidor integrat: tres països, territori, un pacte proposat i una guerra que ja va. */
    private static void preparar(MinecraftServer server, ServerPlayer jo) {
        try {
            // Que no el mati res mentre fa les captures.
            server.setDifficulty(net.minecraft.world.Difficulty.PEACEFUL, true);
            jo.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            OpenPACServerAPI opac = OpenPACServerAPI.get(server);
            IServerClaimsManagerAPI claims = opac.getServerClaimsManager();
            Registre r = Dades.registre(server);
            long ara = System.currentTimeMillis();
            IServerPartyAPI cat = opac.getPartyManager().createPartyForOwner(jo);
            cat.addMember(UUID.randomUUID(), PartyMemberRank.ADMIN, "Anty48");
            cat.addMember(UUID.randomUUID(), PartyMemberRank.CLAIMER, "Pere");
            r.fundar(cat.getId(), "Catalunya", "CAT", 0xF1C40F, ara);
            r.canviarLema(cat.getId(), "Som i serem");
            r.canviarNomRang(cat.getId(), Rang.LIDER, "President/a");
            r.canviarNomRang(cat.getId(), Rang.MINISTRE, "Conseller/a");
            GameProfile gaston = new GameProfile(UUID.randomUUID(), "Gaston");
            IServerPartyAPI occ = opac.getPartyManager().createPartyForOwner(gaston);
            r.fundar(occ.getId(), "Occitània", "OCC", 0xC0392B, ara);
            GameProfile jaume = new GameProfile(UUID.randomUUID(), "Jaume");
            IServerPartyAPI ara2 = opac.getPartyManager().createPartyForOwner(jaume);
            r.fundar(ara2.getId(), "Aragó", "ARA", 0xD35400, ara);
            r.proposar(ara2.getId(), cat.getId(), TipusRelacio.PACTE, 14, ara, Registre.DIA);
            r.pais(cat.getId()).ifPresent(p -> PontOpac.aplicarIdentitat(server, p));

            ResourceLocation dim = jo.level().dimension().location();
            int px = jo.chunkPosition().x;
            int pz = jo.chunkPosition().z;
            for (int i = -2; i <= 0; i++) {
                claims.claim(dim, jo.getUUID(), -1, px + i, pz, false);
            }
            for (int i = 1; i <= 5; i++) {
                claims.claim(dim, gaston.getId(), -1, px + i, pz, false);
                claims.claim(dim, gaston.getId(), -1, px + i, pz + 1, false);
            }
            Chunk a = new Chunk(dim.toString(), px + 1, pz);
            Chunk b = new Chunk(dim.toString(), px + 2, pz);
            Guerra g = Guerres.declarar(server, r.pais(cat.getId()).orElseThrow(), r.pais(occ.getId()).orElseThrow(),
                    TipusObjectiu.CONQUESTA, Set.of(a, b));
            r.acabarPreparacio(g.id(), ara);
            r.capturar(g.id(), List.of(new Registre.Presencia(a, 1, 0)), 400, ParametresGuerra.perDefecte());
            r.sumarPunts(g.id(), cat.getId(), 37);
            r.proposarPau(occ.getId(), g.id(), occ.getId(), Termes.blanca(), ara);
        } catch (Exception ex) {
            PauITreva.LOG.error("Prova de la pantalla: no s'ha pogut preparar", ex);
        }
    }
}
