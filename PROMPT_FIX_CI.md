# Prompt Claude Code — Réparer la CI GitHub Actions

> **Utilisation :** terminal dans `C:\Dev\intelligence_Workout`, lance `claude`, puis :
> « Lis `PROMPT_FIX_CI.md` et exécute-le. Arrête-toi à chaque `[STOP]`. »

---

## 1. Diagnostic — cause exacte de l'échec

### L'erreur

```
Run ant -noinput -buildfile build.xml
Buildfile: build.xml does not exist!
Build failed
Error: Process completed with exit code 1
```

### La cause

Le fichier `.github/workflows/ant.yml` existe **uniquement sur `origin/master`** (ajouté via
l'interface web GitHub, jamais tiré en local). Son contenu :

```yaml
name: Java CI
on:
  push:
    branches: [ "master" ]
  pull_request:
    branches: [ "master" ]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - name: Set up JDK 11
      uses: actions/setup-java@v4
      with:
        java-version: '11'
        distribution: 'temurin'
    - name: Build with Ant
      run: ant -noinput -buildfile build.xml
```

C'est le **template de démarrage « Java with Ant » de GitHub**, proposé par défaut dans l'onglet
Actions et accepté tel quel. Il ne correspond en rien au projet.

**Quatre incompatibilités, pas une seule :**

| # | Problème | Détail |
|---|---|---|
| 1 | **Mauvais outil de build** | Le workflow appelle **Ant**. Le projet est en **Gradle** (`build.gradle`, `settings.gradle`, wrapper Gradle 8.14). Il n'y a **aucun `build.xml`** dans le dépôt, et il ne doit pas y en avoir. → C'est l'erreur affichée. |
| 2 | **Mauvaise version de JDK** | Le workflow installe **JDK 11**. L'**AGP 8.11.1** exige **JDK 17 minimum**, et `app/build.gradle` déclare `sourceCompatibility VERSION_17`. Même corrigé pour Gradle, le build échouerait avec `Android Gradle plugin requires Java 17`. |
| 3 | **Aucun SDK Android** | Le workflow ne prépare pas le SDK Android. Le projet a besoin de la **plateforme 36** (`compileSdk 36` / `targetSdk 36`) et de licences acceptées. |
| 4 | **Branches non couvertes** | Le workflow ne se déclenche que sur `master`. Tout le travail est sur `update-difficulty-levels`, `update-modern-ui`, `update-android-modernization` → aucun retour CI sur les branches réellement utilisées. |

### Ce qui n'est **pas** la cause

- ❌ Ce n'est pas un `build.xml` supprimé par erreur : ce fichier n'a jamais existé dans l'historique.
- ❌ Ce n'est pas un problème de dépendances ni de code applicatif : le build n'a même pas démarré.
- ❌ **Ne crée surtout pas un `build.xml`** pour satisfaire le workflow. Ce serait exactement la
  mauvaise réponse : ça ajouterait un second système de build à maintenir en parallèle de Gradle.

---

## 2. La solution

**Supprimer `ant.yml` et le remplacer par un workflow Android/Gradle correct.**

### Étape 2.1 — Récupérer le fichier fautif en local

Il n'existe que sur `origin/master`. Selon ta situation :

```bash
git fetch origin
git checkout master
git pull --ff-only origin master
```

**[STOP]** Si `master` local diverge de `origin/master`, montre-moi l'état et attends ma décision
avant tout `merge` / `rebase`.

### Étape 2.2 — Supprimer le workflow Ant

```bash
git rm .github/workflows/ant.yml
```

### Étape 2.3 — Créer `.github/workflows/android.yml`

```yaml
name: Android CI

on:
  push:
    branches: [ "master", "update-**" ]
  pull_request:
    branches: [ "master" ]
  workflow_dispatch:

concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

jobs:
  build:
    name: Build & test
    runs-on: ubuntu-latest
    timeout-minutes: 30

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Set up Android SDK
        uses: android-actions/setup-android@v3

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Make gradlew executable
        run: chmod +x ./gradlew

      - name: Assemble debug APK
        run: ./gradlew assembleDebug --stacktrace

      - name: Run unit tests
        run: ./gradlew testDebugUnitTest --stacktrace

      - name: Run lint
        run: ./gradlew lintDebug --stacktrace
        continue-on-error: true

      - name: Upload debug APK
        uses: actions/upload-artifact@v4
        if: success()
        with:
          name: app-debug-apk
          path: app/build/outputs/apk/debug/*.apk
          if-no-files-found: warn
          retention-days: 14

      - name: Upload test & lint reports
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: reports
          path: |
            app/build/reports/tests/
            app/build/reports/lint-results-debug.html
          if-no-files-found: ignore
          retention-days: 14
```

### Justification de chaque choix

| Choix | Pourquoi |
|---|---|
| `java-version: '17'` | Exigence stricte de l'AGP 8.11.1. Ne descends pas à 11, ne monte pas à 21 sans vérifier la compatibilité AGP. |
| `android-actions/setup-android@v3` | Installe le SDK, `sdkmanager`, et **accepte les licences** automatiquement. Sans ça : `Failed to install the following SDK components... licences not accepted`. |
| `gradle/actions/setup-gradle@v4` | Cache les dépendances et le daemon Gradle entre les runs. Divise le temps de build par 3 à 5. (Remplace l'ancien `gradle/gradle-build-action`, déprécié.) |
| `chmod +x ./gradlew` | Filet de sécurité. Le bit exécutable est bien présent dans l'index (`100755`), mais si quelqu'un recommite le fichier depuis Windows il peut passer en `100644` → `Permission denied`. |
| `branches: [ "master", "update-**" ]` | Couvre les branches de travail existantes. |
| `concurrency` + `cancel-in-progress` | Annule les runs obsolètes quand tu pousses plusieurs fois d'affilée. |
| `continue-on-error` sur le lint | Le lint ne doit pas bloquer le build tant que la dette n'est pas résorbée. À retirer plus tard. |
| `timeout-minutes: 30` | Évite qu'un run bloqué consomme les minutes Actions. |

### Étape 2.4 — Ajouter `.gitattributes` (si pas déjà fait)

⚠️ **Point critique et lié.** Les runners GitHub sont sous Linux. Si `gradlew` est commité avec des
fins de ligne **CRLF** depuis Windows, la CI échoue avec :

```
./gradlew: /bin/sh^M: bad interpreter: No such file or directory
```

C'est un risque réel ici : `git status` signale régulièrement `gradlew` comme modifié alors que son
contenu est identique (conversion CRLF↔LF). Crée donc à la racine :

```gitattributes
* text=auto eol=lf

*.bat    text eol=crlf
*.cmd    text eol=crlf
gradlew  text eol=lf

*.png    binary
*.jpg    binary
*.jar    binary
*.apk    binary
*.aab    binary
```

### Étape 2.5 — Vérifier `local.properties`

Ce fichier est correctement listé dans `.gitignore` (il contient `sdk.dir=C:\Dev\Android\Sdk`,
un chemin Windows local). **Il ne doit surtout pas être commité** : sur le runner Linux, il
écraserait le SDK fourni par `setup-android` et casserait le build.

Vérifie avec `git ls-files local.properties` — la sortie doit être **vide**.

---

## 3. Validation locale avant de pousser

Rejoue localement ce que fera la CI :

```powershell
.\gradlew --version          # doit afficher Gradle 8.14 et une JVM 17
.\gradlew assembleDebug --stacktrace
.\gradlew testDebugUnitTest --stacktrace
.\gradlew lintDebug --stacktrace
```

**[STOP]** Si l'un échoue, montre-moi la sortie et **ne pousse rien**. Un workflow correct qui
révèle un vrai bug de build est un progrès, mais je veux le voir avant qu'il parte sur GitHub.

Vérifie aussi la syntaxe YAML du workflow (indentation, pas de tabulations).
Si `actionlint` est disponible, lance-le ; sinon, relis attentivement.

---

## 4. Commits

Deux commits séparés, en Conventional Commits (style déjà en place dans l'historique) :

```
ci: replace Ant starter workflow with Android Gradle CI
chore: normalize line endings with .gitattributes
```

Corps du premier commit :

```
The default "Java with Ant" starter workflow was committed by mistake.
It ran `ant -buildfile build.xml` on a Gradle project with no build.xml,
and set up JDK 11 while AGP 8.11.1 requires JDK 17.

Replace it with a workflow that uses the Gradle wrapper, JDK 17, the
Android SDK, and runs assemble, unit tests and lint.
```

Règles de message : sujet en anglais, impératif, ≤ 72 caractères, pas de point final.
❌ Aucune signature, aucune mention d'IA, aucun `Co-Authored-By`, aucun emoji.

---

## 5. Push et vérification

1. Pousse sur la branche concernée (`master` si c'est là que vit le workflow).
   ❗ Si une protection de branche interdit le push direct sur `master` → passe par une PR.
   **[STOP]** demande-moi confirmation avant de pousser sur `master`.
2. Va voir le run déclenché dans l'onglet **Actions** du dépôt.
3. Rapporte-moi : le job passe-t-il ? Sinon, colle la sortie de l'étape en échec.
4. Vérifie que l'ancien workflow « Java CI » n'apparaît plus dans la liste des workflows.
   ⚠️ Un workflow supprimé peut rester visible dans la barre latérale tant qu'il a des runs
   historiques — c'est normal, il ne se déclenchera plus.

---

## 6. Règles absolues

- ❌ **Ne crée jamais de `build.xml`** ni de configuration Ant.
- ❌ Ne rétrograde pas le JDK sous 17, ne descends pas `compileSdk` sous 36 pour « faire passer » la CI.
- ❌ Pas de `git push --force`, `reset --hard`, `clean -fd`, `--no-verify`.
- ❌ N'ajoute pas de secret, de token, ni de keystore de signature dans le workflow.
- ❌ Ne commite pas `local.properties`.
- ✅ Si un choix est ambigu (quelle branche, faut-il une PR) : **demande, ne devine pas.**

---

## 7. À me proposer ensuite — ne pas exécuter d'office

1. Ajouter un job `instrumentation` avec `reactivecircus/android-emulator-runner` pour les tests UI.
2. Retirer `continue-on-error` sur le lint une fois les warnings résorbés, et ajouter
   `--warning-mode all` pour traquer les API Gradle dépréciées.
3. Ajouter un badge de build dans le `README.md` :
   `![Android CI](https://github.com/ShaD971/intelligence_Workout/actions/workflows/android.yml/badge.svg)`
4. Ajouter `dependabot.yml` pour tenir à jour les actions et les dépendances Gradle.
5. Nettoyer `.gitignore` : il contient des exceptions devenues inutiles
   (`!/build/modernization_report.txt`, `!/build/update_report.txt` — cette dernière en double).
