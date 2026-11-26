# Pau i Treva

Mod de països per a MineUAB Create (Minecraft 1.21.1, NeoForge 21.1), fet sobre Open Parties and Claims (OPAC).
Ha de substituir Nation i OPAC Warfare. El perquè i el que farà, fase per fase, és a [PLANTEJAMENT.md](PLANTEJAMENT.md);
el que hi ha fet i el que falta, a [TASQUES.md](TASQUES.md).

El nom ve de la Pau i Treva de Déu (Toluges, 1027): dies en què no es podia lluitar.

## Què fa ara (0.2.0)

- **Països** sobre les parties d'OPAC: nom, sigla, color, lema, bandera, capital, noms propis per als rangs i permisos
  per rang. El nom i el color del país es posen als claims i a la party del líder (al Xaero, el país surt d'un sol color).
- **Diplomàcia**: aliances (també aliats a OPAC, en tots dos sentits), pactes de no agressió amb data de final, treves
  després de cada pau i un diari diplomàtic públic.
- **Guerra** (vegeu més avall): declaració amb objectiu, preparació, finestres, zona en disputa i PvP només per als
  bel·ligerants, captura per presència,
  punts, baixes, vides, pau proposada, imposada o automàtica, rendició, traspàs de chunks i humiliació.
- **Pantalla del joc** (tecla N), dibuixada pel servidor: País, Membres, Diplomàcia, Guerres i Diari. Tot el que fan les
  ordres es pot fer des d'aquí.
- **Avisos a pantalla amb so** (títols, barra d'acció i barra de guerra), amb paquets de Minecraft normals.
- **Territori del país**: ordre d'admin per passar els claims dels membres al líder (claims de party d'OPAC), i quan canvia
  el líder, el territori passa al nou.
- **API JSON** de només lectura a `http://127.0.0.1:25582/estat.json`: països (membres, títols, territori, humiliació),
  relacions, guerres (objectiu, control de cada chunk, capitals, punts, baixes), finestres i les 100 últimes entrades del diari.

El jar va al servidor i al modpack. Els dos paquets de la pantalla són **opcionals**: qui no tingui el mod pot entrar al
servidor i fer servir les ordres; només no tindrà la tecla N.

## Ordres

### `/nacio`

| Ordre | Qui | Què fa |
|---|---|---|
| `/nacio` o `/nacio info [sigla]` | tothom | Fitxa del país |
| `/nacio membres [sigla]` | tothom | Membres amb el nom del rang |
| `/nacio llista` | tothom | Tots els països |
| `/nacio diari` | tothom | Les 10 últimes entrades del diari diplomàtic |
| `/nacio funda <sigla> <nom>` | qui no té país | Funda un país (crea la party si cal; si ja n'ets d'una, n'has de ser el propietari) |
| `/nacio nom <nom>`, `sigla`, `color #RRGGBB`, `lema [text]`, `bandera [id]`, `capital` | permís `editar` | Canvia les dades del país. La capital és el chunk on ets, i ha de ser del país |
| `/nacio rang <rang> [nom]` | permís `editar` | Posa nom a un rang (sense nom, torna al de per defecte) |
| `/nacio permis <permis> <rang>` | líder | Quin rang cal per a `editar`, `diplomacia`, `trencar`, `guerra` o `pau` |
| `/nacio dissol confirmo` | líder | Dissol el país (les guerres s'acaben en blanc; la party i els claims es queden) |
| `/nacio admin migra prova` | OP 3 | Diu quants chunks i chunks forçats de cada membre passarien al líder, i si cabran als límits d'OPAC |
| `/nacio admin migra fes confirmo` | OP 3 | Ho fa. No es pot fer amb guerres obertes |

### `/diplomacia`

| Ordre | Qui | Què fa |
|---|---|---|
| `/diplomacia` | membres | Relacions i propostes pendents, amb botons |
| `/diplomacia alianca <sigla>` | permís `diplomacia` | Proposa una aliança (o accepta la que t'han proposat) |
| `/diplomacia pacte <sigla> <dies>` | permís `diplomacia` | Proposa un pacte de no agressió |
| `/diplomacia accepta <sigla>`, `rebutja <sigla>` | permís `diplomacia` | Respon una proposta |
| `/diplomacia trenca <sigla>` | permís `trencar` | Trenca una aliança o un pacte (trencar un pacte queda al diari) |

### `/guerra`

| Ordre | Qui | Què fa |
|---|---|---|
| `/guerra` | tothom | Les guerres del teu país (o totes) |
| `/guerra llista`, `/guerra finestres` | tothom | Totes les guerres; quan es pot lluitar |
| `/guerra estat [sigla]` | tothom | Com va una guerra: fase, punts, chunks, capitals, vides i proposta de pau |
| `/guerra objectiu afegeix`, `radi <r>`, `treu`, `buida`, `mostra` | permís `guerra` | Prepara l'objectiu d'una conquesta posant-te als chunks de l'enemic (es veuen amb flames) |
| `/guerra declara <sigla> conquesta\|humiliacio` | permís `guerra` | Declara la guerra (la conquesta, amb l'objectiu preparat) |
| `/guerra pau <sigla> proposa <termes>` | permís `pau` | Proposa la pau. Termes: `blanca`, o `guanyem`/`guanyeu` amb `objectiu` o `controlats` i/o `humiliacio` |
| `/guerra pau <sigla> imposa <termes>` | permís `pau` | La imposa, si teniu prou punts |
| `/guerra pau <sigla> accepta <id>`, `rebutja` | permís `pau` | Respon la proposta (el botó del xat porta l'id de la que s'ha vist) |
| `/guerra rendeix <sigla> confirmo` | permís `pau` | Es rendeix |
| `/guerra admin diagnosi` | OP 2 | Finestra, zona, PvP i si OPAC deixa obrir el PvP |
| `/guerra admin finestra obre\|tanca\|auto` | OP 2 | Obre o tanca la finestra a mà (per provar), o la torna a l'horari |
| `/guerra admin comenca <a> <b>`, `punts <a> <b> <n>`, `acaba <a> <b>` | OP 2 | Acaba la preparació, suma punts a `a`, o acaba la guerra en blanc |

Rangs: `ciutada` (MEMBER d'OPAC), `colon` (CLAIMER), `oficial` (MODERATOR), `ministre` (ADMIN) i `lider` (propietari).
Per defecte, `editar` i `diplomacia` són de ministre; `trencar`, `guerra` i `pau`, del líder.

## La guerra

1. **Declaració.** Per a una conquesta, l'objectiu són de 1 a 20 chunks del defensor (com a molt el 20 % del seu territori),
   d'una sola peça i tocant el territori de l'atacant. Un chunk no pot ser en dues guerres. Cada país pot tenir com a molt dues
   guerres alhora. No es pot declarar a un aliat, amb un pacte o en treva. Les reparacions encara no es poden demanar.
2. **Preparació** (24 h). Els chunks de l'objectiu passen a una sub-config d'OPAC de guerra del seu amo (`pit-guerra-N`, una
   per guerra), amb el nom «(en disputa)» i el color barrejat amb vermell. Ningú no els pot reclamar ni desreclamar fins a la pau.
3. **Finestres** (dl–dv 18:00–23:00, ds–dg 12:00–23:00, hora de Madrid). Mentre és oberta:
   - a la zona en disputa, els membres dels dos països en guerra hi poden trencar, posar i obrir coses; per a la resta
     continua protegida. Hi entren les explosions (aquestes, de qualsevol: no es poden filtrar per qui les fa);
   - PvP només entre enemics, i només al territori dels dos països (a la natura ja n'hi ha sempre): ni companys entre
     ells, ni neutrals, ni al territori d'un tercer país. Val per als cops i les fletxes;
   - **captura per presència**: a cada chunk de l'objectiu i a les dues capitals, qui hi és fa avançar o recular el control
     (un atacant sol pren un chunk en uns 4 minuts; com més atacants, més de pressa, però amb un màxim; els defensors el fan
     recular). Els AFK (5 minuts quiets) i qui ha gastat les 5 vides de la finestra no capturen. Als punts disputats, tothom brilla;
   - **punts** (de -100 a 100): l'atacant en guanya cada minut pel control de l'objectiu (25 per hora amb tot l'objectiu),
     cada bàndol per la capital enemiga que té (40 per hora), el defensor una mica si l'atacant no controla res (5 per hora),
     i 2 per baixa (com a molt 3 vegades per víctima i finestra);
   - barra de guerra a dalt amb els punts i l'hora de tancament.
4. **Pau.** Cada terme costa punts: tots els chunks de l'objectiu en valen 100 (proporcional) i la humiliació, 50. Es pot
   proposar qualsevol cosa (l'altre l'ha d'acceptar) o imposar el que es pugui pagar. Qui es rendeix perd: el defensor dona
   l'objectiu; l'atacant queda humiliat. Si al cap de 7 dies ningú no ha signat, pau automàtica: guanya qui té més punts i
   s'emporta el que pot pagar (els chunks que controla del tot, o la humiliació). Després, 7 dies de treva.
5. **Traspàs.** Els chunks cedits passen al líder del guanyador (amb els chunks forçats) i els altres tornen a la sub-config
   que tenien. El perdedor humiliat ho queda 7 dies.

## Config

`world/serverconfig/pauitreva-server.toml`: caducitat de les propostes (48 h), màxim de dies d'un pacte (30), cada quants
segons es fa el manteniment (30), l'API, i les seccions `guerra` (finestres, zona horària, preparació, durada, treva,
humiliació, mida de l'objectiu, guerres per país, si s'obre la zona i el PvP, AFK, brillar), `captura` i `punts`.

### Config d'OPAC

[`opac/openpartiesandclaims-server.toml`](opac/openpartiesandclaims-server.toml) té les opcions d'OPAC que cal canviar al
Create (`primaryPartySystem = "default"`, `partyOwnedClaims = true`, el bonus de chunks forçats per membre i
`claims.protection.exceptions.blocksByExplosions` a `opConfigurablePlayerConfigOptions`, que és el que deixa obrir la zona en
disputa a les explosions). Sense aquesta darrera, la guerra funciona igual però les explosions no hi entren (ho diu
`/guerra admin diagnosi`). Abans d'activar `partyOwnedClaims`: còpia del món i `/nacio admin migra prova`.

El PvP de guerra no necessita cap opció d'OPAC: el mod hi afegeix una excepció amb un mixin a la funció d'OPAC que decideix
si una entitat en pot atacar una altra (`ProteccioOpacMixin`, fet per a OPAC 0.33.0). Si una versió nova d'OPAC la canvia, el
joc no peta: només no hi ha PvP de guerra, i `/guerra admin diagnosi` ho diu.

## Dades

`world/data/pauitreva.dat` (NBT): països, relacions, propostes, diari (fins a 500 entrades), guerres (amb el control de cada
chunk) i les configs on el mod ha obert el PvP (per tancar-lo si el servidor cau amb la finestra oberta).

## Proves

- `./gradlew test`: el model (regles de països, diplomàcia, guerra, batalla, territori, finestres i API), sense Minecraft.
- `./gradlew runProves`: un servidor dedicat de proves amb OPAC i la config d'OPAC del Create, que fa les GameTests i s'atura;
  el resultat és a `build/proves-servidor/resultat-proves.txt`. Hi ha una guerra sencera (PvP, trencar i explotar dins i fora
  de la zona, captura, baixa, API, pau i traspàs; PvP amb cops i fletxes entre enemics i no amb companys ni neutrals; zona
  per als bel·ligerants i no per als neutrals), una rendició, la migració i el canvi de líder, i les pestanyes de la pantalla.
- `./gradlew runProvaClient`: un client que entra al món `build/proves-client/saves/prova`, obre la pantalla i en fa captures
  a `build/proves-client/screenshots`. Necessita una pantalla (`DISPLAY`); sense ningú davant, una virtual (Xvfb).

## Compilar

GitHub Actions compila cada push a `master` (`.github/workflows/compila.yml`) i deixa el jar com a artefacte
`pauitreva-<commit>`:

```bash
gh run download --repo daagafr/pau-i-treva -n pauitreva-<commit>
```

En local cal un JDK 21 (`./gradlew build`; al portàtil n'hi ha un a `~/.local/share/jdks`, amb `JAVA_HOME`).

## Codi

- `model/`: països, relacions, propostes, diari, guerres, batalla (`Registre`, `Guerra`, `ParametresGuerra`), geometria de
  l'objectiu (`Territori`), finestres i API (`Exportacio`). Sense Minecraft, amb proves JUnit.
- `opac/`: tot el que es llegeix o es canvia d'OPAC (`PontOpac`), la zona en disputa (`ZonaOpac`, i qui hi té accés:
  `AccesGuerra`) i el veto als claims en disputa (`VetClaims`). `mixin/ProteccioOpacMixin` i `guerra/PvpGuerra`: el PvP de guerra.
- `guerra/`: les operacions de guerra senceres (`Guerres`) i la batalla en directe (`Batalles`).
- `pantalla/`: la pantalla que fa el servidor (`Vistes`) i els paquets (`Xarxa`); `client/`: la tecla i el dibuix.
- `mon/Dades`: desar i carregar al món. `ordres/`: `/nacio`, `/diplomacia`, `/guerra`, textos i avisos.
- `Manteniment`: la feina periòdica (caducitats, identitat a OPAC, aliats, líders, API).
- `src/gametest/`: les proves dins del servidor i del client (no entren al jar).
