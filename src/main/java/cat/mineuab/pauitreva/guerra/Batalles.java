package cat.mineuab.pauitreva.guerra;

import cat.mineuab.pauitreva.Config;
import cat.mineuab.pauitreva.PauITreva;
import cat.mineuab.pauitreva.model.Chunk;
import cat.mineuab.pauitreva.model.Esdeveniment;
import cat.mineuab.pauitreva.model.FinestresGuerra;
import cat.mineuab.pauitreva.model.Guerra;
import cat.mineuab.pauitreva.model.Pais;
import cat.mineuab.pauitreva.model.ParametresGuerra;
import cat.mineuab.pauitreva.model.Registre;
import cat.mineuab.pauitreva.mon.Dades;
import cat.mineuab.pauitreva.opac.PontOpac;
import cat.mineuab.pauitreva.opac.ZonaOpac;
import cat.mineuab.pauitreva.ordres.Avisos;
import cat.mineuab.pauitreva.ordres.Missatges;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

/**
 * La guerra en directe. Cada segon: obre o tanca la zona en disputa i el PvP segons la finestra, anuncia les
 * guerres que comencen, acaba les que arriben al límit i, amb la finestra oberta, fa la captura per presència,
 * els punts de cada minut i les barres de guerra.
 */
public final class Batalles {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private static boolean engegat;
    private static long ticks;
    private static Boolean finestraOberta;
    /** Finestra oberta o tancada a mà per un admin ({@code null}: segons l'horari). */
    private static Boolean forcada;
    private static int segonsFinestra;
    private static boolean caldReconciliar;
    private static final Set<UUID> iniciades = new HashSet<>();
    private static final Map<UUID, Quiet> quiets = new HashMap<>();
    private static final Map<String, ServerBossEvent> barres = new HashMap<>();

    /** On era un jugador i des de quan no es mou, per saber si és AFK. */
    private record Quiet(Vec3 pos, float yRot, float xRot, long des) {
    }

    private Batalles() {
    }

    public static void engegar(MinecraftServer server) {
        engegat = true;
        ticks = 0;
        finestraOberta = null;
        segonsFinestra = 0;
        iniciades.clear();
        quiets.clear();
        long ara = System.currentTimeMillis();
        // Les guerres que ja havien començat abans de reiniciar no es tornen a anunciar.
        Dades.registre(server).guerres().stream().filter(g -> g.activa(ara)).forEach(g -> iniciades.add(g.id()));
        caldReconciliar = true;
    }

    public static void aturar() {
        engegat = false;
        barres.values().forEach(ServerBossEvent::removeAllPlayers);
        barres.clear();
    }

    /** Hi ha hagut una declaració o una pau: cal tornar a posar la zona, el PvP i les barres com toca. */
    public static void guerresCanviades(MinecraftServer server) {
        caldReconciliar = true;
        if (engegat) {
            reconciliar(server, Boolean.TRUE.equals(finestraOberta));
            barres(server, Boolean.TRUE.equals(finestraOberta), System.currentTimeMillis());
        }
    }

    /** Per provar: obre ({@code true}) o tanca la finestra a mà, o la torna a l'horari ({@code null}). */
    public static void forcarFinestra(Boolean oberta) {
        forcada = oberta;
    }

    public static boolean finestraOberta() {
        return Boolean.TRUE.equals(finestraOberta);
    }

    public static void tick(MinecraftServer server) {
        if (!engegat || ++ticks % 20 != 0) {
            return;
        }
        try {
            segon(server);
        } catch (RuntimeException e) {
            // Mai no ha de tombar el servidor.
            PauITreva.LOG.error("Pau i Treva: error a la batalla", e);
        }
    }

