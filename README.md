# Killer Duel

Jeu de **Killer Sudoku** pour Android, avec deux modes : entraînement en solo et
duel contre un adversaire. Clone d'apprentissage inspiré du Killer Sudoku
d'Oakever Games pour son langage visuel et ses réglages de difficulté.

## Les deux modes

**Entraînement** — vous choisissez le niveau (Facile, Moyen, Difficile, Killer)
et vous prenez votre temps. Chaque partie terminée est enregistrée avec son
déroulé complet.

**Duel** — même grille pour vous et pour l'adversaire, le premier qui la remplit
gagne. Quitter en cours de route vaut forfait.

## Saisie

Une **réponse juste est acquise** : la case ne se modifie plus, ne s'efface plus,
et un chiffre armé ne peut pas la reprendre. Seule l'annulation revient dessus.
Une réponse fausse, elle, reste ouverte — la reposer à l'identique l'efface.

L'interrupteur **« Chiffre d'abord »**, sous le pavé, inverse l'ordre de saisie :
on arme un chiffre au pavé (il s'allume), puis chaque case touchée le reçoit.
Changer de chiffre se fait au pavé, sans repasser par l'interrupteur ; réappuyer
sur le chiffre armé le désarme. La préférence se souvient d'une partie à l'autre.

## L'adversaire

Un duel en ligne suppose un serveur et des joueurs connectés au même moment :
hors de portée en développement. L'adversaire est donc simulé, mais pas
arbitrairement.

Il ne résout rien. À la génération, le solveur enregistre **l'ordre dans lequel
les cases tombent** pour quelqu'un qui raisonne, et le **coût** de chaque
déduction. L'adversaire suit cet ordre et distribue son temps en conséquence :
une case évidente part vite, une case retorse le retient, du bruit et des pauses
de relecture s'ajoutent. Le tout est calibré pour finir dans un temps plausible
selon le niveau et la force tirée au sort.

### Le rejeu de vos propres parties

Le rythme est isolé dans `PaceProfile` — la simple liste des intervalles entre
coups, **indépendante de la grille**. C'est ce qui permet à
`ReplayOpponentEngine` de rejouer la cadence d'une partie d'entraînement
réellement jouée sur une grille inédite : mêmes hésitations, même temps total,
mais sur un problème que ce joueur n'a jamais vu.

`OpponentPicker` privilégie déjà le rejeu dès qu'un historique existe pour le
niveau ; l'adversaire synthétique n'est que le repli quand vous n'avez encore
rien joué.

## Architecture

```
core/       moteur pur, sans Android — testable en JVM
  Model            grille, cages, géométrie, masques de bits
  Combinations     table des sommes de chiffres distincts (512 sous-ensembles)
  KillerSolver     propagation de contraintes + recherche bornée
  PuzzleGenerator  solution, découpage en cages, retrait sous contrôle d'unicité

opponent/   adversaire — dépend de core, pas d'Android
  Opponent                 contrats : profil, plan, rythme
  SyntheticOpponentEngine  rythme fabriqué
  ReplayOpponentEngine     rythme extrait d'une partie réelle
  OpponentPicker           choisit la source

game/       état et règles
  GameState      transformations pures, testables sans émulateur
  GameViewModel  horloge, navigation, effets de bord

data/       persistance DataStore + JSON
ui/         Compose : grille au Canvas, écrans, thème
```

### Le solveur

Trois techniques, appliquées jusqu'au point fixe :

1. **Singles nus et cachés** — le sudoku classique.
2. **Combinaisons de cages** — pour chaque cage, l'ensemble des jeux de chiffres
   distincts atteignant la somme restante, filtrés par un matching bijectif
   entre chiffres et cases.
3. **Règle des 45** — dans chaque ligne, colonne et région, la somme vaut 45.
   Les cases non couvertes par les cages entièrement incluses forment un groupe
   de somme connue, traité comme une cage virtuelle.

La troisième change tout : elle suffit souvent à résoudre une grille **sans
aucun chiffre donné**, ce qui rend le niveau Killer réalisable. Sans elle, la
recherche explosait.

La recherche en profondeur est bornée par un budget de nœuds. Un découpage qui
dépasse ce budget produirait une grille insoluble à la main : on en essaie un
autre, c'est bien moins cher.

Le découpage lui-même est biaisé : une cage a sept chances sur dix de franchir
la frontière de sa région 3×3. Une cage à cheval laisse des innies dans
plusieurs unités à la fois, donc plus de prise à la règle des 45. Mesuré, ce
seul biais divise par trois le temps de génération d'une grille Killer — le
biais inverse, qui semblait pourtant plus naturel, le multipliait par cinq.

### Contrat de difficulté

| Niveau | Cages | Chiffres donnés | Exigence |
|---|---|---|---|
| Facile | 2–3 | ~32 | résoluble sans aucune recherche |
| Moyen | 2–4 | ~22 | résoluble sans aucune recherche |
| Difficile | 2–4 | ~10 | solution unique |
| Killer | 2–5 | **0** | solution unique, aucune cage d'une case |

Aucun niveau ne comporte de cage d'une seule case : elle offrirait son chiffre.

Coût de génération mesuré par la suite de tests (JVM de bureau) : 1 à 3 ms par
grille, et pour le niveau Killer, sur 40 grilles, **146 ms en médiane, 1,3 s au
pire** — le découpage doit y déterminer la grille à lui seul, ce qui demande
plusieurs essais. La génération tourne hors du thread principal, derrière un
écran de composition.

## Construire et installer

```bash
export ANDROID_HOME=$HOME/Library/Android/sdk
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./gradlew :app:assembleDebug
$ANDROID_HOME/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Tests unitaires (aucun appareil requis) :

```bash
./gradlew :app:testDebugUnitTest
```

## Suite

- Ajuster la force de l'adversaire sur le niveau réel du joueur plutôt que sur
  un tirage.
- Rejouer aussi les *erreurs* d'une partie enregistrée : aujourd'hui seuls les
  coups justes alimentent l'historique.
- Reprendre un duel interrompu, ce qui suppose de sérialiser le plan adverse.
