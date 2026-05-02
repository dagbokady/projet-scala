# Module 1 — Gestion des Étudiants

## Responsable
Branche : `feat/etudiants`

## Description
Ce module gère toutes les opérations sur les étudiants de l'université :
création, modification, recherche par matricule, filtrage par filière/niveau/statut,
comptage et statistiques.

## Structure du projet

```
backend/
├── build.sbt                          ← Configuration sbt
├── .gitignore
├── project/
│   ├── build.properties               ← Version sbt
│   └── plugins.sbt                    ← Plugin sbt-assembly
│
├── data/
│   └── etudiants.csv                  ← Données des 10 étudiants
│
├── src/
│   ├── main/scala/universite/
│   │   ├── MainEtudiants.scala        ← Point d'entrée
│   │   ├── model/
│   │   │   ├── Traits.scala           ← Identifiable, Affichable, Validable...
│   │   │   └── Etudiant.scala         ← case class Etudiant + StatutEtudiant
│   │   ├── service/
│   │   │   └── EtudiantService.scala  ← Logique métier complète
│   │   ├── repository/                ← (réservé aux autres modules)
│   │   ├── bigdata/                   ← (réservé module Big Data)
│   │   └── dashboard/                 ← (réservé module tableau de bord)
│   └── test/scala/universite/         ← Tests unitaires
│
└── output/
    ├── rapports/                      ← Rapports générés
    ├── statistiques/                  ← Exports stats
    └── exports/                       ← Exports CSV/Parquet
```

## Notions Scala couvertes

| Notion          | Utilisation dans ce module                          |
|-----------------|-----------------------------------------------------|
| `sealed trait`  | `StatutEtudiant` (Actif, Suspendu, Diplome)         |
| `case class`    | `Etudiant`                                          |
| `Option`        | `rechercherParMatricule` → `Some/None`              |
| `Try`           | Lecture du fichier CSV                              |
| Pattern matching| Analyse du statut, traitement `Some/None`           |
| Récursivité     | `compterActifsRecursif`, `afficherFiliereRecursif`  |
| `List`          | `List[Etudiant]`                                    |
| `Map`           | `nbParFiliere : Map[String, Int]`                   |
| `filter`        | `parFiliere`, `parNiveau`, `parStatut`              |
| `map`           | Transformation de collections                       |
| Traits          | `Identifiable`, `Affichable`, `Validable`           |

## Lancer le projet

```bash
# Dans le dossier backend/
sbt run
# ou directement dans IntelliJ : clic droit MainEtudiants → Run
```
