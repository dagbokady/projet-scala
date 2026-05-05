// notes.js — Module 5 : Gestion des notes
//
// Charge KPIs (total, moyenne globale, échecs, incomplètes), table,
// classement, étudiants en échec, statistiques par matière.

(function () {
  let etatNotes = [];

  async function chargerKpis() {
    try {
      const [echec, incompletes, classement] = await Promise.all([
        api.notes.echec(),
        api.notes.incompletes(),
        api.notes.classement()
      ]);
      const total = etatNotes.length;
      const moysValides = (classement || [])
        .map(c => c.moyenneGenerale)
        .filter(m => typeof m === 'number');
      const moyG = moysValides.length
        ? moysValides.reduce((a, b) => a + b, 0) / moysValides.length
        : null;
      document.getElementById('kpi-notes-total').textContent  = total;
      document.getElementById('kpi-moy-globale').textContent  = utils.formatNum(moyG);
      document.getElementById('kpi-echecs').textContent       = (echec || []).length;
      document.getElementById('kpi-incompletes').textContent  = (incompletes || []).length;
    } catch (e) {
      utils.showToast('Erreur KPIs notes : ' + e.message, 'error');
    }
  }

  function rendreTable() {
    const tbody = document.getElementById('notes-tbody');
    const recherche = (document.getElementById('note-search').value || '').trim().toLowerCase();
    const filtreMat = document.getElementById('note-filter-matiere').value || '';

    const lignes = etatNotes.filter(n => {
      const okMat = !filtreMat || n.idMatiere === filtreMat;
      const okSearch = !recherche || (n.matricule || '').toLowerCase().includes(recherche);
      return okMat && okSearch;
    });

    if (lignes.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" class="empty">Aucune note ne correspond.</td></tr>';
      return;
    }

    tbody.innerHTML = lignes.map(n => {
      const moy = (typeof n.controleContinu === 'number' && typeof n.examen === 'number')
        ? (n.controleContinu * 0.4 + n.examen * 0.6) : null;
      return '<tr>'
        + '<td><code>' + utils.escapeHtml(n.idNote) + '</code></td>'
        + '<td>' + utils.escapeHtml(n.matricule) + '<br><span class="muted">'
            + utils.escapeHtml(utils.nomEtudiant(n.matricule)) + '</span></td>'
        + '<td>' + utils.escapeHtml(utils.nomMatiere(n.idMatiere)) + '</td>'
        + '<td class="num">' + (n.controleContinu ?? '—') + '</td>'
        + '<td class="num">' + (n.examen ?? '—') + '</td>'
        + '<td class="num">' + utils.badgeMoyenne(moy) + '</td>'
        + '<td><button class="btn btn-sm btn-danger" data-action="del-note" data-id="'
            + utils.escapeHtml(n.idNote) + '">Suppr.</button></td>'
        + '</tr>';
    }).join('');
  }

  async function chargerNotes() {
    try {
      etatNotes = await api.notes.liste() || [];
      rendreTable();
      await chargerKpis();
    } catch (e) {
      utils.showToast('Erreur chargement notes : ' + e.message, 'error');
    }
  }

  async function rendreClassement() {
    const wrap = document.getElementById('ranking');
    wrap.innerHTML = '<div class="muted">Calcul…</div>';
    try {
      const data = await api.notes.classement() || [];
      if (data.length === 0) {
        wrap.innerHTML = '<div class="empty">Aucun étudiant classé.</div>';
        return;
      }
      wrap.innerHTML = data.map((c, idx) =>
        '<div class="rank-row">'
        + '<div class="rank-pos">#' + (idx + 1) + '</div>'
        + '<div class="rank-mat"><strong>' + utils.escapeHtml(c.matricule) + '</strong>'
        +   '<br><span class="muted">' + utils.escapeHtml(utils.nomEtudiant(c.matricule)) + '</span></div>'
        + '<div class="rank-moy">' + utils.badgeMoyenne(c.moyenneGenerale) + '</div>'
        + '</div>'
      ).join('');
    } catch (e) {
      wrap.innerHTML = '<div class="empty">Erreur : ' + utils.escapeHtml(e.message) + '</div>';
    }
  }

  async function rendreEchec() {
    const tbody = document.getElementById('echec-tbody');
    tbody.innerHTML = '<tr><td colspan="3" class="empty">Calcul…</td></tr>';
    try {
      const data = await api.notes.echec() || [];
      if (data.length === 0) {
        tbody.innerHTML = '<tr><td colspan="3" class="empty">Aucun étudiant en échec — bravo.</td></tr>';
        return;
      }
      tbody.innerHTML = data.map(c =>
        '<tr>'
        + '<td><code>' + utils.escapeHtml(c.matricule) + '</code></td>'
        + '<td>' + utils.escapeHtml(utils.nomEtudiant(c.matricule)) + '</td>'
        + '<td class="num">' + utils.badgeMoyenne(c.moyenneGenerale) + '</td>'
        + '</tr>'
      ).join('');
    } catch (e) {
      tbody.innerHTML = '<tr><td colspan="3" class="empty">Erreur : '
        + utils.escapeHtml(e.message) + '</td></tr>';
    }
  }

  async function rendreStats() {
    const wrap = document.getElementById('stats-matiere');
    wrap.innerHTML = '<div class="muted">Calcul…</div>';
    try {
      const data = await api.notes.statsMatiere() || [];
      if (data.length === 0) {
        wrap.innerHTML = '<div class="empty">Pas encore de statistiques.</div>';
        return;
      }
      const max = 20; // sur 20
      wrap.innerHTML = data.map(s => {
        const pct = Math.max(0, Math.min(100, (s.moyenne / max) * 100));
        return '<div class="bar-row">'
          + '<div class="bar-label">' + utils.escapeHtml(utils.nomMatiere(s.idMatiere)) + '</div>'
          + '<div class="bar-track"><div class="bar-fill" style="width:' + pct.toFixed(1) + '%"></div></div>'
          + '<div class="bar-value">' + utils.formatNum(s.moyenne) + '</div>'
          + '</div>';
      }).join('');
    } catch (e) {
      wrap.innerHTML = '<div class="empty">Erreur : ' + utils.escapeHtml(e.message) + '</div>';
    }
  }

  function remplirFiltreMatiere() {
    const sel = document.getElementById('note-filter-matiere');
    const cur = sel.value;
    sel.innerHTML = '<option value="">Toutes les matières</option>'
      + (utils.refCache.matieres || []).map(m =>
          '<option value="' + utils.escapeHtml(m.idMatiere) + '">'
          + utils.escapeHtml(m.nomMatiere) + '</option>'
        ).join('');
    if (cur) sel.value = cur;
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

    const html =
      '<form id="form-note">'
      + '<div class="form-row">'
      +   '<div class="form-group"><label class="form-label">Étudiant</label>'
      +     '<select class="form-select" name="matricule" required>' + optEtu + '</select></div>'
      +   '<div class="form-group"><label class="form-label">Matière</label>'
      +     '<select class="form-select" name="idMatiere" required>' + optMat + '</select></div>'
      + '</div>'
      + '<div class="form-row">'
      +   '<div class="form-group"><label class="form-label">Contrôle continu (/20)</label>'
      +     '<input class="form-input" type="number" name="cc" min="0" max="20" step="0.25" required></div>'
      +   '<div class="form-group"><label class="form-label">Examen (/20)</label>'
      +     '<input class="form-input" type="number" name="exam" min="0" max="20" step="0.25" required></div>'
      + '</div>'
      + '<div class="form-actions">'
      +   '<button type="button" class="btn btn-ghost" data-close-modal>Annuler</button>'
      +   '<button type="submit" class="btn btn-primary">Enregistrer</button>'
      + '</div></form>';

    utils.openModal('Nouvelle note', html);

    document.getElementById('form-note').addEventListener('submit', async (ev) => {
      ev.preventDefault();
      const f = ev.target;
      const payload = {
        matricule: f.matricule.value,
        idMatiere: f.idMatiere.value,
        controleContinu: Number(f.cc.value),
        examen: Number(f.exam.value)
      };
      try {
        await api.notes.creer(payload);
        utils.closeModal();
        utils.showToast('Note enregistrée.', 'success');
        await chargerNotes();
        await rendreClassement();
        await rendreEchec();
        await rendreStats();
      } catch (e) {
        utils.showToast('Échec : ' + e.message, 'error');
      }
    });
  }

  async function supprimerNote(id) {
    if (!confirm('Supprimer cette note ? L\'opération est irréversible.')) return;
    try {
      await api.notes.supprimer(id);
      utils.showToast('Note supprimée.', 'success');
      await chargerNotes();
    } catch (e) {
      utils.showToast('Échec : ' + e.message, 'error');
    }
  }

  async function init() {
    utils.bindTabs('#view-notes');
    remplirFiltreMatiere();

    document.getElementById('note-search').addEventListener('input', utils.debounce(rendreTable, 150));
    document.getElementById('note-filter-matiere').addEventListener('change', rendreTable);
    document.getElementById('btn-new-note').addEventListener('click', ouvrirFormulaire);

    document.getElementById('notes-tbody').addEventListener('click', (e) => {
      const btn = e.target.closest('[data-action="del-note"]');
      if (btn) supprimerNote(btn.dataset.id);
    });

    // Re-rendre l'onglet quand on clique dessus (pour rafraîchir)
    document.querySelectorAll('#view-notes .tab').forEach(t => {
      t.addEventListener('click', () => {
        const target = t.dataset.tab;
        if (target === 'tab-notes-classement') rendreClassement();
        if (target === 'tab-notes-echec') rendreEchec();
        if (target === 'tab-notes-stats') rendreStats();
      });
    });

    await chargerNotes();
    await rendreClassement();
    await rendreEchec();
    await rendreStats();
  }

  window.notesModule = { init };
})();
