// formations.js — Module 3 : Gestion des formations (filières + matières + UE)
(function () {
  let etatMat = [];
  let etatFil = [];

  async function chargerKpis() {
    try {
      const s = await api.matieres.stats();
      const volTotal = Object.values(s.volumeHoraireParUe || {}).reduce((a, b) => a + b, 0);
      document.getElementById('kpi-mat-total').textContent  = s.total;
      document.getElementById('kpi-ue-total').textContent   = (s.ues || []).length;
      document.getElementById('kpi-form-volume').textContent = volTotal + 'h';
      document.getElementById('kpi-fil-total').textContent  = etatFil.length;
    } catch (e) {
      utils.showToast('Erreur stats : ' + e.message, 'error');
    }
  }

  function rendreMatieres() {
    const tbody = document.getElementById('mat-tbody');
    if (etatMat.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" class="empty">Aucune matière.</td></tr>';
      return;
    }
    tbody.innerHTML = etatMat.map(m =>
      '<tr>'
      + '<td><code>' + utils.escapeHtml(m.idMatiere) + '</code></td>'
      + '<td><strong>' + utils.escapeHtml(m.nomMatiere) + '</strong></td>'
      + '<td><span class="muted">' + utils.escapeHtml(m.ue) + '</span></td>'
      + '<td class="num">' + m.coefficient + '</td>'
      + '<td class="num">' + m.volumeHoraire + ' h</td>'
      + '<td>' + utils.escapeHtml(utils.nomEnseignant(m.enseignant)) + '</td>'
      + '<td><button class="btn btn-sm btn-danger" data-action="del-mat" data-id="' + utils.escapeHtml(m.idMatiere) + '">Suppr.</button></td>'
      + '</tr>'
    ).join('');
  }

  function rendreFilieres() {
    const tbody = document.getElementById('fil-tbody');
    if (etatFil.length === 0) {
      tbody.innerHTML = '<tr><td colspan="4" class="empty">Aucune filière.</td></tr>';
      return;
    }
    tbody.innerHTML = etatFil.map(f =>
      '<tr>'
      + '<td><code>' + utils.escapeHtml(f.idFiliere) + '</code></td>'
      + '<td><strong>' + utils.escapeHtml(f.nomFiliere) + '</strong></td>'
      + '<td>' + utils.escapeHtml(utils.nomEnseignant(f.responsable)) + '</td>'
      + '<td><button class="btn btn-sm btn-danger" data-action="del-fil" data-id="' + utils.escapeHtml(f.idFiliere) + '">Suppr.</button></td>'
      + '</tr>'
    ).join('');
  }

  async function rendreParUe() {
    const wrap = document.getElementById('form-ue-volume');
    wrap.innerHTML = '<div class="muted">Calcul…</div>';
    try {
      const s = await api.matieres.stats();
      const ent = Object.entries(s.volumeHoraireParUe || {});
      if (ent.length === 0) { wrap.innerHTML = '<div class="empty">—</div>'; return; }
      const max = Math.max(1, ...ent.map(([_, v]) => v));
      wrap.innerHTML = ent.map(([ue, vol]) => {
        const pct = (vol / max) * 100;
        return '<div class="bar-row">'
          + '<div class="bar-label">' + utils.escapeHtml(ue) + '</div>'
          + '<div class="bar-track"><div class="bar-fill" style="width:' + pct.toFixed(1) + '%"></div></div>'
          + '<div class="bar-value">' + vol + ' h</div>'
          + '</div>';
      }).join('');
    } catch (e) {
      wrap.innerHTML = '<div class="empty">Erreur : ' + utils.escapeHtml(e.message) + '</div>';
    }
  }

  async function chargerTout() {
    try {
      const [mat, fil] = await Promise.all([
        api.matieres.liste(),
        api.filieres.liste()
      ]);
      etatMat = mat || [];
      etatFil = fil || [];
      rendreMatieres();
      rendreFilieres();
      await chargerKpis();
    } catch (e) {
      utils.showToast('Erreur : ' + e.message, 'error');
    }
  }

  function ouvrirFormulaireMatiere() {
    const optEns = (utils.refCache.enseignants || []).map(e =>
      '<option value="' + utils.escapeHtml(e.idEnseignant) + '">' + utils.escapeHtml(e.prenom + ' ' + e.nom) + '</option>'
    ).join('');
    const html =
      '<form id="form-mat">'
      + '<div class="form-row">'
      +   '<div class="form-group"><label class="form-label">ID Matière (auto si vide)</label><input class="form-input" name="idMatiere"></div>'
      +   '<div class="form-group"><label class="form-label">Nom</label><input class="form-input" name="nomMatiere" required></div>'
      + '</div>'
      + '<div class="form-row">'
      +   '<div class="form-group"><label class="form-label">UE</label><input class="form-input" name="ue" placeholder="UE Programmation"></div>'
      +   '<div class="form-group"><label class="form-label">Enseignant</label>'
      +     '<select class="form-select" name="enseignant">' + optEns + '</select></div>'
      + '</div>'
      + '<div class="form-row">'
      +   '<div class="form-group"><label class="form-label">Coefficient</label><input class="form-input" type="number" name="coefficient" min="1" max="10" value="3" required></div>'
      +   '<div class="form-group"><label class="form-label">Volume horaire</label><input class="form-input" type="number" name="volumeHoraire" min="0" value="30" required></div>'
      + '</div>'
      + '<div class="form-actions">'
      +   '<button type="button" class="btn btn-ghost" data-close-modal>Annuler</button>'
      +   '<button type="submit" class="btn btn-primary">Enregistrer</button>'
      + '</div></form>';
    utils.openModal('Nouvelle matière', html);

    document.getElementById('form-mat').addEventListener('submit', async (ev) => {
      ev.preventDefault();
      const f = ev.target;
      const idAuto = 'MAT' + String(Date.now()).slice(-3);
      const payload = {
        idMatiere: f.idMatiere.value || idAuto,
        nomMatiere: f.nomMatiere.value, ue: f.ue.value,
        coefficient: Number(f.coefficient.value),
        volumeHoraire: Number(f.volumeHoraire.value),
        enseignant: f.enseignant.value
      };
      try {
        await api.matieres.creer(payload);
        utils.closeModal();
        utils.showToast('Matière créée.', 'success');
        await utils.loadRefs();
        await chargerTout();
      } catch (e) {
        utils.showToast('Échec : ' + e.message, 'error');
      }
    });
  }

  function ouvrirFormulaireFiliere() {
    const optEns = (utils.refCache.enseignants || []).map(e =>
      '<option value="' + utils.escapeHtml(e.idEnseignant) + '">' + utils.escapeHtml(e.prenom + ' ' + e.nom) + '</option>'
    ).join('');
    const html =
      '<form id="form-fil">'
      + '<div class="form-row">'
      +   '<div class="form-group"><label class="form-label">ID Filière</label><input class="form-input" name="idFiliere" placeholder="FIL004…" required></div>'
      +   '<div class="form-group"><label class="form-label">Nom</label><input class="form-input" name="nomFiliere" required></div>'
      + '</div>'
      + '<div class="form-group"><label class="form-label">Responsable</label>'
      +   '<select class="form-select" name="responsable">' + optEns + '</select></div>'
      + '<div class="form-actions">'
      +   '<button type="button" class="btn btn-ghost" data-close-modal>Annuler</button>'
      +   '<button type="submit" class="btn btn-primary">Enregistrer</button>'
      + '</div></form>';
    utils.openModal('Nouvelle filière', html);

    document.getElementById('form-fil').addEventListener('submit', async (ev) => {
      ev.preventDefault();
      const f = ev.target;
      const payload = {
        idFiliere: f.idFiliere.value,
        nomFiliere: f.nomFiliere.value,
        responsable: f.responsable.value
      };
      try {
        await api.filieres.creer(payload);
        utils.closeModal();
        utils.showToast('Filière créée.', 'success');
        await chargerTout();
      } catch (e) {
        utils.showToast('Échec : ' + e.message, 'error');
      }
    });
  }

  async function supprimerMat(id) {
    if (!confirm('Supprimer la matière ' + id + ' ?')) return;
    try {
      await api.matieres.supprimer(id);
      utils.showToast('Supprimée.', 'success');
      await utils.loadRefs();
      await chargerTout();
    } catch (e) { utils.showToast('Échec : ' + e.message, 'error'); }
  }
  async function supprimerFil(id) {
    if (!confirm('Supprimer la filière ' + id + ' ?')) return;
    try {
      await api.filieres.supprimer(id);
      utils.showToast('Supprimée.', 'success');
      await chargerTout();
    } catch (e) { utils.showToast('Échec : ' + e.message, 'error'); }
  }

  async function init() {
    utils.bindTabs('#view-formations');
    document.getElementById('btn-new-mat').addEventListener('click', ouvrirFormulaireMatiere);
    document.getElementById('btn-new-fil').addEventListener('click', ouvrirFormulaireFiliere);
    document.getElementById('mat-tbody').addEventListener('click', (e) => {
      const b = e.target.closest('[data-action="del-mat"]');
      if (b) supprimerMat(b.dataset.id);
    });
    document.getElementById('fil-tbody').addEventListener('click', (e) => {
      const b = e.target.closest('[data-action="del-fil"]');
      if (b) supprimerFil(b.dataset.id);
    });
    document.querySelectorAll('#view-formations .tab').forEach(t => {
      t.addEventListener('click', () => {
        if (t.dataset.tab === 'tab-form-ue') rendreParUe();
      });
    });
    await chargerTout();
    await rendreParUe();
  }

  window.formationsModule = { init };
})();
