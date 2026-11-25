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
- [ ] Provar la 0.1.0 dins del joc: fundar, canviar color i nom (que el Xaero ho ensenyi), aliança i pacte entre dos països, `/nacio capital`, reiniciar i que tot hi sigui
- [ ] Mirar al servidor si el port 25582 és lliure i si `jdk.httpserver` funciona dins de NeoForge (ho diu el `latest.log` en arrencar)
- [ ] Pantalla del joc (tecla N): pestanyes País, Membres i Diplomàcia, amb paquets de xarxa. A partir d'aquí el jar ha d'anar també al modpack
- [ ] Avisos a pantalla amb so (ara només surten al xat)
- [ ] Claims del país: posar `partyOwnedClaims = true`, `primaryPartySystem = "default"` i els bonus per membre a la config d'OPAC (amb reinici)
- [ ] Ordre d'admin per passar els claims i els chunks forçats de cada membre al líder, amb mode de prova que només compti, i una còpia del món abans
- [ ] Quan canvia el líder d'una party: provar què fa OPAC amb els claims i, si cal, passar-los al nou líder
- [ ] Treure el nom i el color del país dels claims del líder vell quan deixa de ser-ho
- [x] Territori a l'API: dins de cada país a `estat.json`, els chunks reclamats pels membres, per dimensió (25/11)
- [ ] Ruta `nacions.mineuab.org` al túnel de Cloudflare i el host a la CSP de la web

## Fase 2: guerra

- [ ] Declaració amb objectiu (conquesta, humiliació, reparacions), preparació de 24 h i treva en acabar
- [ ] Finestres de guerra configurables: de dilluns a divendres de 18:00 a 23:00, cap de setmana de 12:00 a 23:00 (Madrid)
- [ ] Accés als chunks en disputa amb `IChunkAccessOverriderAPI` (destruir només l'objectiu, PvP a la resta)
- [ ] Captura per presència, baixes, capital i puntuació de guerra
- [ ] Tractats de pau i traspàs de chunks entre líders
- [ ] Pestanyes Guerres i Tractats, i barra de guerra
- [ ] Treure OPAC Warfare (abans, acabar les guerres que tingui obertes)

## Fase 3: títols i mapes

- [ ] Títols a LuckPerms (`server=create`) i `meta-formatting` perquè el xat ensenyi rang i sigla
- [ ] Pàgina `/create/nacions` a la web amb el mapa polític i el diari
- [ ] El mateix mapa dins de la pantalla del joc

## Dubtes i coses a vigilar

- [ ] Nation també sincronitza les parties d'OPAC. Mentre tots dos mods hi siguin, pot ser que es trepitgin: provar-ho amb poca gent o treure Nation quan Pau i Treva tingui pantalla
- [ ] Decidir si s'han de bloquejar les aliances fetes a mà amb `/oparties` entre dos països (ara el mod només afegeix les que falten)
- [ ] `/nacio`, `/diplomacia`: mirar que cap altre mod del Create no faci servir aquests noms
