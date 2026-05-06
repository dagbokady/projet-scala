package universite.repository

import universite.db.Database
import universite.model._

import java.sql.{Date, ResultSet}
import scala.collection.mutable.ListBuffer
import scala.util.{Try, Using}

/**
 * Repository des paiements - Module 8.
 */
class PaiementRepository {

  private def lire(rs: ResultSet): Paiement = Paiement(
    idPaiement   = rs.getString("id_paiement"),
    matricule    = rs.getString("matricule"),
    montantTotal = rs.getDouble("montant_total"),
    montantPaye  = rs.getDouble("montant_paye"),
    datePaiement = Option(rs.getDate("date_paiement")).map(_.toLocalDate),
    mode         = ModePaiement.fromString(Option(rs.getString("mode")).getOrElse(""))
  )

  def listerTous(): Try[List[Paiement]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement(
        "SELECT * FROM paiement ORDER BY id_paiement"
      )) { ps =>
        Using.resource(ps.executeQuery()) { rs =>
          val buf = ListBuffer.empty[Paiement]
          while (rs.next()) buf += lire(rs)
          buf.toList
        }
      }
    }

  def trouverParId(id: String): Try[Option[Paiement]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement(
        "SELECT * FROM paiement WHERE id_paiement = ?"
      )) { ps =>
        ps.setString(1, id)
        Using.resource(ps.executeQuery()) { rs =>
          if (rs.next()) Some(lire(rs)) else None
        }
      }
    }

  def listerParEtudiant(matricule: String): Try[List[Paiement]] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement(
        "SELECT * FROM paiement WHERE matricule = ? ORDER BY date_paiement DESC"
      )) { ps =>
        ps.setString(1, matricule)
        Using.resource(ps.executeQuery()) { rs =>
          val buf = ListBuffer.empty[Paiement]
          while (rs.next()) buf += lire(rs)
          buf.toList
        }
      }
    }

  def enregistrer(p: Paiement): Try[Paiement] =
    Database.withConnection { conn =>
      val sql =
        """INSERT INTO paiement (id_paiement, matricule, montant_total, montant_paye,
          |  date_paiement, mode)
          |VALUES (?, ?, ?, ?, ?, ?)
          |ON CONFLICT (id_paiement) DO UPDATE SET
          |  matricule = EXCLUDED.matricule, montant_total = EXCLUDED.montant_total,
          |  montant_paye = EXCLUDED.montant_paye, date_paiement = EXCLUDED.date_paiement,
          |  mode = EXCLUDED.mode
          |""".stripMargin
      Using.resource(conn.prepareStatement(sql)) { ps =>
        ps.setString(1, p.idPaiement)
        ps.setString(2, p.matricule)
        ps.setDouble(3, p.montantTotal)
        ps.setDouble(4, p.montantPaye)
        p.datePaiement match {
          case Some(d) => ps.setDate(5, Date.valueOf(d))
          case None    => ps.setNull(5, java.sql.Types.DATE)
        }
        ps.setString(6, ModePaiement.toString(p.mode))
        ps.executeUpdate()
      }
      p
    }

  def supprimer(id: String): Try[Int] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement("DELETE FROM paiement WHERE id_paiement = ?")) { ps =>
        ps.setString(1, id)
        ps.executeUpdate()
      }
    }

  def prochainId(): Try[String] =
    Database.withConnection { conn =>
      Using.resource(conn.prepareStatement(
        "SELECT COALESCE(MAX(id_paiement), 'PAY000') AS m FROM paiement"
      )) { ps =>
        Using.resource(ps.executeQuery()) { rs =>
          if (rs.next()) {
            val cur = rs.getString("m")
            val n = scala.util.Try(cur.substring(3).toInt).getOrElse(0) + 1
            f"PAY$n%03d"
          } else "PAY001"
        }
      }
    }
}
