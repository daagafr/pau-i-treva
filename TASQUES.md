# Tasques de Pau i Treva

El plantejament és a [PLANTEJAMENT.md](PLANTEJAMENT.md) i l'ús, a [README.md](README.md).

## Fase 1: nucli i pantalla

- [x] Repositori privat `daagafr/pau-i-treva` i compilació a GitHub Actions (25/11)
- [x] Model: països, rangs amb nom propi, permisos per rang, aliances, pactes, treves, propostes i diari, amb proves JUnit (25/11)
- [x] Pont amb OPAC: país = party, rang d'OPAC → rang del país, nom i color als claims i a la party del líder, aliats en tots dos sentits (25/11)
- [x] Dades al món (`world/data/pauitreva.dat`) i config (`pauitreva-server.toml`) (25/11)
- [x] Ordres `/nacio` i `/diplomacia`, amb botons al xat per acceptar i rebutjar (25/11)
- [x] Manteniment cada 30 s: caducitats, països sense party (després de 3 passades), nom i color a OPAC, aliats (25/11)
- [x] API JSON de només lectura a `127.0.0.1:25582/estat.json` (25/11)
- [ ] Provar-ho al Create amb gent: fundar, canviar color i nom (que el Xaero ho ensenyi), aliança i pacte entre dos països, `/nacio capital`, reiniciar i que tot hi sigui. Al servidor de proves (`./gradlew runProves`) ja ho fan les GameTests, i les dades es llegeixen bé després de reiniciar
- [x] `jdk.httpserver` funciona dins de NeoForge (servidor de proves, 26/11). Escoltava a `::1` per culpa de `preferIPv6Addresses=system`: ara és 127.0.0.1 explícit
- [ ] Mirar al Create si el port 25582 és lliure (ho diu el `latest.log` en arrencar)
- [x] Pantalla del joc (tecla N) **dibuixada pel servidor**: pestanyes País, Membres, Diplomàcia, Guerres i Diari; cada botó fa una ordre del mod. Provada amb captures en un client sense ningú davant (26/11)
- [x] Paquets de xarxa **opcionals** (`PayloadRegistrar.optional()`): qui tingui el modpack vell pot entrar i fa servir les ordres; qui tingui el nou, la pantalla (26/11)
- [ ] Provar d'entrar al Create amb el modpack sense Pau i Treva (que entri i que les ordres vagin)
- [ ] Mirar si l'App de Modrinth actualitza la instància al seu lloc (sense perdre mons, opcions ni mapes del Xaero) quan el modpack és el publicat a Modrinth, i explicar-ho a /create
- [x] Avisos a pantalla amb so: títol i so per als països afectats, so suau per a la resta (26/11)
- [ ] Claims del país: posar al Create les opcions de [`opac/openpartiesandclaims-server.toml`](opac/openpartiesandclaims-server.toml) (amb reinici). Al servidor de proves ja hi són
- [x] Ordre d'admin per passar els claims i els chunks forçats de cada membre al líder, amb mode de prova que només compti i diu si cabran als límits: `/nacio admin migra prova|fes confirmo` (26/11)
- [ ] Al Create: còpia del món, `/nacio admin migra prova`, ajustar els bonus d'OPAC si cal, `fes confirmo` i reiniciar amb la config nova
- [x] Quan canvia el líder d'una party, OPAC no mou els claims: el mod ho mira cada 5 s i passa al nou els chunks que tenia el vell com a líder (amb una foto del territori, perquè no s'emporti els d'una altra party) (26/11)
- [x] Treure el nom i el color del país dels claims del líder vell quan deixa de ser-ho (26/11)
- [x] Territori a l'API: dins de cada país a `estat.json`, els chunks reclamats pels membres, per dimensió (25/11)
- [ ] Ruta `nacions.mineuab.org` al túnel de Cloudflare i el host a la CSP de la web

## Fase 2: guerra

