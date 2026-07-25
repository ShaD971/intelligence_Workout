# Prompt Claude Code — Commit & Push vers GitHub

> **Utilisation :** ouvre un terminal dans `C:\Dev\intelligence_Workout`, lance `claude`, puis tape :
> « Lis `PROMPT_COMMIT_PUSH.md` et exécute-le étape par étape. Arrête-toi à chaque `[STOP]` et attends ma réponse. »

---

## 0. Contexte du dépôt

| Élément | Valeur |
|---|---|
| Dépôt local | `C:\Dev\intelligence_Workout` |
| Remote `origin` | `https://github.com/ShaD971/intelligence_Workout` |
| Branche par défaut | `master` |
| Branche locale courante | `update-difficulty-levels` |
| Upstream configuré | `origin/update-modern-ui` ⚠️ **incohérent, voir §2** |
| Autres branches locales | `master`, `update-android-modernization` |
| Autres branches distantes | `origin/master`, `origin/update-modern-ui` |
| Projet | Android / Java / Gradle 8.14 / AGP 8.11.1 |

**État constaté au moment de la rédaction** (à revérifier, il a pu bouger) :

- **9 commits locaux non poussés** (`9b1a409` → `3b6daa6`, phases 1 à 6 de la refonte).
- **0 commit distant en avance** (pas de retard à rattraper).
- **6 fichiers « modifiés »** qui sont en réalité de faux changements (voir §1).
- **2 fichiers non suivis** : `PROMPT_CLAUDE_CODE.md`, `PROMPT_COMMIT_PUSH.md`.

---

## 1. ⚠️ Piège n°1 — Faux changements de fins de ligne (CRLF ↔ LF)

`git status` signale ces fichiers comme modifiés :

```
 M app/.gitignore
 M app/proguard-rules.pro
 M app/src/main/AndroidManifest.xml
 M gradle.properties
 M gradlew
 M settings.gradle
```

**Aucun d'eux n'a de changement de contenu réel.** Preuve : sur chacun, le nombre d'insertions égale
exactement le nombre de suppressions. Exemple, `settings.gradle` :

```diff
-include ':app'
+include ':app'
```

C'est Windows qui a réécrit les fichiers en CRLF alors que le dépôt les stocke en LF.

### Ce qu'il faut faire

1. Identifier les vrais changements en ignorant les espaces et fins de ligne :
   ```bash
   git diff --stat                      # liste "brute"
   git diff --ignore-all-space --stat   # ne garde que les VRAIS changements
   ```
   Tout fichier présent dans la 1re commande mais absent de la 2e est un **faux changement**.

2. Restaurer chaque faux changement :
   ```bash
   git checkout -- app/.gitignore app/proguard-rules.pro app/src/main/AndroidManifest.xml gradle.properties gradlew settings.gradle
   ```
   ⚠️ Adapte la liste au résultat réel de l'étape 1 — ne restaure pas aveuglément un fichier qui
   contient du vrai travail.

