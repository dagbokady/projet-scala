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

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.util.{Failure, Success}

/**
 * Point d'entree de l'application.
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

  private val noteRoutes        = new NoteRoutes()
  private val absenceRoutes     = new AbsenceRoutes()
  private val seanceRoutes      = new SeanceRoutes()
  private val refRoutes         = new ReferentielRoutes()
  private val etudiantRoutes    = new EtudiantRoutes()
  private val enseignantRoutes  = new EnseignantRoutes()
  private val formationRoutes   = new FormationRoutes()
  private val inscriptionRoutes = new InscriptionRoutes()
  private val paiementRoutes    = new PaiementRoutes()
  private val dashboardRoutes   = new TableauDeBordRoutes()
  private val bigdataRoutes     = new BigDataRoutes()

  // API endpoints sous /api
  private val apiRoutes: Route = pathPrefix("api") {
    concat(
      noteRoutes.routes,
      absenceRoutes.routes,
      seanceRoutes.routes,
      refRoutes.routes,
      etudiantRoutes.routes,
      enseignantRoutes.routes,
      formationRoutes.routes,
      inscriptionRoutes.routes,
      paiementRoutes.routes,
      dashboardRoutes.routes,
      bigdataRoutes.routes
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
      val addr = b.localAddress
      log.info(s"Serveur demarre sur http://${addr.getHostString}:${addr.getPort}")
      log.info(s"Front : http://localhost:$port/")
      log.info(s"API   : 11 modules disponibles sous /api/{etudiants,enseignants,filieres,matieres,inscriptions,notes,absences,seances,paiements,dashboard,bigdata}")
      log.info("Appuyez sur Ctrl+C pour arreter le serveur.")
    case Failure(ex) =>
      log.error(s"Echec demarrage serveur : ${ex.getMessage}")
      system.terminate()
  }

  // Hook d'arret propre
  sys.addShutdownHook {
    log.info("Arret en cours...")
    try { Database.close() } catch { case _: Throwable => () }
    try { system.terminate() } catch { case _: Throwable => () }
  }

  // BLOQUER LE THREAD PRINCIPAL : sans cette ligne, sbt run termine
  // immediatement apres le binding et le serveur est tue.
  Await.result(system.whenTerminated, Duration.Inf)
}
