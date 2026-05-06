package universite.repository

import universite.db.Database
import universite.model._

import java.sql.ResultSet
import scala.collection.mutable.ListBuffer
import scala.util.{Try, Using}

// =====================================================================
// MatiereRepository - Module 3 (Formations)
// =====================================================================
class MatiereRepository {
  private def lire(rs: ResultSet): Matiere =
    Matiere(
      idMatiere     = rs.getString("id_matiere"),
      nomMatiere    = rs.getString("nom_matiere"),
      ue            = Option(rs.getString("ue")).getOrElse(""),
      coefficient   = rs.getInt("coefficient"),
      volumeHoraire = rs.getInt("volume_horaire"),
      enseignant    = Option(rs.getString("enseignant")).getOrElse("")
    )

  def listerToutes(): Try[List[Matiere]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement("SELECT * FROM matiere ORDER BY id_matiere")) { ps =>
        Using.resource(ps.executeQuery()) { rs =>
          val buf = ListBuffer.empty[Matiere]
          while (rs.next()) buf += lire(rs)
          buf.toList
        }
      }
    }

  def trouverParId(id: String): Try[Option[Matiere]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement("SELECT * FROM matiere WHERE id_matiere = ?")) { ps =>
        ps.setString(1, id)
        Using.resource(ps.executeQuery()) { rs =>
          if (rs.next()) Some(lire(rs)) else None
        }
      }
    }

  def listerParEnseignant(idEns: String): Try[List[Matiere]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement(
        "SELECT * FROM matiere WHERE enseignant = ? ORDER BY id_matiere"
      )) { ps =>
        ps.setString(1, idEns)
        Using.resource(ps.executeQuery()) { rs =>
          val buf = ListBuffer.empty[Matiere]
          while (rs.next()) buf += lire(rs)
          buf.toList
        }
      }
    }

  def enregistrer(m: Matiere): Try[Matiere] =
    Database.withConnection { conn =>
      val sql =
        """INSERT INTO matiere (id_matiere, nom_matiere, ue, coefficient, volume_horaire, enseignant)
          |VALUES (?, ?, ?, ?, ?, ?)
          |ON CONFLICT (id_matiere) DO UPDATE SET
          |  nom_matiere = EXCLUDED.nom_matiere, ue = EXCLUDED.ue,
          |  coefficient = EXCLUDED.coefficient, volume_horaire = EXCLUDED.volume_horaire,
          |  enseignant = EXCLUDED.enseignant
          |""".stripMargin
      Using.resource(conn.prepareStatement(sql)) { ps =>
        ps.setString(1, m.idMatiere)
        ps.setString(2, m.nomMatiere)
        ps.setString(3, if (m.ue.isEmpty) null else m.ue)
        ps.setInt(4, m.coefficient)
        ps.setInt(5, m.volumeHoraire)
        ps.setString(6, if (m.enseignant.isEmpty) null else m.enseignant)
        ps.executeUpdate()
      }
      m
    }

  def supprimer(id: String): Try[Int] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement("DELETE FROM matiere WHERE id_matiere = ?")) { ps =>
        ps.setString(1, id)
        ps.executeUpdate()
      }
    }
}

// =====================================================================
// EnseignantRepository - Module 2
// =====================================================================
class EnseignantRepository {
  private def lire(rs: ResultSet): Enseignant =
    Enseignant(
      idEnseignant = rs.getString("id_enseignant"),
      nom          = rs.getString("nom"),
      prenom       = rs.getString("prenom"),
      grade        = Option(rs.getString("grade")).getOrElse(""),
      specialite   = Option(rs.getString("specialite")).getOrElse(""),
      departement  = Option(rs.getString("departement")).getOrElse(""),
      email        = Option(rs.getString("email")).getOrElse(""),
      telephone    = Option(rs.getString("telephone")).getOrElse("")
    )

