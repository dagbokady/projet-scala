package universite.repository

import universite.db.Database
import universite.model._

import java.sql.ResultSet
import scala.collection.mutable.ListBuffer
import scala.util.{Try, Using}

/**
 * Repository des inscriptions - Module 4.
 * Contrainte BDD : UNIQUE (matricule, annee).
 */
class InscriptionRepository {

  private def lire(rs: ResultSet): Inscription = Inscription(
    idInscription = rs.getString("id_inscription"),
    matricule     = rs.getString("matricule"),
    filiere       = Option(rs.getString("filiere")).getOrElse(""),
    niveau        = Option(rs.getString("niveau")).getOrElse(""),
    annee         = Option(rs.getString("annee")).getOrElse(""),
    statut        = StatutInscription.fromString(Option(rs.getString("statut")).getOrElse(""))
  )

  def listerToutes(): Try[List[Inscription]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement(
        "SELECT * FROM inscription ORDER BY id_inscription"
      )) { ps =>
        Using.resource(ps.executeQuery()) { rs =>
          val buf = ListBuffer.empty[Inscription]
          while (rs.next()) buf += lire(rs)
          buf.toList
        }
      }
    }

  def trouverParId(id: String): Try[Option[Inscription]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement(
        "SELECT * FROM inscription WHERE id_inscription = ?"
      )) { ps =>
        ps.setString(1, id)
        Using.resource(ps.executeQuery()) { rs =>
          if (rs.next()) Some(lire(rs)) else None
        }
      }
    }

  def trouverParEtudiantEtAnnee(matricule: String, annee: String): Try[Option[Inscription]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement(
        "SELECT * FROM inscription WHERE matricule = ? AND annee = ?"
      )) { ps =>
        ps.setString(1, matricule)
        ps.setString(2, annee)
        Using.resource(ps.executeQuery()) { rs =>
          if (rs.next()) Some(lire(rs)) else None
        }
      }
    }

  def listerParEtudiant(matricule: String): Try[List[Inscription]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement(
        "SELECT * FROM inscription WHERE matricule = ? ORDER BY annee DESC"
      )) { ps =>
        ps.setString(1, matricule)
        Using.resource(ps.executeQuery()) { rs =>
          val buf = ListBuffer.empty[Inscription]
          while (rs.next()) buf += lire(rs)
          buf.toList
        }
      }
    }

  def listerParStatut(statut: String): Try[List[Inscription]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement(
        "SELECT * FROM inscription WHERE statut = ? ORDER BY id_inscription"
      )) { ps =>
        ps.setString(1, statut)
        Using.resource(ps.executeQuery()) { rs =>
          val buf = ListBuffer.empty[Inscription]
          while (rs.next()) buf += lire(rs)
          buf.toList
        }
      }
    }

  def enregistrer(i: Inscription): Try[Inscription] =
    Database.withConnection { conn =>
      val sql =
        """INSERT INTO inscription (id_inscription, matricule, filiere, niveau, annee, statut)
          |VALUES (?, ?, ?, ?, ?, ?)
          |ON CONFLICT (id_inscription) DO UPDATE SET
          |  matricule = EXCLUDED.matricule, filiere = EXCLUDED.filiere,
          |  niveau = EXCLUDED.niveau, annee = EXCLUDED.annee, statut = EXCLUDED.statut
          |""".stripMargin
      Using.resource(conn.prepareStatement(sql)) { ps =>
        ps.setString(1, i.idInscription)
        ps.setString(2, i.matricule)
        ps.setString(3, if (i.filiere.isEmpty) null else i.filiere)
        ps.setString(4, if (i.niveau.isEmpty)  null else i.niveau)
        ps.setString(5, if (i.annee.isEmpty)   null else i.annee)
        ps.setString(6, StatutInscription.toString(i.statut))
        ps.executeUpdate()
      }
      i
    }

  def supprimer(id: String): Try[Int] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement("DELETE FROM inscription WHERE id_inscription = ?")) { ps =>
        ps.setString(1, id)
        ps.executeUpdate()
      }
    }

  def prochainId(): Try[String] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement(
        "SELECT COALESCE(MAX(id_inscription), 'INS000') AS m FROM inscription"
      )) { ps =>
        Using.resource(ps.executeQuery()) { rs =>
          if (rs.next()) {
            val cur = rs.getString("m")
            val n = scala.util.Try(cur.substring(3).toInt).getOrElse(0) + 1
            f"INS$n%03d"
          } else "INS001"
        }
      }
    }
}
