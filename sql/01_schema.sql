-- =====================================================================
-- Script de creation de la base de donnees PostgreSQL
-- Plateforme intelligente de gestion universitaire
-- Modules 5 (Notes), 6 (Absences) et 7 (Emplois du temps)
-- =====================================================================

-- Creer la base si elle n'existe pas (a executer en superuser)
-- CREATE DATABASE gestion_universite;
-- \c gestion_universite

-- ---------------------------------------------------------------------
-- Suppression des tables (dans l'ordre inverse des dependances)
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS seance_cours CASCADE;
DROP TABLE IF EXISTS absence       CASCADE;
DROP TABLE IF EXISTS note          CASCADE;
DROP TABLE IF EXISTS inscription   CASCADE;
DROP TABLE IF EXISTS matiere       CASCADE;
DROP TABLE IF EXISTS filiere       CASCADE;
DROP TABLE IF EXISTS salle         CASCADE;
DROP TABLE IF EXISTS enseignant    CASCADE;
DROP TABLE IF EXISTS etudiant      CASCADE;

-- ---------------------------------------------------------------------
-- Tables referentielles
-- ---------------------------------------------------------------------

CREATE TABLE etudiant (
    matricule       VARCHAR(20) PRIMARY KEY,
    nom             VARCHAR(80) NOT NULL,
    prenom          VARCHAR(80) NOT NULL,
    sexe            CHAR(1)     CHECK (sexe IN ('M', 'F')),
    date_naissance  DATE,
    email           VARCHAR(120) UNIQUE,
    telephone       VARCHAR(20),
    filiere         VARCHAR(60),
    niveau          VARCHAR(10),
    annee           VARCHAR(20),
    statut          VARCHAR(20) DEFAULT 'Actif'
);

CREATE TABLE enseignant (
    id_enseignant   VARCHAR(20) PRIMARY KEY,
    nom             VARCHAR(80) NOT NULL,
    prenom          VARCHAR(80) NOT NULL,
    grade           VARCHAR(60),
    specialite      VARCHAR(120),
    departement     VARCHAR(60),
    email           VARCHAR(120) UNIQUE,
    telephone       VARCHAR(20)
);

CREATE TABLE filiere (
    id_filiere      VARCHAR(20) PRIMARY KEY,
    nom_filiere     VARCHAR(80) NOT NULL UNIQUE,
    responsable     VARCHAR(20) REFERENCES enseignant(id_enseignant)
);

CREATE TABLE matiere (
    id_matiere      VARCHAR(20) PRIMARY KEY,
    nom_matiere     VARCHAR(120) NOT NULL,
    ue              VARCHAR(80),
    coefficient     INT  CHECK (coefficient > 0),
    volume_horaire  INT  CHECK (volume_horaire >= 0),
    enseignant      VARCHAR(20) REFERENCES enseignant(id_enseignant)
);

CREATE TABLE salle (
    id_salle        VARCHAR(20) PRIMARY KEY,
    nom_salle       VARCHAR(80) NOT NULL,
    capacite        INT CHECK (capacite > 0),
    type_salle      VARCHAR(40)
);

CREATE TABLE inscription (
    id_inscription  VARCHAR(20) PRIMARY KEY,
    matricule       VARCHAR(20) REFERENCES etudiant(matricule) ON DELETE CASCADE,
    filiere         VARCHAR(60),
    niveau          VARCHAR(10),
    annee           VARCHAR(20),
    statut          VARCHAR(20),
    UNIQUE (matricule, annee)
);

-- ---------------------------------------------------------------------
-- Module 5 : Notes
-- ---------------------------------------------------------------------
CREATE TABLE note (
    id_note          VARCHAR(20) PRIMARY KEY,
    matricule        VARCHAR(20) NOT NULL REFERENCES etudiant(matricule)  ON DELETE CASCADE,
    matiere          VARCHAR(20) NOT NULL REFERENCES matiere(id_matiere)  ON DELETE CASCADE,
    controle_continu NUMERIC(5,2) CHECK (controle_continu IS NULL OR (controle_continu >= 0 AND controle_continu <= 20)),
    examen           NUMERIC(5,2) CHECK (examen           IS NULL OR (examen           >= 0 AND examen           <= 20)),
    UNIQUE (matricule, matiere)
);

CREATE INDEX idx_note_matricule ON note(matricule);
CREATE INDEX idx_note_matiere   ON note(matiere);

-- ---------------------------------------------------------------------
-- Module 6 : Absences
-- ---------------------------------------------------------------------
CREATE TABLE absence (
    id_absence      VARCHAR(20) PRIMARY KEY,
    matricule       VARCHAR(20) NOT NULL REFERENCES etudiant(matricule) ON DELETE CASCADE,
    matiere         VARCHAR(20) NOT NULL REFERENCES matiere(id_matiere) ON DELETE CASCADE,
    date_absence    DATE NOT NULL,
    heures          INT  CHECK (heures > 0),
    justifiee       BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_abs_matricule ON absence(matricule);
CREATE INDEX idx_abs_matiere   ON absence(matiere);

-- ---------------------------------------------------------------------
-- Module 7 : Emplois du temps
-- ---------------------------------------------------------------------
CREATE TABLE seance_cours (
    id_seance       VARCHAR(20) PRIMARY KEY,
    matiere         VARCHAR(20) NOT NULL REFERENCES matiere(id_matiere)    ON DELETE CASCADE,
    enseignant      VARCHAR(20) NOT NULL REFERENCES enseignant(id_enseignant),
    salle           VARCHAR(20) NOT NULL REFERENCES salle(id_salle),
    jour            VARCHAR(15) NOT NULL,
    heure_debut     TIME NOT NULL,
    heure_fin       TIME NOT NULL,
    filiere         VARCHAR(60),
    niveau          VARCHAR(10),
    CHECK (heure_fin > heure_debut)
);

CREATE INDEX idx_seance_jour ON seance_cours(jour);
CREATE INDEX idx_seance_salle ON seance_cours(salle);
CREATE INDEX idx_seance_ens   ON seance_cours(enseignant);

-- =====================================================================
-- Donnees initiales
-- =====================================================================

INSERT INTO enseignant VALUES
 ('ENS001','KOFFI','Daniel','Docteur','Scala et Big Data','Informatique','daniel.koffi@univ.edu','0100000001'),
 ('ENS002','OUATTARA','Marie','Maitre Assistant','Bases de donnees','Informatique','marie.ouattara@univ.edu','0100000002'),
 ('ENS003','KONE','Ibrahim','Professeur','Intelligence Artificielle','Data Science','ibrahim.kone@univ.edu','0100000003'),
 ('ENS004','ADOU','Carine','Docteur','Cybersecurite','Cybersecurite','carine.adou@univ.edu','0100000004'),
 ('ENS005','YAPO','Michel','Docteur','Systemes distribues','Informatique','michel.yapo@univ.edu','0100000005');

INSERT INTO filiere VALUES
 ('FIL001','Informatique','ENS001'),
 ('FIL002','Data Science','ENS003'),
 ('FIL003','Cybersecurite','ENS004');

INSERT INTO matiere VALUES
 ('MAT001','Programmation Scala','UE Programmation',4,36,'ENS001'),
 ('MAT002','Bases de donnees avancees','UE Donnees',3,30,'ENS002'),
 ('MAT003','Big Data','UE Donnees',4,36,'ENS005'),
 ('MAT004','Machine Learning','UE Intelligence Artificielle',4,36,'ENS003'),
 ('MAT005','Cryptographie','UE Cybersecurite',3,30,'ENS004'),
 ('MAT006','Systemes distribues','UE Architecture',3,30,'ENS005');

INSERT INTO etudiant VALUES
 ('ETU001','KOFFI','Jean','M','2001-04-12','jean.koffi@mail.com','0700000001','Informatique','M1','2025-2026','Actif'),
 ('ETU002','KOUAME','Aya','F','2002-07-20','aya.kouame@mail.com','0700000002','Informatique','M1','2025-2026','Actif'),
 ('ETU003','TRAORE','Moussa','M','2000-11-05','moussa.traore@mail.com','0700000003','Data Science','M1','2025-2026','Actif'),
 ('ETU004','DIABATE','Fatou','F','2001-09-14','fatou.diabate@mail.com','0700000004','Cybersecurite','M1','2025-2026','Actif'),
 ('ETU005','AKA','Eric','M','1999-02-18','eric.aka@mail.com','0700000005','Informatique','M2','2025-2026','Actif'),
 ('ETU006','YAO','Clarisse','F','2002-01-30','clarisse.yao@mail.com','0700000006','Data Science','M1','2025-2026','Actif'),
 ('ETU007','NGUESSAN','Paul','M','2001-05-10','paul.nguessan@mail.com','0700000007','Cybersecurite','M2','2025-2026','Suspendu'),
 ('ETU008','COULIBALY','Mariam','F','2000-03-22','mariam.coulibaly@mail.com','0700000008','Informatique','M1','2025-2026','Actif'),
 ('ETU009','BAMBA','Serge','M','2002-06-08','serge.bamba@mail.com','0700000009','Data Science','M2','2025-2026','Actif'),
 ('ETU010','GNAGNE','Estelle','F','2001-12-01','estelle.gnagne@mail.com','0700000010','Cybersecurite','M1','2025-2026','Actif');

INSERT INTO salle VALUES
 ('SAL001','Labo Scala',40,'Laboratoire'),
 ('SAL002','Labo Big Data',35,'Laboratoire'),
 ('SAL003','Salle A101',80,'Cours'),
 ('SAL004','Salle B202',60,'Cours'),
 ('SAL005','Amphi 1',250,'Amphitheatre');

INSERT INTO inscription VALUES
 ('INS001','ETU001','Informatique','M1','2025-2026','Validee'),
 ('INS002','ETU002','Informatique','M1','2025-2026','Validee'),
 ('INS003','ETU003','Data Science','M1','2025-2026','Validee'),
 ('INS004','ETU004','Cybersecurite','M1','2025-2026','Validee'),
 ('INS005','ETU005','Informatique','M2','2025-2026','Validee'),
 ('INS006','ETU006','Data Science','M1','2025-2026','Validee'),
 ('INS007','ETU007','Cybersecurite','M2','2025-2026','En attente'),
 ('INS008','ETU008','Informatique','M1','2025-2026','Validee'),
 ('INS009','ETU009','Data Science','M2','2025-2026','Validee'),
 ('INS010','ETU010','Cybersecurite','M1','2025-2026','Validee');

-- Notes
INSERT INTO note VALUES
 ('N001','ETU001','MAT001',14,15),
 ('N002','ETU001','MAT002',12,13),
 ('N003','ETU001','MAT003',16,14),
 ('N004','ETU002','MAT001',10,12),
 ('N005','ETU002','MAT002',9,11),
 ('N006','ETU003','MAT003',15,17),
 ('N007','ETU003','MAT004',14,16),
 ('N008','ETU004','MAT005',13,15),
 ('N009','ETU004','MAT006',11,12),
 ('N010','ETU005','MAT001',17,18),
 ('N011','ETU006','MAT004',12,14),
 ('N012','ETU007','MAT005',8,9),
 ('N013','ETU008','MAT001',13,13),
 ('N014','ETU009','MAT003',16,18),
 ('N015','ETU010','MAT005',15,16);

-- Absences
INSERT INTO absence VALUES
 ('ABS001','ETU001','MAT001','2025-10-02',2,FALSE),
 ('ABS002','ETU002','MAT001','2025-10-03',2,TRUE),
 ('ABS003','ETU003','MAT003','2025-10-05',4,FALSE),
 ('ABS004','ETU004','MAT005','2025-10-07',2,FALSE),
 ('ABS005','ETU005','MAT001','2025-10-10',2,TRUE),
 ('ABS006','ETU006','MAT004','2025-10-11',4,FALSE),
 ('ABS007','ETU007','MAT005','2025-10-12',6,FALSE),
 ('ABS008','ETU008','MAT001','2025-10-15',2,FALSE),
 ('ABS009','ETU009','MAT003','2025-10-16',2,TRUE),
 ('ABS010','ETU010','MAT005','2025-10-18',4,FALSE);

-- Emplois du temps
INSERT INTO seance_cours VALUES
 ('SEA001','MAT001','ENS001','SAL001','Lundi','08:00','10:00','Informatique','M1'),
 ('SEA002','MAT002','ENS002','SAL003','Mardi','10:00','12:00','Informatique','M1'),
 ('SEA003','MAT003','ENS005','SAL002','Mercredi','08:00','11:00','Data Science','M1'),
 ('SEA004','MAT004','ENS003','SAL004','Jeudi','13:00','16:00','Data Science','M1'),
 ('SEA005','MAT005','ENS004','SAL003','Vendredi','08:00','10:00','Cybersecurite','M1'),
 ('SEA006','MAT006','ENS005','SAL005','Lundi','14:00','16:00','Cybersecurite','M2');
