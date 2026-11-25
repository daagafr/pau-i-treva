# Pau i Treva

Mod de països per a MineUAB Create (Minecraft 1.21.1, NeoForge 21.1), fet sobre Open Parties and Claims (OPAC).
Ha de substituir Nation i OPAC Warfare. El perquè i el que farà, fase per fase, és a [PLANTEJAMENT.md](PLANTEJAMENT.md);
el que hi ha fet i el que falta, a [TASQUES.md](TASQUES.md).

El nom ve de la Pau i Treva de Déu (Toluges, 1027): dies en què no es podia lluitar.

## Què fa ara (0.1.0)

Només servidor: ordres i dades. El jar no registra cap paquet de xarxa, així que de moment es pot posar al servidor
sense tocar el modpack.

- Un país és una party d'OPAC (mateix id, membres i rangs) amb nom, sigla, color, lema, bandera, capital i noms propis
  per als rangs.
- El nom i el color del país es posen als claims i a la party del líder, perquè al Xaero el país surti d'un sol color.
- Aliances (també aliats a OPAC, en tots dos sentits) i pactes de no agressió amb data de final. Les treves hi són al
  model, però les farà la fase 2 (en acabar una guerra).
- Diari diplomàtic públic, i avís a tothom quan es funda un país, es fa o es trenca una aliança o un pacte.
- Cada 30 s: caduquen propostes, pactes i treves; es dissolen els països que ja no tenen party; es torna a posar el
  nom i el color a OPAC i es refan els aliats d'OPAC que faltin.

## Ordres

| Ordre | Qui | Què fa |
|---|---|---|
| `/nacio` o `/nacio info [sigla]` | tothom | Fitxa del país |
| `/nacio membres [sigla]` | tothom | Membres amb el nom del rang |
| `/nacio llista` | tothom | Tots els països |
| `/nacio diari` | tothom | Les 10 últimes entrades del diari diplomàtic |
| `/nacio funda <sigla> <nom>` | qui no té país | Funda un país (crea la party si cal; si ja n'ets d'una, n'has de ser el propietari) |
| `/nacio nom <nom>`, `sigla`, `color #RRGGBB`, `lema [text]`, `bandera [id]`, `capital` | permís `editar` (ministre) | Canvia les dades del país. La capital és el chunk on ets, i ha de ser del país |
| `/nacio rang <rang> [nom]` | permís `editar` | Posa nom a un rang (sense nom, torna al de per defecte) |
| `/nacio permis <permis> <rang>` | líder | Quin rang cal per a `editar`, `diplomacia` o `trencar` |
| `/nacio dissol confirmo` | líder | Dissol el país (la party i els claims es queden) |
| `/diplomacia` | membres | Relacions i propostes pendents, amb botons |
| `/diplomacia alianca <sigla>` | permís `diplomacia` (ministre) | Proposa una aliança (o accepta la que t'han proposat) |
| `/diplomacia pacte <sigla> <dies>` | permís `diplomacia` | Proposa un pacte de no agressió |
| `/diplomacia accepta <sigla>`, `rebutja <sigla>` | permís `diplomacia` | Respon una proposta |
| `/diplomacia trenca <sigla>` | permís `trencar` (líder) | Trenca una aliança o un pacte (trencar un pacte queda al diari) |

Rangs: `ciutada` (MEMBER d'OPAC), `colon` (CLAIMER), `oficial` (MODERATOR), `ministre` (ADMIN) i `lider` (propietari).

## Config

`world/serverconfig/pauitreva-server.toml`: caducitat de les propostes (48 h), màxim de dies d'un pacte (30) i cada
quants segons es fa el manteniment (30).

## Dades

`world/data/pauitreva.dat` (NBT): països, relacions, propostes i diari (fins a 500 entrades).

## Compilar

GitHub Actions compila cada push a `master` (`.github/workflows/compila.yml`) i deixa el jar com a artefacte
`pauitreva-<commit>`:

```bash
gh run download --repo daagafr/pau-i-treva -n pauitreva-<commit>
```

En local cal un JDK 21 (`./gradlew build`). Les proves del model (`src/test`) no necessiten Minecraft.

## Codi

- `model/`: països, relacions, propostes, diari i totes les regles (`Registre`). Sense Minecraft, amb proves JUnit.
- `opac/PontOpac`: tot el que es llegeix o es canvia d'OPAC.
- `mon/Dades`: desar i carregar el registre al món.
- `ordres/`: `/nacio`, `/diplomacia` i els textos.
- `Manteniment`: la feina periòdica.
