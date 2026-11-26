package cat.mineuab.pauitreva.proves;

import static cat.mineuab.pauitreva.proves.ProvesGuerra.cert;
import static cat.mineuab.pauitreva.proves.ProvesGuerra.jugador;

import cat.mineuab.pauitreva.PauITreva;
import cat.mineuab.pauitreva.model.Registre;
import cat.mineuab.pauitreva.model.TipusObjectiu;
import cat.mineuab.pauitreva.mon.Dades;
import cat.mineuab.pauitreva.pantalla.Pantalla;
import cat.mineuab.pauitreva.pantalla.Vistes;
import cat.mineuab.pauitreva.pantalla.Xarxa;
import cat.mineuab.pauitreva.guerra.Guerres;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

/** La pantalla: totes les pestanyes es poden fer i passen per la xarxa sense perdre res. */
@GameTestHolder(PauITreva.ID)
@PrefixGameTestTemplate(false)
public final class ProvesPantalla {

    private static final List<String> PESTANYES = List.of(Vistes.PAIS, Vistes.MEMBRES, Vistes.DIPLOMACIA, Vistes.GUERRES,
            Vistes.DIARI, "dissol", "declara:PIN", "rendir:PIN", "una-que-no-existeix");

    private ProvesPantalla() {
    }

    @GameTest(template = "buit", timeoutTicks = 100, batch = "pantalla")
    public static void pestanyes(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        OpenPACServerAPI opac = OpenPACServerAPI.get(server);
        Registre r = Dades.registre(server);
        long ara = System.currentTimeMillis();
        ServerPlayer sensePais = jugador(h, "Rodamon");
        ServerPlayer lider = jugador(h, "Pinyol");
        ServerPlayer altre = jugador(h, "Ametlla");
        IServerPartyAPI p1 = opac.getPartyManager().createPartyForOwner(lider);
        IServerPartyAPI p2 = opac.getPartyManager().createPartyForOwner(altre);
        r.fundar(p1.getId(), "Pinyonia", "PIN", 0x55AA55, ara);
        r.fundar(p2.getId(), "Ametllània", "AME", 0xAA5555, ara);
        try {
            Guerres.declarar(server, r.pais(p2.getId()).orElseThrow(), r.pais(p1.getId()).orElseThrow(),
                    TipusObjectiu.HUMILIACIO, Set.of());
        } catch (Guerres.Error e) {
            h.fail("no s'ha pogut declarar: " + e.getMessage());
            return;
        }
        for (ServerPlayer j : List.of(sensePais, lider, altre)) {
            for (String pestanya : PESTANYES) {
                Pantalla p = Vistes.construir(j, pestanya, List.of(Component.literal("Un avís")));
                cert(h, !p.files().isEmpty(), "la pestanya " + pestanya + " és buida");
                RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess());
                Pantalla.CODI.encode(buf, p);
                Pantalla tornada = Pantalla.CODI.decode(buf);
                cert(h, tornada.equals(p), "la pestanya " + pestanya + " no passa bé per la xarxa");
                cert(h, buf.readableBytes() == 0, "sobren bytes a la pestanya " + pestanya);
            }
        }
        // Des de la pantalla només es poden fer ordres del mod.
        cert(h, Xarxa.permesa("/nacio info"), "/nacio info no és permesa");
        cert(h, Xarxa.permesa("/guerra pau AME proposa blanca"), "/guerra pau no és permesa");
        cert(h, !Xarxa.permesa("/op Pinyol"), "/op és permesa");
        cert(h, !Xarxa.permesa("/nacioX"), "/nacioX és permesa");
        cert(h, !Xarxa.permesa("nacio info"), "una ordre sense barra és permesa");
        cert(h, !Xarxa.permesa("/nacio info\n/op Pinyol"), "un salt de línia és permès");
        r.guerresDe(p1.getId()).forEach(g -> Guerres.acabarEnBlanc(server, g));
        h.succeed();
    }
}
