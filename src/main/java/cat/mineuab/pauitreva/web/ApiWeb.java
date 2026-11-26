package cat.mineuab.pauitreva.web;

import cat.mineuab.pauitreva.Config;
import cat.mineuab.pauitreva.guerra.Batalles;
import cat.mineuab.pauitreva.PauITreva;
import cat.mineuab.pauitreva.model.Exportacio;
import cat.mineuab.pauitreva.model.Rang;
import cat.mineuab.pauitreva.model.Registre;
import cat.mineuab.pauitreva.mon.Dades;
import cat.mineuab.pauitreva.opac.PontOpac;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import net.minecraft.server.MinecraftServer;
import xaero.pac.common.parties.party.member.api.IPartyMemberAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

/**
 * API JSON de només lectura per a la web: {@code http://127.0.0.1:<port>/estat.json}. Només escolta a la
 * mateixa màquina; a fora hi surt pel túnel de Cloudflare, com Credits i Perfil.
 *
 * <p>El JSON es fa al fil del servidor ({@link #actualitzar}, des del manteniment) i el fil de l'HTTP només
 * serveix l'últim que hi ha. Així l'HTTP no toca mai les dades del joc.
 */
public final class ApiWeb {

    private static HttpServer http;
    private static volatile byte[] estat = "{}".getBytes(StandardCharsets.UTF_8);

    private ApiWeb() {
    }

    public static void engegar(int port) {
        aturar();
        try {
            HttpServer s = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
            s.createContext("/", ApiWeb::respondre);
            s.setExecutor(Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "PauITreva-API");
                t.setDaemon(true);
                return t;
            }));
            s.start();
            http = s;
            PauITreva.LOG.info("Pau i Treva: API web a http://127.0.0.1:{}/estat.json", port);
        } catch (IOException | RuntimeException | LinkageError e) {
            // Si el port és ocupat (o falta el mòdul de l'HTTP), el joc segueix igual, sense API.
            PauITreva.LOG.error("Pau i Treva: no s'ha pogut obrir l'API web al port {}", port, e);
        }
    }

    public static void aturar() {
        if (http != null) {
            http.stop(0);
            http = null;
        }
    }

    /** Torna a fer el JSON. S'ha de cridar des del fil del servidor. */
    public static void actualitzar(MinecraftServer server) {
        if (http == null) {
            return;
        }
        Registre r = Dades.registre(server);
        long ara = System.currentTimeMillis();
        String json = Exportacio.estat(r, id -> infoPartit(server, id), ara,
                Exportacio.Finestres.de(Config.finestres(), Batalles.finestraOberta(), ara));
        estat = json.getBytes(StandardCharsets.UTF_8);
    }

    private static Exportacio.InfoPartit infoPartit(MinecraftServer server, java.util.UUID id) {
        IServerPartyAPI partit = PontOpac.partit(server, id);
        if (partit == null) {
            return null;
        }
        List<Exportacio.Membre> membres = new ArrayList<>();
        for (IPartyMemberAPI m : PontOpac.membres(partit)) {
            Rang rang = PontOpac.rang(partit, m.getUUID());
            membres.add(new Exportacio.Membre(m.getUsername(), rang == null ? Rang.CIUTADA : rang));
        }
        return new Exportacio.InfoPartit(PontOpac.nomLider(partit), membres, PontOpac.territori(server, partit));
    }

    private static void respondre(HttpExchange e) throws IOException {
        try (e) {
            String cami = e.getRequestURI().getPath();
            if (!"GET".equals(e.getRequestMethod()) && !"HEAD".equals(e.getRequestMethod())) {
                e.sendResponseHeaders(405, -1);
                return;
            }
            if (!"/estat.json".equals(cami)) {
                e.sendResponseHeaders(404, -1);
                return;
            }
            byte[] cos = estat;
            e.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            e.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            e.getResponseHeaders().set("Cache-Control", "public, max-age=15");
            e.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            if ("HEAD".equals(e.getRequestMethod())) {
                e.sendResponseHeaders(200, -1);
                return;
            }
            e.sendResponseHeaders(200, cos.length);
            try (OutputStream out = e.getResponseBody()) {
                out.write(cos);
            }
        }
    }
}
