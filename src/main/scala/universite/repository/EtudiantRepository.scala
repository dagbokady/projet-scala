package universite.repository

import universite.db.Database
import universite.model._

import java.sql.ResultSet
import java.time.LocalDate
import scala.collection.mutable.ListBuffer
import scala.util.{Try, Using}

/**
 * Repository pour les etudiants : utile pour les jointures et l'affichage
 * dans les modules Notes, Absences et Emplois du temps.
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
}
