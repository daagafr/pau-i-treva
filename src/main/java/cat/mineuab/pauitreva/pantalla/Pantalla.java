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
 * @param capcalera el país de qui mira (escut, nom i xips), a dalt de tot
 * @param activa    id de la pestanya que s'ensenya
 * @param avisos    el que han respost les ordres de l'última acció (errors en vermell), a dalt del contingut
 */
public record Pantalla(Capcalera capcalera, List<Pestanya> pestanyes, String activa, List<Component> avisos,
                       List<Fila> files) {

    /** {@code sigla} buida vol dir que qui mira no té país (es pinta l'escut del mod). */
    public record Capcalera(int color, String sigla, Component nom, Component subtitol, List<Peca> xips) {
    }

    /** {@code icona}: el nom d'una icona de l'atles (vegeu eines/icones.py). */
    public record Pestanya(String id, Component nom, String icona) {
    }

    /** Una línia de la pantalla. Si només té un text, s'ajusta a l'amplada (pot ocupar més d'una línia). */
    public record Fila(List<Peca> peces) {

        public static Fila de(Peca... peces) {
            return new Fila(List.of(peces));
        }
    }

    /** Un tros d'una fila. */
    public sealed interface Peca permits Text, Boto, Entrada, Icona, Barra, Escut, Seccio {
    }

    public record Text(Component text) implements Peca {
    }

    /** {@code accio} buida vol dir un botó desactivat. {@code ajuda} pot ser buit. */
    public record Boto(Component text, String accio, Component ajuda) implements Peca {
    }

    /** Un camp de text. {@code ample} en píxels. */
    public record Entrada(String id, String valor, int ample, int max, Component ajuda) implements Peca {
    }

    /** Una icona amb un text al costat (un «xip»), i el detall en passar-hi el ratolí. */
    public record Icona(String icona, Component text, Component ajuda) implements Peca {
    }

    /**
     * Una barra: la part de l'esquerra ({@code valor}, de 0 a 1) d'un color i la resta d'un altre, amb un text al
     * mig. Per a la puntuació de guerra (el país de qui mira a l'esquerra) i el control dels chunks.
     */
    public record Barra(float valor, int colorEsquerra, int colorDreta, Component text, int ample, Component ajuda)
            implements Peca {
    }

    /** Un escut petit d'un país (color i sigla). */
    public record Escut(int color, String sigla, Component ajuda) implements Peca {
    }

    /** El títol d'una secció. Sol en una fila: les files de sota, fins a la secció següent, van dins d'un marc. */
    public record Seccio(Component titol, String icona) implements Peca {
    }

    // ---------------------------------------------------------------- Codis per a la xarxa

    private static final StreamCodec<RegistryFriendlyByteBuf, Component> TEXT = ComponentSerialization.TRUSTED_STREAM_CODEC;
    private static final StreamCodec<ByteBuf, String> CADENA = ByteBufCodecs.stringUtf8(512);

    public static final StreamCodec<RegistryFriendlyByteBuf, Pantalla> CODI = StreamCodec.of(Pantalla::escriure, Pantalla::llegir);

    private static void escriure(RegistryFriendlyByteBuf buf, Pantalla p) {
        Capcalera c = p.capcalera();
        buf.writeInt(c.color());
        CADENA.encode(buf, c.sigla());
        TEXT.encode(buf, c.nom());
        TEXT.encode(buf, c.subtitol());
        escriurePeces(buf, c.xips());
        buf.writeVarInt(p.pestanyes().size());
        for (Pestanya t : p.pestanyes()) {
            CADENA.encode(buf, t.id());
            TEXT.encode(buf, t.nom());
            CADENA.encode(buf, t.icona());
        }
        CADENA.encode(buf, p.activa());
        buf.writeVarInt(p.avisos().size());
        p.avisos().forEach(a -> TEXT.encode(buf, a));
        buf.writeVarInt(p.files().size());
        for (Fila f : p.files()) {
            escriurePeces(buf, f.peces());
        }
    }

    private static void escriurePeces(RegistryFriendlyByteBuf buf, List<Peca> peces) {
        buf.writeVarInt(peces.size());
        for (Peca peca : peces) {
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
                    case Icona i -> {
                        buf.writeByte(3);
                        CADENA.encode(buf, i.icona());
                        TEXT.encode(buf, i.text());
                        TEXT.encode(buf, i.ajuda());
                    }
                    case Barra b -> {
                        buf.writeByte(4);
                        buf.writeFloat(b.valor());
                        buf.writeInt(b.colorEsquerra());
                        buf.writeInt(b.colorDreta());
                        TEXT.encode(buf, b.text());
                        buf.writeVarInt(b.ample());
                        TEXT.encode(buf, b.ajuda());
                    }
                    case Escut e -> {
                        buf.writeByte(5);
                        buf.writeInt(e.color());
                        CADENA.encode(buf, e.sigla());
                        TEXT.encode(buf, e.ajuda());
                    }
                    case Seccio s -> {
                        buf.writeByte(6);
                        TEXT.encode(buf, s.titol());
                        CADENA.encode(buf, s.icona());
                    }
                }
        }
    }

    private static Pantalla llegir(RegistryFriendlyByteBuf buf) {
        Capcalera capcalera = new Capcalera(buf.readInt(), CADENA.decode(buf), TEXT.decode(buf), TEXT.decode(buf),
                llegirPeces(buf));
        int n = buf.readVarInt();
        List<Pestanya> pestanyes = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            pestanyes.add(new Pestanya(CADENA.decode(buf), TEXT.decode(buf), CADENA.decode(buf)));
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
            files.add(new Fila(llegirPeces(buf)));
        }
        return new Pantalla(capcalera, pestanyes, activa, avisos, files);
    }

    private static List<Peca> llegirPeces(RegistryFriendlyByteBuf buf) {
        int m = buf.readVarInt();
        List<Peca> peces = new ArrayList<>(m);
        for (int j = 0; j < m; j++) {
            byte tipus = buf.readByte();
            peces.add(switch (tipus) {
                case 0 -> new Text(TEXT.decode(buf));
                case 1 -> new Boto(TEXT.decode(buf), CADENA.decode(buf), TEXT.decode(buf));
                case 2 -> new Entrada(CADENA.decode(buf), CADENA.decode(buf), buf.readVarInt(), buf.readVarInt(),
                        TEXT.decode(buf));
                case 3 -> new Icona(CADENA.decode(buf), TEXT.decode(buf), TEXT.decode(buf));
                case 4 -> new Barra(buf.readFloat(), buf.readInt(), buf.readInt(), TEXT.decode(buf), buf.readVarInt(),
                        TEXT.decode(buf));
                case 5 -> new Escut(buf.readInt(), CADENA.decode(buf), TEXT.decode(buf));
                case 6 -> new Seccio(TEXT.decode(buf), CADENA.decode(buf));
                default -> throw new IllegalArgumentException("Peça desconeguda: " + tipus);
            });
        }
        return peces;
    }
}
