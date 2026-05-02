package universite.service

import universite.model._
import scala.util.Try
import scala.io.Source

// ─────────────────────────────────────────────
//  Module 3 : Service Formation
// ─────────────────────────────────────────────

class FormationService(
  private var filieres: List[Filiere] = List.empty,
  private var matieres: List[Matiere] = List.empty
) {

  // ── Chargement CSV ────────────────────────

  def chargerFilieres(chemin: String): Unit =
    Try(Source.fromFile(chemin)) match {
      case scala.util.Success(src) =>
        filieres = src.getLines().toList.tail.flatMap(Filiere.fromCsv)
        src.close()
        println(s"✓ ${filieres.length} filières chargées.")
      case scala.util.Failure(err) =>
        println(s"✗ Erreur filières : ${err.getMessage}")
    }

  def chargerMatieres(chemin: String): Unit =
    Try(Source.fromFile(chemin)) match {
      case scala.util.Success(src) =>
        matieres = src.getLines().toList.tail.flatMap(Matiere.fromCsv)
        src.close()
        println(s"✓ ${matieres.length} matières chargées.")
      case scala.util.Failure(err) =>
        println(s"✗ Erreur matières : ${err.getMessage}")
    }

  // ── Filières ──────────────────────────────

  def ajouterFiliere(f: Filiere): Unit = {
    if (!f.estValide) throw new IllegalArgumentException(s"Filière invalide : ${f.idFiliere}")
    filieres = f :: filieres
    println(s"✓ Filière ${f.nomFiliere} ajoutée.")
  }

  def rechercherFiliere(idOuNom: String): Option[Filiere] =
    filieres.find(f => f.idFiliere == idOuNom || f.nomFiliere.equalsIgnoreCase(idOuNom))

  def afficherFilieres(): Unit = {
    println(s"\n═══ Filières (${filieres.length}) ═══")
    filieres.foreach(f => println(s"  [${f.idFiliere}] ${f.nomFiliere} — Responsable: ${f.responsable}"))
  }

  // ── Matières ──────────────────────────────

  def ajouterMatiere(m: Matiere): Unit = {
    if (!m.estValide) throw new IllegalArgumentException(s"Matière invalide : ${m.idMatiere}")
    matieres = m :: matieres
    println(s"✓ Matière ${m.nomMatiere} ajoutée.")
  }

  def rechercherMatiere(id: String): Option[Matiere] =
    matieres.find(_.idMatiere == id)

  def matiereParEnseignant(idEnseignant: String): List[Matiere] =
    matieres.filter(_.idEnseignant == idEnseignant)

  def matiereParUE(ue: String): List[Matiere] =
    matieres.filter(_.ue.equalsIgnoreCase(ue))

  def afficherMatieres(): Unit = {
    println(s"\n═══ Matières (${matieres.length}) ═══")
    matieres.foreach(_.afficher())
  }

  // ── Récursivité ───────────────────────────

  /** Calcule récursivement le volume horaire total */
  def volumeTotalRecursif(liste: List[Matiere]): Int =
    liste match {
      case Nil           => 0
      case tete :: queue => tete.volumeHoraire + volumeTotalRecursif(queue)
    }

  /** Calcule récursivement le coefficient total */
  def coeffTotalRecursif(liste: List[Matiere]): Int =
    liste match {
      case Nil           => 0
      case tete :: queue => tete.coefficient + coeffTotalRecursif(queue)
    }

  // ── Statistiques ──────────────────────────

  /** Volume horaire par enseignant */
  def volumeParEnseignant: Map[String, Int] =
    matieres.groupBy(_.idEnseignant).map { case (k, v) => k -> v.map(_.volumeHoraire).sum }

  /** Enseignant le plus chargé (Option) */
  def enseignantPlusCharge: Option[String] =
    if (matieres.isEmpty) None
    else Some(volumeParEnseignant.maxBy(_._2)._1)

  /** Nombre de matières par UE */
  def matieresParUE: Map[String, Int] =
    matieres.groupBy(_.ue).map { case (ue, lst) => ue -> lst.length }

  def statistiques(): Unit = {
    println("\n═══ Statistiques Formation ═══")
    println(s"  Filières  : ${filieres.length}")
    println(s"  Matières  : ${matieres.length}")
    println(s"  Volume total (récursif) : ${volumeTotalRecursif(matieres)}h")
    println(s"  Coeff total (récursif)  : ${coeffTotalRecursif(matieres)}")
    println(s"  Enseignant + chargé     : ${enseignantPlusCharge.getOrElse("N/A")}")
    println(s"  Par UE :")
    matieresParUE.foreach { case (ue, n) => println(s"    - $ue : $n matière(s)") }
  }

  def toutesLesFilieres: List[Filiere] = filieres
  def toutesLesMatieres: List[Matiere] = matieres
}
