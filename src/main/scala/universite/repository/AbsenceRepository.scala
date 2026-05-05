package universite.repository

import universite.db.Database
import universite.model.Absence

import java.sql.{Date, ResultSet}
import scala.collection.mutable.ListBuffer
import scala.util.{Try, Using}

class AbsenceRepository {

  private def lire(rs: ResultSet): Absence =
    Absence(
      idAbsence    = rs.getString("id_absence"),
      matricule    = rs.getString("matricule"),
      matiere      = rs.getString("matiere"),
      dateAbsence  = rs.getDate("date_absence").toLocalDate,
      heures       = rs.getInt("heures"),
      justifiee    = rs.getBoolean("justifiee")
    )

  def listerToutes(): Try[List[Absence]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement("SELECT * FROM absence ORDER BY date_absence DESC")) { ps =>
        Using.resource(ps.executeQuery()) { rs =>
          val buf = ListBuffer.empty[Absence]
          while (rs.next()) buf += lire(rs)
          buf.toList
        }
      }
    }

  def trouverParId(id: String): Try[Option[Absence]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement("SELECT * FROM absence WHERE id_absence = ?")) { ps =>
        ps.setString(1, id)
        Using.resource(ps.executeQuery()) { rs =>
          if (rs.next()) Some(lire(rs)) else None
        }
      }
    }

  def listerParEtudiant(matricule: String): Try[List[Absence]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement(
        "SELECT * FROM absence WHERE matricule = ? ORDER BY date_absence DESC"
      )) { ps =>
        ps.setString(1, matricule)
        Using.resource(ps.executeQuery()) { rs =>
          val buf = ListBuffer.empty[Absence]
          while (rs.next()) buf += lire(rs)
          buf.toList
        }
      }
    }

  def listerParMatiere(matiere: String): Try[List[Absence]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement(
        "SELECT * FROM absence WHERE matiere = ? ORDER BY date_absence DESC"
      )) { ps =>
        ps.setString(1, matiere)
        Using.resource(ps.executeQuery()) { rs =>
          val buf = ListBuffer.empty[Absence]
          while (rs.next()) buf += lire(rs)
          buf.toList
        }
      }
    }

  def enregistrer(a: Absence): Try[Absence] =
    Database.withConnection { conn =>
      val sql =
        """INSERT INTO absence (id_absence, matricule, matiere, date_absence, heures, justifiee)
          |VALUES (?, ?, ?, ?, ?, ?)
          |ON CONFLICT (id_absence) DO UPDATE
          |  SET matricule    = EXCLUDED.matricule,
          |      matiere      = EXCLUDED.matiere,
          |      date_absence = EXCLUDED.date_absence,
          |      heures       = EXCLUDED.heures,
          |      justifiee    = EXCLUDED.justifiee
          |""".stripMargin
      Using.resource(conn.prepareStatement(sql)) { ps =>
        ps.setString(1, a.idAbsence)
        ps.setString(2, a.matricule)
        ps.setString(3, a.matiere)
        ps.setDate(4, Date.valueOf(a.dateAbsence))
        ps.setInt(5, a.heures)
        ps.setBoolean(6, a.justifiee)
        ps.executeUpdate()
        a
      }
    }

  /** Met a jour uniquement le statut "justifiee" d'une absence. */
  def justifier(id: String, justifiee: Boolean): Try[Int] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement(
        "UPDATE absence SET justifiee = ? WHERE id_absence = ?"
      )) { ps =>
        ps.setBoolean(1, justifiee)
        ps.setString(2, id)
        ps.executeUpdate()
      }
    }

  def supprimer(id: String): Try[Int] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement("DELETE FROM absence WHERE id_absence = ?")) { ps =>
        ps.setString(1, id)
        ps.executeUpdate()
      }
    }

  def prochainId(): Try[String] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement(
        "SELECT id_absence FROM absence ORDER BY id_absence DESC LIMIT 1"
      )) { ps =>
        Using.resource(ps.executeQuery()) { rs =>
          if (rs.next()) {
            val last = rs.getString("id_absence")
            val num = last.drop(3).toInt + 1
            f"ABS$num%03d"
          } else "ABS001"
        }
      }
    }
}
