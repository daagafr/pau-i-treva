package cat.mineuab.pauitreva.model;

import java.util.UUID;

/** Dos països sense ordre: {@code Parella.de(a, b)} i {@code Parella.de(b, a)} són iguals. */
public record Parella(UUID a, UUID b) {

    public Parella {
        if (a.equals(b)) {
            throw new IllegalArgumentException("Una parella ha de tenir dos països diferents");
        }
        if (a.compareTo(b) > 0) {
            UUID t = a;
            a = b;
            b = t;
        }
    }

    public static Parella de(UUID x, UUID y) {
        return new Parella(x, y);
    }

    public boolean conte(UUID id) {
        return a.equals(id) || b.equals(id);
    }

    public UUID altre(UUID id) {
        if (a.equals(id)) {
            return b;
        }
        if (b.equals(id)) {
            return a;
        }
        throw new IllegalArgumentException(id + " no és d'aquesta parella");
    }
}
