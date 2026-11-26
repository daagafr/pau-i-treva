package cat.mineuab.pauitreva.mon;

import cat.mineuab.pauitreva.PauITreva;
import cat.mineuab.pauitreva.model.Capital;
import cat.mineuab.pauitreva.model.Chunk;
import cat.mineuab.pauitreva.model.Guerra;
import cat.mineuab.pauitreva.model.Termes;
import cat.mineuab.pauitreva.model.TipusObjectiu;
import java.util.LinkedHashSet;
import java.util.Set;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import net.minecraft.nbt.NbtUtils;
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
    private static final int FORMAT = 2;

    private final Registre registre;
    private long versioDesada;
    /** Configs d'OPAC on el mod ha obert el PvP de la guerra (vegeu ZonaOpac#aplicarPvp). */
    private final Set<UUID> pvpObert = new HashSet<>();
    private boolean pvpCanviat;

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

    public static Dades dades(MinecraftServer server) {
        return de(server);
    }

    /** Les configs on el mod ha obert el PvP. Qui la canvia ha de cridar {@link #pvpCanviat()}. */
    public Set<UUID> pvpObert() {
        return pvpObert;
    }

    public void pvpCanviat() {
        pvpCanviat = true;
    }

    @Override
    public boolean isDirty() {
        return registre.versio() != versioDesada || pvpCanviat;
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
            if (p.humiliatFins() > 0) {
                t.putLong("humiliatFins", p.humiliatFins());
            }
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

        ListTag guerres = new ListTag();
        for (Guerra g : registre.guerres()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", g.id());
            t.putUUID("atacant", g.atacant());
            t.putUUID("defensor", g.defensor());
            t.putString("objectiu", g.objectiu().name());
            t.put("chunks", chunks(g.chunks()));
            t.putLong("declarada", g.declarada());
            t.putLong("inici", g.inici());
            t.putLong("limit", g.limit());
            t.putInt("milpunts", g.milpunts());
            t.putInt("zona", g.zona());
            if (g.capitalAtacant() != null) {
                t.put("capitalAtacant", chunk(g.capitalAtacant()));
            }
            if (g.capitalDefensor() != null) {
                t.put("capitalDefensor", chunk(g.capitalDefensor()));
            }
            ListTag control = new ListTag();
            g.controls().forEach((c, v) -> {
                CompoundTag ct = chunk(c);
                ct.putInt("control", v);
                String sub = g.subconfigs().get(c);
                if (sub != null) {
                    ct.putString("sub", sub);
                }
                control.add(ct);
            });
            t.put("control", control);
            t.putInt("controlCapitalDefensor", g.controlCapitalDefensor());
            t.putInt("controlCapitalAtacant", g.controlCapitalAtacant());
            t.putInt("baixesAtacant", g.baixesAtacant());
            t.putInt("baixesDefensor", g.baixesDefensor());
            Registre.PropostaPau pau = registre.propostaPau(g.id()).orElse(null);
            if (pau != null) {
                CompoundTag p = new CompoundTag();
                p.putUUID("de", pau.de());
                p.putUUID("guanyador", pau.guanyador());
                p.put("chunks", chunks(pau.termes().chunks()));
                p.putBoolean("humiliacio", pau.termes().humiliacio());
                p.putLong("creada", pau.creada());
                t.put("pau", p);
            }
            guerres.add(t);
        }
        tag.put("guerres", guerres);

        ListTag pvp = new ListTag();
        for (UUID id : pvpObert) {
            pvp.add(NbtUtils.createUUID(id));
        }
        tag.put("pvpObert", pvp);

        versioDesada = registre.versio();
        pvpCanviat = false;
        return tag;
    }

    private static ListTag chunks(Set<Chunk> chunks) {
        ListTag l = new ListTag();
        for (Chunk c : chunks) {
            l.add(chunk(c));
        }
        return l;
    }

    private static CompoundTag chunk(Chunk c) {
        CompoundTag t = new CompoundTag();
        t.putString("dimensio", c.dimensio());
        t.putInt("x", c.x());
        t.putInt("z", c.z());
        return t;
    }

    private static Chunk chunk(CompoundTag t) {
        return new Chunk(t.getString("dimensio"), t.getInt("x"), t.getInt("z"));
    }

    private static Chunk chunkSiHiEs(CompoundTag pare, String clau) {
        return pare.contains(clau, Tag.TAG_COMPOUND) ? chunk(pare.getCompound(clau)) : null;
    }

    private static Set<Chunk> chunks(ListTag l) {
        Set<Chunk> r = new LinkedHashSet<>();
        for (Tag e : l) {
            r.add(chunk((CompoundTag) e));
        }
        return r;
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
                Registre.carregarHumiliacio(p, t.getLong("humiliatFins"));
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

        for (Tag e : tag.getList("guerres", Tag.TAG_COMPOUND)) {
            try {
                CompoundTag t = (CompoundTag) e;
                // El format 1 desava punts sencers; el 2, mil·lèsimes.
                int milpunts = t.contains("milpunts") ? t.getInt("milpunts") : t.getInt("punts") * Guerra.MIL;
                Guerra g = new Guerra(t.getUUID("id"), t.getUUID("atacant"), t.getUUID("defensor"),
                        TipusObjectiu.valueOf(t.getString("objectiu")), chunks(t.getList("chunks", Tag.TAG_COMPOUND)),
                        t.getLong("declarada"), t.getLong("inici"), t.getLong("limit"),
                        chunkSiHiEs(t, "capitalAtacant"), chunkSiHiEs(t, "capitalDefensor"), milpunts);
                Map<Chunk, Integer> control = new HashMap<>();
                Map<Chunk, String> subs = new HashMap<>();
                for (Tag ec : t.getList("control", Tag.TAG_COMPOUND)) {
                    CompoundTag ct = (CompoundTag) ec;
                    Chunk c = chunk(ct);
                    control.put(c, ct.getInt("control"));
                    if (ct.contains("sub", Tag.TAG_STRING)) {
                        subs.put(c, ct.getString("sub"));
                    }
                }
                g.carregarEstat(control, t.getInt("controlCapitalDefensor"), t.getInt("controlCapitalAtacant"),
                        t.getInt("baixesAtacant"), t.getInt("baixesDefensor"), subs, t.getInt("zona"));
                r.carregarGuerra(g);
                if (t.contains("pau", Tag.TAG_COMPOUND)) {
                    CompoundTag p = t.getCompound("pau");
                    r.carregarPropostaPau(g.id(), new Registre.PropostaPau(p.getUUID("de"), p.getUUID("guanyador"),
                            new Termes(chunks(p.getList("chunks", Tag.TAG_COMPOUND)), p.getBoolean("humiliacio")),
                            p.getLong("creada")));
                }
            } catch (RuntimeException ex) {
                PauITreva.LOG.error("Pau i Treva: no s'ha pogut llegir una guerra: {}", e, ex);
            }
        }

        Dades dades = new Dades(r);
        for (Tag e : tag.getList("pvpObert", Tag.TAG_INT_ARRAY)) {
            try {
                dades.pvpObert.add(NbtUtils.loadUUID(e));
            } catch (RuntimeException ex) {
                PauITreva.LOG.error("Pau i Treva: no s'ha pogut llegir un id de pvpObert: {}", e, ex);
            }
        }

        PauITreva.LOG.info("Pau i Treva: {} països, {} relacions, {} propostes, {} guerres i {} entrades del diari",
                r.paisos().size(), r.totesLesRelacions().size(), r.totesLesPropostes().size(), r.guerres().size(),
                r.diari().size());
        return dades;
    }
}
