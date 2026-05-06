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

Total : **~50 fichiers Scala**, **~13 fichiers JS**, conforme au cahier des charges.
