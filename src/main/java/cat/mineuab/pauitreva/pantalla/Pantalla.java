package cat.mineuab.pauitreva.pantalla;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * El que ha de pintar la pantalla del país (tecla N). La fa el servidor ({@link Vistes}) i el client només la
 * dibuixa: pestanyes, avisos i files de peces (text, botons i camps de text). Així, les fases noves del mod
 * només canvien el jar del servidor.
 *
 * <p>Un botó porta una acció: una ordre del mod ({@code /guerra estat OCC}) o {@code pestanya:<id>}. Si l'acció
 * té {@code {id}}, el client hi posa el que hi hagi escrit al camp de text amb aquest id.
 *
 * @param activa id de la pestanya que s'ensenya
 * @param avisos el que han respost les ordres de l'última acció (errors en vermell), a dalt de tot
 */
public record Pantalla(Component titol, List<Pestanya> pestanyes, String activa, List<Component> avisos,
                       List<Fila> files) {

    public record Pestanya(String id, Component nom) {
    }

    /** Una línia de la pantalla. Si només té un text, s'ajusta a l'amplada (pot ocupar més d'una línia). */
    public record Fila(List<Peca> peces) {

        public static Fila de(Peca... peces) {
            return new Fila(List.of(peces));
        }
    }

    /** Un tros d'una fila. */
    public sealed interface Peca permits Text, Boto, Entrada {
    }

    public record Text(Component text) implements Peca {
    }

    /** {@code accio} buida vol dir un botó desactivat. {@code ajuda} pot ser buit. */
    public record Boto(Component text, String accio, Component ajuda) implements Peca {
    }

    /** Un camp de text. {@code ample} en píxels. */
    public record Entrada(String id, String valor, int ample, int max, Component ajuda) implements Peca {
    }

    // ---------------------------------------------------------------- Codis per a la xarxa

    private static final StreamCodec<RegistryFriendlyByteBuf, Component> TEXT = ComponentSerialization.TRUSTED_STREAM_CODEC;
    private static final StreamCodec<ByteBuf, String> CADENA = ByteBufCodecs.stringUtf8(512);

    public static final StreamCodec<RegistryFriendlyByteBuf, Pantalla> CODI = StreamCodec.of(Pantalla::escriure, Pantalla::llegir);

    private static void escriure(RegistryFriendlyByteBuf buf, Pantalla p) {
        TEXT.encode(buf, p.titol());
        buf.writeVarInt(p.pestanyes().size());
        for (Pestanya t : p.pestanyes()) {
            CADENA.encode(buf, t.id());
            TEXT.encode(buf, t.nom());
        }
        CADENA.encode(buf, p.activa());
        buf.writeVarInt(p.avisos().size());
        p.avisos().forEach(a -> TEXT.encode(buf, a));
        buf.writeVarInt(p.files().size());
        for (Fila f : p.files()) {
            buf.writeVarInt(f.peces().size());
            for (Peca peca : f.peces()) {
                switch (peca) {
                    case Text t -> {
                        buf.writeByte(0);
                        TEXT.encode(buf, t.text());
                    }
                    case Boto b -> {
                        buf.writeByte(1);
                        TEXT.encode(buf, b.text());
                        CADENA.encode(buf, b.accio());
                        TEXT.encode(buf, b.ajuda());
                    }
                    case Entrada e -> {
                        buf.writeByte(2);
                        CADENA.encode(buf, e.id());
                        CADENA.encode(buf, e.valor());
                        buf.writeVarInt(e.ample());
                        buf.writeVarInt(e.max());
                        TEXT.encode(buf, e.ajuda());
                    }
                }
            }
        }
    }

    private static Pantalla llegir(RegistryFriendlyByteBuf buf) {
        Component titol = TEXT.decode(buf);
        int n = buf.readVarInt();
        List<Pestanya> pestanyes = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            pestanyes.add(new Pestanya(CADENA.decode(buf), TEXT.decode(buf)));
        }
        String activa = CADENA.decode(buf);
        n = buf.readVarInt();
        List<Component> avisos = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            avisos.add(TEXT.decode(buf));
        }
        n = buf.readVarInt();
        List<Fila> files = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int m = buf.readVarInt();
            List<Peca> peces = new ArrayList<>(m);
            for (int j = 0; j < m; j++) {
                byte tipus = buf.readByte();
                peces.add(switch (tipus) {
                    case 0 -> new Text(TEXT.decode(buf));
                    case 1 -> new Boto(TEXT.decode(buf), CADENA.decode(buf), TEXT.decode(buf));
                    case 2 -> new Entrada(CADENA.decode(buf), CADENA.decode(buf), buf.readVarInt(), buf.readVarInt(),
                            TEXT.decode(buf));
                    default -> throw new IllegalArgumentException("Peça desconeguda: " + tipus);
                });
            }
            files.add(new Fila(peces));
        }
        return new Pantalla(titol, pestanyes, activa, avisos, files);
    }
}
