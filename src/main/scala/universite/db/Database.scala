package universite.db

import com.typesafe.config.{Config, ConfigFactory}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

import java.sql.Connection
import javax.sql.DataSource
import scala.util.{Try, Using}

/**
 * Gestion centralisee de la connexion PostgreSQL via HikariCP.
 * Lit la configuration depuis application.conf.
 */
object Database {

  private val config: Config = ConfigFactory.load()

  private lazy val ds: HikariDataSource = {
    val hc = new HikariConfig()
    hc.setJdbcUrl(config.getString("db.url"))
    hc.setUsername(config.getString("db.user"))
    hc.setPassword(config.getString("db.password"))
    hc.setDriverClassName(config.getString("db.driver"))
    hc.setMaximumPoolSize(config.getInt("db.pool.maxSize"))
    hc.setMinimumIdle(config.getInt("db.pool.minIdle"))
    hc.setPoolName("gestion-universitaire-pool")
    new HikariDataSource(hc)
  }

  def dataSource: DataSource = ds

  /** Obtient une connexion (a fermer apres usage). */
  def getConnection: Connection = ds.getConnection()

  /**
   * Execute un bloc avec une connexion qui sera automatiquement fermee.
   * Renvoie un `Try[A]` pour que les erreurs soient propagees proprement.
   */
  def withConnection[A](block: Connection => A): Try[A] =
    Using(ds.getConnection())(block)

  /** Ferme proprement le pool a l'arret de l'application. */
  def close(): Unit = if (!ds.isClosed) ds.close()
}
