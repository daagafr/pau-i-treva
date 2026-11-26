# Desplegar els tags de Pau i Treva al Create

Com han de quedar (decidit per David el 26/11):

- **Xat i llista del TAB**: `[ADMIN] UAB [CAT] Dagafr [Física]: hola` (rang, universitat, país, nom i carrera). El tag del
  país és la sigla en el color del país, sense el càrrec.
- **Damunt del cap**: només el país.
- **MineUAB normal**: com ara. El prefix del país té el context `server=create`, i el MineUAB té la seva config.

El mod ja fa la seva part sol: posa el prefix del país a cada jugador (prioritat 5, `server=create`) i registra el
placeholder `%pauitreva-tag%` del TAB. Al servidor cal (arxivant abans cada fitxer, com diu la skill del servidor):

1. `config/luckperms/luckperms.conf`: el bloc `meta-formatting` per el de
   [`luckperms-meta-formatting.conf`](luckperms-meta-formatting.conf). LuckPerms el llegeix en arrencar.
2. `config/lpcmod-common.toml`: el de [`lpcmod-common.toml`](lpcmod-common.toml) (s'aplica sol).
3. El color del nom dels rangs, que fins ara sortia del final del seu prefix, a la consola del Create:

   ```
   lp group admin meta set username-color #8B0000 server=create
   lp group mod meta set username-color &b server=create
   lp group helper meta set username-color &a server=create
   ```

4. `config/tab/groups.yml`, a `_DEFAULT_`:

   ```yaml
   tagprefix: "%pauitreva-tag%"
   tagsuffix: ""
   ```

   (Abans: `tagprefix: "%luckperms-prefix%"` i `tagsuffix: "%luckperms-suffix%"`.) Només quan Pau i Treva ja hi sigui:
   sense el mod, el placeholder no existeix i sortiria escrit tal qual.

5. Reiniciar el Create i mirar al joc el xat, la llista del TAB i el damunt del cap.

El servidor de proves (`./gradlew runProves`) arrenca amb el LuckPerms del Create i aquesta mateixa pila
(`ProvesTags`).
