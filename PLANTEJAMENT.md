# Pau i Treva: mod propi de països per a MineUAB Create

Plantejament del 25/11/2026. Les decisions del final ja estan preses. El 25/11 a la nit es va fer la part de servidor de la fase 1 (0.1.0), i el 26/11 la 0.2.0: la guerra sencera (fase 2), la pantalla del joc, la migració dels claims i el canvi de líder, amb proves dins d'un servidor de veritat amb OPAC. Què hi ha i què falta és a [TASQUES.md](TASQUES.md); com funciona, al [README](README.md).

## Què tenim ara i per què no va bé

Al grup «països» de `scripts/mods.py` hi ha sis mods que es trepitgen:

| Mod | Què fa | Problema |
|---|---|---|
| Nation 1.0-beta | Països amb rangs, guerra, pau i aliances sobre les parties d'OPAC (tecla N) | Tot drets reservats i sense codi: no el podem arreglar ni ampliar. Té 369 baixades. Obliga a portar els dos mapes de Xaero. |
| OPAC Warfare 2.0 (MIT) | Setges chunk a chunk, finestres de raid, vides de guerra | Té la seva pròpia `/war`, que no sap res de la de Nation. Dues guerres que no es parlen. |
| OPAC Essentials 2.3 (Apache) | `/pchat`, permisos de subclaims, cerca al mapa | Ja l'hem hagut de pedaçar (trens). |
| Banner Flags, Betsy Ross | Banderes físiques i amb imatge | Van bé. Cap mod de països no les fa servir. |
| Claim Intrusion Alert | Avisos d'intrusió | Va bé. |

A més, amb la config d'ara d'OPAC:

- Cada membre té els seus claims, amb el seu color i el seu nom. Al mapa de Xaero, un país no es veu com un país.
- Dins d'un claim no hi ha PvP ni explosions (`playersByPlayers = false`, `blocksByExplosions = false`). Cap guerra no pot tocar res de veritat, i el servidor vol girar al voltant de la guerra (02/11).

## La idea

Un sol mod nostre, **Pau i Treva** (id `pauitreva`), que deixa a OPAC el que ja fa bé (claims, protecció, parties, pintar el territori al Xaero) i hi posa al damunt el que falta: identitat del país, diplomàcia, guerres amb objectius, tractats de pau, títols i un mapa polític web en temps real.

Substitueix Nation i OPAC Warfare. La resta es queda.

