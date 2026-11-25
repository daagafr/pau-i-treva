package cat.mineuab.pauitreva.model;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.DateTimeException;
import java.util.Optional;

/**
 * Les hores en què es pot lluitar (com la Treva de Déu, però al revés: fora d'aquestes hores, tot està
 * protegit). Una franja per als dies feiners i una per al cap de setmana, en hora local; l'inici entra i el
 * final no.
 */
public record FinestresGuerra(ZoneId zona, LocalTime iniciFeiner, LocalTime fiFeiner,
                              LocalTime iniciCapDeSetmana, LocalTime fiCapDeSetmana) {

    /** Decidit el 25/11: de dilluns a divendres de 18:00 a 23:00 i el cap de setmana de 12:00 a 23:00. */
    public static FinestresGuerra perDefecte() {
        return new FinestresGuerra(ZoneId.of("Europe/Madrid"), LocalTime.of(18, 0), LocalTime.of(23, 0),
                LocalTime.of(12, 0), LocalTime.of(23, 0));
    }

    /** Llegeix «18:00-23:00», «12:00-23:00» i «Europe/Madrid», com a la config. */
    public static Optional<FinestresGuerra> llegir(String feiners, String capDeSetmana, String zona) {
        try {
            LocalTime[] f = franja(feiners);
            LocalTime[] c = franja(capDeSetmana);
            if (f == null || c == null) {
                return Optional.empty();
            }
            return Optional.of(new FinestresGuerra(ZoneId.of(zona.trim()), f[0], f[1], c[0], c[1]));
        } catch (DateTimeException e) {
            // També hi entren DateTimeParseException (hora mal escrita) i ZoneRulesException (zona que no existeix).
            return Optional.empty();
        }
    }

    private static LocalTime[] franja(String text) {
        String[] parts = text.trim().split("-");
        if (parts.length != 2) {
            return null;
        }
        LocalTime inici = LocalTime.parse(parts[0].trim());
        LocalTime fi = LocalTime.parse(parts[1].trim());
        return inici.isBefore(fi) ? new LocalTime[] {inici, fi} : null;
    }

    public boolean oberta(long ara) {
        ZonedDateTime t = Instant.ofEpochMilli(ara).atZone(zona);
        LocalTime h = t.toLocalTime();
        return !h.isBefore(inici(t.toLocalDate())) && h.isBefore(fi(t.toLocalDate()));
    }

    /** Quan s'obre la propera finestra; si ara ja és oberta, {@code ara}. */
    public long properaObertura(long ara) {
        if (oberta(ara)) {
            return ara;
        }
        LocalDate dia = Instant.ofEpochMilli(ara).atZone(zona).toLocalDate();
        for (int i = 0; i < 8; i++) {
            LocalDate d = dia.plusDays(i);
            long obertura = d.atTime(inici(d)).atZone(zona).toInstant().toEpochMilli();
            if (obertura > ara) {
                return obertura;
            }
        }
        throw new IllegalStateException("No hi ha cap finestra en una setmana");
    }

    /** Quan es tanca la finestra oberta ara, o -1 si ara no n'hi ha cap d'oberta. */
    public long tancament(long ara) {
        if (!oberta(ara)) {
            return -1;
        }
        LocalDate d = Instant.ofEpochMilli(ara).atZone(zona).toLocalDate();
        return d.atTime(fi(d)).atZone(zona).toInstant().toEpochMilli();
    }

    private static boolean capDeSetmana(LocalDate d) {
        return d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private LocalTime inici(LocalDate d) {
        return capDeSetmana(d) ? iniciCapDeSetmana : iniciFeiner;
    }

    private LocalTime fi(LocalDate d) {
        return capDeSetmana(d) ? fiCapDeSetmana : fiFeiner;
    }
}