3. Créer un `.gitattributes` à la racine pour que ça ne revienne plus :
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
   *.keystore binary
   ```
   Puis :
   ```bash
   git add .gitattributes
   git commit -m "chore: normalize line endings with .gitattributes"
   ```

4. ❗ **`gradlew` doit rester en LF.** S'il passe en CRLF, il devient inexécutable sous Git Bash,
   WSL et GitHub Actions (`bad interpreter: /bin/sh^M`). C'est pour ça qu'il a sa ligne dédiée
   dans le `.gitattributes`.

### Ce qu'il ne faut PAS faire

- ❌ `git add -A` en aveugle : ça embarquerait 320 lignes de bruit sur `gradlew`.
- ❌ `git config core.autocrlf true` : ça masque le symptôme et casse `gradlew`. Le `.gitattributes` est la bonne réponse.

---

## 2. ⚠️ Piège n°2 — La branche locale suit la mauvaise branche distante

```
Branche locale : update-difficulty-levels
Upstream       : origin/update-modern-ui     ← ce n'est pas le même nom
```

Conséquence : un `git push` sans argument enverrait les 9 commits sur `origin/update-modern-ui`,
qui n'est probablement pas la branche visée.

**[STOP] Ne pousse rien avant que j'aie choisi entre :**

| Option | Commande | Effet |
|---|---|---|
| **A** (par défaut si je ne réponds pas clairement) | `git push -u origin update-difficulty-levels` | Crée `origin/update-difficulty-levels` et y pousse les 9 commits. L'upstream est recorrigé au passage. |
| **B** | `git push origin update-difficulty-levels:update-modern-ui` | Continue d'alimenter la branche distante existante `update-modern-ui`. |

Présente-moi le tableau, attends ma réponse, **n'exécute rien tant que je n'ai pas tranché.**

---

## 3. ⚠️ Piège n°3 — Identité Git possiblement non configurée

`git config user.name` et `git config user.email` semblent vides au niveau du dépôt.
S'ils ne sont pas non plus définis globalement, **le commit échouera** avec
`Author identity unknown`.

Vérifie d'abord :
```bash
git config user.name
git config user.email
git config --global user.name
git config --global user.email
```

Si tout est vide → **[STOP]**, demande-moi le nom et l'email à utiliser.
Ne configure **rien** de toi-même, et n'invente aucune identité.
Rappel : l'email doit être celui associé au compte GitHub `ShaD971` (ou son alias
`@users.noreply.github.com`) pour que les commits soient attribués correctement.

---

## 4. Étape 1 — Diagnostic (lecture seule)

N'écris rien pendant cette étape. Exécute :

```bash
git status
git diff --stat
git diff --ignore-all-space --stat
git diff --cached --stat
git log --oneline -10
git branch -vv
git remote -v
git rev-parse --abbrev-ref --symbolic-full-name @{u}
git fetch origin --dry-run
git log --oneline @{u}..HEAD     # commits locaux non poussés
git log --oneline HEAD..@{u}     # commits distants non récupérés
git status --ignored --short | head -20
```

Puis produis un **rapport de 6 lignes maximum** :

1. Nombre de fichiers avec un **vrai** changement de contenu, et lesquels.
2. Nombre de fichiers en **faux changement** (fins de ligne).
3. Nombre de fichiers **non suivis** à commiter, et lesquels.
4. Nombre de **commits locaux non poussés**.
5. Faut-il un `pull --rebase` avant de pousser ? (oui/non)
6. L'identité Git est-elle configurée ? (oui/non)

**[STOP]** Montre-moi ce rapport et attends mon feu vert avant l'étape 2.

---

## 5. Étape 2 — Nettoyage avant commit

1. Restaure les faux changements identifiés (§1.2).
2. Crée et commite le `.gitattributes` (§1.3).
3. **Vérifie qu'aucun fichier généré ou secret ne remonte.** Doivent rester exclus :
   ```
   .gradle/        build/          app/build/      captures/
   local.properties               .idea/          *.iml
   *.apk  *.aab  *.dex  *.ap_     *.keystore  *.jks
   ```
   Si l'un d'eux apparaît dans `git status` → **[STOP]**, signale-le-moi :
   c'est un trou dans `.gitignore`, pas quelque chose à commiter.
4. ❗ **`local.properties` ne doit jamais être commité** : il contient le chemin SDK de ma machine
   (`sdk.dir=C:\Dev\Android\Sdk`) et casserait le build de tout autre contributeur.
5. Si un fichier a déjà été commité par erreur dans le passé :
   `git rm --cached <fichier>` (pas `git rm`, qui le supprimerait du disque) — mais **demande-moi avant**.

---

## 6. Étape 3 — Vérification du build

Si les changements à commiter touchent **au moins un** fichier `.java`, `.kt`, `.xml`, `.gradle`
ou `.properties` :

```powershell
.\gradlew assembleDebug
.\gradlew testDebugUnitTest
```

- Les deux doivent passer. **Si l'un échoue → [STOP], ne commite rien**, montre-moi la sortie d'erreur.
- Si seuls des fichiers `.md` sont concernés, saute cette étape (dis-le explicitement).
- Si `gradlew` refuse de s'exécuter, c'est probablement le problème CRLF de §1.4.

---

## 7. Étape 4 — Commits

### Découpage

- **Un commit = une unité logique.** Pas de commit fourre-tout.
- Utilise `git add <fichiers précis>`, jamais `git add -A` ni `git add .`.
- Ici, le découpage attendu est vraisemblablement :
  1. `chore: normalize line endings with .gitattributes`
  2. `docs: add Claude Code working prompts` (les deux `PROMPT_*.md`)
  3. un commit par vrai changement de code restant, s'il y en a.

### Format des messages — Conventional Commits

Respecte le style **déjà en place** dans l'historique :

```
9b1a409 refactor: migrate to AndroidX/Material3 and clean up dead code (Phase 1)
73420b8 feat: extract testable GameEngine and define the 9 levels (Phase 2)
3eb9f40 fix: proper render-thread lifecycle, grid sync, and Canvas tiles (Phase 5.1)
3b6daa6 docs: update README, fix dark-mode/a11y/lint issues (Phase 6)
```

Règles :

- Préfixes autorisés : `feat:`, `fix:`, `refactor:`, `style:`, `test:`, `docs:`, `chore:`, `perf:`, `build:`.
- Sujet **en anglais**, à l'**impératif** (« add », pas « added » ni « adds »).
- ≤ 72 caractères, **pas de point final**, pas de majuscule après le préfixe.
- Corps de message uniquement si le « pourquoi » n'est pas évident ; ligne vide après le sujet,
  lignes de 72 caractères max.
- ❌ **Aucune signature, aucune mention de Claude ou d'IA, aucun `Co-Authored-By`,
  aucun emoji** dans les messages de commit.

### Cas particuliers

- **Aucun vrai changement à commiter ?** Dis-le simplement et passe à l'étape 5 pour pousser
  les 9 commits existants. Ne crée **jamais** de commit vide (`--allow-empty`).
- **Un hook pre-commit échoue ?** [STOP], montre-moi la sortie. Ne contourne jamais avec `--no-verify`.

---

## 8. Étape 5 — Push

```bash
git fetch origin
```

Puis applique cet arbre de décision :

| Situation (`git status -sb` / `git log`) | Action |
|---|---|
| Local en avance seulement | Pousser (option A ou B validée en §2). |
| Local **et** distant ont divergé | `git pull --rebase origin <branche>` puis re-tester le build, puis pousser. |
| Conflit pendant le rebase | **[STOP]** — liste les fichiers en conflit et attends-moi. Ne résous rien seul. Si je te le demande, `git rebase --abort` remet tout en place. |
| Distant en avance seulement | `git pull --rebase`, rien à pousser. |
| Tout est synchro | Ne fais rien, dis-le. |

Après le push réussi, affiche :

- l'URL de la branche : `https://github.com/ShaD971/intelligence_Workout/tree/<branche>`
- l'URL de création de PR si Git la retourne dans sa sortie.

