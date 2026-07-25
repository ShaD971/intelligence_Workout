# Prompt Claude Code — Refonte Intelligence WorkOut

> Colle ce fichier entier dans Claude Code (ou lance `claude` à la racine du projet puis dis :
> « Lis PROMPT_CLAUDE_CODE.md et exécute-le phase par phase, en me demandant validation à la fin de chaque phase »).

---

## 0. Contexte

Projet Android existant : `C:\Dev\intelligence_Workout`
Package : `com.example.shad.projetosnomade`
Build : Gradle 8.14 / AGP 8.11.1 / compileSdk 36 / **minSdk 15** / Java, **aucune dépendance AndroidX**.

Fichiers actuels :

| Fichier | Rôle |
|---|---|
| `Start_Activity.java` | Écran d'accueil, choix Facile/Moyen/Difficile |
| `IntelligenceWorkout_Activity.java` | Écran de jeu, HUD, dialog de victoire |
| `IntelligenceWorkoutView.java` | `SurfaceView` + `Runnable` : grille, rendu, tactile, score |
| `res/layout/activity_main.xml` | Accueil |
| `res/layout/main.xml` | Jeu (HUD + SurfaceView) |
| `res/values/styles.xml` | Thème `Theme.Holo.Light.NoActionBar.Fullscreen` (déprécié) |

Règle du jeu : une grille carrée de tuiles bleues/rouges. Le joueur fait tourner **cycliquement** une ligne
ou une colonne entière pour reproduire la grille cible affichée en miniature en haut.

---

## 1. Objectifs de la mission

