package cat.mineuab.pauitreva.titols;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.event.plugin.TabLoadEvent;

/**
 * El placeholder {@code %pauitreva-tag%} del TAB: el tag del país del jugador, o buit. Al Create, la config del TAB el
 * fa servir damunt del cap ({@code tagprefix}). Només es carrega si el TAB hi és (vegeu {@link Tags}).
 */
final class TabTags {

    static final String PLACEHOLDER = "%pauitreva-tag%";

    private TabTags() {
    }

    static void registrar() {
        TabAPI api = TabAPI.getInstance();
        registrar(api);
        // Un /tab reload torna a començar de zero: s'ha de tornar a registrar.
        api.getEventBus().register(TabLoadEvent.class, e -> registrar(TabAPI.getInstance()));
    }

    private static void registrar(TabAPI api) {
        api.getPlaceholderManager().registerPlayerPlaceholder(PLACEHOLDER, 1000, jugador -> Tags.tag(jugador.getUniqueId()));
    }
}