### Si l'authentification échoue

Symptômes : `Authentication failed`, `could not read Username`, `403`, prompt bloqué.

→ **[STOP]**, préviens-moi. Ne tente **pas** de :
- installer `gh` (GitHub CLI) ou un credential helper,
- modifier l'URL du remote pour y injecter un token,
- chercher un token dans mes fichiers ou variables d'environnement.

Je gère mes identifiants moi-même.

---

## 9. Étape 6 — Vérification finale

```bash
git status                      # doit être "working tree clean"
git log --oneline -5
git log --oneline @{u}..HEAD    # doit être VIDE
git branch -vv                  # l'upstream doit être cohérent avec le nom de la branche
```

Confirme en une phrase que local et distant sont synchronisés, et rappelle le nombre de commits poussés.

---

## 10. Règles absolues

- ❌ Jamais `git push --force` ni `--force-with-lease` sans mon accord écrit explicite.
- ❌ Jamais `git reset --hard`, `git clean -fd`, `git checkout .` global, ni `git stash drop`.
- ❌ Jamais réécrire un commit déjà présent sur `origin` (`--amend`, `rebase -i`, `filter-branch`).
- ❌ Jamais pousser directement sur `master`.
- ❌ Jamais `--no-verify`.
- ❌ Jamais supprimer une branche, locale ou distante.
- ❌ Ne modifie pas `.git/config` autrement que via `git branch --set-upstream-to`.
- ✅ En cas de doute sur la branche, le découpage ou un fichier : **demande, ne devine pas.**
- ✅ Avant toute commande destructive ou irréversible, annonce-la et attends validation.

---

## 11. Après le push — à me proposer, pas à exécuter

Une fois tout poussé, demande-moi si je veux :

1. **Ouvrir une PR** vers `master` avec un résumé des 9 commits (phases 1 → 6 de la refonte).
2. **Consolider les branches** : `update-android-modernization`, `update-modern-ui` et
   `update-difficulty-levels` semblent couvrir le même chantier — voir s'il faut en fusionner
   ou en supprimer certaines.
3. **Nettoyer le `.gitignore`** : il contient des exceptions devenues inutiles
   (`!/build/modernization_report.txt`, `!/build/difficulty_update_report.txt`,
   `!/build/ui_gameplay_update_report.txt`, `!/build/update_report.txt` — cette dernière étant
   d'ailleurs dupliquée).