- [x] Model de la guerra amb proves: declaració amb objectiu (conquesta de 1 a N chunks, humiliació, reparacions), preparació, punts de -100 a 100, pau imposada (amb prou punts) o negociada, rendició, treva en acabar, i desat al món (25/11)
- [x] Finestres de guerra amb proves: de dilluns a divendres de 18:00 a 23:00, cap de setmana de 12:00 a 23:00, hora de Madrid, amb el canvi d'hora (25/11)
- [x] Una pau proposada no es pot canviar just abans que l'altre l'accepti (cal dir quina s'ha vist) (25/11)
- [x] Ordres `/guerra` (objectiu, declara, estat, llista, finestres, pau, rendeix i admin) i finestres a la config (26/11)
- [x] Comprovar a OPAC, en declarar una conquesta, que els chunks són del defensor, d'una sola peça i tocant la frontera de l'atacant, i que no són en una altra guerra (26/11)
- [ ] Reparacions: decidir com es paguen (Numismatics o ítems); ara el model només fa conquesta i humiliació als termes
- [x] Guerres i finestres a l'API (`estat.json`, versió 2) (26/11)
- [x] Zona en disputa: no amb `IChunkAccessOverriderAPI` (no compta per al PvP ni per a les explosions) sinó amb una sub-config d'OPAC per guerra sense protecció durant la finestra; PvP amb `playersByPlayers`. Provat amb la protecció real d'OPAC (26/11)
- [x] Captura per presència, baixes, vides, AFK, capitals, puntuació i pau automàtica (26/11)
- [x] Traspàs dels chunks a OPAC en signar la pau, amb els chunks forçats (26/11)
- [x] Pestanya Guerres (amb la pau i la rendició a dins) i barra de guerra (la de Minecraft, sense mod al client) (26/11)
- [ ] Treure OPAC Warfare (abans, acabar les guerres que tingui obertes)

## Fase 3: títols i mapes

- [x] Tag del país al xat i a la llista del TAB (`[ADMIN] UAB [CAT] Dagafr [Física]`, sense el càrrec) com a prefix de LuckPerms amb `server=create`, i damunt del cap només el país (placeholder `%pauitreva-tag%` del TAB). Decidit per David el 26/11; provat amb el nostre LuckPerms (`ProvesTags`)
- [x] Al Create, la pila de prefixos de LuckPerms (rang, universitat, propi) i el color del nom dels rangs a `username-color`: posat en calent el 26/11 (vegeu `desplegament/README.md` i PENDENTS.md del servidor)
- [ ] En desplegar el mod: `tagprefix: "%pauitreva-tag%"` al `groups.yml` del TAB del Create
- [ ] Títols guanyats (Fundador/a, Conqueridor/a, Traïdor/a...) i quin porta cadascú
- [ ] Pàgina `/create/nacions` a la web amb el mapa polític i el diari
- [ ] El mateix mapa dins de la pantalla del joc

## Dubtes i coses a vigilar

- [ ] Nation també sincronitza les parties d'OPAC. Mentre tots dos mods hi siguin, pot ser que es trepitgin: provar-ho amb poca gent o treure Nation quan Pau i Treva tingui pantalla
- [ ] Decidir si s'han de bloquejar les aliances fetes a mà amb `/oparties` entre dos països (ara el mod només afegeix les que falten)
- [ ] `/nacio`, `/diplomacia`, `/guerra`: mirar que cap altre mod del Create no faci servir aquests noms

## Per fer tu (David)

- [x] Zona en disputa només per als bel·ligerants (decidit el 26/11): accés amb `IChunkAccessOverriderAPI`; les explosions, de qualsevol
- [x] PvP de guerra només entre enemics (decidit el 26/11): mixin a OPAC, provat amb cops i fletxes
- [ ] Mirar els números de la guerra (`captura` i `punts` a la config) després de la primera guerra de veritat
- [ ] Pujar els commits a GitHub perquè Actions en faci el jar (`git push` a `pau-i-treva/`)

## Errors trobats i arreglats (26/11)

- [x] L'API escoltava a `::1`: NeoForge arrenca la JVM amb `preferIPv6Addresses=system`
- [x] `tryToReset` d'OPAC torna `playersByPlayers` a `true` (el de fàbrica), no al `false` del Create: el PvP s'hauria quedat obert després de cada guerra
- [x] Dues guerres alhora compartien la sub-config de guerra d'un mateix amo; i un chunk podia ser en dues guerres
- [x] Una sub-config feta a mà amb el nom `pit-guerra-...` deixava els chunks fora de la zona; ara només se salten els que ja són a la de la seva guerra, i es repassa cada 3 s

## Interfície

- [ ] Refer la pantalla (tecla N): la d'ara fa servir els botons i el fons de Minecraft de sèrie i és lletja. Mirar com ho fan els jocs de Paradox (Europa Universalis, Crusader Kings, Hearts of Iron, Victoria): panells amb marc, la bandera i el nom del país a dalt, icones en lloc de text, barres per als punts i el control, el mapa al centre, i tooltips amb el detall. Com que és part del client, s'ha de fer abans de posar Pau i Treva al modpack, perquè el modpack només canviï un cop (David, 26/11)
  - [x] Primera versió (26/11): marc de fusta fosca i or, capçalera amb l'escut i el nom del país i xips (líder, membres, territori, capital, guerres), pestanyes amb icona, seccions amb marc, botons, camps i textos d'ajuda propis, escuts petits a les llistes, barra de puntuació de guerra a l'estil EU4 i barres de control de cada chunk. 16 icones pixel art (`eines/icones.py`). Protocol de la pantalla v2
  - [x] Icones refetes a 32×32 amb volum i més detall, com va demanar David (26/11)
  - [ ] Que David ho miri i digui què li agrada i què no (captures a `build/proves-client/screenshots`, amb `./gradlew runProvaClient`)
  - [ ] Pestanya Mapa: la graella de chunks del voltant amb els colors dels països, per triar l'objectiu fent clic
  - [x] Les banderes de veritat (les 70 del datapack `mineuab-banderes`): a la capçalera i a les llistes amb marc d'or, galeria per triar-la d'un clic, `/nacio bandera` comprova que existeixi, i l'API les serveix a `/banderes/<espai>/<nom>.png`. El servidor envia les imatges al client: afegir-ne una al datapack no demana tocar el modpack (26/11)
  - [ ] Les pintures que pugen els jugadors amb Immersive Paintings (ara només surten les dels datapacks)
  - [ ] Barra de guerra pròpia a dalt de la pantalla de joc (en lloc de la boss bar), amb els dos escuts
  - [ ] Diari més compacte (data i text en una línia) i amb filtres (guerres, diplomàcia, el meu país)
  - [ ] Etiquetes alineades als formularis (ara cada camp comença on acaba la seva etiqueta)
  - [ ] Sons de clic i de canvi de pestanya

## Idees (aprovades per David el 26/11)

- [ ] Aliats a la guerra: quan declaren la guerra a un aliat, demanar-li si hi entra (bàndols de més d'un país)
- [ ] Títol de «Traïdor/a» en trencar un pacte, i prestigi per guanyar guerres (fase 3)
- Descartat (26/11): que declarar la guerra costi diners o ítems
- [ ] Mapa a la pantalla: pestanya amb la graella de chunks del voltant, per triar l'objectiu fent clic en lloc de caminar
- [ ] Avisar el defensor quan entren enemics a la zona en disputa durant la finestra (com Claim Intrusion Alert, però de guerra)
- [ ] Recompte de la batalla al diari en tancar cada finestra (baixes, chunks presos), a la web i a Discord (amb un webhook, com Warfare)
- [ ] Les explosions de Create Big Cannons i les contraptions: provar-les a la zona en disputa al servidor de proves (ara només s'ha provat la TNT)
