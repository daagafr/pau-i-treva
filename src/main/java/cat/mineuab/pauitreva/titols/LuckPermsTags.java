package cat.mineuab.pauitreva.titols;

import java.util.UUID;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.node.types.PrefixNode;

/**
 * El tag del país com a prefix de LuckPerms. Només es carrega si LuckPerms hi és (vegeu {@link Tags}).
 *
 * <p>El prefix té una prioritat fixa ({@code tagPrioritat}, 5 per defecte: per sota de les universitats, que tenen
 * 9) i el context {@code server=<tagServidor>}. Els prefixos d'aquesta prioritat i amb només aquest context són del
 * mod: es treuen i es tornen a posar, sense tocar cap altre prefix del jugador.
 */
final class LuckPermsTags {

    private LuckPermsTags() {
    }

    /** {@code tag} {@code null} treu el tag. */
    static void posar(UUID jugador, String tag, int prioritat, String servidor) {
        LuckPermsProvider.get().getUserManager().modifyUser(jugador, user -> {
            user.data().clear(n -> n instanceof PrefixNode p && p.getPriority() == prioritat && esNostre(p, servidor));
            if (tag != null) {
                user.data().add(PrefixNode.builder(tag, prioritat).withContext(DefaultContextKeys.SERVER_KEY, servidor).build());
            }
        });
    }

    private static boolean esNostre(PrefixNode p, String servidor) {
        var c = p.getContexts();
        return c.size() == 1 && c.contains(DefaultContextKeys.SERVER_KEY, servidor);
    }
}
