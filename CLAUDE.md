# Killer Duel — notes de travail

Jeu de Killer Sudoku Android (Kotlin + Jetpack Compose). Voir `README.md` pour
l'architecture et le fonctionnement de l'adversaire.

## Environnement

Le shell non interactif n'a pas l'environnement de build : exporter à chaque fois.

```bash
export LANG=en_US.UTF-8
export ANDROID_HOME=$HOME/Library/Android/sdk
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

`timeout` n'existe pas sur macOS — ne pas l'utiliser pour borner une commande.

## Commandes

```bash
./gradlew :app:testDebugUnitTest    # moteur, adversaire, règles — sans appareil
./gradlew :app:lintDebug            # doit rester sans erreur
./gradlew :app:assembleDebug
```

Vérification à l'écran : l'appareil physique est souvent verrouillé, l'émulateur
`KillerTest` (arm64, API 34) sert aux captures.

```bash
$ANDROID_HOME/platform-tools/adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
$ANDROID_HOME/platform-tools/adb -s emulator-5554 exec-out screencap -p > /tmp/shot.png
```

L'identifiant applicatif en debug porte le suffixe `.debug` :
`com.killerduel.app.debug/com.killerduel.app.MainActivity`.

## Ce à quoi faire attention

- **`core/` et `opponent/` ne dépendent pas d'Android.** C'est ce qui rend le
  moteur testable en JVM ; ne pas y introduire de `Context` ni de `android.*`.
- **Toute recherche du solveur doit rester bornée** par `nodeBudget`. Une
  recherche libre sur une grille Killer sans chiffre donné ne termine pas en
  temps utile.
- **La règle des 45** (`virtualCages`) porte l'essentiel de la puissance du
  solveur. La retirer fait exploser la génération.
- **`PaceProfile` ne doit pas dépendre de la grille.** C'est ce qui permet de
  rejouer une partie enregistrée sur une grille inédite.
- Les transformations de `GameState.kt` sont **pures** : les tester là plutôt
  qu'au travers du ViewModel.
- Les commentaires expliquent **pourquoi**, jamais ce que le code dit déjà.
