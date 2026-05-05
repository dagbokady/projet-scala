package universite

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import universite.api._
import universite.db.Database
import universite.util.CorsHandler

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

/**
 * Point d'entree de l'application :
 *
 *  1. Charge la configuration
 *  2. Initialise le pool de connexions PostgreSQL
 *  3. Demarre un serveur Akka HTTP qui sert :
 *      - l'API REST pour les modules 5, 6, 7
 *      - les fichiers du front (HTML/CSS/JS) sous /
 *
 *  Les fichiers du front sont cherches dans le dossier `frontend/` a la racine
 *  du projet.
 */
object Main extends App with CorsHandler {

  private val log = LoggerFactory.getLogger(getClass)

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "gestion-universitaire")
  implicit val ec: ExecutionContextExecutor = system.executionContext

  private val config = ConfigFactory.load()
  private val host   = config.getString("server.host")
  private val port   = config.getInt("server.port")

  // Verification de la connexion BDD au demarrage
  Database.withConnection { conn =>
    val st = conn.createStatement()
    val rs = st.executeQuery("SELECT 1")
    rs.next()
    rs.close(); st.close()
  } match {
    case Success(_)  => log.info("Connexion PostgreSQL : OK")
    case Failure(ex) =>
      log.error(s"Echec connexion BDD : ${ex.getMessage}")
      log.error("Verifiez application.conf et que la base est accessible.")
  }

  private val noteRoutes   = new NoteRoutes()
  private val absenceRoutes = new AbsenceRoutes()
  private val seanceRoutes = new SeanceRoutes()
  private val refRoutes    = new ReferentielRoutes()

  // API endpoints sous /api
  private val apiRoutes: Route = pathPrefix("api") {
    concat(
      noteRoutes.routes,
      absenceRoutes.routes,
      seanceRoutes.routes,
      refRoutes.routes
    )
  }

  // Frontend statique
  private val staticRoutes: Route = concat(
    pathSingleSlash {
      getFromFile("frontend/index.html")
    },
    pathPrefix("css") {
      getFromDirectory("frontend/css")
    },
    pathPrefix("js") {
      getFromDirectory("frontend/js")
    },
    path(Segment) { name =>
      getFromFile(s"frontend/$name")
    }
  )

  private val routes: Route = corsHandler(apiRoutes ~ staticRoutes)

  private val bindingFuture = Http().newServerAt(host, port).bind(routes)

  bindingFuture.onComplete {
    case Success(b) =>
      log.info(s"Serveur demarre sur http://${b.localAddress.getHostString}:${b.localAddress.getPort}")
      log.info(s"Front : http://localhost:$port/")
      log.info(s"API   : http://localhost:$port/api/notes, /api/absences, /api/seances")
    case Failure(ex) =>
      log.error(s"Echec demarrage serveur : ${ex.getMessage}")
      system.terminate()
  }
  scala.io.StdIn.readLine()
  // Hook d'arret propre
  sys.addShutdownHook {
    log.info("Arret en cours...")
    Database.close()
    system.terminate()
  }
}
