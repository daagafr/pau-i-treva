package cat.mineuab.pauitreva.mon;

import cat.mineuab.pauitreva.PauITreva;
import cat.mineuab.pauitreva.model.Capital;
import cat.mineuab.pauitreva.model.Esdeveniment;
import cat.mineuab.pauitreva.model.EstatRelacio;
import cat.mineuab.pauitreva.model.Pais;
import cat.mineuab.pauitreva.model.Parella;
import cat.mineuab.pauitreva.model.Permis;
import cat.mineuab.pauitreva.model.Proposta;
import cat.mineuab.pauitreva.model.Rang;
import cat.mineuab.pauitreva.model.Registre;
import cat.mineuab.pauitreva.model.TipusRelacio;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Les dades del mod dins del món: {@code world/data/pauitreva.dat}. Així entren a la còpia de cada nit.
 *
 * <p>Es desa quan el {@link Registre} ha canviat ({@link #isDirty()} compara versions), sense haver de
 * recordar-se de cridar {@code setDirty()} a cada ordre.
 */
public final class Dades extends SavedData {

    public static final String NOM = PauITreva.ID;
    private static final int FORMAT = 1;

    private final Registre registre;
    private long versioDesada;

    private Dades(Registre registre) {
        this.registre = registre;
        this.versioDesada = registre.versio();
    }

    public static Dades de(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(() -> new Dades(new Registre()), Dades::carregar, null), NOM);
    }

    public static Registre registre(MinecraftServer server) {
        return de(server).registre;
    }

    @Override
    public boolean isDirty() {
        return registre.versio() != versioDesada;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider proveidor) {
        tag.putInt("format", FORMAT);

        ListTag paisos = new ListTag();
        for (Pais p : registre.paisos()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", p.id());
            t.putString("nom", p.nom());
            t.putString("sigla", p.sigla());
            t.putInt("color", p.color());
            t.putLong("fundat", p.fundat());
            t.putString("lema", p.lema());
            t.putString("bandera", p.bandera());
            if (p.capital() != null) {
                CompoundTag c = new CompoundTag();
                c.putString("dimensio", p.capital().dimensio());
                c.putInt("x", p.capital().chunkX());
                c.putInt("z", p.capital().chunkZ());
                t.put("capital", c);
            }
            CompoundTag rangs = new CompoundTag();
            p.nomsRangsPropis().forEach((rang, nom) -> rangs.putString(rang.clau(), nom));
            t.put("rangs", rangs);
            CompoundTag permisos = new CompoundTag();
            p.permisosPropis().forEach((permis, rang) -> permisos.putString(permis.clau(), rang.clau()));
            t.put("permisos", permisos);
            paisos.add(t);
        }
        tag.put("paisos", paisos);

        ListTag relacions = new ListTag();
        registre.totesLesRelacions().forEach((parella, estat) -> {
            CompoundTag t = new CompoundTag();
            t.putUUID("a", parella.a());
            t.putUUID("b", parella.b());
            t.putString("tipus", estat.tipus().name());
            t.putLong("des", estat.des());
            t.putLong("fins", estat.fins());
            relacions.add(t);
        });
        tag.put("relacions", relacions);

        ListTag propostes = new ListTag();
        for (Proposta p : registre.totesLesPropostes()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("de", p.de());
            t.putUUID("a", p.a());
            t.putString("tipus", p.tipus().name());
            t.putInt("dies", p.dies());
            t.putLong("creada", p.creada());
            t.putLong("caduca", p.caduca());
            propostes.add(t);
        }
        tag.put("propostes", propostes);

        ListTag diari = new ListTag();
        for (Esdeveniment e : registre.diari()) {
            CompoundTag t = new CompoundTag();
            t.putLong("quan", e.quan());
            t.putString("tipus", e.tipus());
            t.putString("text", e.text());
            diari.add(t);
        }
        tag.put("diari", diari);

        versioDesada = registre.versio();
        return tag;
    }

    /** Llegeix les dades. Una entrada dolenta s'avisa al registre i se salta, sense perdre la resta. */
    private static Dades carregar(CompoundTag tag, HolderLookup.Provider proveidor) {
        Registre r = new Registre();

        for (Tag e : tag.getList("paisos", Tag.TAG_COMPOUND)) {
            try {
                CompoundTag t = (CompoundTag) e;
                Pais p = new Pais(t.getUUID("id"), t.getString("nom"), t.getString("sigla"), t.getInt("color"),
                        t.getLong("fundat"));
                Capital capital = null;
                if (t.contains("capital", Tag.TAG_COMPOUND)) {
                    CompoundTag c = t.getCompound("capital");
                    capital = new Capital(c.getString("dimensio"), c.getInt("x"), c.getInt("z"));
                }
                Map<Rang, String> rangs = new EnumMap<>(Rang.class);
                CompoundTag tr = t.getCompound("rangs");
                for (String clau : tr.getAllKeys()) {
                    Rang.perClau(clau).ifPresent(rang -> rangs.put(rang, tr.getString(clau)));
                }
                Map<Permis, Rang> permisos = new EnumMap<>(Permis.class);
                CompoundTag tp = t.getCompound("permisos");
                for (String clau : tp.getAllKeys()) {
                    Permis.perClau(clau).ifPresent(permis ->
                            Rang.perClau(tp.getString(clau)).ifPresent(rang -> permisos.put(permis, rang)));
                }
                Registre.carregarDetalls(p, t.getString("lema"), t.getString("bandera"), capital, rangs, permisos);
                r.carregarPais(p);
            } catch (RuntimeException ex) {
                PauITreva.LOG.error("Pau i Treva: no s'ha pogut llegir un país: {}", e, ex);
            }
        }

        for (Tag e : tag.getList("relacions", Tag.TAG_COMPOUND)) {
            try {
                CompoundTag t = (CompoundTag) e;
                r.carregarRelacio(Parella.de(t.getUUID("a"), t.getUUID("b")), new EstatRelacio(
                        TipusRelacio.valueOf(t.getString("tipus")), t.getLong("des"), t.getLong("fins")));
            } catch (RuntimeException ex) {
                PauITreva.LOG.error("Pau i Treva: no s'ha pogut llegir una relació: {}", e, ex);
            }
        }

        for (Tag e : tag.getList("propostes", Tag.TAG_COMPOUND)) {
            try {
                CompoundTag t = (CompoundTag) e;
                r.carregarProposta(new Proposta(t.getUUID("de"), t.getUUID("a"),
                        TipusRelacio.valueOf(t.getString("tipus")), t.getInt("dies"), t.getLong("creada"),
                        t.getLong("caduca")));
            } catch (RuntimeException ex) {
                PauITreva.LOG.error("Pau i Treva: no s'ha pogut llegir una proposta: {}", e, ex);
            }
        }

        for (Tag e : tag.getList("diari", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) e;
            r.carregarEsdeveniment(new Esdeveniment(t.getLong("quan"), t.getString("tipus"), t.getString("text")));
        }

        PauITreva.LOG.info("Pau i Treva: {} països, {} relacions, {} propostes i {} entrades del diari",
                r.paisos().size(), r.totesLesRelacions().size(), r.totesLesPropostes().size(), r.diari().size());
        return new Dades(r);
    }
}
