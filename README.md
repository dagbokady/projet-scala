# Plateforme universitaire — Modules 5, 6, 7

Application Scala connectée à PostgreSQL pour la **gestion des notes**, des **absences** et des **emplois du temps**, avec un frontend HTML/CSS/JS servi par le même serveur Akka HTTP.

> Projet pédagogique conforme au cahier des charges « Plateforme intelligente de gestion universitaire » (modules 5, 6 et 7).

---

## 1. Architecture

```
┌──────────────────────┐
│  Frontend (HTML/CSS/JS)
│  — sidebar + 3 vues  │
└──────────┬───────────┘
           │   fetch /api/*
           ▼
┌──────────────────────┐
│  Akka HTTP (port 8080)
│  Routes /api/notes    │
│         /api/absences │
│         /api/seances  │
│         /api/ref/*    │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  Services métier (Scala)
│  + récursion / fold  │
│  + Option / Try      │
│  + pattern matching  │
└──────────┬───────────┘
           │ JDBC + HikariCP
           ▼
┌──────────────────────┐
│  PostgreSQL          │
│  base : gestion_universite
└──────────────────────┘
```

**Stack** : Scala 2.13.12 · sbt 1.9.8 · Akka HTTP 10.5.3 · PostgreSQL 14+ · HikariCP · spray-json.

---

## 2. Modules couverts

| Module | Description | Endpoints clés |
|---|---|---|
| **5 — Notes** | Saisie CC/Examen, calcul de moyennes (40% CC + 60% examen), classement, échecs | `/api/notes`, `/api/notes/classement`, `/api/notes/echec`, `/api/notes/stats/parmatiere` |
| **6 — Absences** | Enregistrement, justification, alerte de dépassement (>= 10 h), taux par filière | `/api/absences`, `/api/absences/alerte`, `/api/absences/stats/filiere`, `PATCH /api/absences/{id}/justifier` |
| **7 — Emplois du temps** | Création de séances, détection de conflits (salle ou enseignant), grille hebdo | `/api/seances`, `/api/seances/conflits`, `POST /api/seances/verifier`, `/api/seances/classe?filiere=&niveau=` |

---

## 3. Notions Scala mises en œuvre (cahier des charges)

| Notion | Où la voir |
|---|---|
| **Trait** | `model/Traits.scala` — `Identifiable`, `Affichable`, `Validable`, `Calculable`, `Recherchable` |
| **Héritage** | `Personne` → `Etudiant`, `Enseignant` |
| **Pattern matching** | `StatutEtudiant`, `DecisionAcademique`, `Try` dans les repos |
| **Option** | `Note.moyenneOption`, `EtudiantRepository.findByMatricule` |
| **Try** | `Database.withConnection[A]: Try[A]` (toutes les opérations BDD) |
| **Récursivité (`@tailrec`)** | `NoteService.sommeRecursive`, `AbsenceService.totalHeuresRecursive` |
| **Fonctions d'ordre supérieur** | `foldLeft` pour la moyenne pondérée par coefficient, `map`/`filter`/`groupBy` partout |
| **Collections** | `List`, `Set`, `Map` dans tous les services |
| **Organisation en packages** | `model`, `repository`, `service`, `api`, `db`, `util` |

---

## 4. Prérequis

- **JDK 11+** (testé avec JDK 21)
- **sbt 1.9+** ([install guide](https://www.scala-sbt.org/download.html))
- **PostgreSQL 14+** avec un utilisateur `postgres` / mot de passe `postgres` (configurable dans `src/main/resources/application.conf`)

---

## 5. Démarrage

### a. Préparer la base de données

```bash
# Créer la base
createdb -U postgres gestion_universite

# Charger le schéma + les données initiales du PDF
psql -U postgres -d gestion_universite -f sql/01_schema.sql
```

### b. Lancer l'application

```bash
sbt run
```

Au démarrage, vous devriez voir :

```
[INFO] Connexion BDD vérifiée.
[INFO] Serveur démarré sur http://localhost:8080/
```

### c. Utiliser l'interface

Ouvrez http://localhost:8080/ dans votre navigateur.

L'interface propose trois vues dans la sidebar :
1. **Notes** — saisie, classement, étudiants en échec, stats par matière
2. **Absences** — saisie, justification en un clic, alerte > 10 h, stats par filière
3. **Emplois du temps** — grille hebdo, liste, détection de conflits, création avec preview

---

## 6. Configuration

`src/main/resources/application.conf` :

```hocon
db {
  url      = "jdbc:postgresql://localhost:5432/gestion_universite"
  user     = "postgres"
  password = "postgres"
  poolSize = 10
}

server {
  host = "0.0.0.0"
  port = 8080
}
```

---

## 7. Structure du projet

```
gestion-universitaire-scala/
├── build.sbt
├── project/build.properties
├── sql/01_schema.sql                 ← schéma + données initiales
├── src/main/
│   ├── resources/application.conf
│   └── scala/universite/
│       ├── Main.scala                ← point d'entrée Akka HTTP
│       ├── model/                    ← classes métier + traits
│       ├── db/Database.scala         ← pool HikariCP
│       ├── repository/               ← couche d'accès BDD
│       ├── service/                  ← logique métier (récursion, folds)
│       ├── api/                      ← routes Akka HTTP + spray-json
│       └── util/CorsHandler.scala
└── frontend/
    ├── index.html
    ├── css/style.css
    └── js/{api,utils,notes,absences,emplois,app}.js
```

---

## 8. Tests rapides via curl

```bash
# Liste des notes
curl http://localhost:8080/api/notes

# Classement
curl http://localhost:8080/api/notes/classement

# Étudiants en échec
curl http://localhost:8080/api/notes/echec

# Étudiants > 10h d'absence
curl "http://localhost:8080/api/absences/alerte?seuil=10"

# Justifier une absence
curl -X PATCH http://localhost:8080/api/absences/ABS001/justifier

# Conflits d'emploi du temps
curl http://localhost:8080/api/seances/conflits

# Vérifier (sans créer) une nouvelle séance
curl -X POST http://localhost:8080/api/seances/verifier \
  -H "Content-Type: application/json" \
  -d '{"idMatiere":"MAT001","idEnseignant":"ENS001","idSalle":"SAL001",
       "jour":"Lundi","heureDebut":"09:00:00","heureFin":"11:00:00",
       "filiere":"Informatique","niveau":"M1"}'
```

---

## 9. Notes pédagogiques

- **Formule de moyenne** : `moyenne = 0.4 × CC + 0.6 × examen` (PDF §5.5)
- **Décision académique** :
  - `>= 10` → **Admis**
  - `[8, 10[` → **Ajourné**
  - `< 8` → **Redoublement**
- **Seuil d'alerte d'absences** : 10 heures *non justifiées* (paramétrable via `?seuil=`)
- **Conflit d'emploi du temps** = chevauchement temporel **+** (même salle **OU** même enseignant)
