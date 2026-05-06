package universite.repository

import universite.db.Database
import universite.model._

import java.sql.{Date, ResultSet}
import scala.collection.mutable.ListBuffer
import scala.util.{Try, Using}

/**
 * Repository des etudiants - Module 1 (CRUD complet).
 */
class EtudiantRepository {

  private def lire(rs: ResultSet): Etudiant = {
    val rawDate = Option(rs.getDate("date_naissance")).map(_.toLocalDate)
    Etudiant(
      matricule     = rs.getString("matricule"),
      nom           = rs.getString("nom"),
      prenom        = rs.getString("prenom"),
      sexe          = Option(rs.getString("sexe")).getOrElse(""),
      dateNaissance = rawDate,
      email         = Option(rs.getString("email")).getOrElse(""),
      telephone     = Option(rs.getString("telephone")).getOrElse(""),
      filiere       = Option(rs.getString("filiere")).getOrElse(""),
      niveau        = Option(rs.getString("niveau")).getOrElse(""),
      annee         = Option(rs.getString("annee")).getOrElse(""),
      statut        = StatutEtudiant.fromString(Option(rs.getString("statut")).getOrElse("Inconnu"))
    )
  }

  // -------- Lecture --------

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

  def trouverParMatricule(matricule: String): Try[Option[Etudiant]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement("SELECT * FROM etudiant WHERE matricule = ?")) { ps =>
        ps.setString(1, matricule)
        Using.resource(ps.executeQuery()) { rs =>
          if (rs.next()) Some(lire(rs)) else None
        }
      }
    }

  def listerParFiliere(filiere: String): Try[List[Etudiant]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement(
        "SELECT * FROM etudiant WHERE filiere = ? ORDER BY matricule"
      )) { ps =>
        ps.setString(1, filiere)
        Using.resource(ps.executeQuery()) { rs =>
          val buf = ListBuffer.empty[Etudiant]
          while (rs.next()) buf += lire(rs)
          buf.toList
        }
      }
    }

  def listerParNiveau(niveau: String): Try[List[Etudiant]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement(
        "SELECT * FROM etudiant WHERE niveau = ? ORDER BY matricule"
      )) { ps =>
        ps.setString(1, niveau)
        Using.resource(ps.executeQuery()) { rs =>
          val buf = ListBuffer.empty[Etudiant]
          while (rs.next()) buf += lire(rs)
          buf.toList
        }
      }
    }

  def listerParStatut(statut: String): Try[List[Etudiant]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement(
        "SELECT * FROM etudiant WHERE statut = ? ORDER BY matricule"
      )) { ps =>
        ps.setString(1, statut)
        Using.resource(ps.executeQuery()) { rs =>
          val buf = ListBuffer.empty[Etudiant]
          while (rs.next()) buf += lire(rs)
          buf.toList
        }
      }
    }

  // -------- Ecriture (UPSERT) --------

  def enregistrer(e: Etudiant): Try[Etudiant] =
    Database.withConnection { conn =>
      val sql =
        """INSERT INTO etudiant (matricule, nom, prenom, sexe, date_naissance, email,
          |  telephone, filiere, niveau, annee, statut)
          |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          |ON CONFLICT (matricule) DO UPDATE SET
          |  nom = EXCLUDED.nom, prenom = EXCLUDED.prenom, sexe = EXCLUDED.sexe,
          |  date_naissance = EXCLUDED.date_naissance, email = EXCLUDED.email,
          |  telephone = EXCLUDED.telephone, filiere = EXCLUDED.filiere,
          |  niveau = EXCLUDED.niveau, annee = EXCLUDED.annee, statut = EXCLUDED.statut
          |""".stripMargin
      Using.resource(conn.prepareStatement(sql)) { ps =>
        ps.setString(1, e.matricule)
        ps.setString(2, e.nom)
        ps.setString(3, e.prenom)
        ps.setString(4, if (e.sexe.isEmpty) null else e.sexe)
        e.dateNaissance match {
          case Some(d) => ps.setDate(5, Date.valueOf(d))
          case None    => ps.setNull(5, java.sql.Types.DATE)
        }
        ps.setString(6, if (e.email.isEmpty) null else e.email)
        ps.setString(7, if (e.telephone.isEmpty) null else e.telephone)
        ps.setString(8, if (e.filiere.isEmpty) null else e.filiere)
        ps.setString(9, if (e.niveau.isEmpty) null else e.niveau)
        ps.setString(10, if (e.annee.isEmpty) null else e.annee)
        ps.setString(11, StatutEtudiant.toString(e.statut))
        ps.executeUpdate()
      }
      e
    }

  def supprimer(matricule: String): Try[Int] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement("DELETE FROM etudiant WHERE matricule = ?")) { ps =>
        ps.setString(1, matricule)
        ps.executeUpdate()
      }
    }

  /** Genere un identifiant matricule auto (ETU + numero). */
  def prochainMatricule(): Try[String] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement(
        "SELECT COALESCE(MAX(matricule), 'ETU000') AS m FROM etudiant"
      )) { ps =>
        Using.resource(ps.executeQuery()) { rs =>
          if (rs.next()) {
            val cur = rs.getString("m")
            val n = Try(cur.substring(3).toInt).getOrElse(0) + 1
            f"ETU$n%03d"
          } else "ETU001"
        }
      }
    }
}
