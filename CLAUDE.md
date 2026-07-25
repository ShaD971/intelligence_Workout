# Intelligence WorkOut — contexte projet

Fichier de contexte lu automatiquement par Claude Code. À tenir à jour.

## Le projet

Jeu Android : grille de tuiles bleues/rouges, on fait tourner **cycliquement** une ligne ou une
colonne pour reproduire une grille cible. 9 niveaux (3 par palier Facile/Moyen/Difficile),
progression déblocable avec étoiles.

| | |
|---|---|
| Repo | `ShaD971/intelligence_Workout` |
| Local | `C:\Dev\intelligence_Workout` |
| Langage | **Java** — pas de Kotlin, pas de Compose |
| Rendu | `SurfaceView` + Canvas custom (`IntelligenceWorkoutView`) |
| Build | Gradle 8.14, AGP 8.11.1, **JDK 17** |
| SDK | minSdk 21, compileSdk/targetSdk 36 |
| UI | AndroidX + Material 3 (`themes.xml`), mode clair/sombre |
| Package | `com.example.shad.projetosnomade` — **ne pas renommer** (casserait `applicationId` et la progression sauvegardée) |

Architecture : la logique est dans le package `game/` (`GameEngine`, `LevelRepository`,
`GridScrambler`, `Difficulty`, `Level`) sans dépendance Android → testable en JVM pure.
La progression est dans `progress/` (`ProgressStore` sur `SharedPreferences`).

Les grilles de départ sont **générées** en appliquant N rotations à la cible avec un seed
déterministe → solvabilité garantie par construction. Ne jamais écrire une grille de départ à la main.

## Conventions

- **Conventional Commits**, sujet en anglais, impératif, ≤ 72 caractères, pas de point final.
  Préfixes : `feat: fix: refactor: style: test: docs: chore: perf: build: ci:`
- ❌ Aucune signature, mention d'IA, `Co-Authored-By` ni emoji dans les messages de commit.
- Identité git des commits : `ShaD971 <kevin.toleon@hotmail.fr>`
- Branches nommées `chore/...`, `feat/...`, `fix/...` (les anciennes `update-*` sont supprimées).
- Un commit = une unité logique. `git add <fichiers>` ciblé, jamais `git add -A` en aveugle.
- Avant tout commit touchant du code : `.\gradlew assembleDebug` + `testDebugUnitTest` doivent passer.
- ❌ Jamais `--force`, `reset --hard`, `clean -fd`, `--no-verify`, ni push direct sur `master`.
- Demander avant toute opération destructive ou ambiguë.

## Pièges connus de ce dépôt

1. **CRLF ↔ LF.** Windows réécrit régulièrement les fichiers ; `git status` affiche des fichiers
   « modifiés » dont le contenu est identique (insertions = suppressions). Vérifier avec
   `git diff --ignore-all-space --stat` et restaurer les faux changements.
   Un `.gitattributes` est en place, avec `gradlew` forcé en **LF** — s'il passe en CRLF, la CI
   Linux échoue avec `/bin/sh^M: bad interpreter`.
2. **`local.properties`** contient `sdk.dir=C:\Dev\Android\Sdk` → jamais commité.
3. **`gradle-wrapper.jar`** doit être le jar officiel, régénéré via
   `gradlew wrapper --gradle-version 8.14 --distribution-type all`. Un jar non officiel est rejeté
   par la validation de sécurité de `gradle/actions/setup-gradle`.
4. Ne **jamais** créer de `build.xml` : le template CI « Java with Ant » avait été commité par erreur,
   c'est ce qui cassait la CI. Le projet est en Gradle.

## CI

`.github/workflows/android.yml` — JDK 17, `android-actions/setup-android`,
`gradle/actions/setup-gradle`, puis `assembleDebug` + `testDebugUnitTest` + `lintDebug`
(`continue-on-error`), upload de l'APK et des rapports.

### Filtre de branches au push — décidé le 25/07/2026

```yaml
push:
  branches: [ "**" ]
```

**Option retenue : toutes les branches.** Le filtre précédent était
`[ "master", "update-**" ]`, calibré sur les anciennes branches `update-*` toutes supprimées
depuis — une branche `chore/...` ou `feat/...` ne déclenchait alors **aucun run au push**, donc
on poussait à l'aveugle jusqu'à l'ouverture d'une PR.

Sur un dépôt solo, la CI sur chaque push évite exactement ce trou, et le bloc
`concurrency: cancel-in-progress` déjà présent empêche l'empilement de runs.

L'alternative écartée était une liste de préfixes conventionnels
(`master`, `feat/**`, `fix/**`, `chore/**`, `ci/**`, `refactor/**`) : plus sélective, mais elle
recrée le même risque d'oubli dès qu'un nouveau préfixe apparaît.

## État au 25/07/2026

- `master` = `270972a` (merge PR #2). Branches restantes : `master`, `chore/repo-cleanup`.
- `chore/repo-cleanup` (`609f05a`) poussée, **PR pas encore ouverte** vers `master`.
- Refonte phases 1 → 6 terminée et fusionnée (moteur extrait, 9 niveaux, progression,
  Material 3, drag qui suit le doigt, tests, docs).

## Reste à faire

- [ ] Ouvrir la PR `chore/repo-cleanup` → `master`, vérifier le check, merger.
- [ ] Retirer `continue-on-error` du lint une fois la dette résorbée.
- [ ] Job d'instrumentation UI (`reactivecircus/android-emulator-runner`).
- [ ] `dependabot.yml` pour les actions et les dépendances Gradle.

## Prompts de travail

`PROMPT_CLAUDE_CODE.md` (refonte, phases 1→6), `PROMPT_COMMIT_PUSH.md` (commit/push),
`PROMPT_FIX_CI.md` (réparation CI). Historiques, conservés comme référence.
