// emplois.js — Module 7 : Gestion des emplois du temps
//
// KPIs (séances, conflits, salles, enseignants), grille hebdomadaire
// (Lundi -> Vendredi × créneaux horaires) avec filtre filière+niveau,
// table liste, conflits, modal de création avec POST /seances/verifier
// en preview avant le POST final.

(function () {
  let etatSeances = [];
  const JOURS = ['Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi'];
  // Créneaux horaires affichés (08h -> 18h, par tranches d'une heure)
  const CRENEAUX = [];
  for (let h = 8; h < 18; h++) CRENEAUX.push(h);

  function parseHeure(s) {
    // "08:00" -> 8.0, "08:30" -> 8.5
    if (!s) return null;
    const [h, m] = s.split(':').map(x => parseInt(x, 10));
    return h + (m || 0) / 60;
  }

  async function chargerKpis() {
    try {
      const conflits = await api.seances.conflits();
      const sallesUniq = new Set(etatSeances.map(s => s.idSalle));
      const ensUniq = new Set(etatSeances.map(s => s.idEnseignant));
      document.getElementById('kpi-seances').textContent = etatSeances.length;
      document.getElementById('kpi-conflits').textContent = (conflits || []).length;
      document.getElementById('kpi-salles').textContent  = sallesUniq.size;
      document.getElementById('kpi-ens').textContent     = ensUniq.size;

      // Souligner si conflits
      const kpi = document.getElementById('kpi-conflits').closest('.kpi-card');
      if (kpi) kpi.classList.toggle('kpi-warn', (conflits || []).length > 0);
    } catch (e) {
      // silencieux
    }
  }

  function remplirFiltresGrille() {
    const filieres = Array.from(new Set((utils.refCache.etudiants || []).map(e => e.filiere))).sort();
    const niveaux  = Array.from(new Set((utils.refCache.etudiants || []).map(e => e.niveau))).sort();
    const selF = document.getElementById('grid-filiere');
    const selN = document.getElementById('grid-niveau');
    selF.innerHTML = filieres.map(f => '<option value="' + utils.escapeHtml(f) + '">'
      + utils.escapeHtml(f) + '</option>').join('');
    selN.innerHTML = niveaux.map(n => '<option value="' + utils.escapeHtml(n) + '">'
      + utils.escapeHtml(n) + '</option>').join('');
  }

  async function rendreGrille() {
    const filiere = document.getElementById('grid-filiere').value;
    const niveau  = document.getElementById('grid-niveau').value;
    const grille  = document.getElementById('schedule');

    let seances;
    try {
      seances = (filiere && niveau)
        ? await api.seances.parClasse(filiere, niveau)
        : etatSeances;
    } catch (e) {
      grille.innerHTML = '<div class="empty">Erreur : ' + utils.escapeHtml(e.message) + '</div>';
      return;
    }

    // En-tête : col vide + 5 jours
    let html = '<div class="schedule-cell schedule-head"></div>';
    JOURS.forEach(j => {
      html += '<div class="schedule-cell schedule-head">' + j + '</div>';
    });

    // Pour chaque créneau horaire
    CRENEAUX.forEach(h => {
      const heureLabel = (h < 10 ? '0' + h : h) + ':00';
      html += '<div class="schedule-cell schedule-time">' + heureLabel + '</div>';
      JOURS.forEach(jour => {
        // Trouver les séances qui se déroulent dans cette case
        const matches = (seances || []).filter(s => {
          if (s.jour !== jour) return false;
          const hd = parseHeure(s.heureDebut);
          const hf = parseHeure(s.heureFin);
          return hd <= h && hf > h;
        });
        if (matches.length === 0) {
          html += '<div class="schedule-cell"></div>';
        } else {
          html += '<div class="schedule-cell">'
            + matches.map(s => {
                const hd = parseHeure(s.heureDebut);
                const hf = parseHeure(s.heureFin);
                const debut = (hd === h); // afficher seulement à l'heure de début
                if (!debut) return ''; // pas de duplication sur les heures couvertes
                const span = Math.max(1, hf - hd);
                return '<div class="schedule-event" style="--span:' + span + '" '
                  + 'title="' + utils.escapeHtml(utils.nomMatiere(s.idMatiere)
                      + ' • ' + utils.nomEnseignant(s.idEnseignant)
                      + ' • ' + utils.nomSalle(s.idSalle)) + '">'
                  + '<strong>' + utils.escapeHtml(utils.nomMatiere(s.idMatiere)) + '</strong>'
                  + '<span class="muted">' + utils.formatHeure(s.heureDebut)
                      + '–' + utils.formatHeure(s.heureFin) + '</span>'
                  + '<span>' + utils.escapeHtml(utils.nomSalle(s.idSalle)) + '</span>'
                  + '</div>';
              }).join('')
            + '</div>';
        }
      });
    });

    grille.innerHTML = html;
  }

  function rendreTable() {
    const tbody = document.getElementById('seances-tbody');
    if (etatSeances.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8" class="empty">Aucune séance enregistrée.</td></tr>';
      return;
    }
    tbody.innerHTML = etatSeances.map(s =>
      '<tr>'
      + '<td><code>' + utils.escapeHtml(s.idSeance) + '</code></td>'
      + '<td>' + utils.escapeHtml(utils.nomMatiere(s.idMatiere)) + '</td>'
      + '<td>' + utils.escapeHtml(utils.nomEnseignant(s.idEnseignant)) + '</td>'
      + '<td>' + utils.escapeHtml(utils.nomSalle(s.idSalle)) + '</td>'
      + '<td>' + utils.escapeHtml(s.jour) + '</td>'
      + '<td>' + utils.formatHeure(s.heureDebut) + '–' + utils.formatHeure(s.heureFin) + '</td>'
      + '<td>' + utils.escapeHtml(s.filiere) + ' / ' + utils.escapeHtml(s.niveau) + '</td>'
      + '<td><button class="btn btn-sm btn-danger" data-action="del-seance" data-id="'
          + utils.escapeHtml(s.idSeance) + '">Suppr.</button></td>'
      + '</tr>'
    ).join('');
  }

  async function chargerSeances() {
    try {
      etatSeances = await api.seances.liste() || [];
      rendreTable();
      await chargerKpis();
      await rendreGrille();
    } catch (e) {
      utils.showToast('Erreur chargement séances : ' + e.message, 'error');
    }
  }

  async function rendreConflits() {
    const wrap = document.getElementById('conflits-list');
    wrap.innerHTML = '<div class="muted">Calcul…</div>';
    try {
      const data = await api.seances.conflits() || [];
      if (data.length === 0) {
        wrap.innerHTML = '<div class="empty">Aucun conflit détecté — emploi du temps cohérent.</div>';
        return;
      }
      wrap.innerHTML = data.map(c => {
        const a = c.seanceA, b = c.seanceB;
        return '<div class="conflit-card">'
          + '<div class="conflit-pair">'
          +   '<div class="conflit-seance">'
          +     '<strong>' + utils.escapeHtml(utils.nomMatiere(a.idMatiere)) + '</strong>'
          +     '<span class="muted">' + utils.escapeHtml(a.jour) + ' '
          +       utils.formatHeure(a.heureDebut) + '–' + utils.formatHeure(a.heureFin) + '</span>'
          +     '<span>' + utils.escapeHtml(utils.nomSalle(a.idSalle)) + ' • '
          +       utils.escapeHtml(utils.nomEnseignant(a.idEnseignant)) + '</span>'
          +   '</div>'
          +   '<div class="conflit-vs">⚡</div>'
          +   '<div class="conflit-seance">'
          +     '<strong>' + utils.escapeHtml(utils.nomMatiere(b.idMatiere)) + '</strong>'
          +     '<span class="muted">' + utils.escapeHtml(b.jour) + ' '
          +       utils.formatHeure(b.heureDebut) + '–' + utils.formatHeure(b.heureFin) + '</span>'
          +     '<span>' + utils.escapeHtml(utils.nomSalle(b.idSalle)) + ' • '
          +       utils.escapeHtml(utils.nomEnseignant(b.idEnseignant)) + '</span>'
          +   '</div>'
          + '</div>'
          + '<div class="conflit-raison"><span class="badge badge-bad">Conflit</span> '
          +   utils.escapeHtml(c.raison || 'Chevauchement détecté') + '</div>'
          + '</div>';
      }).join('');
    } catch (e) {
      wrap.innerHTML = '<div class="empty">Erreur : ' + utils.escapeHtml(e.message) + '</div>';
    }
  }

  function ouvrirFormulaire() {
    const optMat = (utils.refCache.matieres || []).map(m =>
      '<option value="' + utils.escapeHtml(m.idMatiere) + '">'
      + utils.escapeHtml(m.nomMatiere) + '</option>'
    ).join('');
    const optEns = (utils.refCache.enseignants || []).map(e =>
      '<option value="' + utils.escapeHtml(e.idEnseignant) + '">'
      + utils.escapeHtml(e.prenom + ' ' + e.nom) + '</option>'
    ).join('');
    const optSal = (utils.refCache.salles || []).map(s =>
      '<option value="' + utils.escapeHtml(s.idSalle) + '">'
      + utils.escapeHtml(s.nomSalle + ' (' + s.capacite + ' places)') + '</option>'
    ).join('');
    const optJour = JOURS.map(j => '<option value="' + j + '">' + j + '</option>').join('');
    const filieres = Array.from(new Set((utils.refCache.etudiants || []).map(e => e.filiere))).sort();
    const niveaux  = Array.from(new Set((utils.refCache.etudiants || []).map(e => e.niveau))).sort();
    const optFil = filieres.map(f => '<option value="' + utils.escapeHtml(f) + '">'
      + utils.escapeHtml(f) + '</option>').join('');
    const optNiv = niveaux.map(n => '<option value="' + utils.escapeHtml(n) + '">'
      + utils.escapeHtml(n) + '</option>').join('');

    const html =
      '<form id="form-seance">'
      + '<div class="form-row">'
      +   '<div class="form-group"><label class="form-label">Matière</label>'
      +     '<select class="form-select" name="idMatiere" required>' + optMat + '</select></div>'
      +   '<div class="form-group"><label class="form-label">Enseignant</label>'
      +     '<select class="form-select" name="idEnseignant" required>' + optEns + '</select></div>'
      + '</div>'
      + '<div class="form-row">'
      +   '<div class="form-group"><label class="form-label">Salle</label>'
      +     '<select class="form-select" name="idSalle" required>' + optSal + '</select></div>'
      +   '<div class="form-group"><label class="form-label">Jour</label>'
      +     '<select class="form-select" name="jour" required>' + optJour + '</select></div>'
      + '</div>'
      + '<div class="form-row">'
      +   '<div class="form-group"><label class="form-label">Heure début</label>'
      +     '<input class="form-input" type="time" name="heureDebut" value="08:00" required></div>'
      +   '<div class="form-group"><label class="form-label">Heure fin</label>'
      +     '<input class="form-input" type="time" name="heureFin" value="10:00" required></div>'
      + '</div>'
      + '<div class="form-row">'
      +   '<div class="form-group"><label class="form-label">Filière</label>'
      +     '<select class="form-select" name="filiere" required>' + optFil + '</select></div>'
      +   '<div class="form-group"><label class="form-label">Niveau</label>'
      +     '<select class="form-select" name="niveau" required>' + optNiv + '</select></div>'
      + '</div>'
      + '<div id="seance-preview" class="preview-zone"></div>'
      + '<div class="form-actions">'
      +   '<button type="button" class="btn btn-ghost" data-close-modal>Annuler</button>'
      +   '<button type="button" class="btn" id="btn-verifier-seance">Vérifier les conflits</button>'
      +   '<button type="submit" class="btn btn-primary">Créer la séance</button>'
      + '</div></form>';

    utils.openModal('Nouvelle séance', html);

    function lirePayload() {
      const f = document.getElementById('form-seance');
      return {
        idMatiere:    f.idMatiere.value,
        idEnseignant: f.idEnseignant.value,
        idSalle:      f.idSalle.value,
        jour:         f.jour.value,
        heureDebut:   f.heureDebut.value + ':00',
        heureFin:     f.heureFin.value + ':00',
        filiere:      f.filiere.value,
        niveau:       f.niveau.value
      };
    }

    document.getElementById('btn-verifier-seance').addEventListener('click', async () => {
      const preview = document.getElementById('seance-preview');
      preview.innerHTML = '<div class="muted">Vérification…</div>';
      try {
        const res = await api.seances.verifier(lirePayload());
        const conflits = (res && res.conflits) || [];
        if (conflits.length === 0) {
          preview.innerHTML = '<div class="preview-ok"><span class="badge badge-ok">✓ Aucun conflit</span> '
            + 'La séance peut être créée.</div>';
        } else {
          preview.innerHTML = '<div class="preview-warn"><span class="badge badge-bad">'
            + conflits.length + ' conflit(s) détecté(s)</span><ul>'
            + conflits.map(c => '<li>' + utils.escapeHtml(c.raison || 'chevauchement')
                + ' avec <code>' + utils.escapeHtml(c.seanceA?.idSeance || c.seanceB?.idSeance || '?')
                + '</code></li>').join('')
            + '</ul></div>';
        }
      } catch (e) {
        preview.innerHTML = '<div class="empty">Erreur : ' + utils.escapeHtml(e.message) + '</div>';
      }
    });

    document.getElementById('form-seance').addEventListener('submit', async (ev) => {
      ev.preventDefault();
      try {
        await api.seances.creer(lirePayload());
        utils.closeModal();
        utils.showToast('Séance créée.', 'success');
        await chargerSeances();
        await rendreConflits();
      } catch (e) {
        utils.showToast('Échec : ' + e.message, 'error');
      }
    });
  }

  async function supprimerSeance(id) {
    if (!confirm('Supprimer cette séance ?')) return;
    try {
      await api.seances.supprimer(id);
      utils.showToast('Séance supprimée.', 'success');
      await chargerSeances();
      await rendreConflits();
    } catch (e) {
      utils.showToast('Échec : ' + e.message, 'error');
    }
  }

  async function init() {
    utils.bindTabs('#view-emplois');
    remplirFiltresGrille();

    document.getElementById('grid-filiere').addEventListener('change', rendreGrille);
    document.getElementById('grid-niveau').addEventListener('change', rendreGrille);
    document.getElementById('btn-new-seance').addEventListener('click', ouvrirFormulaire);

    document.getElementById('seances-tbody').addEventListener('click', (e) => {
      const btn = e.target.closest('[data-action="del-seance"]');
      if (btn) supprimerSeance(btn.dataset.id);
    });

    document.querySelectorAll('#view-emplois .tab').forEach(t => {
      t.addEventListener('click', () => {
        if (t.dataset.tab === 'tab-emp-grille') rendreGrille();
        if (t.dataset.tab === 'tab-emp-conflits') rendreConflits();
      });
    });

    await chargerSeances();
    await rendreConflits();
  }

  window.emploisModule = { init };
})();