    private static void segon(MinecraftServer server) {
        long ara = System.currentTimeMillis();
        FinestresGuerra f = Config.finestres();
        boolean oberta = forcada != null ? forcada : f.oberta(ara);
        Registre r = Dades.registre(server);
        ParametresGuerra p = Config.parametres();
        seguirQuiets(server, ara);

        if (finestraOberta == null || oberta != finestraOberta) {
            boolean primer = finestraOberta == null;
            finestraOberta = oberta;
            segonsFinestra = 0;
            if (oberta) {
                r.novaFinestra();
            }
            if (!primer) {
                anunciarFinestra(server, r, f, oberta, ara);
            }
            caldReconciliar = true;
        }

        for (Guerra g : List.copyOf(r.guerres())) {
            if (g.activa(ara) && iniciades.add(g.id())) {
                comenca(server, r, g, f, ara);
                caldReconciliar = true;
            }
        }
        for (Guerra g : r.guerresAlLimit(ara)) {
            Guerres.pauAutomatica(server, g);
        }

        if (oberta) {
            segonsFinestra++;
            Map<UUID, UUID> paisDe = paisos(server);
            for (Guerra g : List.copyOf(r.guerres())) {
                if (g.activa(ara)) {
                    capturar(server, r, p, g, paisDe, ara);
                    if (segonsFinestra % 60 == 0) {
                        r.puntuarMinut(g.id(), p);
                    }
                }
            }
        }

        if (caldReconciliar || ticks % (20 * 15) == 0) {
            reconciliar(server, oberta);
        }
        barres(server, oberta, ara);
    }

    // ---------------------------------------------------------------- Zona, PvP i barres

    /** Posa la zona en disputa i el PvP com han d'estar ara. També arregla el que algú hagi canviat a mà. */
    private static void reconciliar(MinecraftServer server, boolean oberta) {
        caldReconciliar = false;
        long ara = System.currentTimeMillis();
        Registre r = Dades.registre(server);
        boolean obrirZona = Config.o(Config.OBRIR_ZONA, true);
        Set<UUID> pvp = new HashSet<>();
        for (Guerra g : r.guerres()) {
            boolean lluita = oberta && g.activa(ara);
            ZonaOpac.aplicarZona(server, g, lluita && obrirZona);
            if (lluita) {
                pvp.add(g.atacant());
                pvp.add(g.defensor());
            }
        }
        if (!Config.o(Config.OBRIR_PVP, true)) {
            pvp.clear();
        }
        Dades dades = Dades.dades(server);
        if (ZonaOpac.aplicarPvp(server, pvp, dades.pvpObert())) {
            dades.pvpCanviat();
        }
    }

    /** Una barra de guerra per a cada bàndol de cada guerra activa, mentre la finestra és oberta. */
    private static void barres(MinecraftServer server, boolean oberta, long ara) {
        Registre r = Dades.registre(server);
        Set<String> vives = new HashSet<>();
        if (oberta) {
            long tanca = Config.finestres().tancament(ara);
            for (Guerra g : r.guerres()) {
                if (!g.activa(ara)) {
                    continue;
                }
                for (UUID bandol : List.of(g.atacant(), g.defensor())) {
                    Pais enemic = r.pais(g.enemic(bandol)).orElse(null);
                    if (enemic == null) {
                        continue;
                    }
                    String clau = g.id() + ":" + bandol;
                    vives.add(clau);
                    ServerBossEvent barra = barres.computeIfAbsent(clau, k -> new ServerBossEvent(Component.empty(),
                            BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_20));
                    int punts = g.puntsDe(bandol);
                    barra.setName(Component.literal("⚔ ").withStyle(ChatFormatting.RED)
                            .append(Missatges.pais(enemic))
                            .append(Component.literal(" · " + (punts > 0 ? "+" : "") + punts + " punts"
                                    + (tanca > 0 ? " · fins a les " + HORA.withZone(Config.finestres().zona())
                                    .format(Instant.ofEpochMilli(tanca)) : "")).withStyle(ChatFormatting.WHITE)));
                    barra.setProgress((punts + Guerra.PUNTS_MAX) / (2f * Guerra.PUNTS_MAX));
                    barra.setColor(punts > 0 ? BossEvent.BossBarColor.GREEN
                            : punts < 0 ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.YELLOW);
                    List<ServerPlayer> seus = Avisos.jugadors(server, bandol);
                    for (ServerPlayer j : List.copyOf(barra.getPlayers())) {
                        if (!seus.contains(j)) {
                            barra.removePlayer(j);
                        }
                    }
                    seus.forEach(barra::addPlayer);
                }
            }
        }
        barres.entrySet().removeIf(e -> {
            if (vives.contains(e.getKey())) {
                return false;
            }
            e.getValue().removeAllPlayers();
            return true;
        });
    }

    // ---------------------------------------------------------------- Captura

