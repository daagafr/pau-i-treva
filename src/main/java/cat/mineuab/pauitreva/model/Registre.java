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

    /**
     * Declara la guerra. Qui crida ha de comprovar abans, a OPAC, que els chunks d'una conquesta són del
     * defensor (i, si cal, que toquen la frontera de l'atacant).
     *
     * @param chunks      per a una conquesta, de 1 a {@code maxChunks} chunks; per als altres objectius, cap
     * @param preparacio  quant temps passa fins que es pot lluitar
     */
    public Resultat declararGuerra(UUID atacant, UUID defensor, TipusObjectiu objectiu, java.util.Set<Chunk> chunks,
                                   long ara, long preparacio, int maxChunks) {
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
        if (conquesta ? chunks.isEmpty() || chunks.size() > maxChunks : !chunks.isEmpty()) {
            return Resultat.OBJECTIU_INVALID;
        }
        Guerra g = new Guerra(UUID.randomUUID(), atacant, defensor, objectiu, chunks, ara, ara + preparacio, 0);
        guerres.put(g.id(), g);
        Parella parella = Parella.de(atacant, defensor);
        relacions.put(parella, new EstatRelacio(TipusRelacio.GUERRA, ara, 0));
        propostes.removeIf(x -> parella.equals(Parella.de(x.de(), x.a())));
        String perque = conquesta
                ? "per conquerir " + chunks.size() + (chunks.size() == 1 ? " chunk" : " chunks")
                : "per " + objectiu.nom();
        long hores = Math.round(preparacio / (double) HORA);
        apuntar(ara, Esdeveniment.GUERRA, etiqueta(a) + " declara la guerra a " + etiqueta(d) + " " + perque
                + ". Es podrà lluitar d'aquí a " + hores + (hores == 1 ? " hora." : " hores."));
        canvi();
        return Resultat.DECLARADA;
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
        int abans = g.punts();
        g.sumar(perAquest, punts);
        if (g.punts() != abans) {
            canvi();
        }
        return Resultat.FET;
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

    /** L'altre bàndol accepta la pau proposada. */
    public Resultat acceptarPau(UUID qui, UUID guerra, long ara, long treva) {
        Guerra g = guerres.get(guerra);
        PropostaPau pr = propostesPau.get(guerra);
        if (g == null) {
            return Resultat.NO_HI_HA_GUERRA;
        }
        if (pr == null || !g.hiEs(qui) || pr.de().equals(qui)) {
            return Resultat.NO_HI_HA_PROPOSTA;
        }
        return ferPau(guerra, pr.guanyador(), pr.termes(), false, ara, treva);
    }

    /** Qui es rendeix dona a l'enemic tot l'objectiu de la guerra (si l'enemic és l'atacant) o una pau blanca. */
    public Resultat rendir(UUID qui, UUID guerra, long ara, long treva) {
        Guerra g = guerres.get(guerra);
        if (g == null) {
            return Resultat.NO_HI_HA_GUERRA;
        }
        if (!g.hiEs(qui)) {
            return Resultat.TIPUS_INVALID;
        }
        UUID guanyador = g.enemic(qui);
        Termes termes = guanyador.equals(g.atacant())
                ? new Termes(g.chunks(), g.objectiu() == TipusObjectiu.HUMILIACIO)
                : new Termes(java.util.Set.of(), true);
        return ferPau(guerra, guanyador, termes, false, ara, treva);
    }

    /**
     * Signa la pau i acaba la guerra: treu la guerra, posa una treva de {@code treva} mil·lisegons i ho apunta
     * al diari. Els chunks dels termes els ha de passar a OPAC qui crida (del líder del defensor al del
     * guanyador).
     *
     * @param imposada si és {@code true}, el guanyador ha de tenir prou punts per pagar els termes
     */
    public Resultat ferPau(UUID guerra, UUID guanyador, Termes termes, boolean imposada, long ara, long treva) {
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
        String guanya = paisos.containsKey(guanyador) ? etiqueta(paisos.get(guanyador)) : "?";
        UUID perdedor = g.enemic(guanyador);
        String perd = paisos.containsKey(perdedor) ? etiqueta(paisos.get(perdedor)) : "?";
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
}