  def listerTous(): Try[List[Enseignant]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement("SELECT * FROM enseignant ORDER BY id_enseignant")) { ps =>
        Using.resource(ps.executeQuery()) { rs =>
          val buf = ListBuffer.empty[Enseignant]
          while (rs.next()) buf += lire(rs)
          buf.toList
        }
      }
    }

  def trouverParId(id: String): Try[Option[Enseignant]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement("SELECT * FROM enseignant WHERE id_enseignant = ?")) { ps =>
        ps.setString(1, id)
        Using.resource(ps.executeQuery()) { rs =>
          if (rs.next()) Some(lire(rs)) else None
        }
      }
    }

  def listerParDepartement(dep: String): Try[List[Enseignant]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement(
        "SELECT * FROM enseignant WHERE departement = ? ORDER BY id_enseignant"
      )) { ps =>
        ps.setString(1, dep)
        Using.resource(ps.executeQuery()) { rs =>
          val buf = ListBuffer.empty[Enseignant]
          while (rs.next()) buf += lire(rs)
          buf.toList
        }
      }
    }

  def enregistrer(e: Enseignant): Try[Enseignant] =
    Database.withConnection { conn =>
      val sql =
        """INSERT INTO enseignant (id_enseignant, nom, prenom, grade, specialite,
          |  departement, email, telephone)
          |VALUES (?, ?, ?, ?, ?, ?, ?, ?)
          |ON CONFLICT (id_enseignant) DO UPDATE SET
          |  nom = EXCLUDED.nom, prenom = EXCLUDED.prenom, grade = EXCLUDED.grade,
          |  specialite = EXCLUDED.specialite, departement = EXCLUDED.departement,
          |  email = EXCLUDED.email, telephone = EXCLUDED.telephone
          |""".stripMargin
      Using.resource(conn.prepareStatement(sql)) { ps =>
        ps.setString(1, e.idEnseignant)
        ps.setString(2, e.nom)
        ps.setString(3, e.prenom)
        ps.setString(4, if (e.grade.isEmpty) null else e.grade)
        ps.setString(5, if (e.specialite.isEmpty) null else e.specialite)
        ps.setString(6, if (e.departement.isEmpty) null else e.departement)
        ps.setString(7, if (e.email.isEmpty) null else e.email)
        ps.setString(8, if (e.telephone.isEmpty) null else e.telephone)
        ps.executeUpdate()
      }
      e
    }

  def supprimer(id: String): Try[Int] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement("DELETE FROM enseignant WHERE id_enseignant = ?")) { ps =>
        ps.setString(1, id)
        ps.executeUpdate()
      }
    }
}

// =====================================================================
// SalleRepository
// =====================================================================
class SalleRepository {
  private def lire(rs: ResultSet): Salle =
    Salle(
      idSalle   = rs.getString("id_salle"),
      nomSalle  = rs.getString("nom_salle"),
      capacite  = rs.getInt("capacite"),
      typeSalle = Option(rs.getString("type_salle")).getOrElse("")
    )

  def listerToutes(): Try[List[Salle]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement("SELECT * FROM salle ORDER BY id_salle")) { ps =>
        Using.resource(ps.executeQuery()) { rs =>
          val buf = ListBuffer.empty[Salle]
          while (rs.next()) buf += lire(rs)
          buf.toList
        }
      }
    }
}

// =====================================================================
// FiliereRepository - Module 3
// =====================================================================
class FiliereRepository {
  private def lire(rs: ResultSet): Filiere =
    Filiere(
      idFiliere   = rs.getString("id_filiere"),
      nomFiliere  = rs.getString("nom_filiere"),
      responsable = Option(rs.getString("responsable")).getOrElse("")
    )

  def listerToutes(): Try[List[Filiere]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement("SELECT * FROM filiere ORDER BY id_filiere")) { ps =>
        Using.resource(ps.executeQuery()) { rs =>
          val buf = ListBuffer.empty[Filiere]
          while (rs.next()) buf += lire(rs)
          buf.toList
        }
      }
    }

  def trouverParId(id: String): Try[Option[Filiere]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement("SELECT * FROM filiere WHERE id_filiere = ?")) { ps =>
        ps.setString(1, id)
        Using.resource(ps.executeQuery()) { rs =>
          if (rs.next()) Some(lire(rs)) else None
        }
      }
    }

  def enregistrer(f: Filiere): Try[Filiere] =
    Database.withConnection { conn =>
      val sql =
        """INSERT INTO filiere (id_filiere, nom_filiere, responsable)
          |VALUES (?, ?, ?)
          |ON CONFLICT (id_filiere) DO UPDATE SET
          |  nom_filiere = EXCLUDED.nom_filiere, responsable = EXCLUDED.responsable
          |""".stripMargin
      Using.resource(conn.prepareStatement(sql)) { ps =>
        ps.setString(1, f.idFiliere)
        ps.setString(2, f.nomFiliere)
        ps.setString(3, if (f.responsable.isEmpty) null else f.responsable)
        ps.executeUpdate()
      }
      f
    }

  def supprimer(id: String): Try[Int] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement("DELETE FROM filiere WHERE id_filiere = ?")) { ps =>
        ps.setString(1, id)
        ps.executeUpdate()
      }
    }
}
