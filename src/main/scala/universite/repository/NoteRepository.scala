package universite.repository

import universite.db.Database
import universite.model.Note

import java.sql.ResultSet
import scala.collection.mutable.ListBuffer
import scala.util.{Try, Using}

class NoteRepository {

  private def lire(rs: ResultSet): Note = {
    val cc = {
      val v = rs.getBigDecimal("controle_continu")
      if (rs.wasNull() || v == null) None else Some(v.doubleValue())
    }
    val ex = {
      val v = rs.getBigDecimal("examen")
      if (rs.wasNull() || v == null) None else Some(v.doubleValue())
    }
    Note(
      idNote          = rs.getString("id_note"),
      matricule       = rs.getString("matricule"),
      matiere         = rs.getString("matiere"),
      controleContinu = cc,
      examen          = ex
    )
  }

  def listerToutes(): Try[List[Note]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement("SELECT * FROM note ORDER BY id_note")) { ps =>
        Using.resource(ps.executeQuery()) { rs =>
          val buf = ListBuffer.empty[Note]
          while (rs.next()) buf += lire(rs)
          buf.toList
        }
      }
    }

  def trouverParId(id: String): Try[Option[Note]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement("SELECT * FROM note WHERE id_note = ?")) { ps =>
        ps.setString(1, id)
        Using.resource(ps.executeQuery()) { rs =>
          if (rs.next()) Some(lire(rs)) else None
        }
      }
    }

  def listerParEtudiant(matricule: String): Try[List[Note]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement(
        "SELECT * FROM note WHERE matricule = ? ORDER BY matiere"
      )) { ps =>
        ps.setString(1, matricule)
        Using.resource(ps.executeQuery()) { rs =>
          val buf = ListBuffer.empty[Note]
          while (rs.next()) buf += lire(rs)
          buf.toList
        }
      }
    }

  def listerParMatiere(idMatiere: String): Try[List[Note]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement(
        "SELECT * FROM note WHERE matiere = ? ORDER BY matricule"
      )) { ps =>
        ps.setString(1, idMatiere)
        Using.resource(ps.executeQuery()) { rs =>
          val buf = ListBuffer.empty[Note]
          while (rs.next()) buf += lire(rs)
          buf.toList
        }
      }
    }

  /**
   * Insertion ou mise a jour atomique (UPSERT PostgreSQL).
   * On utilise la contrainte d'unicite sur (matricule, matiere).
   */
  def enregistrer(n: Note): Try[Note] =
    Database.withConnection { conn =>
      val sql =
        """INSERT INTO note (id_note, matricule, matiere, controle_continu, examen)
          |VALUES (?, ?, ?, ?, ?)
          |ON CONFLICT (matricule, matiere) DO UPDATE
          |  SET controle_continu = EXCLUDED.controle_continu,
          |      examen           = EXCLUDED.examen
          |""".stripMargin
      Using.resource(conn.prepareStatement(sql)) { ps =>
        ps.setString(1, n.idNote)
        ps.setString(2, n.matricule)
        ps.setString(3, n.matiere)
        n.controleContinu match {
          case Some(v) => ps.setBigDecimal(4, java.math.BigDecimal.valueOf(v))
          case None    => ps.setNull(4, java.sql.Types.NUMERIC)
        }
        n.examen match {
          case Some(v) => ps.setBigDecimal(5, java.math.BigDecimal.valueOf(v))
          case None    => ps.setNull(5, java.sql.Types.NUMERIC)
        }
        ps.executeUpdate()
        n
      }
    }

  def supprimer(id: String): Try[Int] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement("DELETE FROM note WHERE id_note = ?")) { ps =>
        ps.setString(1, id)
        ps.executeUpdate()
      }
    }

  /** Genere le prochain id de la forme N016, N017, ... */
  def prochainId(): Try[String] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement(
        "SELECT id_note FROM note ORDER BY id_note DESC LIMIT 1"
      )) { ps =>
        Using.resource(ps.executeQuery()) { rs =>
          if (rs.next()) {
            val last = rs.getString("id_note")
            val num = last.drop(1).toInt + 1
            f"N$num%03d"
          } else "N001"
        }
      }
    }
}
