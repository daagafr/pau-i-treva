package cat.mineuab.pauitreva.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Tots els països, les relacions entre ells, les propostes pendents i el diari diplomàtic.
 *
 * <p>Aquí hi ha totes les regles, sense res de Minecraft ni d'OPAC, perquè es puguin provar amb JUnit. Qui
 * el fa servir (les ordres, el manteniment) s'encarrega de comprovar els permisos del jugador i d'aplicar
 * els canvis a OPAC. Cada canvi apuja {@link #versio()}, i així les dades del món saben quan s'han de desar.
 *
 * <p>Les hores són mil·lisegons des de 1970 ({@code System.currentTimeMillis()}), passats sempre com a
 * paràmetre perquè les proves puguin fer passar el temps.
 */
public final class Registre {

    public static final long HORA = 60L * 60 * 1000;
    public static final long DIA = 24 * HORA;
    public static final int MAX_DIARI = 500;

    private final Map<UUID, Pais> paisos = new LinkedHashMap<>();
    private final Map<Parella, EstatRelacio> relacions = new HashMap<>();
    private final List<Proposta> propostes = new ArrayList<>();
    private final ArrayDeque<Esdeveniment> diari = new ArrayDeque<>();
    private final Map<UUID, Guerra> guerres = new LinkedHashMap<>();
    /** Per a cada guerra, la proposta de pau pendent (com a molt una). */
    private final Map<UUID, PropostaPau> propostesPau = new HashMap<>();
    private long versio;

    /** Una pau proposada per {@code de}, que l'altre bàndol ha d'acceptar. */
    public record PropostaPau(UUID de, UUID guanyador, Termes termes, long creada) {
    }

    public long versio() {
        return versio;
    }

    private void canvi() {
        versio++;
    }

    // ---------------------------------------------------------------- Consultes

    public Optional<Pais> pais(UUID id) {
        return Optional.ofNullable(paisos.get(id));
    }

    public Collection<Pais> paisos() {
        return Collections.unmodifiableCollection(paisos.values());
    }

    public Optional<Pais> perSigla(String sigla) {
        String s = Validacio.normalitzarSigla(sigla);
        return paisos.values().stream().filter(p -> p.sigla().equals(s)).findFirst();
    }

    public Optional<Pais> perNom(String nom) {
        String n = Validacio.normalitzarText(nom);
        return paisos.values().stream().filter(p -> p.nom().equalsIgnoreCase(n)).findFirst();
    }

    public TipusRelacio relacio(UUID a, UUID b) {
        return estat(a, b).map(EstatRelacio::tipus).orElse(TipusRelacio.NEUTRAL);
    }

    public Optional<EstatRelacio> estat(UUID a, UUID b) {
        if (a.equals(b)) {
            return Optional.empty();
        }
        return Optional.ofNullable(relacions.get(Parella.de(a, b)));
    }

    /** Les relacions d'un país, per l'id de l'altre país. */
    public Map<UUID, EstatRelacio> relacionsDe(UUID id) {
        Map<UUID, EstatRelacio> r = new LinkedHashMap<>();
        relacions.forEach((parella, estat) -> {
            if (parella.conte(id)) {
                r.put(parella.altre(id), estat);
            }
        });
        return r;
    }

    public Map<Parella, EstatRelacio> totesLesRelacions() {
        return Collections.unmodifiableMap(relacions);
    }

    public List<Proposta> propostesRebudes(UUID id) {
        return propostes.stream().filter(p -> p.a().equals(id)).toList();
    }

    public List<Proposta> propostesFetes(UUID id) {
        return propostes.stream().filter(p -> p.de().equals(id)).toList();
    }

    public List<Proposta> totesLesPropostes() {
        return Collections.unmodifiableList(propostes);
    }

    /** Les {@code n} entrades més noves del diari, de la més nova a la més vella. */
    public List<Esdeveniment> darrers(int n) {
        List<Esdeveniment> r = new ArrayList<>(Math.min(n, diari.size()));
        Iterator<Esdeveniment> it = diari.descendingIterator();
        while (it.hasNext() && r.size() < n) {
            r.add(it.next());
        }
        return r;
    }

    /** Tot el diari, del més vell al més nou. */
    public List<Esdeveniment> diari() {
        return List.copyOf(diari);
    }

    public static String etiqueta(Pais p) {
        return "[" + p.sigla() + "] " + p.nom();
    }

    // ---------------------------------------------------------------- Fundar i editar

    public Resultat fundar(UUID id, String nom, String sigla, int color, long ara) {
        String n = Validacio.normalitzarText(nom);
        String s = Validacio.normalitzarSigla(sigla);
        if (paisos.containsKey(id)) {
            return Resultat.JA_EXISTEIX;
        }
        if (!Validacio.nomValid(n)) {
            return Resultat.NOM_INVALID;
        }
        if (!Validacio.siglaValida(s)) {
            return Resultat.SIGLA_INVALIDA;
        }
        if (perSigla(s).isPresent()) {
            return Resultat.SIGLA_OCUPADA;
        }
        if (perNom(n).isPresent()) {
            return Resultat.NOM_OCUPAT;
        }
        Pais p = new Pais(id, n, s, color, ara);
        paisos.put(id, p);
        apuntar(ara, Esdeveniment.FUNDACIO, "S'ha fundat " + etiqueta(p) + ".");
        canvi();
        return Resultat.FET;
    }

    public Resultat canviarNom(UUID id, String nom, long ara) {
        Pais p = paisos.get(id);
        if (p == null) {
            return Resultat.NO_EXISTEIX;
        }
        String n = Validacio.normalitzarText(nom);
        if (!Validacio.nomValid(n)) {
            return Resultat.NOM_INVALID;
        }
        Optional<Pais> altre = perNom(n);
        if (altre.isPresent() && altre.get() != p) {
            return Resultat.NOM_OCUPAT;
        }
        String abans = etiqueta(p);
        p.posarNom(n);
        apuntar(ara, Esdeveniment.NOM, abans + " ara es diu " + etiqueta(p) + ".");
        canvi();
        return Resultat.FET;
    }

    public Resultat canviarSigla(UUID id, String sigla, long ara) {
        Pais p = paisos.get(id);
        if (p == null) {
            return Resultat.NO_EXISTEIX;
        }
        String s = Validacio.normalitzarSigla(sigla);
        if (!Validacio.siglaValida(s)) {
            return Resultat.SIGLA_INVALIDA;
        }
        Optional<Pais> altre = perSigla(s);
        if (altre.isPresent() && altre.get() != p) {
            return Resultat.SIGLA_OCUPADA;
        }
        String abans = etiqueta(p);
        p.posarSigla(s);
        apuntar(ara, Esdeveniment.NOM, abans + " ara es diu " + etiqueta(p) + ".");
        canvi();
        return Resultat.FET;
    }

    public Resultat canviarColor(UUID id, int color) {
        return editar(id, p -> p.posarColor(color));
    }

    public Resultat canviarLema(UUID id, String lema) {
        String l = Validacio.normalitzarText(lema);
        if (!Validacio.lemaValid(l)) {
            return Resultat.NOM_INVALID;
        }
        return editar(id, p -> p.posarLema(l));
    }

    public Resultat canviarBandera(UUID id, String bandera) {
        String b = bandera.trim();
        if (!Validacio.banderaValida(b)) {
            return Resultat.NOM_INVALID;
        }
        return editar(id, p -> p.posarBandera(b));
    }

    /** {@code capital} pot ser {@code null} per treure-la. */
    public Resultat canviarCapital(UUID id, Capital capital) {
        return editar(id, p -> p.posarCapital(capital));
    }

    /** {@code nom} buit torna el rang al nom de per defecte. */
    public Resultat canviarNomRang(UUID id, Rang rang, String nom) {
        String n = Validacio.normalitzarText(nom);
        if (!n.isEmpty() && !Validacio.nomRangValid(n)) {
            return Resultat.NOM_INVALID;
        }
        return editar(id, p -> p.posarNomRang(rang, n));
    }

    public Resultat canviarPermis(UUID id, Permis permis, Rang rang) {
        if (!permis.configurable()) {
            return Resultat.TIPUS_INVALID;
        }
        return editar(id, p -> p.posarPermis(permis, rang));
    }

    private Resultat editar(UUID id, java.util.function.Consumer<Pais> accio) {
        Pais p = paisos.get(id);
        if (p == null) {
            return Resultat.NO_EXISTEIX;
        }
        accio.accept(p);
        canvi();
        return Resultat.FET;
    }

    /** Treu el país, les seves relacions i les propostes on surt. {@code motiu} pot ser buit. */
    public Resultat dissoldre(UUID id, String motiu, long ara) {
        Pais p = paisos.remove(id);
        if (p == null) {
            return Resultat.NO_EXISTEIX;
        }
        relacions.keySet().removeIf(parella -> parella.conte(id));
        propostes.removeIf(pr -> pr.de().equals(id) || pr.a().equals(id));
        guerres.values().removeIf(g -> {
            if (g.hiEs(id)) {
                propostesPau.remove(g.id());
                return true;
            }
            return false;
        });
        String text = etiqueta(p) + " s'ha dissolt" + (motiu == null || motiu.isEmpty() ? "." : ": " + motiu + ".");
        apuntar(ara, Esdeveniment.DISSOLUCIO, text);
        canvi();
        return Resultat.FET;
    }

    // ---------------------------------------------------------------- Diplomàcia

    /**
     * Proposa una aliança o un pacte de no agressió. Si l'altre país ja havia proposat el mateix, la proposta
     * s'accepta (amb els dies que deia la seva).
     *
     * @param dies     per a un pacte, quants dies ha de durar; per a una aliança no compta
     * @param caducitat quant temps val la proposta si ningú no la respon
     */
    public Resultat proposar(UUID de, UUID a, TipusRelacio tipus, int dies, long ara, long caducitat) {
        if (de.equals(a)) {
            return Resultat.MATEIX_PAIS;
        }
        if (!paisos.containsKey(de) || !paisos.containsKey(a)) {
            return Resultat.NO_EXISTEIX;
        }
        if (tipus != TipusRelacio.ALIANCA && tipus != TipusRelacio.PACTE) {
            return Resultat.TIPUS_INVALID;
        }
        if (tipus == TipusRelacio.PACTE && dies < 1) {
            return Resultat.DIES_INVALIDS;
        }
        int d = tipus == TipusRelacio.PACTE ? dies : 0;
        Resultat bloqueig = bloqueig(relacio(de, a), tipus);
        if (bloqueig != null) {
            return bloqueig;
        }
        Optional<Proposta> alReves = proposta(a, de, tipus);
        if (alReves.isPresent()) {
            establir(alReves.get(), ara);
            return Resultat.ACCEPTADA;
        }
        if (proposta(de, a, tipus).isPresent()) {
            return Resultat.JA_PROPOSAT;
        }
        propostes.add(new Proposta(de, a, tipus, d, ara, ara + caducitat));
        canvi();
        return Resultat.PROPOSADA;
    }

    /** Accepta la proposta que {@code de} ha fet a {@code qui} (la més nova, si n'hi ha més d'una). */
    public Resultat acceptar(UUID qui, UUID de, long ara) {
        Proposta pr = null;
        for (Proposta p : propostes) {
            if (p.de().equals(de) && p.a().equals(qui) && !p.caducada(ara)) {
                pr = p;
            }
        }
        if (pr == null) {
            return Resultat.NO_HI_HA_PROPOSTA;
        }
        Resultat bloqueig = bloqueig(relacio(qui, de), pr.tipus());
        if (bloqueig != null) {
            propostes.remove(pr);
            canvi();
            return bloqueig;
        }
        establir(pr, ara);
        return Resultat.ACCEPTADA;
    }

    public Resultat rebutjar(UUID qui, UUID de) {
        boolean tret = propostes.removeIf(p -> p.de().equals(de) && p.a().equals(qui));
        if (!tret) {
            return Resultat.NO_HI_HA_PROPOSTA;
        }
        canvi();
        return Resultat.FET;
    }

    /** Trenca l'aliança o el pacte entre {@code qui} i {@code altre}. Les treves i les guerres no es poden trencar. */
    public Resultat trencar(UUID qui, UUID altre, long ara) {
        Pais p = paisos.get(qui);
        Pais q = paisos.get(altre);
        if (p == null || q == null) {
            return Resultat.NO_EXISTEIX;
        }
        if (qui.equals(altre)) {
            return Resultat.MATEIX_PAIS;
        }
        switch (relacio(qui, altre)) {
            case ALIANCA -> {
                relacions.remove(Parella.de(qui, altre));
                apuntar(ara, Esdeveniment.ALIANCA_TRENCADA, etiqueta(p) + " ha trencat l'aliança amb " + etiqueta(q) + ".");
                canvi();
                return Resultat.TRENCADA_ALIANCA;
            }
            case PACTE -> {
                relacions.remove(Parella.de(qui, altre));
                apuntar(ara, Esdeveniment.PACTE_TRENCAT, etiqueta(p) + " ha trencat abans d'hora el pacte de no agressió amb "
                        + etiqueta(q) + ".");
                canvi();
                return Resultat.TRENCAT_PACTE;
            }
            case TREVA -> {
                return Resultat.EN_TREVA;
            }
            case GUERRA -> {
                return Resultat.EN_GUERRA;
            }
            default -> {
                return Resultat.NO_HI_HA_RES;
            }
        }
    }

    /**
     * Treu les propostes que ningú no ha respost a temps i acaba els pactes i les treves que ja han passat.
     *
     * @return les entrades noves del diari, per avisar els jugadors
     */
    public List<Esdeveniment> caducar(long ara) {
        List<Esdeveniment> nous = new ArrayList<>();
        boolean canviat = propostes.removeIf(p -> p.caducada(ara));
        Iterator<Map.Entry<Parella, EstatRelacio>> it = relacions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Parella, EstatRelacio> e = it.next();
            EstatRelacio estat = e.getValue();
            if (!estat.caducada(ara)) {
                continue;
            }
            it.remove();
            canviat = true;
            String a = paisos.containsKey(e.getKey().a()) ? etiqueta(paisos.get(e.getKey().a())) : "?";
            String b = paisos.containsKey(e.getKey().b()) ? etiqueta(paisos.get(e.getKey().b())) : "?";
            if (estat.tipus() == TipusRelacio.PACTE) {
                nous.add(apuntar(ara, Esdeveniment.PACTE_ACABAT, "S'ha acabat el pacte de no agressió entre " + a + " i " + b + "."));
            } else if (estat.tipus() == TipusRelacio.TREVA) {
                nous.add(apuntar(ara, Esdeveniment.TREVA_ACABADA, "S'ha acabat la treva entre " + a + " i " + b + "."));
            }
        }
        if (canviat) {
            canvi();
        }
        return nous;
    }

    /** Apunta una entrada al diari. Si n'hi ha massa, treu les més velles. */
    public Esdeveniment apuntar(long ara, String tipus, String text) {
        Esdeveniment e = new Esdeveniment(ara, tipus, text);
        diari.addLast(e);
        while (diari.size() > MAX_DIARI) {
            diari.removeFirst();
        }
        canvi();
        return e;
    }

    /** Per què no es pot fer aquesta proposta amb la relació d'ara, o {@code null} si es pot. */
    private static Resultat bloqueig(TipusRelacio ara, TipusRelacio proposta) {
        return switch (ara) {
            case GUERRA -> Resultat.EN_GUERRA;
            case ALIANCA -> Resultat.JA_HO_SOU;
            case PACTE -> proposta == TipusRelacio.PACTE ? Resultat.JA_HO_SOU : null;
            case TREVA -> proposta == TipusRelacio.PACTE ? Resultat.EN_TREVA : null;
            case NEUTRAL -> null;
        };
    }

    private Optional<Proposta> proposta(UUID de, UUID a, TipusRelacio tipus) {
        return propostes.stream()
                .filter(p -> p.de().equals(de) && p.a().equals(a) && p.tipus() == tipus)
                .findFirst();
    }

    private void establir(Proposta pr, long ara) {
        Parella parella = Parella.de(pr.de(), pr.a());
        Pais p = paisos.get(pr.de());
        Pais q = paisos.get(pr.a());
        if (pr.tipus() == TipusRelacio.ALIANCA) {
            relacions.put(parella, new EstatRelacio(TipusRelacio.ALIANCA, ara, 0));
            // Una aliança ja inclou no atacar-se: les propostes de pacte entre els dos sobren.
            propostes.removeIf(x -> parella.equals(Parella.de(x.de(), x.a())));
            apuntar(ara, Esdeveniment.ALIANCA, etiqueta(p) + " i " + etiqueta(q) + " s'han aliat.");
        } else {
            relacions.put(parella, new EstatRelacio(TipusRelacio.PACTE, ara, ara + pr.dies() * DIA));
            propostes.removeIf(x -> parella.equals(Parella.de(x.de(), x.a())) && x.tipus() == TipusRelacio.PACTE);
            apuntar(ara, Esdeveniment.PACTE, etiqueta(p) + " i " + etiqueta(q) + " han signat un pacte de no agressió de "
                    + pr.dies() + (pr.dies() == 1 ? " dia." : " dies."));
        }
        canvi();
    }

    // ---------------------------------------------------------------- Guerra

    /** Quants punts de captura té cada bàndol en un chunk ara mateix. */
    public record Presencia(Chunk chunk, int delAtacant, int delDefensor) {
    }

    /** Un punt de captura que ha canviat de mans del tot. */
    public record CanviControl(UUID guerra, Chunk chunk, UUID quiPren, boolean capturat) {
    }

    /** Quins chunks de l'objectiu entren en uns termes de pau. */
    public enum QuinsChunks {
        CAP,
        CONTROLATS,
        OBJECTIU
    }

    public Collection<Guerra> guerres() {
        return Collections.unmodifiableCollection(guerres.values());
    }

    public Optional<Guerra> guerra(UUID id) {
        return Optional.ofNullable(guerres.get(id));
    }

    public Optional<Guerra> guerraEntre(UUID a, UUID b) {
        return guerres.values().stream().filter(g -> g.hiEs(a) && g.hiEs(b)).findFirst();
    }

    public List<Guerra> guerresDe(UUID pais) {
        return guerres.values().stream().filter(g -> g.hiEs(pais)).toList();
    }

    public Optional<PropostaPau> propostaPau(UUID guerra) {
        return Optional.ofNullable(propostesPau.get(guerra));
    }

    /** Com {@link #declararGuerra(UUID, UUID, TipusObjectiu, java.util.Set, long, ParametresGuerra, Chunk, Chunk)}, sense capitals. */
    public Resultat declararGuerra(UUID atacant, UUID defensor, TipusObjectiu objectiu, java.util.Set<Chunk> chunks,
                                   long ara, long preparacio, int maxChunks) {
        ParametresGuerra d = ParametresGuerra.perDefecte();
        ParametresGuerra p = new ParametresGuerra(preparacio, d.treva(), d.durada(), d.humiliacio(), maxChunks, 100,
                d.calFrontera(), Integer.MAX_VALUE, d.capturaBase(), d.capturaExtra(), d.capturaMax(), d.pesDefensor(),
                d.decadencia(), d.factorCapital(), d.ocupacioPerHora(), d.capitalPerHora(), d.defensaPerHora(),
                d.puntsBaixa(), d.baixesMaxVictima(), d.vides());
        return declararGuerra(atacant, defensor, objectiu, chunks, ara, p, null, null);
    }

    /**
     * Declara la guerra. Qui crida ha de comprovar abans, a OPAC, que els chunks d'una conquesta són del
     * defensor (amb {@link Territori#validar}); aquí només es mira que no en siguin més de {@code p.maxChunks()}.
     *
     * @param chunks          per a una conquesta, els chunks de l'objectiu; per als altres objectius, cap
     * @param capitalAtacant  la capital de l'atacant durant la guerra (la pot prendre el defensor), o {@code null}
     * @param capitalDefensor la capital del defensor durant la guerra (la pot prendre l'atacant), o {@code null}
     */
    public Resultat declararGuerra(UUID atacant, UUID defensor, TipusObjectiu objectiu, java.util.Set<Chunk> chunks,
                                   long ara, ParametresGuerra p, Chunk capitalAtacant, Chunk capitalDefensor) {
        if (atacant.equals(defensor)) {
            return Resultat.MATEIX_PAIS;
        }
        Pais a = paisos.get(atacant);
        Pais d = paisos.get(defensor);
        if (a == null || d == null) {
            return Resultat.NO_EXISTEIX;
        }
        switch (relacio(atacant, defensor)) {
            case ALIANCA -> {
                return Resultat.ALIATS;
            }
            case PACTE -> {
                return Resultat.EN_PACTE;
            }
            case TREVA -> {
                return Resultat.EN_TREVA;
            }
            case GUERRA -> {
                return Resultat.EN_GUERRA;
            }
            default -> {
            }
        }
        boolean conquesta = objectiu == TipusObjectiu.CONQUESTA;
        if (conquesta ? chunks.isEmpty() || chunks.size() > p.maxChunks() : !chunks.isEmpty()) {
            return Resultat.OBJECTIU_INVALID;
        }
        if (guerresDe(atacant).size() >= p.guerresMax() || guerresDe(defensor).size() >= p.guerresMax()) {
            return Resultat.MASSA_GUERRES;
        }
        long inici = ara + p.preparacio();
        Guerra g = new Guerra(UUID.randomUUID(), atacant, defensor, objectiu, chunks, ara, inici,
                p.durada() > 0 ? inici + p.durada() : 0, capitalAtacant, capitalDefensor, 0);
        guerres.put(g.id(), g);
        Parella parella = Parella.de(atacant, defensor);
        relacions.put(parella, new EstatRelacio(TipusRelacio.GUERRA, ara, 0));
        propostes.removeIf(x -> parella.equals(Parella.de(x.de(), x.a())));
        String perque = conquesta
                ? "per conquerir " + chunks.size() + (chunks.size() == 1 ? " chunk" : " chunks")
                : "per " + objectiu.nom();
        long hores = Math.round(p.preparacio() / (double) HORA);
        apuntar(ara, Esdeveniment.GUERRA, etiqueta(a) + " declara la guerra a " + etiqueta(d) + " " + perque
                + ". Es podrà lluitar d'aquí a " + hores + (hores == 1 ? " hora." : " hores."));
        canvi();
        return Resultat.DECLARADA;
    }

    /** Per provar: la guerra deixa d'estar en preparació ara mateix. */
    public Resultat acabarPreparacio(UUID guerra, long ara) {
        Guerra g = guerres.get(guerra);
        if (g == null) {
            return Resultat.NO_HI_HA_GUERRA;
        }
        g.avancarInici(ara);
        canvi();
        return Resultat.FET;
    }

    /** Suma punts de guerra al país {@code perAquest} (i els treu al seu enemic). */
    public Resultat sumarPunts(UUID guerra, UUID perAquest, int punts) {
        Guerra g = guerres.get(guerra);
        if (g == null) {
            return Resultat.NO_HI_HA_GUERRA;
        }
        if (!g.hiEs(perAquest)) {
            return Resultat.TIPUS_INVALID;
        }
        int abans = g.milpunts();
        g.sumar(perAquest, punts);
        if (g.milpunts() != abans) {
            canvi();
        }
        return Resultat.FET;
    }

    /**
     * Un segon (o uns quants) de batalla: mou el control de cada punt de captura segons qui hi ha. Només s'ha de
     * cridar amb la finestra oberta i la guerra activa.
     *
     * @param presencies qui hi ha a cada punt de captura (els que no hi surten, buits)
     * @return els punts que han canviat de mans del tot, per avisar els jugadors
     */
    public List<CanviControl> capturar(UUID guerra, Collection<Presencia> presencies, int segons, ParametresGuerra p) {
        Guerra g = guerres.get(guerra);
        if (g == null) {
            return List.of();
        }
        Map<Chunk, Presencia> per = new HashMap<>();
        for (Presencia pr : presencies) {
            per.put(pr.chunk(), pr);
        }
        java.util.Set<Chunk> punts = new java.util.LinkedHashSet<>(g.chunks());
        if (g.capitalDefensor() != null) {
            punts.add(g.capitalDefensor());
        }
        if (g.capitalAtacant() != null) {
            punts.add(g.capitalAtacant());
        }
        List<CanviControl> canvis = new ArrayList<>();
        boolean canviat = false;
        for (Chunk c : punts) {
            Presencia pr = per.getOrDefault(c, new Presencia(c, 0, 0));
            UUID quiPren = g.quiPren(c);
            boolean prenAtacant = quiPren.equals(g.atacant());
            int prenen = prenAtacant ? pr.delAtacant() : pr.delDefensor();
            int defensen = prenAtacant ? pr.delDefensor() : pr.delAtacant();
            boolean capital = !g.chunks().contains(c);
            int abans = g.controlDe(c);
            int delta = p.deltaCaptura(prenen, defensen, capital);
            if (prenen == 0 && defensen == 0 && abans > 0 && abans < Guerra.CONTROL_MAX) {
                delta = -p.decadencia();
            }
            if (delta == 0) {
                continue;
            }
            int despres = g.moureControl(c, delta * segons);
            if (despres != abans) {
                canviat = true;
            }
            if (abans < Guerra.CONTROL_MAX && despres >= Guerra.CONTROL_MAX) {
                canvis.add(new CanviControl(guerra, c, quiPren, true));
            } else if (abans >= Guerra.CONTROL_MAX && despres < Guerra.CONTROL_MAX) {
                canvis.add(new CanviControl(guerra, c, quiPren, false));
            }
        }
        if (canviat) {
            canvi();
        }
        return canvis;
    }

    /**
     * Un minut de finestra oberta amb la guerra activa: l'atacant guanya punts per l'objectiu que controla, cada
     * bàndol per la capital enemiga que té, i el defensor guanya una mica si l'atacant no controla res.
     */
    public void puntuarMinut(UUID guerra, ParametresGuerra p) {
        Guerra g = guerres.get(guerra);
        if (g == null) {
            return;
        }
        int abans = g.milpunts();
        if (!g.chunks().isEmpty()) {
            long suma = 0;
            for (int v : g.controls().values()) {
                suma += v;
            }
            double mil = p.ocupacioPerHora() * (double) Guerra.MIL * suma / (60.0 * g.chunks().size() * Guerra.CONTROL_MAX);
            g.sumarMil(g.atacant(), (int) Math.round(mil));
        }
        int capitalMinut = perMinut(p.capitalPerHora());
        boolean capitalDefensorPresa = g.capitalDefensor() != null && !g.chunks().contains(g.capitalDefensor())
                && g.controlCapitalDefensor() >= Guerra.CONTROL_MAX;
        if (capitalDefensorPresa) {
            g.sumarMil(g.atacant(), capitalMinut);
        }
        if (g.capitalAtacant() != null && g.controlCapitalAtacant() >= Guerra.CONTROL_MAX) {
            g.sumarMil(g.defensor(), capitalMinut);
        }
        if (g.controlats().isEmpty() && !capitalDefensorPresa) {
            g.sumarMil(g.defensor(), perMinut(p.defensaPerHora()));
        }
        if (g.milpunts() != abans) {
            canvi();
        }
    }

    /** Mil·lèsimes de punt per minut, per a uns punts per hora (arrodonit). */
    static int perMinut(int puntsPerHora) {
        return (int) Math.round(puntsPerHora * (double) Guerra.MIL / 60);
    }

    /**
     * Un jugador del país {@code paisAssassi} ha matat {@code victima}, que és del país enemic, amb la finestra
     * oberta. Compta per a les vides de la víctima i, si no ha mort massa vegades a mans de l'enemic en
     * aquesta finestra, dona punts.
     *
     * @return si la baixa ha donat punts
     */
    public boolean baixa(UUID guerra, UUID paisAssassi, UUID victima, ParametresGuerra p) {
        Guerra g = guerres.get(guerra);
        if (g == null || !g.hiEs(paisAssassi)) {
            return false;
        }
        g.mort(victima);
        g.comptarBaixa(paisAssassi);
        boolean compta = g.baixaDe(victima) <= p.baixesMaxVictima();
        if (compta) {
            g.sumar(paisAssassi, p.puntsBaixa());
        }
        canvi();
        return compta;
    }

    /** Un jugador d'aquesta guerra ha mort sense que el mati l'enemic (caiguda, lava...): perd una vida. */
    public void mort(UUID guerra, UUID jugador) {
        Guerra g = guerres.get(guerra);
        if (g != null) {
            g.mort(jugador);
        }
    }

    /** Si el jugador encara pot capturar en aquesta finestra (no ha gastat totes les vides). */
    public boolean potCapturar(UUID guerra, UUID jugador, ParametresGuerra p) {
        Guerra g = guerres.get(guerra);
        return g != null && g.mortsDe(jugador) < p.vides();
    }

    /** S'obre una finestra nova: tothom torna a tenir totes les vides. */
    public void novaFinestra() {
        guerres.values().forEach(Guerra::novaFinestra);
    }

    /**
     * Els termes d'una pau a favor de {@code guanyador}. Els chunks només els pot guanyar l'atacant; si qui guanya
     * és el defensor, {@code quins} no compta.
     */
    public static Termes termes(Guerra g, UUID guanyador, QuinsChunks quins, boolean humiliacio) {
        java.util.Set<Chunk> chunks = new java.util.LinkedHashSet<>();
        if (guanyador.equals(g.atacant())) {
            switch (quins) {
                case OBJECTIU -> chunks.addAll(g.chunks());
                case CONTROLATS -> chunks.addAll(g.controlats());
                case CAP -> {
                }
            }
        }
        return new Termes(chunks, humiliacio);
    }

    /**
     * Proposa una pau. L'altre bàndol l'ha d'acceptar amb {@link #acceptarPau}; si {@code de} ja té prou punts
     * per als termes i és qui guanya, la pot imposar directament amb {@link #ferPau}.
     */
    public Resultat proposarPau(UUID de, UUID guerra, UUID guanyador, Termes termes, long ara) {
        Guerra g = guerres.get(guerra);
        if (g == null) {
            return Resultat.NO_HI_HA_GUERRA;
        }
        if (!g.hiEs(de) || !g.hiEs(guanyador)) {
            return Resultat.TIPUS_INVALID;
        }
        Resultat valids = termesValids(g, guanyador, termes);
        if (valids != null) {
            return valids;
        }
        propostesPau.put(g.id(), new PropostaPau(de, guanyador, termes, ara));
        canvi();
        return Resultat.PAU_PROPOSADA;
    }

    /** Treu la proposta de pau d'una guerra, si n'hi ha (la rebutja l'altre bàndol o la retira qui l'ha feta). */
    public Resultat retirarPau(UUID qui, UUID guerra) {
        Guerra g = guerres.get(guerra);
        if (g == null) {
            return Resultat.NO_HI_HA_GUERRA;
        }
        if (!g.hiEs(qui) || propostesPau.remove(guerra) == null) {
            return Resultat.NO_HI_HA_PROPOSTA;
        }
        canvi();
        return Resultat.FET;
    }

    /**
     * L'altre bàndol accepta la pau proposada.
     *
     * @param creada quan es va fer la proposta que el jugador ha vist ({@link PropostaPau#creada()}). Si
     *               mentrestant n'hi ha una altra, no s'accepta: així ningú no pot canviar els termes just
     *               abans que l'altre faci clic a «Accepta».
     */
    public Resultat acceptarPau(UUID qui, UUID guerra, long creada, long ara, long treva) {
        return acceptarPau(qui, guerra, creada, ara, treva, ParametresGuerra.perDefecte().humiliacio());
    }

    public Resultat acceptarPau(UUID qui, UUID guerra, long creada, long ara, long treva, long humiliacio) {
        Guerra g = guerres.get(guerra);
        PropostaPau pr = propostesPau.get(guerra);
        if (g == null) {
            return Resultat.NO_HI_HA_GUERRA;
        }
        if (pr == null || !g.hiEs(qui) || pr.de().equals(qui) || pr.creada() != creada) {
            return Resultat.NO_HI_HA_PROPOSTA;
        }
        return ferPau(guerra, pr.guanyador(), pr.termes(), false, ara, treva, humiliacio);
    }

    /**
     * Qui es rendeix perd: si és el defensor, dona a l'atacant tot l'objectiu (i la humiliació, si era l'objectiu);
     * si és l'atacant, queda humiliat.
     */
    public Resultat rendir(UUID qui, UUID guerra, long ara, long treva) {
        return rendir(qui, guerra, ara, treva, ParametresGuerra.perDefecte().humiliacio());
    }

    public Resultat rendir(UUID qui, UUID guerra, long ara, long treva, long humiliacio) {
        Guerra g = guerres.get(guerra);
        if (g == null) {
            return Resultat.NO_HI_HA_GUERRA;
        }
        if (!g.hiEs(qui)) {
            return Resultat.TIPUS_INVALID;
        }
        return ferPau(guerra, g.enemic(qui), termesRendicio(g, qui), false, ara, treva, humiliacio);
    }

    /** Els termes de la rendició de {@code qui} (vegeu {@link #rendir}). */
    public static Termes termesRendicio(Guerra g, UUID qui) {
        return g.enemic(qui).equals(g.atacant())
                ? new Termes(g.chunks(), g.objectiu() == TipusObjectiu.HUMILIACIO)
                : new Termes(java.util.Set.of(), true);
    }

    /**
     * Acaba una guerra que ha arribat al límit: guanya qui té més punts i s'emporta el que pot pagar amb ells.
     * L'atacant, els chunks que controla del tot (els que pugui pagar, primer els més controlats) i la humiliació
     * si era l'objectiu; el defensor, la humiliació de l'atacant. Si ningú no pot pagar res, pau blanca.
     */
    public Resultat pauAutomatica(UUID guerra, long ara, ParametresGuerra p) {
        Guerra g = guerres.get(guerra);
        if (g == null) {
            return Resultat.NO_HI_HA_GUERRA;
        }
        Termes termes = termesAutomatics(g);
        UUID guanyador = g.punts() >= 0 ? g.atacant() : g.defensor();
        apuntar(ara, Esdeveniment.LIMIT, "La guerra entre " + etiquetaDe(g.atacant()) + " i " + etiquetaDe(g.defensor())
                + " ha arribat al límit de temps.");
        return ferPau(guerra, guanyador, termes, false, ara, p.treva(), p.humiliacio());
    }

    /** Els termes de la pau automàtica (vegeu {@link #pauAutomatica}). */
    public static Termes termesAutomatics(Guerra g) {
        int punts = g.punts();
        if (punts > 0) {
            boolean humilia = g.objectiu() == TipusObjectiu.HUMILIACIO && punts >= Termes.COST_HUMILIACIO;
            int resten = punts - (humilia ? Termes.COST_HUMILIACIO : 0);
            java.util.Set<Chunk> chunks = new java.util.LinkedHashSet<>();
            for (Chunk c : g.controlats()) {
                java.util.Set<Chunk> prova = new java.util.LinkedHashSet<>(chunks);
                prova.add(c);
                if (new Termes(prova, false).cost(g) > resten) {
                    break;
                }
                chunks = prova;
            }
            return new Termes(chunks, humilia);
        }
        if (punts < 0 && -punts >= Termes.COST_HUMILIACIO) {
            return new Termes(java.util.Set.of(), true);
        }
        return Termes.blanca();
    }

    /** Les guerres que ja han arribat al límit de temps. */
    public List<Guerra> guerresAlLimit(long ara) {
        return guerres.values().stream().filter(g -> g.limit() > 0 && ara >= g.limit()).toList();
    }

    public Resultat ferPau(UUID guerra, UUID guanyador, Termes termes, boolean imposada, long ara, long treva) {
        return ferPau(guerra, guanyador, termes, imposada, ara, treva, ParametresGuerra.perDefecte().humiliacio());
    }

    /**
     * Signa la pau i acaba la guerra: treu la guerra, posa una treva de {@code treva} mil·lisegons, humilia el
     * perdedor si ho diuen els termes i ho apunta al diari. Els chunks dels termes els ha de passar a OPAC qui
     * crida (abans, perquè un cop signada la guerra ja no hi és).
     *
     * @param imposada si és {@code true}, el guanyador ha de tenir prou punts per pagar els termes
     */
    public Resultat ferPau(UUID guerra, UUID guanyador, Termes termes, boolean imposada, long ara, long treva,
                           long humiliacio) {
        Guerra g = guerres.get(guerra);
        if (g == null) {
            return Resultat.NO_HI_HA_GUERRA;
        }
        if (!g.hiEs(guanyador)) {
            return Resultat.TIPUS_INVALID;
        }
        Resultat valids = termesValids(g, guanyador, termes);
        if (valids != null) {
            return valids;
        }
        if (imposada && termes.cost(g) > g.puntsDe(guanyador)) {
            return Resultat.PUNTS_INSUFICIENTS;
        }
        guerres.remove(guerra);
        propostesPau.remove(guerra);
        relacions.put(Parella.de(g.atacant(), g.defensor()), new EstatRelacio(TipusRelacio.TREVA, ara, ara + treva));
        String guanya = etiquetaDe(guanyador);
        UUID perdedor = g.enemic(guanyador);
        String perd = etiquetaDe(perdedor);
        if (termes.humiliacio() && paisos.containsKey(perdedor)) {
            paisos.get(perdedor).posarHumiliatFins(ara + humiliacio);
        }
        String text;
        if (termes.esBlanca()) {
            text = "Pau blanca entre " + guanya + " i " + perd + ".";
        } else {
            List<String> parts = new ArrayList<>();
            if (!termes.chunks().isEmpty()) {
                int n = termes.chunks().size();
                parts.add(perd + " cedeix " + n + (n == 1 ? " chunk" : " chunks") + " a " + guanya);
            }
            if (termes.humiliacio()) {
                parts.add(perd + " queda humiliat");
            }
            text = "Pau entre " + guanya + " i " + perd + ": " + String.join("; ", parts) + ".";
        }
        apuntar(ara, Esdeveniment.PAU, text);
        canvi();
        return Resultat.PAU_SIGNADA;
    }

    /** Els chunks dels termes han de ser de l'objectiu, i només els pot guanyar l'atacant. */
    private static Resultat termesValids(Guerra g, UUID guanyador, Termes termes) {
        if (!termes.chunks().isEmpty()
                && (!guanyador.equals(g.atacant()) || !g.chunks().containsAll(termes.chunks()))) {
            return Resultat.OBJECTIU_INVALID;
        }
        return null;
    }

    private String etiquetaDe(UUID id) {
        Pais p = paisos.get(id);
        return p == null ? "?" : etiqueta(p);
    }

    // ---------------------------------------------------------------- Càrrega de les dades desades

    public void carregarGuerra(Guerra g) {
        guerres.put(g.id(), g);
        canvi();
    }

    public void carregarPropostaPau(UUID guerra, PropostaPau p) {
        propostesPau.put(guerra, p);
        canvi();
    }

    public Map<UUID, PropostaPau> totesLesPropostesPau() {
        return Collections.unmodifiableMap(propostesPau);
    }

    /** Només per carregar les dades del món: no comprova res ni apunta res al diari. */
    public void carregarPais(Pais p) {
        paisos.put(p.id(), p);
        canvi();
    }

    public void carregarRelacio(Parella parella, EstatRelacio estat) {
        relacions.put(parella, estat);
        canvi();
    }

    public void carregarProposta(Proposta p) {
        propostes.add(p);
        canvi();
    }

    public void carregarEsdeveniment(Esdeveniment e) {
        diari.addLast(e);
        while (diari.size() > MAX_DIARI) {
            diari.removeFirst();
        }
        canvi();
    }

    /** Per carregar les dades del món: els camps d'un país que no van al constructor. */
    public static void carregarDetalls(Pais p, String lema, String bandera, Capital capital,
                                       Map<Rang, String> nomsRangs, Map<Permis, Rang> permisos) {
        p.posarLema(lema);
        p.posarBandera(bandera);
        p.posarCapital(capital);
        nomsRangs.forEach(p::posarNomRang);
        permisos.forEach(p::posarPermis);
    }

    /** Per carregar les dades del món. */
    public static void carregarHumiliacio(Pais p, long fins) {
        p.posarHumiliatFins(fins);
    }
}
