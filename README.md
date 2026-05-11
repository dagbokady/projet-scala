# Plateforme universitaire — Tous les modules

Application Scala connectée à PostgreSQL pour la **gestion universitaire complète** :
référentiels (étudiants, enseignants, formations), inscriptions, notes, absences,
emplois du temps, paiements, tableau de bord et exports Big Data.

> Projet pédagogique conforme au cahier des charges « Plateforme intelligente de gestion universitaire » (modules 1 à 10).

---

## 1. Architecture

```
┌──────────────────────────────────────┐
│   Frontend HTML/CSS/JS (10 vues)     │
└────────────────┬─────────────────────┘
                 │  fetch /api/*
                 ▼
┌──────────────────────────────────────┐
│   Akka HTTP (port 8080)              │
│   /api/etudiants  /api/enseignants   │
│   /api/filieres   /api/matieres      │
│   /api/inscriptions  /api/notes      │
│   /api/absences    /api/seances      │
│   /api/paiements   /api/dashboard    │
│   /api/bigdata                       │
└────────────────┬─────────────────────┘
                 │
                 ▼
┌──────────────────────────────────────┐
│   Services métier (Scala)            │
│   • récursion @tailrec               │
│   • Option / Try                     │
│   • foldLeft, groupBy, map/filter    │
│   • pattern matching                 │
│   • héritage + traits                │
└────────────────┬─────────────────────┘
                 │ JDBC + HikariCP
                 ▼
┌──────────────────────────────────────┐
│   PostgreSQL — gestion_universite    │
└──────────────────────────────────────┘
```

**Stack** : Scala 2.13 · sbt 1.9 · Akka HTTP 10.5 · PostgreSQL 14+ · HikariCP · spray-json.

---

## 2. Les 10 modules implémentés

| Module | Nom | Endpoints clés |
|---|---|---|
| **1** | Étudiants | `GET/POST /api/etudiants`, `GET /api/etudiants/stats`, `PATCH /api/etudiants/{m}/statut` |
| **2** | Enseignants | `GET/POST /api/enseignants`, `GET /api/enseignants/{id}/cours`, `GET /api/enseignants/stats` |
| **3** | Formations | `GET/POST /api/filieres`, `GET/POST /api/matieres`, `GET /api/matieres/par-ue` |
| **4** | Inscriptions | `GET/POST /api/inscriptions`, `PATCH /api/inscriptions/{id}/statut` |
| **5** | Notes | `GET/POST /api/notes`, `GET /api/notes/classement`, `GET /api/notes/echec` |
| **6** | Absences | `GET/POST /api/absences`, `GET /api/absences/alerte`, `PATCH /api/absences/{id}/justifier` |
| **7** | Emplois du temps | `GET/POST /api/seances`, `POST /api/seances/verifier`, `GET /api/seances/conflits` |
| **8** | Paiements | `GET/POST /api/paiements`, `GET /api/paiements/synthese`, `GET /api/paiements/dette` |
| **9** | Tableau de bord | `GET /api/dashboard/synthese`, `/reussite`, `/financier`, `/risque`, `/topEnseignants` |
| **10** | Big Data | `POST /api/bigdata/export`, `GET /api/bigdata/promotion`, `/manquantes`, `/absences-mois` |

---

## 3. Notions Scala mises en œuvre (toutes exigées par le PDF)

| Notion | Où la voir |
|---|---|
| **Trait** | `model/Traits.scala` — `Identifiable`, `Affichable`, `Validable`, `Calculable`, `Recherchable` |
| **Héritage** | `Personne` → `Etudiant`, `Enseignant` |
| **Pattern matching** | `StatutEtudiant`, `StatutInscription`, `ModePaiement`, `DecisionAcademique`, `Try` |
| **Option** | `Note.moyenneOption`, `Etudiant.dateNaissance`, `Paiement.datePaiement` |
| **Try** | Toutes les opérations BDD via `Database.withConnection[A]: Try[A]` |
| **Récursivité (`@tailrec`)** | `NoteService.sommeRecursive`, `AbsenceService.totalHeuresRecursive`, `PaiementService.totalPayeRecursif`, `EtudiantService.chercherRecursif`, `EtudiantService.compterActifsRecursif` |
| **Fonctions d'ordre supérieur** | `foldLeft` pour pondération, `map`/`filter`/`groupBy` partout |
| **Collections** | `List`, `Set` (filières uniques), `Map` (associations matière→moyenne) |
| **Organisation en packages** | `model`, `repository`, `service`, `api`, `db`, `util` |
| **sealed traits** | `StatutEtudiant`, `StatutInscription`, `ModePaiement`, `DecisionAcademique` |

---

## 4. Prérequis

- **JDK 11+** (testé avec JDK 21)
- **sbt 1.9+**
- **PostgreSQL 14+**

---

## 5. Démarrage

```bash
# Préparer la BDD
createdb -U postgres gestion_universite
psql -U postgres -d gestion_universite -f sql/01_schema.sql

# Lancer l'application
sbt run
```

→ ouvrir **http://localhost:8080/**

L'interface a une **sidebar à 10 modules** organisée en sections : Pilotage,
Référentiels, Académique, Finance & Data.

---

## 6. Indicateurs décisionnels disponibles (Module 9)

- **Synthèse globale** : nombre d'étudiants/enseignants/filières/matières + moyennes/taux
- **Top 5 étudiants** classement pondéré
- **Étudiants à risque** : moyenne < 10 OU absences ≥ 10 h
- **Taux de réussite par filière**
- **Filière au meilleur taux de réussite**
- **Matière la plus difficile**
- **Top enseignants par volume horaire**
- **Synthèse financière par filière** (attendu, encaissé, taux de recouvrement)
- **Taux d'absentéisme par filière**

---

## 7. Module Big Data (Module 10)

Le module exporte des fichiers CSV dans `./output/` :
- `rapport_academique.csv` — vue à plat de tous les étudiants avec leur situation
- `performances_par_matiere.csv` — moyennes triées par matière
- `indicateurs_financiers.csv` — synthèse par filière

Et expose des analyses agrégées :
- **Performance par promotion** (`/api/bigdata/promotion`)
- **Tendance des absences par mois** (`/api/bigdata/absences-mois`)
- **Tendance des paiements par mois** (`/api/bigdata/paiements-mois`)
- **Détection de valeurs manquantes** (`/api/bigdata/manquantes`)

> Spark est déclaré en `provided` dans `build.sbt` et peut être activé pour scaler. Le code utilise `groupBy/foldLeft/map/filter` qui se traduisent directement en `DataFrame`.

---

## 8. Tests rapides via curl

```bash
# Synthèse du tableau de bord
curl http://localhost:8080/api/dashboard/synthese | python3 -m json.tool

# Liste des étudiants en filière Informatique
curl "http://localhost:8080/api/etudiants?filiere=Informatique"

# Cours assurés par un enseignant
curl http://localhost:8080/api/enseignants/ENS001/cours

# Reste à payer pour un étudiant
curl http://localhost:8080/api/paiements/etudiant/ETU003/reste

# Étudiants à risque (moyenne <10 ou absences >= 10h)
curl "http://localhost:8080/api/dashboard/risque?seuil=10"

# Lancer tous les exports Big Data
curl -X POST http://localhost:8080/api/bigdata/export
```

---

## 9. Structure du projet

```
gestion-universitaire-scala/
├── README.md
├── build.sbt
├── project/build.properties
├── sql/01_schema.sql                     ← schéma + données initiales
├── src/main/
│   ├── resources/{application.conf, logback.xml}
│   └── scala/universite/
│       ├── Main.scala                    ← point d'entrée Akka HTTP
│       ├── model/                        ← 8 fichiers
│       ├── db/Database.scala             ← pool HikariCP
│       ├── repository/                   ← 7 repositories
│       ├── service/                      ← 10 services métier
│       ├── api/                          ← 12 routes
│       └── util/CorsHandler.scala
├── frontend/
│   ├── index.html                        ← 10 vues + sidebar
│   ├── css/style.css
│   └── js/                               ← 13 modules JS
└── output/                               ← exports Big Data générés
```

# 📘 Documentation technique — Plateforme universitaire Scala

