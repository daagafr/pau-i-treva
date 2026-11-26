package cat.mineuab.pauitreva.proves;

import static cat.mineuab.pauitreva.proves.ProvesGuerra.cert;
import static cat.mineuab.pauitreva.proves.ProvesGuerra.jugador;

import cat.mineuab.pauitreva.PauITreva;
import cat.mineuab.pauitreva.model.Registre;
import cat.mineuab.pauitreva.mon.Dades;
import cat.mineuab.pauitreva.titols.Tags;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import net.luckperms.api.query.QueryOptions;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

/**
 * El tag del país al xat: amb el LuckPerms del Create i la pila de prefixos de {@code desplegament/}, un admin de la
 * UAB d'un país porta «[ADMIN] UAB [TAG]», i al MineUAB (server=global) el país no hi surt.
 */
@GameTestHolder(PauITreva.ID)
@PrefixGameTestTemplate(false)
public final class ProvesTags {

    private static final String ADMIN = "#8B0000&l[ADMIN]&r #8B0000";
    private static final String UAB = "#007E11&lUAB&r ";

    private ProvesTags() {
    }

    @GameTest(template = "buit", timeoutTicks = 300, batch = "tags")
    public static void tagAlXat(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        LuckPerms lp = LuckPermsProvider.get();
        // Els grups com al servidor de veritat: admin (track staff), uab i física.
        Group admin = lp.getGroupManager().createAndLoadGroup("admin").join();
        admin.data().add(PrefixNode.builder(ADMIN, 999).build());
        lp.getGroupManager().saveGroup(admin).join();
        Group uab = lp.getGroupManager().createAndLoadGroup("uab").join();
        uab.data().add(PrefixNode.builder(UAB, 9).build());
        lp.getGroupManager().saveGroup(uab).join();
        Group fisica = lp.getGroupManager().createAndLoadGroup("fisica").join();
        fisica.data().add(SuffixNode.builder(" &c[Física]&f", 1).build());
        lp.getGroupManager().saveGroup(fisica).join();
        var staff = lp.getTrackManager().createAndLoadTrack("staff").join();
        staff.appendGroup(admin);
        lp.getTrackManager().saveTrack(staff).join();

        ServerPlayer j = jugador(h, "Tagger");
        ServerPlayer sense = jugador(h, "Sensepais");
        lp.getUserManager().modifyUser(j.getUUID(), u -> {
            u.data().add(InheritanceNode.builder("admin").build());
            u.data().add(InheritanceNode.builder("uab").build());
            u.data().add(InheritanceNode.builder("fisica").build());
        }).join();
        lp.getUserManager().modifyUser(sense.getUUID(), u -> u.data().add(InheritanceNode.builder("uab").build())).join();

        OpenPACServerAPI opac = OpenPACServerAPI.get(server);
        IServerPartyAPI partit = opac.getPartyManager().createPartyForOwner(j);
        Registre r = Dades.registre(server);
        r.fundar(partit.getId(), "Tagonia", "TAG", 0x2255CC, System.currentTimeMillis());
        Tags.actualitzar(server);
        cert(h, "#2255CC&l[TAG]&r ".equals(Tags.tag(j.getUUID())), "el tag no és el que toca: " + Tags.tag(j.getUUID()));
        cert(h, Tags.tag(sense.getUUID()).isEmpty(), "qui no té país porta tag");

        h.startSequence()
                .thenExecuteAfter(40, () -> {
                    User u = lp.getUserManager().loadUser(j.getUUID()).join();
                    QueryOptions create = QueryOptions.contextual(ImmutableContextSet.of("server", "create"));
                    QueryOptions mineuab = QueryOptions.contextual(ImmutableContextSet.of("server", "global"));
                    String prefix = u.getCachedData().getMetaData(create).getPrefix();
                    cert(h, (ADMIN + UAB + "#2255CC&l[TAG]&r ").equals(prefix), "al Create el prefix és «" + prefix + "»");
                    cert(h, " &c[Física]&f".equals(u.getCachedData().getMetaData(create).getSuffix()), "la carrera no hi és");
                    cert(h, !u.getCachedData().getMetaData(mineuab).getPrefixes().containsValue("#2255CC&l[TAG]&r "),
                            "el país surt al MineUAB");
                    User s = lp.getUserManager().loadUser(sense.getUUID()).join();
                    String prefixSense = s.getCachedData().getMetaData(create).getPrefix();
                    cert(h, UAB.equals(prefixSense), "sense país, el prefix és «" + prefixSense + "»");
                    // Es dissol el país: el tag se'n va.
                    r.dissoldre(partit.getId(), "", System.currentTimeMillis());
                    Tags.actualitzar(server);
                    cert(h, Tags.tag(j.getUUID()).isEmpty(), "el tag del TAB no s'ha tret");
                })
                .thenExecuteAfter(40, () -> {
                    User u = lp.getUserManager().loadUser(j.getUUID()).join();
                    String prefix = u.getCachedData().getMetaData(QueryOptions.contextual(
                            ImmutableContextSet.of("server", "create"))).getPrefix();
                    cert(h, (ADMIN + UAB).equals(prefix), "després de dissoldre el país el prefix és «" + prefix + "»");
                })
                .thenSucceed();
    }
}