1. **Corriger les bugs et dettes techniques** listés en §2 (bloquant : le thread de rendu ne s'arrête jamais).
2. **Passer de 3 à 9 niveaux** : 3 niveaux par palier de difficulté (Facile / Moyen / Difficile),
   avec **progression déblocable**, étoiles et sauvegarde locale.
3. **Refondre le design** en Material 3, thème sombre cohérent, tuiles dessinées en Canvas (plus de PNG).
4. **Rendre le jeu fluide** : drag qui suit le doigt, animation de rotation interpolée, 60 fps, retour haptique.

Contrainte forte : **on garde Java et le rendu Canvas/SurfaceView**. Pas de migration Kotlin ni Compose.
On modernise l'infrastructure (AndroidX + Material Components), on ne réécrit pas le moteur en autre chose.

---

## 2. Audit — bugs à corriger impérativement

Corrige-les dans la Phase 1 et la Phase 3, chacun avec un commit distinct :

### Cycle de vie / threading
- **B1** — `cvThread` est démarré dans `initParameters()` (donc dans le constructeur), avant que la surface
  existe, et `in` n'est jamais remis à `false`. Le thread survit à la destruction de l'Activity → fuite mémoire
  et exceptions silencieuses avalées par le `catch (Exception)` de `run()`.
  → Démarrer le thread dans `surfaceCreated()`, l'arrêter et le `join()` dans `surfaceDestroyed()`.
- **B2** — Le tableau `carte` est écrit depuis le thread UI (`onTouchEvent`) et lu depuis le thread de rendu
  sans synchronisation → tearing visuel possible. Introduire un verrou (`synchronized` sur un objet dédié)
  ou un double-buffer d'état immuable.
- **B3** — `IntelligenceWorkout_Activity` ne gère ni `onPause()` ni `onResume()` : le `Handler` du chrono
  continue à tourner en arrière-plan et le temps écoulé s'incrémente pendant que l'app est en pause.
  → Mettre le chrono en pause et cumuler le temps réellement joué.
- **B4** — Aucune gestion de la rotation d'écran / `onSaveInstanceState` : la partie est perdue.
  → Sauvegarder l'état de la grille, les coups, le score et le temps.

### Logique de jeu
- **B5** — Dans `onTouchEvent`, `xchange = (event.getX() - anchor) / tileSize` : le cast `(int)` tronque
  vers zéro, donc un toucher entre `-tileSize` et `0` donne `0` et est considéré **dans** la grille.
  → Utiliser `Math.floorDiv()` ou tester les bornes en pixels avant division.
- **B6** — `rotateColumnDown(xtemp)` / `rotateColumnUp(xtemp)` utilisent `xtemp` alors que la branche
  précédente vient éventuellement de réassigner `xtemp = xchange`. Le comportement dépend de l'ordre des `if`.
  → Capturer la ligne et la colonne d'origine du geste au `ACTION_DOWN` et ne plus les modifier en cours de geste.
- **B7** — Un `ACTION_MOVE` qui traverse 3 cellules déclenche 3 rotations et **3 pénalités de score**
  dans la même frame, sans aucune animation → sensation de saccade et de score qui s'effondre.
  → Voir Phase 5 : un geste = une rotation validée au `ACTION_UP`, avec aperçu animé pendant le drag.
- **B8** — `copyGrid()` fait `new int[source.length][source.length]` : suppose une grille carrée.
  → Utiliser `source[row].length` (nécessaire si tu ajoutes des grilles non carrées plus tard).
- **B9** — `checkForWin()` est appelé à la fois par `recordMove()` et par `ACTION_UP`, et
  `notifyGameState()` peut donc déclencher `showVictoryDialog` deux fois. Le garde-fou `victoryDialogShown`
  est remis à `false` dans `onGameStateChanged` quand `gameWon == false` → fragile. Centraliser.

### Performance / rendu
- **B10** — `drawTile()` alloue un `new Rect(...)` par tuile **à chaque frame** (36 objets × 25 fps sur
  Difficile) → pression GC inutile. Réutiliser un `Rect` membre.
- **B11** — `Thread.sleep(40)` fixe = 25 fps. Passer à une boucle avec delta-time visant 60 fps.
- **B12** — Les tuiles sont des PNG (`blue.png`, `red.png`, `miniblue.png`, `minired.png`) redimensionnés
  à la volée. Les remplacer par du dessin Canvas (rect arrondi + ombre) : plus net, plus léger, thémable.

### Divers
- **B13** — `strings.xml` ne contient que `app_name` = `"projetOSnomade"`. Tous les textes sont en dur
  dans les layouts et le code. → Tout externaliser dans `strings.xml`, renommer l'app « Intelligence WorkOut ».
- **B14** — `Log.e()` utilisé pour du flux nominal (`"cv_thread.start()"`, `"PB DANS RUN"`).
  → Supprimer ou passer en `Log.d` derrière un `BuildConfig.DEBUG`.
- **B15** — `res/menu/main.xml` est gonflé par `Start_Activity.onCreateOptionsMenu` alors que le thème est
  `NoActionBar` → code mort. Supprimer.
- **B16** — `android:onClick="onClick"` sur `button28` dans le XML **en plus** du `setOnClickListener`
  Java → double enregistrement. Garder uniquement le Java, et renommer les ids (`button28` → `startButton`).

---

## 3. Phase 1 — Fondations techniques

**Objectif : compiler sur une base moderne sans changer le gameplay.**

1. `app/build.gradle` :
   - `minSdk 21` (Holo n'est plus tenable, et 15 est mort depuis longtemps).
   - Ajouter :
     ```gradle
     buildFeatures { viewBinding true }
     compileOptions {
         sourceCompatibility JavaVersion.VERSION_17
         targetCompatibility JavaVersion.VERSION_17
     }
     dependencies {
         implementation 'androidx.appcompat:appcompat:1.7.0'
         implementation 'com.google.android.material:material:1.12.0'
         implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
         implementation 'androidx.core:core:1.13.1'
         implementation 'androidx.preference:preference:1.2.1'
         testImplementation 'junit:junit:4.13.2'
     }
     ```
   - Vérifier que `gradle.properties` contient `android.useAndroidX=true`.
2. Migrer `Activity` → `AppCompatActivity`, remplacer `findViewById` par le **view binding**.
3. Remplacer `AlertDialog` par `MaterialAlertDialogBuilder`.
4. Nouveau thème dans `res/values/themes.xml` :
   `Theme.Material3.DayNight.NoActionBar`, avec une palette définie en §6.
   Supprimer `styles.xml` et le `Theme.Holo`. Garder `values-night/`.
5. Externaliser **tous** les textes dans `strings.xml` (`app_name` = `Intelligence WorkOut`).
6. Nettoyer : `res/menu/main.xml`, `Lizez-moi.txt` (vide), `ApplicationTest.java` (obsolète),
   les PNG `blue/red/miniblue/minired` dans `mipmap-*` une fois la Phase 5 faite.
7. Renommer les ids XML sans signification (`button28`, `textView2`, `view`) en noms parlants.

**Livrable** : l'app compile (`.\gradlew assembleDebug`) et se comporte comme avant, mais en Material 3.

---

## 4. Phase 2 — Moteur de jeu extrait et 9 niveaux

**Objectif : sortir toute la logique de la `View`, la rendre testable, et définir les 9 niveaux.**

Crée un package `com.example.shad.projetosnomade.game` :

### 4.1 `Difficulty.java` (enum)
```java
public enum Difficulty {
    EASY("easy",   R.string.difficulty_easy,   3, 120, 4,  60,  15),
    MEDIUM("medium", R.string.difficulty_medium, 5, 220, 8, 140,  30),
    HARD("hard",   R.string.difficulty_hard,   6, 360, 12, 280,  50);
    // id, labelRes, gridSize par défaut, startScore, movePenalty, finishBonus, quickMoveBonus
}
```

### 4.2 `Level.java`
Modèle immuable : `id` (ex. `easy_1`), `Difficulty`, `int gridSize`, `int[][] target`,
`int scrambleMoves`, `long seed`, `int parMoves` (nombre de coups optimal estimé), `int labelRes`.

### 4.3 `LevelRepository.java`
Retourne les 9 niveaux, **3 par palier, difficulté croissante à l'intérieur du palier**.
Utilise `0 = BLEU`, `1 = ROUGE`.

**FACILE — grilles 3×3**

- `easy_1` « Croix » — scramble 3 coups, par 3
  ```
  0 1 0
  1 1 1
  0 1 0
  ```
- `easy_2` « Diagonale » — scramble 4 coups, par 4
  ```
  1 0 0
  0 1 0
  0 0 1
  ```
- `easy_3` « Damier » — scramble 5 coups, par 5
  ```
  1 0 1
  0 1 0
  1 0 1
  ```

**MOYEN — grilles 4×4 puis 5×5**

- `medium_1` « Cadre » (4×4) — scramble 5 coups, par 5
  ```
  1 1 1 1
  1 0 0 1
  1 0 0 1
  1 1 1 1
  ```
- `medium_2` « Sablier » (5×5) — scramble 6 coups, par 6
  ```
  1 1 1 1 1
  0 1 1 1 0
  0 0 1 0 0
  0 1 1 1 0
  1 1 1 1 1
  ```
- `medium_3` « Viseur » (5×5) — scramble 7 coups, par 7
  ```
  0 0 1 0 0
  0 0 1 0 0
  1 1 1 1 1
  0 0 1 0 0
  0 0 1 0 0
  ```

**DIFFICILE — grilles 6×6**

- `hard_1` « Nœud papillon » — scramble 8 coups, par 8
  ```
  1 0 0 0 0 1
  0 1 0 0 1 0
  0 0 1 1 0 0
  0 0 1 1 0 0
  0 1 0 0 1 0
  1 0 0 0 0 1
  ```
- `hard_2` « Spirale » — scramble 10 coups, par 10
  ```
  1 1 1 1 1 1
  1 0 0 0 0 0
  1 0 1 1 1 0
  1 0 1 0 0 0
  1 0 1 1 1 1
  1 0 0 0 0 0
  ```
- `hard_3` « Zébré » — scramble 12 coups, par 12
  ```
  1 1 0 0 1 1
  1 1 0 0 1 1
  0 0 1 1 0 0
  0 0 1 1 0 0
  1 1 0 0 1 1
  1 1 0 0 1 1
  ```

### 4.4 `GridScrambler.java` — **point critique**
❗ Les grilles de départ actuelles sont écrites à la main dans `startForDifficulty()` : **rien ne garantit
qu'elles soient solvables** (le nombre de rouges peut même différer de la cible).

Nouvelle approche obligatoire : **la grille de départ est générée en appliquant N rotations aléatoires à la
cible**, avec un `Random(seed)` **déterministe** (même niveau ⇒ même grille pour tous les joueurs).
La solvabilité est alors garantie par construction.

Règles du scrambler :
- Tire un axe (ligne/colonne), un index, un sens ; applique la rotation ; répète `scrambleMoves` fois.
- **Interdire d'annuler le coup précédent** (même axe + même index + sens opposé).
- À la fin, si la grille obtenue est identique à la cible, relancer avec `seed + 1`.
- Ajouter une méthode `verifySolvable(Level)` utilisée en test.

### 4.5 `GameEngine.java`
Contient : `int[][] grid`, `int[][] target`, `moves`, `score`, `elapsedMillis`, `won`,
un historique `Deque<Move>` pour l'**undo**, et les méthodes
`rotateRow(int row, int dir)`, `rotateColumn(int col, int dir)`, `undo()`, `reset()`, `isWon()`,
`computeStars()`.
**Zéro dépendance Android** dans cette classe → testable en JVM pure.

### 4.6 Barème d'étoiles
| Étoiles | Condition |
|---|---|
| ★★★ | `moves <= parMoves` |
| ★★☆ | `moves <= parMoves * 1.5` |
| ★☆☆ | victoire |

Score final = `startScore - moves * movePenalty + finishBonus + max(0, (parMoves - moves + 1) * quickMoveBonus)`,
borné à 0.

---

## 5. Phase 3 — Progression et sauvegarde

Crée `ProgressStore.java` (wrapper `SharedPreferences`, fichier `iw_progress`) :

- `int getStars(String levelId)` / `void setStars(String levelId, int stars)` (ne descend jamais).
- `int getBestScore(String levelId)` / `long getBestTimeMillis(String levelId)` / `int getBestMoves(String levelId)`.
- `boolean isUnlocked(String levelId)` :
  - le **premier niveau de chaque palier est toujours débloqué** (pour ne pas frustrer) ;
  - `easy_2` se débloque quand `easy_1` a ≥ 1 étoile, etc.
- `int getTotalStars()` (sur 27).
- `void resetProgress()` (bouton dans l'accueil, avec confirmation).

Ajoute `@VisibleForTesting` sur les setters et un test unitaire de la logique de déverrouillage.

---

## 6. Phase 4 — Design

### 6.1 Palette (`res/values/colors.xml` + `values-night/colors.xml`)

Thème sombre par défaut, cohérent avec le fond du Canvas :

| Token | Clair | Sombre |
|---|---|---|
| `iw_background` | `#F2F5F9` | `#101418` |
| `iw_surface` | `#FFFFFF` | `#1A2027` |
| `iw_surface_variant` | `#E4EAF2` | `#232C35` |
| `iw_on_surface` | `#17212B` | `#E6EDF3` |
| `iw_on_surface_muted` | `#5B6B7A` | `#8FA3B5` |
| `iw_primary` | `#0B7FAB` | `#3FB6E0` |
| `iw_accent` | `#D62D49` | `#FF5C77` |
| `iw_tile_blue` | `#2E7FD1` → dégradé `#1F5FA8` | idem |
| `iw_tile_red` | `#E0475F` → dégradé `#B02840` | idem |
| `iw_gold` (étoiles) | `#FFC107` | `#FFCA28` |

Supprime `mipmap/background.png` : le fond devient un dégradé vertical `iw_background` → `iw_surface_variant`
via un `<shape>` XML (fonctionne à toutes les densités, aucun PNG à maintenir).

### 6.2 Écran d'accueil (`activity_start.xml`)
- `ConstraintLayout`, plus de `ScrollView` inutile.
- Logo + titre « Intelligence WorkOut » + sous-titre explicatif.
- Compteur d'étoiles global « ★ 12 / 27 » en haut à droite.
- **3 `MaterialCardView` de palier** (Facile / Moyen / Difficile), chacune affichant :
  nom, 3 pastilles de niveau (verrouillé = cadenas, sinon 0–3 étoiles), et progression du palier.
  Un tap sur une carte ouvre l'écran de sélection de niveau.
- Bouton texte « Réinitialiser la progression » discret en bas.
- Bouton « Comment jouer ? » ouvrant une `BottomSheetDialog` avec 3 illustrations simples.

### 6.3 Écran de sélection de niveau (`activity_level_select.xml`, nouvelle activity)
- `Toolbar` avec le nom du palier et flèche retour.
- `RecyclerView` en `GridLayoutManager` (2 colonnes) de cartes niveau :
  numéro, aperçu miniature de la grille cible dessiné par un petit `LevelPreviewView`,
  étoiles obtenues, meilleur score. Niveau verrouillé = carte grisée + icône cadenas, non cliquable.

### 6.4 Écran de jeu (`activity_game.xml`)
- HUD en haut sous forme de `MaterialCardView` horizontale : trois blocs **Coups / Temps / Score**
  avec label petit + valeur grande, séparés par des dividers verticaux.
- La grille cible n'est plus dessinée dans le `SurfaceView` mais dans une petite carte « Cible »
  posée en haut à droite du HUD → le Canvas ne s'occupe plus que de la grille jouable.
- Barre d'actions en bas : `Undo` (icône flèche retour), `Indice`, `Réinitialiser`, `Menu`.
  Boutons `MaterialButton` style `OutlinedButton`, `Undo` désactivé si historique vide.
- Le `SurfaceView` occupe tout l'espace restant et se centre proprement.

### 6.5 Écran / feuille de victoire
Remplace l'`AlertDialog` par une `BottomSheetDialogFragment` :
- Animation d'apparition des étoiles une par une (scale + fade, 150 ms d'écart), avec un son court optionnel.
- Lignes : Temps, Coups (et « optimal : N »), Score, « Nouveau record ! » si applicable.
- Boutons : **Niveau suivant** (primaire, masqué si dernier niveau du palier), **Rejouer**, **Menu**.

### 6.6 Ressources
- Icônes : vecteurs `res/drawable/ic_*.xml` (undo, hint, reset, home, lock, star, star_outline).
  Pas de PNG.
- Typo : `android:fontFamily="sans-serif-medium"` pour les valeurs, `sans-serif` pour les labels.
- Toutes les dimensions dans `res/values/dimens.xml` (`iw_spacing_s/m/l`, `iw_corner_radius`, …).

---

## 7. Phase 5 — Fluidité et animations

**C'est la phase qui change le plus le ressenti. À ne pas bâcler.**

### 7.1 Boucle de rendu
- Remplacer `Thread.sleep(40)` par une boucle delta-time visant 16,6 ms, avec calcul du `dt` réel
  passé aux animations. Sortir proprement quand `running == false`.
- Réutiliser un `Rect`/`RectF` et un `Paint` membres — **aucune allocation dans `nDraw()`**.

### 7.2 Drag qui suit le doigt (remplace B7)
Machine à états explicite :
1. `ACTION_DOWN` → mémoriser `(rowOrigin, colOrigin)` et la position en pixels. État = `PENDING`.
2. `ACTION_MOVE` → au-delà d'un seuil de `ViewConfiguration.getScaledTouchSlop()`, déterminer l'axe
   dominant (|dx| vs |dy|) et **le verrouiller pour tout le geste**. État = `DRAGGING_ROW` ou `DRAGGING_COL`.
3. Pendant le drag, la ligne/colonne concernée est **dessinée décalée de `offset` pixels**, avec les tuiles
   qui sortent d'un côté et réapparaissent de l'autre (wrap visuel). L'offset est borné à ±`gridSize*tileSize`.
4. `ACTION_UP` → `steps = Math.round(offset / tileSize)`.
   - Si `steps == 0` → animation de retour élastique à 0, **aucun coup compté**.
   - Sinon → animation de snap vers `steps * tileSize` (`DecelerateInterpolator`, 180 ms), puis application
     de `steps` rotations dans le modèle et **1 seul coup compté**.
5. Ajouter la gestion de `ACTION_CANCEL` (retour élastique).

### 7.3 Animations
- **Snap** : 180 ms, `DecelerateInterpolator(1.6f)`.
- **Victoire** : vague de « pop » sur les tuiles depuis le coin haut-gauche (scale 1 → 1.12 → 1,
  décalage de 25 ms par diagonale), puis flash blanc léger.
- **Tuile correcte** : une tuile déjà à sa place dans la cible reçoit un liseré subtil
  (à activer/désactiver dans les options — c'est une aide).
- **Indice** : fait clignoter 2 fois la ligne/colonne à bouger (coût : −1 étoile potentielle sur le niveau).
- **Transitions d'activity** : `overridePendingTransition` fade + slide léger, ou `ActivityOptionsCompat`.

### 7.4 Retours sensoriels
- `HapticFeedbackConstants.CLOCK_TICK` à chaque cellule franchie pendant le drag.
- `VibrationEffect.createOneShot(20, DEFAULT_AMPLITUDE)` (API 26+) au snap validé.
- Vibration plus longue (2 pulses) à la victoire.
- Tout cela derrière un flag `hapticsEnabled` stocké dans `ProgressStore`.

### 7.5 Rendu des tuiles (remplace les PNG)
```java
// pseudo-code
RectF r = tempRect;
r.set(left + gap, top + gap, left + size - gap, top + size - gap);
paint.setShader(new LinearGradient(...)); // shader mis en cache, PAS recréé par frame
canvas.drawRoundRect(r, radius, radius, paint);
```
- `radius = size * 0.18f`, `gap = size * 0.06f`.
- Deux `LinearGradient` construits une seule fois par changement de `tileSize` (dans `surfaceChanged`).
- Ajouter une ombre portée douce via un `Paint` avec `setShadowLayer` sur un layer logiciel,
  ou simplement un rect plus sombre décalé de 2 dp dessiné en dessous.

### 7.6 Accessibilité
- `contentDescription` sur tous les boutons.
- Sur le `SurfaceView`, exposer un résumé via `setContentDescription` mis à jour à chaque coup
  (« grille 5 sur 5, 7 tuiles bien placées sur 25 »).
- Vérifier les contrastes ≥ 4.5:1 pour tout le texte du HUD.

---

## 8. Phase 6 — Tests et documentation

1. Tests JVM (`app/src/test/java/.../game/`) :
   - `GameEngineTest` : rotations ligne/colonne dans les deux sens, invariance du nombre de rouges,
     `undo()` restaure exactement l'état précédent, détection de victoire, calcul du score et des étoiles.
   - `GridScramblerTest` : pour **les 9 niveaux**, la grille générée est ≠ de la cible, contient le même
     multiset de valeurs, et est résolue en rejouant les coups inverses. Déterminisme : même seed ⇒ même grille.
   - `ProgressStoreTest` (avec Robolectric ou une abstraction `KeyValueStore` mockée) :
     déverrouillage en cascade, étoiles jamais décroissantes.
2. Supprimer `ApplicationTest.java`.
3. Mettre à jour `README.md` : nouvelle liste des 9 niveaux, système d'étoiles, captures d'écran,
   prérequis, commandes de build et de test (`.\gradlew testDebugUnitTest`).
4. Lancer `.\gradlew lintDebug` et corriger les warnings bloquants.

---

## 9. Méthode de travail attendue

- Travaille **phase par phase**, dans l'ordre. À la fin de chaque phase :
  `.\gradlew assembleDebug` doit passer, puis fais un commit atomique avec un message conventionnel
  (`fix:`, `feat:`, `refactor:`, `style:`, `test:`).
- Avant de coder une phase, liste en 5 lignes max ce que tu vas modifier, et attends mon feu vert.
- Ne modifie jamais `local.properties`, `gradle/wrapper/`, ni le dossier `build/`.
- Si un choix de design est ambigu, propose 2 options courtes plutôt que de trancher seul.
- Ne crée pas de fichiers de documentation supplémentaires (pas de `CHANGELOG.md`, pas de `NOTES.md`)
  sauf demande explicite : mets à jour `README.md`.

---

## 10. Critères d'acceptation

- [ ] `.\gradlew assembleDebug` et `.\gradlew testDebugUnitTest` passent.
- [ ] 9 niveaux jouables, tous vérifiés solvables par test automatisé.
- [ ] La progression (étoiles, meilleurs scores, déverrouillages) survit à la fermeture de l'app.
- [ ] Faire glisser une ligne fait bouger la ligne **sous le doigt** en temps réel, et un geste = un coup.
- [ ] Aucune allocation dans la boucle de rendu (vérifié au profiler ou par relecture de `nDraw`).
- [ ] Le thread de rendu s'arrête à `surfaceDestroyed` (vérifiable : plus de log après le retour à l'accueil).
- [ ] Rotation d'écran en cours de partie : la partie est conservée.
- [ ] Aucun texte en dur hors `strings.xml`.
- [ ] Mode sombre et mode clair tous deux lisibles.

---

## 11. À ne pas faire

- Ne pas migrer vers Kotlin ni Jetpack Compose.
- Ne pas remplacer le `SurfaceView` par des `View` Android classiques par tuile.
- Ne pas ajouter de dépendance réseau, d'analytics, de pub, ni de backend.
- Ne pas générer les niveaux aléatoirement au runtime : les 9 niveaux sont fixes et déterministes.
- Ne pas renommer le package `com.example.shad.projetosnomade` (ça casserait `applicationId` et la
  progression sauvegardée) — sauf si je te le demande explicitement.
