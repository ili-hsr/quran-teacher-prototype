# Architecture de معلّم القرآن

## Principe directeur

L'application est un **professeur** hors ligne, pas un lecteur de Coran.
Trois règles non négociables :

1. **Accessibilité d'abord.** Rien n'existe à l'écran sans équivalent vocal.
   Toute action est atteignable au focus TalkBack et à la voix.
2. **Aucune synthèse vocale sur un verset.** La voix du professeur sert aux
   consignes uniquement. Les versets doivent venir d'enregistrements
   authentiques (jamais un TTS).
3. **Hors ligne par défaut.** Aucun téléchargement, aucun compte, aucune
   dépendance réseau pour les fonctions essentielles.

## Cible

| Élément | Valeur |
|---|---|
| Téléphone | Samsung Galaxy XCover 4, Android 9 |
| Mémoire | 2 Go de RAM, 32 Go de stockage + carte SD |
| Langage | Java 17, Android natif |
| Build | Gradle 8.9, AGP 8.7.3, minSdk 28, targetSdk 28 |
| Dépendances | aucune bibliothèque externe (poids et mémoire maîtrisés) |

## Trois langues indépendantes

C'est le point structurant : l'interface, la voix du professeur et la
reconnaissance vocale sont trois réglages distincts.

```
AppPrefs.uiLang()       -> locale appliqué dans attachBaseContext (toute l'UI)
AppPrefs.teacherLang()  -> contexte séparé pour les phrases du professeur
AppPrefs.recogLang()    -> locale passé au moteur de reconnaissance
```

Exemples possibles : interface française + professeur arabe + reconnaissance
arabe (cas de l'utilisatrice), ou interface néerlandaise + professeur français.

Les phrases du professeur vivent dans les mêmes `strings.xml` mais
`BaseActivity.say()` les résout dans `teacherCtx`, donc dans SA langue.

## Fichiers

```
app/src/main/java/org/example/quranteacher/
  BaseActivity.java      écran commun : voix, annonces, navigation, micro, temps
  MainActivity.java      accueil
  LessonActivity.java    apprentissage verset par verset
  MemoryActivity.java    mémorisation (verset caché, validation)
  ExamActivity.java      examen oral avec auto-évaluation
  ProgressActivity.java  tableau de bord
  SettingsActivity.java  langues et voix
  AppPrefs.java          les trois langues + réglages de voix
  TeacherVoice.java      synthèse vocale (file d'attente, repli de langue)
  CommandKeywords.java   commandes vocales multilingues (4 langues à la fois)
  ProgressStore.java     sauvegarde locale (commit, pas apply)
  QuranData.java         texte coranique embarqué
  Ui.java                style + accessibilité en un seul endroit
app/src/main/res/values/      français (défaut)
app/src/main/res/values-ar/   arabe (RTL)
app/src/main/res/values-en/   anglais
app/src/main/res/values-nl/   néerlandais
```

Ajouter une langue = ajouter `values-xx/strings.xml` et une entrée dans les
listes de `SettingsActivity` et `CommandKeywords.LOCALES`. Rien d'autre.

## Choix techniques et raisons

- **Une Activity par écran** plutôt qu'un routeur maison : TalkBack annonce
  correctement le changement de fenêtre, et le bouton Retour du téléphone
  fonctionne sans code spécifique.
- **Écran construit une seule fois.** La version précédente reconstruisait tout
  l'arbre de vues à chaque appui : le focus TalkBack repartait en haut et la
  consigne était répétée. Désormais on modifie les `TextView` en place.
- **Annonce unique.** Soit le professeur parle, soit TalkBack annonce, jamais
  les deux : deux voix simultanées rendent l'application inutilisable.
- **`commit()` et non `apply()`** pour la progression : la donnée doit survivre
  à une coupure de batterie.
- **Texte arabe en `TEXT_DIRECTION_FIRST_STRONG_RTL`** : il reste correctement
  orienté dans une interface française.

## Étapes

| Étape | Contenu | État |
|---|---|---|
| 1 | Fondations : TalkBack, navigation, sauvegarde, architecture, 4 langues | fait |
| 2 | Coran audio : récitations, SAF carte SD, verset par verset | à faire |
| 3 | Reconnaissance vocale hors ligne (moteur local évalué pour 2 Go) | à faire |
| 4 | Mémorisation intelligente, répétitions, révisions espacées | à faire |
| 5 | Examens analysés, continuité de récitation | à faire |
| 6 | Objectifs quotidiens, accompagnement familial, optimisation | à faire |

## Suivi des progrès

`ProgressStore` conserve : répétitions par verset, versets mémorisés,
répétitions du jour, objectif quotidien, temps d'apprentissage, résultats
d'examen (nombre, moyenne, meilleur score). Tout est local, dans
`SharedPreferences` versionné (`quran_progress_v2`).
