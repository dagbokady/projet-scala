package universite.repository

import universite.db.Database
import universite.model._

import java.sql.ResultSet
import scala.collection.mutable.ListBuffer
import scala.util.{Try, Using}

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
}

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
}

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
