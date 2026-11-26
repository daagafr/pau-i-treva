package cat.mineuab.pauitreva.ordres;

import cat.mineuab.pauitreva.model.Pais;
import cat.mineuab.pauitreva.model.Resultat;
import cat.mineuab.pauitreva.model.Territori;
import cat.mineuab.pauitreva.model.Validacio;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;

/** Els textos que veuen els jugadors, en català. */
public final class Missatges {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Europe/Madrid"));
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.of("Europe/Madrid"));

    private Missatges() {
    }

    public static String data(long ms) {
        return DATA.format(Instant.ofEpochMilli(ms));
    }

    public static String dia(long ms) {
        return DIA.format(Instant.ofEpochMilli(ms));
    }

    /** «[CAT] Catalunya» amb el color del país; en passar-hi el ratolí, el lema. */
    public static MutableComponent pais(Pais p) {
        MutableComponent hover = Component.literal(p.nom()).withStyle(s -> s.withColor(TextColor.fromRgb(p.color())));
        if (!p.lema().isEmpty()) {
            hover.append(Component.literal("\n«" + p.lema() + "»").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        }
        hover.append(Component.literal("\n/nacio info " + p.sigla()).withStyle(ChatFormatting.DARK_GRAY));
        return Component.literal("[" + p.sigla() + "] " + p.nom()).withStyle(s -> s
                .withColor(TextColor.fromRgb(p.color()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nacio info " + p.sigla())));
    }

    public static MutableComponent bo(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GREEN);
    }

    public static MutableComponent gris(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GRAY);
    }

    public static MutableComponent titol(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
    }

    /** Un botó del xat que executa una ordre. */
    public static MutableComponent boto(String text, String ordre, ChatFormatting color) {
        return Component.literal("[" + text + "]").withStyle(s -> s
                .withColor(color)
                .withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, ordre))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(ordre))));
    }

    /** Avís per a tots els jugadors connectats (declaracions, aliances, paus...). */
    public static void anunciar(MinecraftServer server, String text) {
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("⚖ ").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(text).withStyle(ChatFormatting.YELLOW)),
                false);
    }

    /** Per què no val un objectiu de conquesta. */
    public static String errorObjectiu(Territori.Error e, int max) {
        return switch (e) {
            case BUIT -> "L'objectiu és buit. Afegeix-hi chunks amb /guerra objectiu afegeix o /guerra objectiu radi <r>.";
            case MASSA_CHUNKS -> "L'objectiu té massa chunks: contra aquest país, com a molt " + max + ".";
            case NO_ES_DEL_DEFENSOR -> "Algun chunk de l'objectiu ja no és del país que ataques.";
            case DIMENSIONS_BARREJADES -> "Tots els chunks de l'objectiu han de ser de la mateixa dimensió.";
            case NO_CONNECTAT -> "Els chunks de l'objectiu han de formar una sola zona (tocant-se pels costats).";
            case NO_TOCA_FRONTERA -> "L'objectiu ha de tocar el teu territori: almenys un chunk ha de fer frontera amb un dels teus.";
        };
    }

    public static String resultat(Resultat r) {
        return switch (r) {
            case FET -> "Fet.";
            case PROPOSADA -> "Proposta enviada.";
            case ACCEPTADA -> "Acceptat.";
            case TRENCADA_ALIANCA -> "Aliança trencada.";
            case TRENCAT_PACTE -> "Pacte trencat.";
            case DECLARADA -> "Guerra declarada.";
            case PAU_PROPOSADA -> "Pau proposada.";
            case PAU_SIGNADA -> "Pau signada.";
            case ALIATS -> "Sou aliats: per declarar-los la guerra, primer cal trencar l'aliança.";
            case EN_PACTE -> "Teniu un pacte de no agressió: trencar-lo abans d'hora queda al diari.";
            case OBJECTIU_INVALID -> "L'objectiu de la guerra no val.";
            case JA_EN_DISPUTA -> "Algun chunk de l'objectiu ja està en disputa en una altra guerra.";
            case MASSA_GUERRES -> "Un dels dos països ja té massa guerres obertes.";
            case NO_HI_HA_GUERRA -> "Aquesta guerra no existeix (o ja s'ha acabat).";
            case PUNTS_INSUFICIENTS -> "No teniu prou punts de guerra per imposar aquests termes.";
            case JA_EXISTEIX -> "Aquesta party ja és un país.";
            case NO_EXISTEIX -> "Aquest país no existeix.";
            case NOM_INVALID -> "Aquest nom no val: de " + Validacio.NOM_MIN + " a " + Validacio.NOM_MAX
                    + " caràcters, començant per una lletra, i només lletres, xifres, espais i signes senzills.";
            case SIGLA_INVALIDA -> "La sigla ha de tenir de 2 a 4 lletres o xifres, sense accents.";
            case NOM_OCUPAT -> "Ja hi ha un país amb aquest nom.";
            case SIGLA_OCUPADA -> "Ja hi ha un país amb aquesta sigla.";
            case MATEIX_PAIS -> "No pots fer això amb el teu propi país.";
            case TIPUS_INVALID -> "Això no es pot fer.";
            case DIES_INVALIDS -> "El nombre de dies no val.";
            case JA_PROPOSAT -> "Ja els ho has proposat. Espera que responguin.";
            case JA_HO_SOU -> "Ja ho sou.";
            case EN_GUERRA -> "Sou en guerra.";
            case EN_TREVA -> "Sou en treva: no es pot fer fins que s'acabi.";
            case NO_HI_HA_PROPOSTA -> "No hi ha cap proposta d'aquest país (o ja ha caducat).";
            case NO_HI_HA_RES -> "No teniu cap aliança ni cap pacte.";
        };
    }
}