> Cette documentation décrit le projet depuis l'architecture jusqu'au détail de chaque fichier et fonction. Elle est conçue pour qu'un développeur débarquant sur le projet puisse comprendre chaque ligne de code en 1 à 2 heures de lecture.

---

## 📑 Table des matières

1. [Vue d'ensemble](#1-vue-densemble)
2. [Stack technique](#2-stack-technique)
3. [Architecture en couches](#3-architecture-en-couches)
4. [Structure des dossiers](#4-structure-des-dossiers)
5. [Configuration et démarrage](#5-configuration-et-démarrage)
6. [Base de données](#6-base-de-données)
7. [Couche modèle (`model/`)](#7-couche-modèle-model)
8. [Couche base de données (`db/`)](#8-couche-base-de-données-db)
9. [Couche repository (`repository/`)](#9-couche-repository-repository)
10. [Couche service (`service/`)](#10-couche-service-service)
11. [Couche API (`api/`)](#11-couche-api-api)
12. [Point d'entrée (`Main.scala`)](#12-point-dentrée-mainscala)
13. [Frontend (`frontend/`)](#13-frontend-frontend)
14. [Notions Scala mises en œuvre](#14-notions-scala-mises-en-œuvre)
15. [Guide de modifications courantes](#15-guide-de-modifications-courantes)

---

## 1. Vue d'ensemble

### Objectif
Cette application est une **plateforme intelligente de gestion universitaire** qui couvre **10 modules fonctionnels** : étudiants, enseignants, formations, inscriptions, notes, absences, emplois du temps, paiements, tableau de bord et Big Data.

### Type d'application
C'est une application **client-serveur monolithique** :
- **Serveur** : un programme Scala qui écoute sur `localhost:8080` et expose une API REST + sert les fichiers HTML/CSS/JS du frontend.
- **Client** : un navigateur web qui consomme cette API en JavaScript.
- **Données** : tout est stocké dans une base PostgreSQL locale.

### Flux d'une requête utilisateur
Quand l'utilisateur clique sur "Voir les notes" :

```
[Navigateur]           → fetch GET /api/notes
[Akka HTTP server]     → matche la route /api/notes
[NoteRoutes]           → délègue à NoteService
[NoteService]          → délègue à NoteRepository
[NoteRepository]       → exécute "SELECT * FROM note" via JDBC
[PostgreSQL]           → renvoie les lignes
[NoteRepository]       → reconstruit des objets Note
[NoteService]          → applique éventuellement de la logique métier
[NoteRoutes]           → sérialise en JSON
[Akka HTTP server]     → renvoie la réponse HTTP
[Navigateur]           → JavaScript reçoit le JSON et l'affiche
```

---

## 2. Stack technique

### Backend (Scala)

| Outil | Version | Rôle |
|---|---|---|
| **Scala** | 2.13.12 | Langage de programmation fonctionnelle |
| **sbt** | 1.9.8 | Outil de build (équivalent de npm/maven) |
| **Akka HTTP** | 10.5.3 | Serveur HTTP + routage REST |
| **spray-json** | 1.3.6 | Sérialisation JSON ↔ objets Scala |
| **HikariCP** | 5.1.0 | Pool de connexions à PostgreSQL |
| **PostgreSQL JDBC** | 42.7.1 | Pilote pour communiquer avec PostgreSQL |
| **logback** | 1.4.14 | Système de logs (équivalent de console.log mais structuré) |
| **Typesafe Config** | 1.4.3 | Lecture du fichier `application.conf` |

### Base de données
- **PostgreSQL 14+** : moteur de base relationnelle libre
- 8 tables : `etudiant`, `enseignant`, `filiere`, `matiere`, `salle`, `inscription`, `note`, `absence`, `seance_cours`, `paiement`

### Frontend
- **HTML5/CSS3/JavaScript ES6** (pas de framework, pas de transpilation)
- **Fetch API** pour les appels HTTP
- **Google Fonts** : Fraunces (serif), Geist (sans-serif), JetBrains Mono (mono)

---

## 3. Architecture en couches

Le projet suit un **modèle en 4 couches** où chaque couche a une responsabilité unique :

```
┌──────────────────────────────────────────┐
│  FRONTEND (frontend/)                    │
│  HTML + CSS + 13 modules JavaScript      │
│  ─────────────────────────────────────── │
│  Affiche les données, gère les clics,   │
│  envoie des requêtes HTTP.               │
└────────────────┬─────────────────────────┘
                 │ fetch() vers /api/*
                 ▼
┌──────────────────────────────────────────┐
│  COUCHE API (universite/api/)            │
│  12 fichiers : *Routes.scala + JsonFormats│
│  ─────────────────────────────────────── │
│  Définit les URLs (/api/notes, etc.)    │
│  Parse les requêtes HTTP, convertit en  │
│  appel de méthode service, sérialise la │
│  réponse en JSON.                        │
└────────────────┬─────────────────────────┘
                 │ appel direct de méthode
                 ▼
┌──────────────────────────────────────────┐
│  COUCHE SERVICE (universite/service/)    │
│  10 fichiers : *Service.scala            │
│  ─────────────────────────────────────── │
│  Logique métier : calculs de moyennes,  │
│  détection de conflits, agrégations,    │
│  règles de validation.                   │
└────────────────┬─────────────────────────┘
                 │ appel direct de méthode
                 ▼
┌──────────────────────────────────────────┐
│  COUCHE REPOSITORY (universite/repository)│
│  7 fichiers : *Repository.scala          │
│  ─────────────────────────────────────── │
│  Accès BDD : SELECT, INSERT, UPDATE,    │
│  DELETE. Convertit ResultSet ↔ objets.  │
└────────────────┬─────────────────────────┘
                 │ JDBC via HikariCP pool
                 ▼
┌──────────────────────────────────────────┐
│  POSTGRESQL                              │
└──────────────────────────────────────────┘
```

### Pourquoi cette séparation ?

| Couche | Avantage |
|---|---|
| **Routes / API** | On peut changer d'implémentation HTTP (passer à Play Framework par ex.) sans toucher la logique métier |
| **Service** | Pour tester la logique métier, on n'a besoin que d'un faux repository, pas d'une vraie BDD |
| **Repository** | Si on passe à MongoDB plus tard, on ne réécrit que cette couche |
| **Model** | Pas de dépendance externe ; les classes métier restent pures |

---

## 4. Structure des dossiers

```
gestion-universitaire-scala/
│
├── build.sbt                           ← Configuration sbt + dépendances
├── README.md                           ← Quick start
├── DOCUMENTATION.md                    ← Ce fichier
├── .gitignore
│
├── project/
│   └── build.properties                ← Version de sbt (1.9.8)
│
├── sql/
│   └── 01_schema.sql                   ← Schéma de la BDD + données initiales
│
├── src/main/
│   ├── resources/
│   │   ├── application.conf            ← Config BDD + serveur (port, host)
│   │   └── logback.xml                 ← Config des logs
│   │
│   └── scala/universite/
│       ├── Main.scala                  ← Point d'entrée du programme
│       │
│       ├── model/                      ← Classes métier
│       │   ├── Traits.scala            ← 5 traits réutilisables
│       │   ├── Personnes.scala         ← Personne, Etudiant, Enseignant
│       │   ├── Referentiels.scala      ← Filiere, Matiere, Salle
│       │   ├── Note.scala
│       │   ├── Absence.scala
│       │   ├── SeanceCours.scala
│       │   ├── Inscription.scala
│       │   └── Paiement.scala
│       │
│       ├── db/
│       │   └── Database.scala          ← Pool HikariCP + helper Try
│       │
│       ├── repository/                 ← Couche d'accès BDD
│       │   ├── EtudiantRepository.scala
│       │   ├── ReferentielRepositories.scala  (3 repos : Matiere, Enseignant, Salle, Filiere)
│       │   ├── NoteRepository.scala
│       │   ├── AbsenceRepository.scala
│       │   ├── SeanceRepository.scala
│       │   ├── InscriptionRepository.scala
│       │   └── PaiementRepository.scala
│       │
│       ├── service/                    ← Logique métier
│       │   ├── EtudiantService.scala
│       │   ├── EnseignantService.scala
│       │   ├── FormationService.scala
│       │   ├── InscriptionService.scala
│       │   ├── NoteService.scala
│       │   ├── AbsenceService.scala
│       │   ├── EmploiDuTempsService.scala
│       │   ├── PaiementService.scala
│       │   ├── TableauDeBordService.scala
│       │   └── BigDataService.scala
│       │
│       ├── api/                        ← Routes HTTP
│       │   ├── JsonFormats.scala       ← Sérialisation JSON
│       │   ├── EtudiantRoutes.scala
│       │   ├── EnseignantRoutes.scala
│       │   ├── FormationRoutes.scala
│       │   ├── InscriptionRoutes.scala
│       │   ├── NoteRoutes.scala
│       │   ├── AbsenceRoutes.scala
│       │   ├── SeanceRoutes.scala
│       │   ├── PaiementRoutes.scala
│       │   ├── TableauDeBordRoutes.scala
│       │   ├── BigDataRoutes.scala
│       │   └── ReferentielRoutes.scala  ← Listes brutes pour les <select>
│       │
│       └── util/
│           └── CorsHandler.scala       ← Gestion CORS (cross-origin)
│
└── frontend/
    ├── index.html                      ← Une seule page (SPA)
    ├── css/style.css                   ← Tout le style
    └── js/                             ← 13 modules JavaScript
        ├── api.js                      ← Wrapper fetch (toutes les routes)
        ├── utils.js                    ← Helpers (modal, toast, formatage)
        ├── app.js                      ← Routing entre vues + bootstrap
        ├── dashboard.js                ← Module 9
        ├── etudiants.js                ← Module 1
        ├── enseignants.js              ← Module 2
        ├── formations.js               ← Module 3
        ├── inscriptions.js             ← Module 4
        ├── notes.js                    ← Module 5
        ├── absences.js                 ← Module 6
        ├── emplois.js                  ← Module 7
        ├── paiements.js                ← Module 8
        └── bigdata.js                  ← Module 10
```

---

## 5. Configuration et démarrage

### Fichier `build.sbt`
```scala
name := "gestion-universitaire-scala"
version := "1.0.0"
scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  "com.typesafe.akka"  %% "akka-actor-typed"      % "2.8.5",
  "com.typesafe.akka"  %% "akka-stream"           % "2.8.5",
  "com.typesafe.akka"  %% "akka-http"             % "10.5.3",
  "com.typesafe.akka"  %% "akka-http-spray-json"  % "10.5.3",
  "org.postgresql"     %  "postgresql"            % "42.7.1",
  "com.zaxxer"         %  "HikariCP"              % "5.1.0",
  "ch.qos.logback"     %  "logback-classic"       % "1.4.14"
)

Compile / run / fork := true     // Lance une nouvelle JVM (sinon sbt tue le serveur)
Compile / run / connectInput := true
```

**Explication ligne par ligne** :
- `name`, `version`, `scalaVersion` : métadonnées
- `libraryDependencies` : bibliothèques tierces téléchargées par sbt
- `%%` (double pourcent) : sbt ajoute automatiquement le suffixe `_2.13` (version Scala)
- `%` (simple) : pas de suffixe Scala (bibliothèque Java pure)
- `fork := true` : crée un sous-processus pour que sbt rende la main pendant que le serveur tourne. **Sans ça, le serveur s'arrête immédiatement** car sbt considère que `run` est terminé.

### Fichier `application.conf`
```hocon
db {
  url      = "jdbc:postgresql://localhost:5432/gestion_universite"
  user     = "postgres"
  password = "postgres"
  poolSize = 10
}

server {
  host = "0.0.0.0"   # écoute sur toutes les interfaces réseau
  port = 8080
}

akka {
  http.server.preview.enable-http2 = on
}
```

C'est le **HOCON** (Human-Optimized Config Object Notation), un format JSON-like utilisé par les libs de l'écosystème Typesafe.

### Commandes
```bash
# Préparer la base
createdb -U postgres gestion_universite
psql -U postgres -d gestion_universite -f sql/01_schema.sql

# Compiler et lancer
sbt run                    # → http://localhost:8080/
sbt compile                # juste compiler
sbt clean                  # nettoyer le cache
sbt test                   # lancer les tests (si présents)
```

---

## 6. Base de données

### Schéma relationnel

```
┌──────────┐     ┌────────────┐     ┌─────────┐
│ filiere  │     │  enseignant│     │  salle  │
│──────────│     │────────────│     │─────────│
│ id_fil PK│     │ id_ens   PK│     │ id_sal PK│
│ nom_fil  │     │ nom        │     │ nom_sal │
│ resp.   →┼──┐  │ prenom     │     │ capacite│
└──────────┘  │  │ grade      │     │ type    │
              │  │ specialite │     └─────────┘
              │  │ departement│
              │  │ email      │
              │  │ telephone  │
              │  └─────┬──────┘
              │        │
              │        ▼ référencé par
              │  ┌────────────┐
              └─→│  matiere   │
                 │────────────│
                 │ id_mat   PK│
                 │ nom_mat    │
                 │ ue         │
                 │ coef       │
                 │ volume     │
                 │ enseig →   │
                 └─────┬──────┘
                       │
┌──────────┐           ▼ référencé par
│ etudiant │     ┌────────────┐     ┌──────────────┐
│──────────│     │   note     │     │  inscription │
│ matri PK │←────│ matri FK   │     │──────────────│
│ nom      │     │ matiere FK │     │ id_insc   PK │
│ prenom   │←────│ cc         │     │ matri  → FK  │
│ sexe     │     │ examen     │     │ filiere      │
│ datnaiss │     └────────────┘     │ niveau       │
│ email    │                        │ annee        │
│ telephone│                        │ statut       │
│ filiere  │     ┌────────────┐     │ UNIQUE(matri,│
│ niveau   │←────│  absence   │     │    annee)    │
│ annee    │     │ matri FK   │     └──────────────┘
│ statut   │     │ matiere FK │
└──────────┘     │ date       │     ┌──────────────┐
                 │ heures     │     │seance_cours  │
                 │ justifiee  │     │──────────────│
                 └────────────┘     │ id_sea    PK │
                                    │ matiere FK   │
                 ┌────────────┐     │ enseignant FK│
                 │ paiement   │     │ salle FK     │
                 │────────────│     │ jour         │
                 │ id_pay  PK │     │ heure_debut  │
                 │ matri → FK │     │ heure_fin    │
                 │ montant_t  │     │ filiere      │
                 │ montant_p  │     │ niveau       │
                 │ date_pay   │     │ CHECK h_f>h_d│
                 │ mode       │     └──────────────┘
                 └────────────┘
```

### Contraintes notables
- `note(matricule, matiere) UNIQUE` : un étudiant a **une seule note par matière** (le UPSERT met à jour)
- `inscription(matricule, annee) UNIQUE` : un étudiant ne peut être inscrit qu'une fois par année académique
- `seance_cours.heure_fin > heure_debut` : `CHECK constraint` côté BDD
- `ON DELETE CASCADE` : supprimer un étudiant supprime ses notes, absences et paiements

### Données initiales
Le fichier `sql/01_schema.sql` charge environ :
- **100 étudiants** (ETU001 à ETU100)
- **5 enseignants** (ENS001-ENS005)
- **3 filières** (Informatique, Data Science, Cybersécurité)
- **6 matières** (MAT001-MAT006)
- **5 salles** (SAL001-SAL005)
- **~310 notes**
- **~250 absences**
- **100 paiements**
- **21 séances**

---

## 7. Couche modèle (`model/`)

Cette couche définit les **objets métier** : des classes Scala qui représentent les entités du domaine.

### `Traits.scala` — 5 traits réutilisables

Un **trait** en Scala est comme une interface Java + des méthodes par défaut.

```scala
trait Identifiable { def id: String }              // a un identifiant
trait Affichable   { def afficher: String }        // peut être imprimé joliment
trait Validable    { def estValide: Boolean }      // peut être validé
trait Calculable   { def calculer: Double }        // produit un calcul
trait Recherchable[A] { def correspond(critere: String): Boolean }
```

**Pourquoi ?** Plutôt que de redéfinir `id` dans chaque classe, on déclare `Identifiable` et toutes les entités l'implémentent. Cela permet aussi du **polymorphisme** : une fonction qui prend `Identifiable` peut traiter tout type ayant un id.

### `Personnes.scala`

```scala
abstract class Personne(val nom: String, val prenom: String, val email: String, val telephone: String)
```

C'est une **classe abstraite** : on ne peut pas créer un `Personne` tout seul, on en hérite.

```scala
case class Etudiant(
  matricule: String, nom: String, prenom: String, sexe: String,
  dateNaissance: Option[LocalDate], email: String, telephone: String,
  filiere: String, niveau: String, annee: String, statut: StatutEtudiant
) extends Personne(nom, prenom, email, telephone) with Identifiable with Affichable
```

**Décortiqué** :
- `case class` : Scala génère automatiquement `equals`, `hashCode`, `toString`, `copy`, et le constructeur sans `new`
- `Option[LocalDate]` : la date peut être absente. C'est `Some(date)` ou `None`. **Plus sûr que `null`**.
- `extends Personne(...)` : passe les arguments au constructeur parent
- `with Identifiable with Affichable` : implémente plusieurs traits

```scala
sealed trait StatutEtudiant
object StatutEtudiant {
  case object Actif    extends StatutEtudiant
  case object Suspendu extends StatutEtudiant
  case object Diplome  extends StatutEtudiant
  case object Inconnu  extends StatutEtudiant

  def fromString(s: String): StatutEtudiant = s.toLowerCase match {
    case "actif"    => Actif
    case "suspendu" => Suspendu
    // ...
  }
}
```

**`sealed trait`** = **type fermé** : Scala connaît toutes les variantes possibles. Le compilateur prévient si un `match` est incomplet. C'est la base du **pattern matching exhaustif** typique des langages fonctionnels.

### `Note.scala`

```scala
case class Note(
  idNote: String, matricule: String, matiere: String,
  controleContinu: Option[Double], examen: Option[Double]
) extends Identifiable with Validable with Calculable {

  def moyenneOption: Option[Double] = (controleContinu, examen) match {
    case (Some(cc), Some(ex)) => Some(cc * 0.4 + ex * 0.6)
    case _ => None
  }

  def calculer: Double = moyenneOption.getOrElse(0.0)
  def estValide: Boolean =
    controleContinu.forall(n => n >= 0 && n <= 20) &&
    examen.forall(n => n >= 0 && n <= 20)
}
```

**Points clés** :
- `controleContinu` et `examen` sont `Option[Double]` → certaines notes peuvent être incomplètes
- `moyenneOption` utilise le **pattern matching** sur un tuple `(Option, Option)` : retourne `Some(moyenne)` seulement si les deux notes existent
- **Formule métier** : `moyenne = 0.4 × CC + 0.6 × examen` (exigée par le cahier des charges)

```scala
sealed trait DecisionAcademique
object DecisionAcademique {
  case object Admis        extends DecisionAcademique
  case object Ajourne      extends DecisionAcademique
  case object Redoublement extends DecisionAcademique

  def fromMoyenne(m: Double): DecisionAcademique =
    if (m >= 10) Admis
    else if (m >= 8) Ajourne
    else Redoublement
}
```

### `Absence.scala`

```scala
case class Absence(
  idAbsence: String, matricule: String, matiere: String,
  dateAbsence: LocalDate, heures: Int, justifiee: Boolean
) extends Identifiable with Validable
```

Simple et plat. `justifiee: Boolean` est essentiel : seules les absences non justifiées comptent pour les alertes.

### `SeanceCours.scala`

```scala
case class SeanceCours(
  idSeance: String, idMatiere: String, idEnseignant: String, idSalle: String,
  jour: String, heureDebut: LocalTime, heureFin: LocalTime,
  filiere: String, niveau: String
) extends Identifiable with Affichable {

  def chevaucheAvec(autre: SeanceCours): Boolean =
    jour == autre.jour &&
    heureDebut.isBefore(autre.heureFin) &&
    autre.heureDebut.isBefore(heureFin)

  def enConflitAvec(autre: SeanceCours): Option[String] = {
    if (!chevaucheAvec(autre)) None
    else if (idSalle == autre.idSalle) Some(s"Salle $idSalle occupée")
    else if (idEnseignant == autre.idEnseignant) Some(s"Enseignant occupé")
    else None
  }
}
```

**`chevaucheAvec`** : algorithme classique de détection de chevauchement d'intervalles temporels. Deux séances se chevauchent si `A.debut < B.fin && B.debut < A.fin`.

**`enConflitAvec`** retourne :
- `None` : pas de conflit
- `Some(raison)` : conflit avec la raison sous forme de chaîne

### `Inscription.scala` et `Paiement.scala`
Suivent le même pattern : `case class` + `sealed trait` pour le statut/mode.

`Paiement` ajoute des **méthodes calculées** :
```scala
def reste: Double = math.max(0.0, montantTotal - montantPaye)
def tauxPaiement: Double = if (montantTotal <= 0) 0 else montantPaye / montantTotal
def estSolde: Boolean = reste <= 0.0
```

---

## 8. Couche base de données (`db/`)

### `Database.scala` — le pool de connexions

```scala
object Database {
  private val config = ConfigFactory.load()
  private val hikariConfig = new HikariConfig()
  hikariConfig.setJdbcUrl(config.getString("db.url"))
  hikariConfig.setUsername(config.getString("db.user"))
  hikariConfig.setPassword(config.getString("db.password"))
  hikariConfig.setMaximumPoolSize(config.getInt("db.poolSize"))
  hikariConfig.setMinimumIdle(2)

  private val pool: HikariDataSource = new HikariDataSource(hikariConfig)

  def withConnection[A](operation: Connection => A): Try[A] = {
    Try {
      val conn = pool.getConnection()
      try operation(conn)
      finally conn.close()
    }
  }

  def close(): Unit = pool.close()
}
```

**Qu'est-ce que HikariCP fait ?**
Ouvrir une connexion PostgreSQL coûte ~50ms. Avec **10 utilisateurs simultanés**, on perdrait 500ms à chaque requête. HikariCP **garde 10 connexions ouvertes en permanence** et les prête à la demande. Quand le code fait `conn.close()`, HikariCP la **réinjecte dans le pool** au lieu de la fermer vraiment.

**`withConnection[A](operation: Connection => A): Try[A]`** :
- `[A]` = type générique (la méthode marche pour n'importe quel type retourné)
- `operation: Connection => A` = une fonction qui prend une `Connection` et renvoie un `A`
- Renvoie `Try[A]` : soit `Success(valeur)`, soit `Failure(exception)`

**Pourquoi `Try` ?** En Scala fonctionnel, on évite les exceptions qui « cassent » le flux. `Try` les **encapsule** : le code appelant peut faire `result.map(...)` sans crasher.

**Exemple d'utilisation** :
```scala
Database.withConnection { conn =>
  val ps = conn.prepareStatement("SELECT * FROM etudiant")
  val rs = ps.executeQuery()
  // ... lire ...
}  // → Try[List[Etudiant]]
```

---

## 9. Couche repository (`repository/`)

Chaque repository a la **même structure** : un objet par table, qui sait lire, insérer, modifier et supprimer.

### Pattern général

```scala
class EtudiantRepository {

  // Convertit une ligne SQL en objet Scala
  private def lire(rs: ResultSet): Etudiant = {
    Etudiant(
      matricule = rs.getString("matricule"),
      nom = rs.getString("nom"),
      // ...
      dateNaissance = Option(rs.getDate("date_naissance")).map(_.toLocalDate)
      //  ↑ Option() transforme un éventuel null en None
    )
  }

  // Liste tous les étudiants
  def listerTous(): Try[List[Etudiant]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement("SELECT * FROM etudiant ORDER BY matricule")) { ps =>
        Using.resource(ps.executeQuery()) { rs =>
          val buf = ListBuffer.empty[Etudiant]
          while (rs.next()) buf += lire(rs)
          buf.toList
        }
      }
    }

  // ... idem pour trouverParMatricule, enregistrer, supprimer, etc.
}
```

**`Using.resource(...)`** : équivalent du `try-with-resources` Java. Ferme automatiquement le `PreparedStatement` et le `ResultSet` à la sortie du bloc, même en cas d'exception.

**`ListBuffer`** : une liste mutable performante. À la fin, `.toList` la convertit en `List` immuable (plus sûre, plus idiomatique en Scala).

### UPSERT : insertion ou mise à jour
```scala
def enregistrer(e: Etudiant): Try[Etudiant] =
  Database.withConnection { conn =>
    val sql = """
      INSERT INTO etudiant (matricule, nom, ...) VALUES (?, ?, ...)
      ON CONFLICT (matricule) DO UPDATE SET
        nom = EXCLUDED.nom, ...
    """
    Using.resource(conn.prepareStatement(sql)) { ps =>
      ps.setString(1, e.matricule)
      ps.setString(2, e.nom)
      // ...
      ps.executeUpdate()
    }
    e
  }
```

**`ON CONFLICT ... DO UPDATE`** : syntaxe PostgreSQL. Si la clé existe déjà, met à jour au lieu de planter. Évite d'avoir 2 méthodes `insert` et `update`.

**`EXCLUDED.nom`** : référence à la nouvelle valeur qui aurait été insérée.

### Génération d'ID auto
```scala
def prochainMatricule(): Try[String] =
  Database.withConnection { conn =>
    Using.resource(conn.prepareStatement(
      "SELECT COALESCE(MAX(matricule), 'ETU000') AS m FROM etudiant"
    )) { ps =>
      Using.resource(ps.executeQuery()) { rs =>
        if (rs.next()) {
          val cur = rs.getString("m")
          val n = Try(cur.substring(3).toInt).getOrElse(0) + 1
          f"ETU$n%03d"   // ETU001, ETU002, ...
        } else "ETU001"
      }
    }
  }
```

**`f"ETU$n%03d"`** : interpolation formattée. `%03d` = entier formaté sur 3 chiffres avec zéros à gauche.

### Tous les repositories suivent ce pattern
| Repository | Méthodes principales |
|---|---|
| `EtudiantRepository` | `listerTous`, `trouverParMatricule`, `listerParFiliere`, `listerParNiveau`, `enregistrer`, `supprimer`, `prochainMatricule` |
| `MatiereRepository` | `listerToutes`, `trouverParId`, `listerParEnseignant`, `enregistrer`, `supprimer` |
| `EnseignantRepository` | `listerTous`, `trouverParId`, `listerParDepartement`, `enregistrer`, `supprimer` |
| `SalleRepository` | `listerToutes` (lecture seule pour le moment) |
| `FiliereRepository` | `listerToutes`, `trouverParId`, `enregistrer`, `supprimer` |
| `NoteRepository` | `listerToutes`, `parEtudiant`, `parMatiere`, `enregistrer` (UPSERT sur `matricule+matiere`), `supprimer` |
| `AbsenceRepository` | idem |
| `SeanceRepository` | idem + `listerParEnseignant`, `listerParSalle`, `listerParFiliereNiveau` |
| `InscriptionRepository` | + `trouverParEtudiantEtAnnee` (pour règle d'unicité) |
| `PaiementRepository` | `listerParEtudiant`, etc. |

---

## 10. Couche service (`service/`)

Les services contiennent la **logique métier** : calculs, validations, agrégations. C'est ici qu'on trouve les notions Scala les plus avancées.

### `NoteService.scala`

```scala
class NoteService(repo: NoteRepository = new NoteRepository) {

  // ---------- RÉCURSION ----------
  /** Somme des valeurs d'une liste de doubles par récursion terminale. */
  @tailrec
  final def sommeRecursive(nums: List[Double], acc: Double = 0.0): Double = nums match {
    case Nil => acc                                   // cas de base : liste vide
    case head :: tail => sommeRecursive(tail, acc + head)  // appel récursif
  }
```

**`@tailrec`** : annotation qui demande au compilateur de **vérifier que la récursion est terminale**. Si oui, il la transforme en boucle → pas de risque de stack overflow.

**Pattern matching sur liste** :
- `Nil` = liste vide
- `head :: tail` = première élément + reste (le `::` est l'opérateur "cons")

```scala
  // ---------- MOYENNE PONDÉRÉE par coefficient (foldLeft) ----------
  def moyennePondereeEtudiant(matricule: String): Try[Option[Double]] =
    for {
      notes    <- repo.listerParEtudiant(matricule)
      matieres <- matiereRepo.listerToutes()
    } yield {
      val mapCoef: Map[String, Int] = matieres.map(m => m.idMatiere -> m.coefficient).toMap

      val (sommePond, sommeCoef) = notes
        .filter(n => n.moyenneOption.isDefined)
        .foldLeft((0.0, 0)) { case ((accNum, accDen), note) =>
          val coef = mapCoef.getOrElse(note.matiere, 1)
          val moy  = note.moyenneOption.get
          (accNum + moy * coef, accDen + coef)
        }

      if (sommeCoef == 0) None
      else Some(sommePond / sommeCoef)
    }
```

**Décortiqué** :
1. `for { ... } yield` : compréhension. Équivalent de `repo.listerParEtudiant(...).flatMap(notes => matiereRepo.listerToutes().map(matieres => ...))`. Permet de chaîner des `Try` proprement.
2. `Map[String, Int]` : table de correspondance ID matière → coefficient
3. `foldLeft((0.0, 0))` : démarre avec `(somme=0, coef=0)` et parcourt chaque note en accumulant
4. `case ((accNum, accDen), note) =>` : déstructure l'accumulateur (un tuple) et la note courante
5. À la fin, divise pour obtenir la moyenne pondérée

**Pourquoi `foldLeft` plutôt qu'une boucle ?** C'est plus court, **immuable** (pas de variable qui change), et **composable** (peut s'enchaîner avec d'autres opérations).

```scala
  // ---------- CLASSEMENT (HOFs en chaîne) ----------
  def classement(): Try[List[(String, Double)]] = {
    for {
      etudiants <- etudiantRepo.listerTous()
      moyennes  <- Try(etudiants.flatMap { e =>
        moyennePondereeEtudiant(e.matricule).toOption.flatten.map(m => e.matricule -> m)
      })
    } yield moyennes.sortBy(-_._2)  // tri décroissant
  }
```

**`flatMap` + `Option`** : transforme une `List[Etudiant]` en `List[(String, Double)]` en supprimant les étudiants sans moyenne. C'est un pattern fonctionnel ultra-courant.

### `AbsenceService.scala` — récursion sur les absences

```scala
class AbsenceService(repo: AbsenceRepository = new AbsenceRepository) {

  @tailrec
  final def totalHeuresRecursive(absences: List[Absence], acc: Int = 0): Int = absences match {
    case Nil => acc
    case head :: tail => totalHeuresRecursive(tail, acc + head.heures)
  }

  def etudiantsEnAlerte(seuil: Int = 10): Try[List[(String, Int)]] =
    repo.listerToutes().map { abs =>
      abs
        .filter(!_.justifiee)
        .groupBy(_.matricule)
        .view
        .mapValues(lst => totalHeuresRecursive(lst))
        .filter(_._2 >= seuil)
        .toList
        .sortBy(-_._2)
    }
}
```

**`groupBy(_.matricule)`** : groupe les absences par étudiant → `Map[String, List[Absence]]`
**`.view.mapValues(...)`** : applique une fonction sur les valeurs sans recopier la map
**`.filter(_._2 >= seuil)`** : ne garde que les paires où la valeur (les heures) dépasse le seuil
**`.sortBy(-_._2)`** : tri décroissant

### `EmploiDuTempsService.scala` — détection de conflits

```scala
def detecterTousConflits(): Try[List[ConflitInfo]] =
  repo.listerToutes().map { seances =>
    // On compare chaque paire (i, j) avec j > i pour éviter les doublons
    val paires = for {
      i <- seances.indices
      j <- (i + 1) until seances.length
    } yield (seances(i), seances(j))

    paires.toList.flatMap { case (a, b) =>
      a.enConflitAvec(b).map(raison => ConflitInfo(a, b, raison))
    }
  }
```

**Compréhension `for`** : génère toutes les paires uniques. Sur N séances → N(N-1)/2 comparaisons.

### `PaiementService.scala`

```scala
@tailrec
final def totalPayeRecursif(paiements: List[Paiement], acc: Double = 0.0): Double =
  paiements match {
    case Nil => acc
    case head :: tail => totalPayeRecursif(tail, acc + head.montantPaye)
  }

def syntheseParFiliere(): Try[Map[String, (Double, Double, Double)]] =
  for {
    etus <- etuRepo.listerTous()
    paie <- paieRepo.listerTous()
  } yield {
    val parFil = etus.groupBy(_.filiere).view.mapValues(_.map(_.matricule)).toMap
    parFil.map { case (fil, matricules) =>
      val pertinents = paie.filter(p => matricules.contains(p.matricule))
      val du   = pertinents.foldLeft(0.0)((a, p) => a + p.montantTotal)
      val paye = pertinents.foldLeft(0.0)((a, p) => a + p.montantPaye)
      val taux = if (du <= 0) 0.0 else paye / du
      fil -> (du, paye, taux)
    }
  }
```

Retourne par filière : `(montantAttendu, montantEncaisse, tauxRecouvrement)`.

### `TableauDeBordService.scala`

Le plus gros service : c'est un **agrégateur** qui s'appuie sur tous les autres services pour produire des KPIs.

```scala
case class SyntheseGlobale(
  nbEtudiants: Int, nbEnseignants: Int, nbFilieres: Int, nbMatieres: Int,
  moyenneGlobale: Option[Double],
  tauxAbsenteisme: Map[String, Double],
  tauxRecouvrement: Double,
  tauxReussiteGlobal: Double
)

def syntheseGlobale(): Try[SyntheseGlobale] =
  for {
    etus    <- etudiantService.listerTous()
    ens     <- enseignantService.listerTous()
    fil     <- formationService.listerFilieres()
    mat     <- formationService.listerMatieres()
    classmt <- noteService.classement()
    tauxAbs <- absenceService.tauxParFiliere()
    tauxRec <- paiementService.tauxRecouvrement()
  } yield {
    val moyennes = classmt.map(_._2)
    val moyG = if (moyennes.isEmpty) None else Some(moyennes.sum / moyennes.size)
    val tauxReussite = if (moyennes.isEmpty) 0.0 else moyennes.count(_ >= 10).toDouble / moyennes.size
    SyntheseGlobale(etus.size, ens.size, fil.size, mat.size, moyG, tauxAbs, tauxRec, tauxReussite)
  }
```

**`for ... yield`** sur 7 `Try` consécutifs : si l'un échoue, le résultat global est `Failure(...)`. Sinon les valeurs sont combinées.

### `BigDataService.scala` — exports CSV

```scala
def exporterRapportAcademique(): Try[String] =
  for {
    etus       <- etuService.listerTous()
    classement <- noteService.classement()
    alertesAbs <- absService.etudiantsEnAlerte(seuil = 0)
    enDette    <- paiService.etudiantsEnDette()
  } yield {
    val moyByMat = classement.toMap
    val absByMat = alertesAbs.toMap
    val detteByMat = enDette.toMap
    val lignes = etus.map { e =>
      val moy = moyByMat.get(e.matricule)
      val abs = absByMat.getOrElse(e.matricule, 0)
      val dette = detteByMat.getOrElse(e.matricule, 0.0)
      val dec = moy.map(DecisionAcademique.fromMoyenne).map(DecisionAcademique.toString).getOrElse("--")
      List(e.matricule, e.nom, e.prenom, e.filiere, e.niveau,
           moy.map("%.2f".format(_)).getOrElse(""), dec, abs.toString, "%.0f".format(dette))
    }
    ecrireCsv(s"$outputDir/rapport_academique.csv",
      List("matricule","nom","prenom","filiere","niveau","moyenne","decision","heures_absence","reste_a_payer"),
      lignes
    ).getOrElse("")
  }
```

Croise **4 sources de données** pour produire un CSV consolidé.

---

## 11. Couche API (`api/`)

### `JsonFormats.scala` — la sérialisation JSON

C'est ici qu'on dit à spray-json comment transformer une `case class` en JSON et vice-versa.

```scala
object JsonFormats extends DefaultJsonProtocol {

  // Pour les types simples : une ligne suffit
  implicit val matiereFormat: RootJsonFormat[Matiere] = jsonFormat6(Matiere)

  // Pour les types complexes (héritage, sealed traits), on écrit manuellement :
  implicit object EtudiantFormat extends RootJsonFormat[Etudiant] {
    override def write(e: Etudiant): JsValue = JsObject(
      "matricule" -> JsString(e.matricule),
      "nom"       -> JsString(e.nom),
      // ...
      "statut"    -> JsString(StatutEtudiant.toString(e.statut))  // sealed trait → String
    )
    override def read(v: JsValue): Etudiant = {
      val o = v.asJsObject
      Etudiant(
        matricule = o.fields("matricule").convertTo[String],
        // ...
      )
    }
  }
}
```

**`implicit`** : ce mot-clé signifie que Scala peut chercher ce format **automatiquement** dans le contexte. Quand le code fait `complete(etudiant)`, Akka HTTP cherche un `JsonFormat[Etudiant]` implicite pour sérialiser.

**Pourquoi des formats manuels pour `Etudiant` ?** Parce qu'il **hérite de `Personne`** : spray-json `jsonFormatN` ne sait pas gérer les champs hérités.

**Formats `Map[String, Int/Double/String]`** : ajoutés manuellement parce que spray-json ne les fournit pas par défaut.

### Pattern d'une route : `EtudiantRoutes.scala`

```scala
class EtudiantRoutes(service: EtudiantService = new EtudiantService) {

  val routes: Route = pathPrefix("etudiants") {
    concat(

      // GET /api/etudiants ou POST /api/etudiants
      pathEndOrSingleSlash {
        get {
          parameters("filiere".?, "niveau".?, "statut".?) { (fil, niv, st) =>
            val res = (fil, niv, st) match {
              case (Some(f), _, _) => service.filtrerParFiliere(f)
              case (_, Some(n), _) => service.filtrerParNiveau(n)
              case (_, _, Some(s)) => service.filtrerParStatut(s)
              case _               => service.listerTous()
            }
            res match {
              case Success(ls) => complete(ls)
              case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
            }
          }
        } ~
        post {
          entity(as[Etudiant]) { e =>
            service.enregistrer(e) match {
              case Success(saved) => complete(StatusCodes.Created, saved)
              case Failure(ex)    => complete(StatusCodes.BadRequest, ErreurReponse(ex.getMessage))
            }
          }
        }
      },

      // GET /api/etudiants/{matricule}
      path(Segment) { matricule =>
        get {
          service.trouverParMatricule(matricule) match {
            case Success(Some(e)) => complete(e)
            case Success(None)    => complete(StatusCodes.NotFound, ErreurReponse(s"Etudiant $matricule introuvable"))
            case Failure(ex)      => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      }
    )
  }
}
```

### Le DSL Akka HTTP

| Élément | Signification |
|---|---|
| `pathPrefix("etudiants")` | matche toute URL commençant par `/etudiants` |
| `pathEndOrSingleSlash` | matche `/etudiants` (exactement) ou `/etudiants/` |
| `path(Segment) { x => }` | matche `/etudiants/XXX` et capture `XXX` dans `x` |
| `get { ... }` | filtre les méthodes GET |
| `post { ... }` | filtre les méthodes POST |
| `~` (tilde) | « ou » : essaye le suivant si le précédent ne matche pas |
| `concat(a, b, c)` | équivalent de `a ~ b ~ c` |
| `parameters("filiere".?)` | récupère `?filiere=xxx` (optionnel grâce au `?`) |
| `entity(as[Etudiant])` | parse le body JSON en `Etudiant` |
| `complete(...)` | envoie une réponse |

### Tableau récapitulatif des endpoints

| Module | URL | Méthodes |
|---|---|---|
| **1. Étudiants** | `/api/etudiants[/{m}][/stats][/{m}/statut]` | GET, POST, PUT, DELETE, PATCH |
| **2. Enseignants** | `/api/enseignants[/{id}][/stats][/{id}/cours]` | GET, POST, PUT, DELETE |
| **3. Formations** | `/api/filieres[/{id}]`, `/api/matieres[/{id}][/stats][/par-ue]` | GET, POST, PUT, DELETE |
| **4. Inscriptions** | `/api/inscriptions[/{id}][/stats][/{id}/statut]` | GET, POST, PATCH, DELETE |
| **5. Notes** | `/api/notes[/{id}][/classement][/echec][/stats/parmatiere][/incompletes]` | GET, POST, DELETE |
| **6. Absences** | `/api/absences[/{id}/justifier][/alerte][/stats/filiere]` | GET, POST, PATCH, DELETE |
| **7. Séances** | `/api/seances[/{id}][/classe][/conflits][/verifier]` | GET, POST, PUT, DELETE |
| **8. Paiements** | `/api/paiements[/{id}][/dette][/synthese][/etudiant/{m}/reste]` | GET, POST, DELETE |
| **9. Dashboard** | `/api/dashboard/{synthese,etudiants,reussite,absenteisme,financier,risque,topEnseignants}` | GET |
| **10. Big Data** | `/api/bigdata/{export,manquantes,promotion,absences-mois,paiements-mois}` | GET, POST |
| **Référentiels** | `/api/ref/{etudiants,matieres,salles,enseignants,filieres}` | GET |

---

## 12. Point d'entrée (`Main.scala`)

```scala
object Main extends App with CorsHandler {

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "gestion-universitaire")
  implicit val ec: ExecutionContextExecutor = system.executionContext

  private val config = ConfigFactory.load()
  private val host   = config.getString("server.host")
  private val port   = config.getInt("server.port")

  // Vérifier la connexion BDD au démarrage
  Database.withConnection { conn =>
    val st = conn.createStatement()
    st.executeQuery("SELECT 1").next()
  } match {
    case Success(_)  => log.info("Connexion PostgreSQL : OK")
    case Failure(ex) => log.error(s"Echec connexion BDD : ${ex.getMessage}")
  }

  // Instancier les routes
  private val noteRoutes      = new NoteRoutes()
  // ... 10 autres
  
  // Combiner toutes les routes API sous /api
  private val apiRoutes: Route = pathPrefix("api") {
    concat(noteRoutes.routes, absenceRoutes.routes, /* ... */)
  }

  // Routes statiques (sert le frontend)
  private val staticRoutes: Route = concat(
    pathSingleSlash { getFromFile("frontend/index.html") },
    pathPrefix("css") { getFromDirectory("frontend/css") },
    pathPrefix("js")  { getFromDirectory("frontend/js") }
  )

  // Tout combiner avec CORS
  private val routes: Route = corsHandler(apiRoutes ~ staticRoutes)

  // Démarrer le serveur
  private val bindingFuture = Http().newServerAt(host, port).bind(routes)

  // Hook pour Ctrl+C
  sys.addShutdownHook {
    Database.close()
    system.terminate()
  }

  // BLOQUER LE THREAD PRINCIPAL (sans ça, sbt termine et tue le serveur)
  Await.result(system.whenTerminated, Duration.Inf)
}
```

**Points critiques** :
- `implicit val system` : Akka HTTP a besoin d'un `ActorSystem` (machine à exécuter du code asynchrone). Le mot `implicit` le rend disponible automatiquement aux fonctions qui en demandent un.
- `Await.result(system.whenTerminated, Duration.Inf)` : **bloque le thread main indéfiniment**. Sans cette ligne, sbt run finit sa méthode `main`, JVM s'éteint, serveur mort. Cette ligne attend qu'on tue le système d'acteurs (via Ctrl+C qui déclenche le `addShutdownHook`).

### `CorsHandler.scala`

Pour que le frontend puisse appeler l'API depuis n'importe quelle origine (utile en dev) :

```scala
trait CorsHandler {
  private val corsHeaders = List(
    `Access-Control-Allow-Origin`.*,
    `Access-Control-Allow-Credentials`(true),
    `Access-Control-Allow-Headers`("Origin","X-Requested-With","Content-Type","Accept","Authorization")
  )

  def corsHandler(r: Route): Route = addCorsHeaders { preflightRequestHandler ~ r }

  private def preflightRequestHandler: Route = options {
    complete(HttpResponse(StatusCodes.OK)
      .withHeaders(`Access-Control-Allow-Methods`(OPTIONS, POST, PUT, GET, DELETE, PATCH)))
  }
}
```

**CORS = Cross-Origin Resource Sharing**. Le navigateur bloque par sécurité les appels d'un site à un autre, sauf si le serveur dit explicitement « OK » via ces headers.

**Preflight** : avant un POST/PUT/DELETE, le navigateur envoie une requête `OPTIONS` pour demander la permission. Notre `preflightRequestHandler` répond OK.

---

## 13. Frontend (`frontend/`)

### `index.html`
**Une seule page HTML** qui contient **10 vues `<section>`** dont une seule est visible à la fois (classe `.is-active`). C'est une **Single Page Application (SPA) maison**, sans framework.

Structure :
```html
<aside class="sidebar"> ... 10 boutons de navigation ... </aside>
<main class="main">
  <section class="view is-active" id="view-dashboard"> ... </section>
  <section class="view" id="view-etudiants"> ... </section>
  ...
</main>
<div class="modal" id="modal"> ... </div>
<div class="toast" id="toast"> ... </div>
```

### `style.css`
~750 lignes. Esthétique éditoriale avec :
- Variables CSS pour les couleurs (`--terracotta`, `--ivory`)
- Grille responsive
- Composants : `.kpi-card`, `.table`, `.badge`, `.tab`, `.modal`, `.toast`, `.rank-row`, `.bar-row`, `.schedule-cell`, `.conflit-card`

### `api.js` — le wrapper de fetch

```javascript
async function request(method, path, body) {
  const opts = { method, headers: { 'Accept': 'application/json' } };
  if (body !== undefined && body !== null) {
    opts.headers['Content-Type'] = 'application/json';
    opts.body = JSON.stringify(body);
  }
  const resp = await fetch(BASE_URL + path, opts);
  const text = await resp.text();
  const data = text ? JSON.parse(text) : null;
  if (!resp.ok) throw new Error(data?.message || `Erreur HTTP ${resp.status}`);
  return data;
}

const api = {
  notes:      { liste: () => request('GET', '/notes'), creer: (n) => request('POST', '/notes', n), ... },
  absences:   { ... },
  etudiants:  { ... },
  // ... un namespace par module
};
```

**Centralisé** : si l'URL de base change ou si on veut ajouter un header d'authentification, on modifie un seul endroit.

### `utils.js` — les helpers transversaux

| Fonction | Rôle |
|---|---|
| `showToast(msg, kind)` | Affiche une notification temporaire en bas |
| `openModal(title, html)` | Ouvre la fenêtre modale avec un contenu |
| `closeModal()` | Ferme la modale |
| `formatDate(s)` | Convertit `"2025-10-12"` en `"12 oct. 2025"` |
| `formatNum(n, dec)` | `12.345 → "12.35"` |
| `escapeHtml(s)` | Échappe `<`, `>`, `&` pour éviter les injections XSS |
| `debounce(fn, ms)` | Empêche un appel trop fréquent (utile pour les inputs de recherche) |
| `badgeMoyenne(m)` | Renvoie le HTML d'un badge coloré selon la moyenne |
| `bindTabs(rootSelector)` | Active la logique de tabs (cliquer change la vue active) |
| `loadRefs()` | Charge une fois les listes d'étudiants, matières, salles, enseignants en cache |
| `nomEtudiant(m)` / `nomMatiere(id)` | Récupère le nom complet depuis le cache |

### `app.js` — routing + bootstrap

```javascript
function changerVue(target) {
  document.querySelectorAll('.view').forEach(v => v.classList.remove('is-active'));
  document.getElementById('view-' + target).classList.add('is-active');
  initVue(target);  // initialise le module si premier passage
}

function initVue(target) {
  if (initialises.has(target)) return;
  initialises.add(target);
  switch (target) {
    case 'dashboard':   dashboardModule.init(); break;
    case 'etudiants':   etudiantsModule.init(); break;
    // ...
  }
}

document.addEventListener('DOMContentLoaded', async () => {
  bindNav();
  await pingApi();
  await utils.loadRefs();
  initVue('dashboard');  // vue par défaut
});
```

**Initialisation paresseuse** : on initialise un module seulement lorsque l'utilisateur clique sur sa vue → démarrage plus rapide.

### Pattern d'un module métier : `notes.js`

Chaque module suit ce squelette :

```javascript
(function () {
  let etat = [];  // état local (toutes les notes)

  // 1. CHARGER LES KPIs
  async function chargerKpis() {
    const echec = await api.notes.echec();
    document.getElementById('kpi-echecs').textContent = echec.length;
    // ...
  }

  // 2. RENDRE LA TABLE
  function rendreTable() {
    const tbody = document.getElementById('notes-tbody');
    tbody.innerHTML = etat.map(n => `<tr>...</tr>`).join('');
  }

  // 3. CHARGER LES DONNÉES
  async function chargerNotes() {
    etat = await api.notes.liste();
    rendreTable();
    await chargerKpis();
  }

  // 4. OUVRIR LE FORMULAIRE DE CRÉATION
  function ouvrirFormulaire() {
    const html = `<form id="form-note">...</form>`;
    utils.openModal('Nouvelle note', html);
    document.getElementById('form-note').addEventListener('submit', async (ev) => {
      ev.preventDefault();
      await api.notes.creer({ ... });
      utils.closeModal();
      await chargerNotes();
    });
  }

  // 5. POINT D'ENTRÉE
  async function init() {
    utils.bindTabs('#view-notes');
    document.getElementById('btn-new-note').addEventListener('click', ouvrirFormulaire);
    await chargerNotes();
  }

  window.notesModule = { init };
})();
```

**IIFE** (Immediately Invoked Function Expression) : `(function(){...})()` crée un scope privé. Les variables `etat`, `chargerKpis`, etc. ne polluent pas le scope global.

**`window.notesModule = { init }`** : on n'expose qu'`init` à l'extérieur.

### `dashboard.js` — vue d'ensemble

Le module le plus stratégique. Il appelle :
- `api.dashboard.synthese()` → KPIs globaux
- `api.dashboard.reussite()` → top 5 + matière la plus difficile
- `api.dashboard.risque(10)` → étudiants à risque
- `api.dashboard.financier()` → synthèse par filière

Affiche tout dans **6 sections** (KPIs, top 5, étudiants à risque, taux de réussite par filière, finance par filière).

### `bigdata.js` — analyses massives

```javascript
async function lancerExports() {
  const r = await api.bigdata.exporter();
  showToast('Exports terminés', 'success');
  document.getElementById('bd-export-list').innerHTML = `
    <div class="export-item">Rapport académique : <code>${r.rapportAcademique}</code></div>
    ...`;
}
```

Le clic déclenche le `POST /api/bigdata/export` qui génère 3 fichiers CSV dans `./output/`.

---

## 14. Notions Scala mises en œuvre

| Notion | Où la voir | Pourquoi |
|---|---|---|
| **Trait** | `model/Traits.scala` (5 traits) | Polymorphisme, code DRY |
| **Héritage de classe** | `Etudiant extends Personne` | Réutilisation |
| **Case class** | Toutes les entités (`Etudiant`, `Note`, ...) | Immutabilité, `equals`/`hashCode`/`copy` auto |
| **Sealed trait + objects** | `StatutEtudiant`, `DecisionAcademique` | Énumérations exhaustives, pattern matching sûr |
| **Pattern matching** | `match { case ... }` partout | Code lisible sur les types fermés |
| **Option** | `dateNaissance: Option[LocalDate]`, `moyenneOption` | Évite les `null` |
| **Try** | `Database.withConnection`, toute la chaîne | Gestion d'erreurs sans exception |
| **For-comprehension** | `for { a <- Try1; b <- Try2 } yield ...` | Chaîner des `Try` proprement |
| **Récursivité `@tailrec`** | `sommeRecursive`, `totalHeuresRecursive`, `totalPayeRecursif`, `chercherRecursif`, `compterActifsRecursif` | Récursion sûre (optimisée en boucle) |
| **Fonctions d'ordre supérieur** | `foldLeft`, `map`, `filter`, `groupBy`, `flatMap`, `sortBy` | Style fonctionnel |
| **Collections immuables** | `List`, `Set` (filières uniques), `Map` (associations) | Sûreté multithread |
| **Tuples** | `(String, Double)` pour le classement | Données ad-hoc sans définir de case class |
| **Implicit** | `implicit val ... : RootJsonFormat[X]` | Résolution automatique |
| **Generics `[A]`** | `Try[A]`, `Option[A]`, `withConnection[A](...)` | Code paramétré par type |
| **Packages** | `universite.model`, `.service`, `.api`, `.db`, `.repository`, `.util` | Organisation modulaire |
| **`Using.resource`** | Tous les repositories | Fermeture automatique des ressources |

---

## 15. Guide de modifications courantes

### Ajouter un champ à `Etudiant`

1. **SQL** (`01_schema.sql`) : ajouter la colonne `ALTER TABLE etudiant ADD ...`
2. **Model** (`Personnes.scala`) : ajouter le paramètre à la `case class`
3. **Repository** (`EtudiantRepository.scala`) :
   - dans `lire(rs)` : lire la nouvelle colonne
   - dans `enregistrer(e)` : ajouter dans `INSERT` + `ON CONFLICT DO UPDATE`
4. **JsonFormats** (`JsonFormats.scala`) : ajouter le champ dans `write` et `read` du `EtudiantFormat`
5. **Frontend** (`etudiants.js`) :
   - dans `rendreTable` : ajouter une colonne
   - dans `ouvrirFormulaire` : ajouter le champ du formulaire
   - dans le `submit` : inclure le champ dans le payload
6. **HTML** (`index.html`) : ajouter `<th>` dans le `<thead>`

### Ajouter un nouvel endpoint

1. **Service** : créer la méthode métier dans le `*Service.scala` approprié
2. **Route** : ajouter dans `*Routes.scala` :
   ```scala
   path("ma-nouvelle-route") {
     get {
       service.maMethode() match {
         case Success(x) => complete(x)
         case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
       }
     }
   }
   ```
3. **API JS** : ajouter la méthode dans `api.js`
4. **Utilisation** : appeler depuis le module concerné

### Ajouter une nouvelle table

1. **SQL** : créer la table + données initiales
2. **Model** : créer une nouvelle `case class` qui implémente les traits utiles
3. **Repository** : nouveau fichier `XRepository.scala` (copier-coller `EtudiantRepository` pour la structure)
4. **Service** : nouveau fichier `XService.scala`
5. **JsonFormats** : ajouter le format (automatique avec `jsonFormatN(X)` si pas d'héritage)
6. **Routes** : nouveau fichier `XRoutes.scala`
7. **Main.scala** : instancier `new XRoutes()` et l'ajouter au `concat(...)` dans `apiRoutes`
8. **API JS** : ajouter le namespace dans `api.js`
9. **Frontend** : nouvelle vue dans `index.html` + nouveau module `x.js` + bouton dans la sidebar
10. **app.js** : ajouter le cas dans `initVue`

### Debugger

| Problème | Action |
|---|---|
| Le serveur ne démarre pas | Vérifier `application.conf`, faire `psql -U postgres -d gestion_universite -c "SELECT 1"` pour tester la BDD |
| Erreur 500 sur une route | Regarder les logs sbt — le stack trace pointe la ligne |
| Erreur JSON côté frontend | Ouvrir l'onglet Network du navigateur, voir la réponse exacte |
| Boutons qui ne réagissent pas | Console JS du navigateur (F12) → chercher les erreurs rouges |
| Compilation Scala échoue | Lire le message — il dit la ligne, la classe, le type attendu/reçu |

---

## 🎓 Ressources pour approfondir

- **Akka HTTP** : https://doc.akka.io/docs/akka-http/current/
- **spray-json** : https://github.com/spray/spray-json
- **HikariCP** : https://github.com/brettwooldridge/HikariCP
- **Scala par l'exemple** : https://docs.scala-lang.org/tour/tour-of-scala.html

---

**Fin de la documentation.** Pour toute question, lire le code source : il est documenté en commentaires Scala et chaque fichier fait moins de 300 lignes.
