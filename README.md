# معلّم القرآن — Professeur de Coran hors ligne

Application Android native (Java) pour apprendre, mémoriser et réviser le Coran
sans Internet, conçue pour une utilisatrice totalement aveugle.

- Interface : français, arabe, anglais, néerlandais (RTL pris en charge).
- Langue du professeur indépendante de celle de l'interface.
- Navigation complète à TalkBack, commandes vocales multilingues.
- Aucune récitation coranique générée par synthèse vocale.
- Cible : Android 9, 2 Go de RAM, carte SD.

Architecture et feuille de route : [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Compiler

Le dépôt compile via GitHub Actions (`.github/workflows/build-apk.yml`) :
chaque envoi sur `main` produit `QuranTeacher-prototype-debug` dans les
artefacts du workflow. Aucune installation locale n'est nécessaire.
