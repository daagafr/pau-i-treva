package cat.mineuab.pauitreva.model;

import java.util.Locale;
import java.util.OptionalInt;
import java.util.regex.Pattern;

/** Regles dels noms, sigles, colors, lemes i banderes. Sense res de Minecraft, perquè es pugui provar sol. */
public final class Validacio {

    public static final int NOM_MIN = 3;
    public static final int NOM_MAX = 32;
    public static final int LEMA_MAX = 80;
    public static final int NOM_RANG_MAX = 24;

    /** De 2 a 4 lletres majúscules o xifres. */
    private static final Pattern SIGLA = Pattern.compile("^[A-Z0-9]{2,4}$");

    /**
     * Els caràcters que OPAC accepta al nom dels claims (opció {@code claims.name}), més el punt volat i el
     * punt, que en català calen («Col·lectiu») i que {@link #nomPerOpac} tradueix.
     */
    private static final Pattern NOM = Pattern.compile("^[\\p{L}0-9 _'\"!?,\\-&%*():·.]+$");

    /** El que OPAC deixa posar a {@code claims.name}. */
    public static final Pattern NOM_OPAC = Pattern.compile("^(\\p{L}|[0-9 _'\"!?,\\-&%*\\(\\):])*$");

    /** Id d'una pintura d'Immersive Paintings o de Betsy Ross («mineuab:catalunya»). */
    private static final Pattern BANDERA = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_./-]+$");

    private Validacio() {
    }

    public static String normalitzarSigla(String sigla) {
        return sigla.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean siglaValida(String sigla) {
        return SIGLA.matcher(sigla).matches();
    }

    /** Treu els espais dels extrems i deixa un sol espai entre paraules. */
    public static String normalitzarText(String text) {
        return text.trim().replaceAll("\\s+", " ");
    }

    public static boolean nomValid(String nom) {
        return nom.length() >= NOM_MIN && nom.length() <= NOM_MAX
                && NOM.matcher(nom).matches()
                && Character.isLetter(nom.codePointAt(0));
    }

    public static boolean lemaValid(String lema) {
        return lema.length() <= LEMA_MAX && lema.codePoints().noneMatch(Character::isISOControl);
    }

    /** Com els noms dels països, però també amb barra («Ministre/a»). */
    private static final Pattern NOM_RANG = Pattern.compile("^[\\p{L}0-9 _'\"!?,\\-&%*():·./]+$");

    public static boolean nomRangValid(String nom) {
        return !nom.isEmpty() && nom.length() <= NOM_RANG_MAX && NOM_RANG.matcher(nom).matches();
    }

    /** Buida vol dir «sense bandera». */
    public static boolean banderaValida(String bandera) {
        return bandera.isEmpty() || BANDERA.matcher(bandera).matches();
    }

    /** Llegeix «#RRGGBB» o «RRGGBB». */
    public static OptionalInt color(String text) {
        String t = text.trim();
        if (t.startsWith("#")) {
            t = t.substring(1);
        }
        if (!t.matches("^[0-9a-fA-F]{6}$")) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(Integer.parseInt(t, 16));
    }

    public static String colorText(int color) {
        return String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF);
    }

    /**
     * El nom del país tal com es pot posar als claims d'OPAC: el punt volat passa a guionet i el punt
     * desapareix, perquè OPAC no els accepta.
     */
    public static String nomPerOpac(String nom) {
        String r = nom.replace('·', '-').replace(".", "");
        return NOM_OPAC.matcher(r).matches() ? r : r.replaceAll("[^\\p{L}0-9 _'\"!?,\\-&%*():]", "");
    }
}