    /** Per a cada jugador connectat que és d'un país, l'id del país. */
    private static Map<UUID, UUID> paisos(MinecraftServer server) {
        Registre r = Dades.registre(server);
        Map<UUID, UUID> m = new HashMap<>();
        for (ServerPlayer j : server.getPlayerList().getPlayers()) {
            IServerPartyAPI partit = PontOpac.partitDe(server, j.getUUID());
            if (partit != null && r.pais(partit.getId()).isPresent()) {
                m.put(j.getUUID(), partit.getId());
            }
        }
        return m;
    }

    private static void capturar(MinecraftServer server, Registre r, ParametresGuerra p, Guerra g,
                                 Map<UUID, UUID> paisDe, long ara) {
        Map<Chunk, int[]> comptes = new HashMap<>();
        Map<Chunk, List<ServerPlayer>> presents = new HashMap<>();
        long afk = Config.o(Config.AFK_MINUTS, 5) * 60_000L;
        for (ServerPlayer j : server.getPlayerList().getPlayers()) {
            UUID pais = paisDe.get(j.getUUID());
            if (pais == null || !g.hiEs(pais) || j.isSpectator() || !j.isAlive()) {
                continue;
            }
            Quiet q = quiets.get(j.getUUID());
            if (q != null && ara - q.des() > afk) {
                continue;
            }
            if (!r.potCapturar(g.id(), j.getUUID(), p)) {
                continue;
            }
            Chunk c = new Chunk(j.level().dimension().location().toString(), j.chunkPosition().x, j.chunkPosition().z);
            if (!g.esPuntDeCaptura(c)) {
                continue;
            }
            comptes.computeIfAbsent(c, k -> new int[2])[pais.equals(g.atacant()) ? 0 : 1]++;
            presents.computeIfAbsent(c, k -> new ArrayList<>()).add(j);
        }
        List<Registre.Presencia> presencies = new ArrayList<>();
        comptes.forEach((c, n) -> presencies.add(new Registre.Presencia(c, n[0], n[1])));
        for (Registre.CanviControl canvi : r.capturar(g.id(), presencies, 1, p)) {
            anunciarCaptura(server, r, g, canvi, ara);
        }
        if (Config.o(Config.BRILLAR, true)) {
            comptes.forEach((c, n) -> {
                if (n[0] > 0 && n[1] > 0) {
                    presents.get(c).forEach(j -> j.addEffect(
                            new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, false)));
                }
            });
        }
        // Qui és en un punt de captura veu com va.
        presents.forEach((c, jugadors) -> {
            int control = g.controlDe(c) * 100 / Guerra.CONTROL_MAX;
            boolean capital = c.equals(g.capitalAtacant()) || c.equals(g.capitalDefensor());
            for (ServerPlayer j : jugadors) {
                boolean pren = g.quiPren(c).equals(paisDe.get(j.getUUID()));
                Avisos.accio(j, Component.literal((capital ? "Capital" : "Chunk en disputa") + ": " + control
                        + "% de " + (pren ? "conquesta" : "l'enemic")).withStyle(pren ? ChatFormatting.GREEN
                        : ChatFormatting.RED), null);
            }
        });
    }

    private static void anunciarCaptura(MinecraftServer server, Registre r, Guerra g, Registre.CanviControl canvi,
                                        long ara) {
        Pais pren = r.pais(canvi.quiPren()).orElse(null);
        Pais altre = r.pais(g.enemic(canvi.quiPren())).orElse(null);
        if (pren == null || altre == null) {
            return;
        }
        boolean capital = !g.chunks().contains(canvi.chunk());
        String que = capital ? "la capital de " + Registre.etiqueta(altre)
                : "el chunk " + canvi.chunk().x() + ", " + canvi.chunk().z();
        String text = canvi.capturat()
                ? Registre.etiqueta(pren) + " ha pres " + que + "."
                : Registre.etiqueta(altre) + " ha recuperat " + (capital ? "la seva capital" : que) + ".";
        if (capital) {
            r.apuntar(ara, Esdeveniment.CAPTURA, text);
        }
        for (ServerPlayer j : Avisos.jugadors(server, g.atacant(), g.defensor())) {
            UUID seu = PontOpac.partitDe(server, j.getUUID()) == null ? null
                    : PontOpac.partitDe(server, j.getUUID()).getId();
            boolean bo = canvi.capturat() == canvi.quiPren().equals(seu);
            j.sendSystemMessage(Component.literal("⚔ " + text).withStyle(bo ? ChatFormatting.GREEN : ChatFormatting.RED));
            Avisos.so(j, bo ? Avisos.So.CAPTURA : Avisos.So.PERDUA);
        }
    }

    /** Segueix on és cada jugador: qui fa massa estona que no es mou ni gira el cap és AFK. */
    private static void seguirQuiets(MinecraftServer server, long ara) {
        Set<UUID> connectats = new HashSet<>();
        for (ServerPlayer j : server.getPlayerList().getPlayers()) {
            connectats.add(j.getUUID());
            Quiet q = quiets.get(j.getUUID());
            Vec3 pos = j.position();
            if (q == null || q.pos().distanceToSqr(pos) > 0.01 || q.yRot() != j.getYRot() || q.xRot() != j.getXRot()) {
                quiets.put(j.getUUID(), new Quiet(pos, j.getYRot(), j.getXRot(), ara));
            }
        }
        quiets.keySet().retainAll(connectats);
    }

    // ---------------------------------------------------------------- Avisos de la guerra

    private static void comenca(MinecraftServer server, Registre r, Guerra g, FinestresGuerra f, long ara) {
        Pais a = r.pais(g.atacant()).orElse(null);
        Pais d = r.pais(g.defensor()).orElse(null);
        if (a == null || d == null) {
            return;
        }
        long batalla = f.properaObertura(ara);
        String quan = batalla <= ara ? "La finestra és oberta: es lluita ara mateix."
                : "La primera batalla serà el " + Missatges.data(batalla) + ".";
        r.apuntar(ara, Esdeveniment.GUERRA_COMENCA, "S'acaba la preparació: comença la guerra entre "
                + Registre.etiqueta(a) + " i " + Registre.etiqueta(d) + ".");
        Avisos.anunciar(server, r.darrers(1).get(0).text() + " " + quan, Avisos.So.GUERRA,
                Component.literal("⚔ Comença la guerra").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                Component.literal(quan).withStyle(ChatFormatting.YELLOW), g.atacant(), g.defensor());
    }

    private static void anunciarFinestra(MinecraftServer server, Registre r, FinestresGuerra f, boolean oberta,
                                         long ara) {
        List<Guerra> actives = r.guerres().stream().filter(g -> g.activa(ara)).toList();
        if (actives.isEmpty()) {
            return;
        }
        Set<UUID> paisos = new HashSet<>();
        actives.forEach(g -> {
            paisos.add(g.atacant());
            paisos.add(g.defensor());
        });
        List<ServerPlayer> jugadors = Avisos.jugadors(server, paisos.toArray(UUID[]::new));
        long quan = oberta ? f.tancament(ara) : f.properaObertura(ara);
        String hora = quan > 0 ? HORA.withZone(f.zona()).format(Instant.ofEpochMilli(quan)) : "que ho digui un admin";
        if (oberta) {
            Avisos.titol(jugadors, Component.literal("⚔ Batalla").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    Component.literal("Es pot lluitar fins a les " + hora).withStyle(ChatFormatting.YELLOW),
                    Avisos.So.GUERRA);
            Avisos.xat(jugadors, Component.literal("⚔ S'obre la finestra de guerra fins a les " + hora
                    + ". Els chunks en disputa ja no estan protegits.").withStyle(ChatFormatting.RED));
        } else {
            Avisos.titol(jugadors, Component.literal("Fi de la batalla").withStyle(ChatFormatting.GOLD),
                    Component.literal("Tot torna a estar protegit").withStyle(ChatFormatting.YELLOW),
                    Avisos.So.FINESTRA);
            for (Guerra g : actives) {
                MutableComponent resum = Component.literal("⚔ Fi de la batalla. ").withStyle(ChatFormatting.GOLD);
                r.pais(g.atacant()).ifPresent(p -> resum.append(Missatges.pais(p)));
                resum.append(Component.literal(" " + g.punts() + " punts contra ").withStyle(ChatFormatting.GRAY));
                r.pais(g.defensor()).ifPresent(p -> resum.append(Missatges.pais(p)));
                resum.append(Component.literal(". Propera batalla: " + Missatges.data(f.properaObertura(ara)) + ".")
                        .withStyle(ChatFormatting.GRAY));
                Avisos.xat(Avisos.jugadors(server, g.atacant(), g.defensor()), resum);
            }
        }
    }

    /** En entrar, qui és d'un país en guerra veu com va. */
    public static void entrar(ServerPlayer jugador) {
        MinecraftServer server = jugador.getServer();
        if (server == null) {
            return;
        }
        IServerPartyAPI partit = PontOpac.partitDe(server, jugador.getUUID());
        if (partit == null) {
            return;
        }
        Registre r = Dades.registre(server);
        long ara = System.currentTimeMillis();
        FinestresGuerra f = Config.finestres();
        for (Guerra g : r.guerresDe(partit.getId())) {
            r.pais(g.enemic(partit.getId())).ifPresent(enemic -> {
                MutableComponent m = Component.literal("⚔ Sou en guerra amb ").withStyle(ChatFormatting.RED)
                        .append(Missatges.pais(enemic));
                if (!g.activa(ara)) {
                    m.append(Missatges.gris(". Preparació fins al " + Missatges.data(g.inici()) + "."));
                } else if (f.oberta(ara)) {
                    m.append(Missatges.gris(". La finestra és oberta: es lluita ara."));
                } else {
                    m.append(Missatges.gris(". Propera batalla: " + Missatges.data(f.properaObertura(ara)) + "."));
                }
                m.append(Component.literal(" ")).append(Missatges.boto("Estat", "/guerra estat " + enemic.sigla(),
                        ChatFormatting.GOLD));
                jugador.sendSystemMessage(m);
            });
        }
    }

    /** Un jugador ha mort: si era en guerra amb la finestra oberta, perd una vida i potser dona punts. */
    public static void mort(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer victima) || !finestraOberta()) {
            return;
        }
        MinecraftServer server = victima.getServer();
        if (server == null) {
            return;
        }
        IServerPartyAPI pv = PontOpac.partitDe(server, victima.getUUID());
        if (pv == null) {
            return;
        }
        Registre r = Dades.registre(server);
        long ara = System.currentTimeMillis();
        ParametresGuerra p = Config.parametres();
        Entity causa = e.getSource().getEntity();
        ServerPlayer assassi = causa instanceof ServerPlayer sp && sp != victima ? sp : null;
        IServerPartyAPI pa = assassi == null ? null : PontOpac.partitDe(server, assassi.getUUID());
        for (Guerra g : r.guerresDe(pv.getId())) {
            if (!g.activa(ara)) {
                continue;
            }
            if (pa != null && g.hiEs(pa.getId()) && !pa.getId().equals(pv.getId())) {
                if (r.baixa(g.id(), pa.getId(), victima.getUUID(), p)) {
                    Avisos.accio(assassi, Component.literal("+" + p.puntsBaixa() + " punts de guerra")
                            .withStyle(ChatFormatting.GREEN), Avisos.So.CAPTURA);
                }
            } else {
                r.mort(g.id(), victima.getUUID());
            }
            int queden = Math.max(0, p.vides() - g.mortsDe(victima.getUUID()));
            victima.sendSystemMessage(Component.literal(queden > 0
                    ? "⚔ Has perdut una vida de guerra: et " + (queden == 1 ? "queda 1." : "queden " + queden + ".")
                    : "⚔ No et queden vides: fins a la propera batalla, ja no captures.").withStyle(ChatFormatting.RED));
        }
    }

    /** Per a /guerra admin: l'estat de les coses que no es veuen. */
    public static List<String> diagnosi(MinecraftServer server) {
        List<String> r = new ArrayList<>();
        long ara = System.currentTimeMillis();
        FinestresGuerra f = Config.finestres();
        r.add("Finestra: " + (f.oberta(ara) ? "oberta fins a les " + HORA.withZone(f.zona()).format(Instant.ofEpochMilli(f.tancament(ara)))
                : "tancada; s'obre el " + Missatges.data(f.properaObertura(ara))) + " (" + f.zona() + ")");
        r.add("Zona oberta en finestra: " + Config.o(Config.OBRIR_ZONA, true) + " · PvP: " + Config.o(Config.OBRIR_PVP, true)
                + (ZonaOpac.pvpBloquejat() ? " (OPAC NO HO DEIXA: mira el registre)" : ""));
        r.add("Configs amb el PvP obert pel mod: " + Dades.dades(server).pvpObert().size());
        r.add("Barres de guerra: " + barres.size() + " · jugadors seguits: " + quiets.size());
        return r;
    }

    static ZoneId zona() {
        return Config.finestres().zona();
    }
}
