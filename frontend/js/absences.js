// absences.js — Module 6 : Gestion des absences
//
// KPIs (total, heures cumulées, alertes >=10h, non justifiées),
// table avec filtre justifiée/non, alerte, stats par filière, modal,
// bouton "justifier" inline (PATCH).

(function () {
  let etatAbs = [];

  async function chargerKpis() {
    const totalAbs = etatAbs.length;
    // total des heures non justifiees uniquement (cohérent avec service backend)
    const heuresNJ = etatAbs.filter(a => !a.justifiee).reduce((s, a) => s + (a.heures || 0), 0);
    const nonJust = etatAbs.filter(a => !a.justifiee).length;
    document.getElementById('kpi-abs-total').textContent = totalAbs;
    document.getElementById('kpi-heures').textContent    = heuresNJ + ' h';
    document.getElementById('kpi-nonjust').textContent   = nonJust;
    try {
      const alertes = await api.absences.alerte(10);
      document.getElementById('kpi-alerte').textContent = (alertes || []).length;
    } catch (e) {
      document.getElementById('kpi-alerte').textContent = '—';
    }
  }

  function rendreTable() {
    const tbody = document.getElementById('abs-tbody');
    const recherche = (document.getElementById('abs-search').value || '').trim().toLowerCase();
    const filtre = document.getElementById('abs-filter-just').value || '';

    const lignes = etatAbs.filter(a => {
      const okSearch = !recherche || (a.matricule || '').toLowerCase().includes(recherche);
      const okJust = !filtre
          || (filtre === 'oui' && a.justifiee)
          || (filtre === 'non' && !a.justifiee);
      return okSearch && okJust;
    });

    if (lignes.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" class="empty">Aucune absence ne correspond.</td></tr>';
      return;
    }

    tbody.innerHTML = lignes.map(a => {
      const justifBadge = a.justifiee
          ? '<span class="badge badge-ok">Justifiée</span>'
          : '<span class="badge badge-bad">Non justifiée</span>';
      const btnJustifier = a.justifiee ? ''
          : '<button class="btn btn-sm" data-action="just-abs" data-id="'
          + utils.escapeHtml(a.idAbsence) + '">Justifier</button> ';
      return '<tr>'
          + '<td><code>' + utils.escapeHtml(a.idAbsence) + '</code></td>'
          + '<td>' + utils.escapeHtml(a.matricule) + '<br><span class="muted">'
          + utils.escapeHtml(utils.nomEtudiant(a.matricule)) + '</span></td>'
          + '<td>' + utils.escapeHtml(utils.nomMatiere(a.matiere)) + '</td>'
          + '<td>' + utils.formatDate(a.dateAbsence) + '</td>'
          + '<td class="num">' + (a.heures ?? '—') + ' h</td>'
          + '<td>' + justifBadge + '</td>'
          + '<td>' + btnJustifier
          + '<button class="btn btn-sm btn-danger" data-action="del-abs" data-id="'
          + utils.escapeHtml(a.idAbsence) + '">Suppr.</button></td>'
          + '</tr>';
    }).join('');
  }

  async function chargerAbsences() {
    try {
      etatAbs = await api.absences.liste() || [];
      rendreTable();
      await chargerKpis();
    } catch (e) {
      utils.showToast('Erreur chargement absences : ' + e.message, 'error');
    }
  }

  async function rendreAlerte() {
    const tbody = document.getElementById('alerte-tbody');
    tbody.innerHTML = '<tr><td colspan="3" class="empty">Calcul…</td></tr>';
    try {
      const data = await api.absences.alerte(10) || [];
      if (data.length === 0) {
        tbody.innerHTML = '<tr><td colspan="3" class="empty">Aucun étudiant au-dessus du seuil de 10 h.</td></tr>';
        return;
      }
      tbody.innerHTML = data.map(d =>
          '<tr>'
          + '<td><code>' + utils.escapeHtml(d.matricule) + '</code></td>'
          + '<td>' + utils.escapeHtml(utils.nomEtudiant(d.matricule)) + '</td>'
          + '<td class="num"><span class="badge badge-bad">' + d.heures + ' h</span></td>'
          + '</tr>'
      ).join('');
    } catch (e) {
      tbody.innerHTML = '<tr><td colspan="3" class="empty">Erreur : '
          + utils.escapeHtml(e.message) + '</td></tr>';
    }
  }

  async function rendreStatsFiliere() {
    const wrap = document.getElementById('stats-filiere');
    wrap.innerHTML = '<div class="muted">Calcul…</div>';
    try {
      const data = await api.absences.statsFiliere() || [];
      if (data.length === 0) {
        wrap.innerHTML = '<div class="empty">Aucune statistique disponible.</div>';
        return;
      }
      const max = Math.max(1, ...data.map(d => d.taux || 0));
      wrap.innerHTML = data.map(d => {
        const pct = Math.max(0, Math.min(100, (d.taux / max) * 100));
        return '<div class="bar-row">'
            + '<div class="bar-label">' + utils.escapeHtml(d.filiere) + '</div>'
            + '<div class="bar-track"><div class="bar-fill" style="width:' + pct.toFixed(1) + '%"></div></div>'
            + '<div class="bar-value">' + utils.formatNum(d.taux) + ' h/étud.</div>'
            + '</div>';
      }).join('');
    } catch (e) {
      wrap.innerHTML = '<div class="empty">Erreur : ' + utils.escapeHtml(e.message) + '</div>';
    }
  }

  function ouvrirFormulaire() {
    const optEtu = (utils.refCache.etudiants || []).map(e =>
        '<option value="' + utils.escapeHtml(e.matricule) + '">'
        + utils.escapeHtml(e.matricule + ' — ' + e.prenom + ' ' + e.nom) + '</option>'
    ).join('');
    const optMat = (utils.refCache.matieres || []).map(m =>
        '<option value="' + utils.escapeHtml(m.idMatiere) + '">'
        + utils.escapeHtml(m.nomMatiere) + '</option>'
    ).join('');
    const aujourdhui = new Date().toISOString().substring(0, 10);

    const html =
        '<form id="form-abs">'
        + '<div class="form-row">'
        +   '<div class="form-group"><label class="form-label">Étudiant</label>'
        +     '<select class="form-select" name="matricule" required>' + optEtu + '</select></div>'
        +   '<div class="form-group"><label class="form-label">Matière</label>'
        +     '<select class="form-select" name="idMatiere" required>' + optMat + '</select></div>'
        + '</div>'
        + '<div class="form-row">'
        +   '<div class="form-group"><label class="form-label">Date</label>'
        +     '<input class="form-input" type="date" name="dateAbsence" value="' + aujourdhui + '" required></div>'
        +   '<div class="form-group"><label class="form-label">Heures</label>'
        +     '<input class="form-input" type="number" name="heures" min="1" max="12" step="1" value="2" required></div>'
        + '</div>'
        + '<div class="form-group checkbox-row">'
        +   '<label><input type="checkbox" name="justifiee"> Absence justifiée</label>'
        + '</div>'
        + '<div class="form-actions">'
        +   '<button type="button" class="btn btn-ghost" data-close-modal>Annuler</button>'
        +   '<button type="submit" class="btn btn-primary">Enregistrer</button>'
        + '</div></form>';

    utils.openModal('Nouvelle absence', html);

    document.getElementById('form-abs').addEventListener('submit', async (ev) => {
      ev.preventDefault();
      const f = ev.target;
      const payload = {
        matricule: f.matricule.value,
        matiere: f.idMatiere.value,
        dateAbsence: f.dateAbsence.value,
        heures: Number(f.heures.value),
        justifiee: f.justifiee.checked
      };
      try {
        await api.absences.creer(payload);
        utils.closeModal();
        utils.showToast('Absence enregistrée.', 'success');
        await chargerAbsences();
        await rendreAlerte();
        await rendreStatsFiliere();
      } catch (e) {
        utils.showToast('Échec : ' + e.message, 'error');
      }
    });
  }

  async function justifierAbsence(id) {
    try {
      await api.absences.justifier(id);
      utils.showToast('Absence justifiée.', 'success');
      await chargerAbsences();
      await rendreAlerte();
    } catch (e) {
      utils.showToast('Échec : ' + e.message, 'error');
    }
  }

  async function supprimerAbsence(id) {
    if (!confirm('Supprimer cette absence ?')) return;
    try {
      await api.absences.supprimer(id);
      utils.showToast('Absence supprimée.', 'success');
      await chargerAbsences();
      await rendreAlerte();
    } catch (e) {
      utils.showToast('Échec : ' + e.message, 'error');
    }
  }

  async function init() {
    utils.bindTabs('#view-absences');

    document.getElementById('abs-search').addEventListener('input', utils.debounce(rendreTable, 150));
    document.getElementById('abs-filter-just').addEventListener('change', rendreTable);
    document.getElementById('btn-new-absence').addEventListener('click', ouvrirFormulaire);

    document.getElementById('abs-tbody').addEventListener('click', (e) => {
      const btnJ = e.target.closest('[data-action="just-abs"]');
      if (btnJ) return justifierAbsence(btnJ.dataset.id);
      const btnD = e.target.closest('[data-action="del-abs"]');
      if (btnD) return supprimerAbsence(btnD.dataset.id);
    });

    document.querySelectorAll('#view-absences .tab').forEach(t => {
      t.addEventListener('click', () => {
        if (t.dataset.tab === 'tab-abs-alerte') rendreAlerte();
        if (t.dataset.tab === 'tab-abs-stats') rendreStatsFiliere();
      });
    });

    await chargerAbsences();
    await rendreAlerte();
    await rendreStatsFiliere();
  }

  window.absencesModule = { init };
})();