package universite.repository

import universite.db.Database
import universite.model.SeanceCours

import java.sql.{ResultSet, Time}
import java.time.LocalTime
import scala.collection.mutable.ListBuffer
import scala.util.{Try, Using}

class SeanceRepository {

  private def lire(rs: ResultSet): SeanceCours =
    SeanceCours(
      idSeance   = rs.getString("id_seance"),
      matiere    = rs.getString("matiere"),
      enseignant = rs.getString("enseignant"),
      salle      = rs.getString("salle"),
      jour       = rs.getString("jour"),
      heureDebut = rs.getTime("heure_debut").toLocalTime,
      heureFin   = rs.getTime("heure_fin").toLocalTime,
      filiere    = Option(rs.getString("filiere")).getOrElse(""),
      niveau     = Option(rs.getString("niveau")).getOrElse("")
    )

  def listerToutes(): Try[List[SeanceCours]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement("SELECT * FROM seance_cours ORDER BY jour, heure_debut")) { ps =>
        Using.resource(ps.executeQuery()) { rs =>
          val buf = ListBuffer.empty[SeanceCours]
          while (rs.next()) buf += lire(rs)
          buf.toList
        }
      }
    }

  def trouverParId(id: String): Try[Option[SeanceCours]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement("SELECT * FROM seance_cours WHERE id_seance = ?")) { ps =>
        ps.setString(1, id)
        Using.resource(ps.executeQuery()) { rs =>
          if (rs.next()) Some(lire(rs)) else None
        }
      }
    }

  def listerParFiliereEtNiveau(filiere: String, niveau: String): Try[List[SeanceCours]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement(
        "SELECT * FROM seance_cours WHERE filiere = ? AND niveau = ? ORDER BY jour, heure_debut"
      )) { ps =>
        ps.setString(1, filiere)
        ps.setString(2, niveau)
        Using.resource(ps.executeQuery()) { rs =>
          val buf = ListBuffer.empty[SeanceCours]
          while (rs.next()) buf += lire(rs)
          buf.toList
        }
      }
    }

  def listerParEnseignant(idEnseignant: String): Try[List[SeanceCours]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement(
        "SELECT * FROM seance_cours WHERE enseignant = ? ORDER BY jour, heure_debut"
      )) { ps =>
        ps.setString(1, idEnseignant)
        Using.resource(ps.executeQuery()) { rs =>
          val buf = ListBuffer.empty[SeanceCours]
          while (rs.next()) buf += lire(rs)
          buf.toList
        }
      }
    }

  def listerParSalle(idSalle: String): Try[List[SeanceCours]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement(
        "SELECT * FROM seance_cours WHERE salle = ? ORDER BY jour, heure_debut"
      )) { ps =>
        ps.setString(1, idSalle)
        Using.resource(ps.executeQuery()) { rs =>
          val buf = ListBuffer.empty[SeanceCours]
          while (rs.next()) buf += lire(rs)
          buf.toList
        }
      }
    }

  def enregistrer(s: SeanceCours): Try[SeanceCours] =
    Database.withConnection { conn =>
      val sql =
        """INSERT INTO seance_cours
          |(id_seance, matiere, enseignant, salle, jour, heure_debut, heure_fin, filiere, niveau)
          |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
          |ON CONFLICT (id_seance) DO UPDATE SET
          |  matiere     = EXCLUDED.matiere,
          |  enseignant  = EXCLUDED.enseignant,
          |  salle       = EXCLUDED.salle,
          |  jour        = EXCLUDED.jour,
          |  heure_debut = EXCLUDED.heure_debut,
          |  heure_fin   = EXCLUDED.heure_fin,
          |  filiere     = EXCLUDED.filiere,
          |  niveau      = EXCLUDED.niveau
          |""".stripMargin
      Using.resource(conn.prepareStatement(sql)) { ps =>
        ps.setString(1, s.idSeance)
        ps.setString(2, s.matiere)
        ps.setString(3, s.enseignant)
        ps.setString(4, s.salle)
        ps.setString(5, s.jour)
        ps.setTime(6, Time.valueOf(s.heureDebut))
        ps.setTime(7, Time.valueOf(s.heureFin))
        ps.setString(8, s.filiere)
        ps.setString(9, s.niveau)
        ps.executeUpdate()
        s
      }
    }

  def supprimer(id: String): Try[Int] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement("DELETE FROM seance_cours WHERE id_seance = ?")) { ps =>
        ps.setString(1, id)
        ps.executeUpdate()
      }
    }

  def prochainId(): Try[String] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement(
        "SELECT id_seance FROM seance_cours ORDER BY id_seance DESC LIMIT 1"
      )) { ps =>
        Using.resource(ps.executeQuery()) { rs =>
          if (rs.next()) {
            val last = rs.getString("id_seance")
            val num = last.drop(3).toInt + 1
            f"SEA$num%03d"
          } else "SEA001"
        }
      }
    }
}