OPAC té una API pensada per a això (llicència LGPL, es pot fer servir des d'un mod qualsevol):

- `IChunkAccessOverriderAPI`: per a cada accés a un chunk reclamat, el nostre mod pot dir `ALLOW`, `PROTECT` o `PASS`. És la peça clau de la guerra: durant una batalla, els enemics poden entrar a uns chunks concrets i la resta continua protegida.
- `IClaimActionListenerAPI`: permet vetar o seguir cada claim i unclaim (per exemple, no deixar desreclamar un chunk en disputa).
- Gestor de parties (membres, rangs, aliats), gestor de claims (llegir, reclamar i treure chunks) i configs de jugador (color i nom dels claims, opcions de protecció).

## D'on surt el nom

El 1027, a l'assemblea de Toluges, l'abat Oliba va fer acceptar la Treva de Déu: uns dies concrets de la setmana no es podia lluitar. Les assemblees de Pau i Treva es consideren un precedent de les Corts catalanes. El mod fa el mateix mil anys després: la guerra només es pot fer en unes hores concretes, i s'acaba amb tractats de pau i treves.

## Què fa

### 1. El país és una party d'OPAC

- Cada país va lligat a una party (mateix UUID). Les parties que ja hi ha es poden convertir en país sense perdre membres ni aliats.
- Dades del país: nom, sigla (3 o 4 lletres, per al xat), color, bandera, capital (un chunk), lema i forma de govern.
- Rangs: líder, ministres, oficials, colons i ciutadans. Es corresponen amb els cinc rangs d'OPAC (propietari, ADMIN, MODERATOR, CLAIMER, MEMBER), però cada país els posa nom («President/a», «Secretari/ària General», «Comissari/ària»...).
- Permisos interns per rang: convidar i expulsar, reclamar, fer diplomàcia, declarar la guerra, signar la pau, canviar la bandera.
- **Territori del país** (decidit el 25/11): els claims són del país, no de cada jugador. És l'opció `partyOwnedClaims` d'OPAC:
  - Tècnicament, l'amo dels claims és el líder de la party. Els colons (CLAIMER) i els rangs de sobre poden reclamar i desreclamar en nom del país. Amb «Whole Party Can Claim», ho pot fer tothom.
  - La config dels claims és una de sola, la del líder. Els ministres (ADMIN) la poden canviar, i els oficials (MODERATOR) poden posar i treure gent dels grups de jugadors. La política per als aliats (portes, màquines, accés total) es tria un cop per a tot el país.
  - El mod posa el color i el nom del país a aquesta config. Al Xaero, el país surt d'un sol color amb el seu nom.
  - El límit de claims i de chunks forçats és del país i creix amb els membres (`claimBonusPerPartyMember`, `forceloadBonusPerPartyMember`).
  - Si algú marxa del país, els claims es queden al país. Per endur-se'n, li els ha de cedir el líder o els ha de guanyar en una guerra d'independència.

### 2. Banderes i icones

- La bandera del país és una imatge: una de les 70 del datapack `mineuab-banderes` o una de pujada amb Betsy Ross. És la mateixa que es penja al joc amb Betsy Ross i la que surt al mapa web, a la pantalla del país i a la fitxa de la web.
- Icones al mapa web: la bandera a la capital de cada país.
- Icones dins del Xaero: el país ja surt amb el seu color i nom. Posar-hi la bandera vol dir entrar dins del codi del Xaero, que no té API per a això. Queda per al final, i només si val la pena.

### 3. Diplomàcia

Entre dos països hi pot haver: res (neutral), aliança, pacte de no agressió, guerra o treva.

- **Aliança:** es proposa i l'altre l'accepta. El mod els fa aliats a OPAC en tots dos sentits (a OPAC, ser aliat és d'una sola direcció). Si declaren la guerra a un aliat teu, et demana si hi entres.
- **Pacte de no agressió:** mentre dura (els dies que s'acordin), cap dels dos no pot declarar la guerra a l'altre. Trencar-lo abans d'hora queda al diari i dona el títol de «Traïdor/a» al líder.
- **Treva:** després de cada pau, uns dies sense poder tornar a declarar la guerra.
- Cada canvi surt a tots els jugadors amb un avís i un so, i queda al **diari diplomàtic** (al joc i a la web).

### 4. Guerres amb objectius

Una guerra passa per quatre fases: declaració, preparació (24 h, perquè el defensor s'organitzi), guerra activa i pau.

**Objectius (casus belli).** Per declarar la guerra cal triar què vols:

- **Conquesta:** una zona de chunks de l'enemic que toqui la teva frontera, amb un màxim (per exemple, 20 chunks o el 20 % del seu territori). Si guanyes, te la quedes amb tot el que hi ha construït.
- **Humiliació:** no canvia el territori. El perdedor queda marcat com a humiliat una setmana i el guanyador guanya prestigi.
- **Reparacions:** el perdedor paga. Amb Numismatics si la seva API ho permet (s'ha de mirar); si no, en ítems.
- Més endavant: **independència** (una part d'un país se'n va amb els seus claims) i **vassallatge**.

**Batalles i finestres.** Només es lluita dins de la finestra de guerra, en hora de Madrid: de dilluns a divendres, de 18:00 a 23:00; dissabte i diumenge, de 12:00 a 23:00 (decidit el 25/11). Ha de ser configurable. Fora de l'horari, tot torna a estar protegit. Durant la finestra:

- Hi ha PvP dins dels claims dels bel·ligerants.
- Els chunks de l'objectiu són **zona en disputa**: s'hi poden trencar i posar blocs i hi entren les explosions. La resta del territori dels bel·ligerants només té PvP: no s'hi pot trencar res (decidit el 25/11).
- Els països que no són a la guerra continuen igual de protegits.

**Com es fa a OPAC (26/11).** `IChunkAccessOverriderAPI` no serveix: OPAC no el consulta ni per al PvP ni per a les explosions. En lloc d'això, en declarar la guerra els chunks de l'objectiu passen a una sub-config del seu amo (una per guerra, `pit-guerra-N`) que durant la finestra té `protectClaimedChunks = false`; i el PvP s'obre posant `playersByPlayers = true` a la config dels membres dels dos països, cosa que només es pot fer si l'opció és a `opConfigurablePlayerConfigOptions` (vegeu `opac/openpartiesandclaims-server.toml`). Conseqüència: durant la finestra, la zona en disputa és oberta a tothom (també als neutrals), i el PvP del territori dels dos països també. Si es vol limitar als bel·ligerants, vegeu les decisions pendents a TASQUES.md.

**Puntuació de guerra (0–100).** Surt de:

- **Captura per presència** dels chunks en disputa: si hi tens gent i l'enemic no, la barra avança; si hi ha defensors, recula. Com a Warfare, que és MIT: se n'aprofiten les idees (i el codi, si cal, citant-lo), com les vides de guerra, la protecció contra els que van en massa i que els AFK no compten.
- **Baixes** d'enemics dins de la zona de guerra.
- **Capital:** prendre-la val molt.
- **Temps:** cada finestra que l'atacant aguanta objectius en suma.

### 5. Tractats de pau

- Qualsevol bàndol proposa uns termes, i cada terme costa punts de guerra: chunks (tants punts per chunk), reparacions, humiliació, treva.
- L'altre bàndol accepta o rebutja. Amb 100 punts, el guanyador els pot imposar. Si el defensor guanya, també pot imposar termes a l'atacant.
- Si una guerra s'allarga massa sense que ningú avanci, s'acaba sola amb pau blanca (sense canvis).
- En signar, el mod aplica els termes sol: amb l'API d'OPAC, passa els chunks del líder del perdedor al líder del guanyador, sense mirar els límits de claims. Les construccions que hi ha queden on són. Com que els claims són del país, no cal decidir a quin jugador van.
- Tots els tractats queden publicats a la web.

### 6. Títols

- **Del país:** el nom del rang i la sigla, amb el color del país. Al xat: `[ADMIN] [CAT] Dagafr [Física]: hola`. El títol sencer surt al TAB i en passar el ratolí per sobre a la pantalla del país.
- **Guanyats:** «Fundador/a», «Conqueridor/a» (guanyar una conquesta), «Pacificador/a» (signar tres tractats), «Mariscal» (més baixes en una guerra), «Traïdor/a» (trencar un pacte)... Cada jugador tria quin porta.
- **Com surten:** el mod els escriu a LuckPerms com a meta del jugador, amb el context `server=create`. Així LuckPerms Chat i el TAB els ensenyen sense tocar-los, i al MineUAB no hi surten (la base de dades és compartida). Cal canviar el `meta-formatting` del LuckPerms del Create perquè ensenyi el rang i la sigla alhora.

### 7. Mapa web en temps real i diplomàcia

- El mod obre una API JSON de només lectura a `127.0.0.1:25582`, com Credits (25580) i Perfil (25581). Surt pel túnel de Cloudflare com a `nacions.mineuab.org`.
  - `paisos.json` (nom, sigla, color, bandera, capital, membres i títols), `territori.json` (chunks de cada país), `guerres.json`, `tractats.json`, `diari.json` i `banderes/<id>.png`.
- La web té una pàgina nova, `/create/nacions`:
  - **Mapa polític:** els chunks pintats del color de cada país, la bandera a cada capital, les zones en disputa ratllades i les aliances.
  - **Fitxa de cada país:** bandera, membres amb els títols, relacions, guerres i tractats.
  - **Diari diplomàtic:** declaracions, aliances, batalles i paus.
  - Es posa al dia sola cada 30 segons.
- Al principi, el mapa no té relleu: només la graella de chunks i els colors. Si després es vol el terreny de fons, squaremap (2D, MIT, té versió per a NeoForge 1.21.1) és molt més lleuger que BlueMap.
- No ensenya on són els jugadors. En un servidor de guerra seria espionatge gratis.
- Cal afegir `nacions.mineuab.org` a la CSP de `next.config.mjs`.

### 8. Dins del joc

- Tecla **N** (la que deixa lliure Nation): pantalla del país amb pestanyes: País, Membres, Diplomàcia, Guerres, Tractats i Mapa (el mateix mapa polític que la web, dibuixat dins del joc). Tot el que es pot fer amb ordres s'ha de poder fer des d'aquí, perquè sigui la manera normal de jugar-hi.
- Avisos a pantalla, amb so, quan algú declara la guerra, fa una aliança o signa la pau.
- Durant la finestra de guerra, una barra a dalt amb la puntuació, les vides i el temps que queda.
- Ordres per a tot, en català i amb àlies en anglès: `/nacio`, `/diplomacia`, `/guerra`, `/pau`, `/titol`.

## Què es queda i què marxa

| Mod | Amb Pau i Treva |
|---|---|
| Open Parties and Claims | Es queda: és la base. |
| Xaero's Minimap i World Map | Es queden (OPAC hi pinta els claims). Ja no caldria que fossin obligatoris. |
| Nation | **Marxa.** |
| OPAC Warfare | **Marxa.** Les seves idees es refan dins de Pau i Treva. |
| OPAC Essentials | Es queda de moment (subclaims, `/pchat`). |
| Claim Intrusion Alert, Banner Flags, Betsy Ross | Es queden. |
| LuckPerms, LuckPerms Chat, TAB | Ensenyen els títols. |
| Numismatics | Reparacions de guerra, si es pot. |

## Com es construeix

- NeoForge 21.1 (Minecraft 1.21.1), Java 21 i ModDevGradle. Un sol jar per al client i el servidor.
- Depèn d'OPAC. LuckPerms i Numismatics són opcionals: si no hi són, no hi ha títols al xat o no hi ha reparacions en diners.
- Les dades van al món (`world/data/pauitreva.dat`), i així entren a la còpia de restic de cada nit.
- Rendiment: OPAC pregunta al mod cada vegada que algú toca un bloc d'un claim. La resposta ha de ser una consulta directa a la memòria, sense recórrer llistes.
- **Codi i compilació** (decidit el 25/11): repositori privat `daagafr/pau-i-treva` a GitHub, amb un workflow com el `compila.yml` dels plugins (Temurin 21, `./gradlew build`, el jar com a artefacte amb el commit al nom). Es baixa amb `gh run download`. La còpia local és aquesta carpeta, `MineUAB-Create/pau-i-treva/`, i aquest plantejament hi va a dins.
- Al portàtil només hi ha el JRE 21, sense `javac`. Amb un JDK (`sudo apt install openjdk-21-jdk-headless`) es pot compilar aquí i no cal esperar Actions a cada canvi, però no és obligatori.
- Proves: un servidor de proves petit amb NeoForge, OPAC, LuckPerms i Pau i Treva (sense els 180 mods), per no provar res al Create amb gent dins. GameTests per a la lògica (puntuació, tractats, traspàs de chunks).
- Al modpack entra com els altres jars propis (`-mineuab`), amb la cadena de la skill `versio-create`. Cal una versió nova del modpack per treure Nation i posar Pau i Treva.

## Migració

- Parties: cada party d'OPAC passa a ser un país, amb el mateix UUID, els mateixos membres i els mateixos aliats.
- Claims: activar `partyOwnedClaims` no mou cap claim (ho diu la config d'OPAC). Els claims que ara té cada membre s'han de passar al líder del seu país, amb els chunks forçats inclosos (hi ha granges que en depenen). Es fa un sol cop, amb el servidor aturat o des del mod en arrencar, i amb una còpia del món abans.
- Config d'OPAC (només s'aplica en reiniciar): `partyOwnedClaims = true` i `primaryPartySystem = "default"` (per defecte diu `ftb_teams`, que aquí no hi és). Els bonus per membre s'han de posar perquè ningú no perdi capacitat. Per exemple, `forceloadBonusPerPartyMember = 30` (ara cada jugador en té 30; per defecte serien 2).
- Qui no és a cap país continua amb els seus claims de jugador.
- Nation: el seu format de dades és tancat. Si no es pot llegir, cada líder torna a posar els rangs (a la beta hi ha pocs països).
- Warfare: abans de treure'l, cal acabar o anul·lar les guerres que hi hagi.
- Encara no s'ha mirat què hi ha ara al servidor (des d'aquest portàtil no s'hi arriba per Tailscale).

## Fases

La pantalla del joc va des de la primera versió (decidit el 25/11). Cada fase hi afegeix les seves pestanyes, i les ordres hi són sempre com a drecera.

1. **Nucli.** País sobre la party, dades, claims del país (amb la migració), color i nom al Xaero, aliances i pactes, API JSON. Pantalla (tecla N) amb les pestanyes País, Membres i Diplomàcia, i els avisos. Ja pot substituir Nation.
2. **Guerra.** Declaració amb objectiu, preparació, finestres, accés als chunks en disputa, captura per presència, puntuació, tractats i traspàs de chunks. Pestanyes Guerres i Tractats, i barra de guerra. Substitueix Warfare.
3. **Títols i mapes.** Títols a LuckPerms, xat i TAB. Pàgina `/create/nacions` amb el mapa polític, i el mateix mapa dins de la pantalla.
4. **Extres.** Reparacions amb Numismatics, independència i vassalls, terreny de fons amb squaremap, banderes dins del Xaero.

## Coses que cal provar abans

- ~~Si el `IChunkAccessOverriderAPI` d'OPAC també decideix sobre les explosions~~: no (26/11), i per això la zona en disputa es fa amb una sub-config sense protecció. Les explosions de TNT hi entren (provat); falta provar els obusos de Create Big Cannons i els míssils.
- Com tracta OPAC les contraptions de Create (trepants, serres) en un claim enemic. Warfare té un mod germà, Better Protection, que ho cobreix; s'ha de mirar si ens cal.
- ~~Claims del país: què passa amb els claims quan el país canvia de líder~~: OPAC els deixa al vell (26/11). El mod els passa al nou (provat).
- Que els chunks forçats continuïn funcionant després de passar-los al líder (el claim continua marcat com a forçat: provat; falta veure que la granja segueixi anant al Create), i que el Xaero ensenyi el nom del país i no el del líder.

## Decisions per al David

1. **Què es pot destruir en una guerra?** **Decidit el 25/11:** només els chunks de l'objectiu, i només durant la finestra. A la resta del territori dels bel·ligerants hi ha PvP, però no s'hi pot trencar res. (Es van descartar «res, només PvP» i «tot el territori enemic».)
2. **Horari de les finestres de guerra.** **Decidit el 25/11:** de dilluns a divendres, de 18:00 a 23:00; dissabte i diumenge, de 12:00 a 23:00 (hora de Madrid).
3. **Claims de cada jugador o claims del país?** **Decidit el 25/11:** del país (`partyOwnedClaims` d'OPAC). Cal migrar els claims de cada membre al líder (vegeu «Migració»).
4. **Primera versió només amb ordres, o directament amb pantalla pròpia?** **Decidit el 25/11:** amb pantalla des de la primera versió (vegeu «Fases»).
5. **On viu el codi i com es compila?** **Decidit el 25/11:** repositori privat `daagafr/pau-i-treva` amb GitHub Actions, com els plugins (vegeu «Com es construeix»). El repositori es crea quan es comenci a programar.
6. **Nom del mod.** **Decidit el 25/11:** **Pau i Treva** (id `pauitreva`). Vegeu «D'on surt el nom». La web i l'API continuen dient-se «nacions» (`/create/nacions`, `nacions.mineuab.org`), perquè als jugadors els diu de què va la pàgina.
