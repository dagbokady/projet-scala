// inscriptions.js — Module 4 : Gestion des inscriptions
(function () {
  let etat = [];

  async function chargerKpis() {
    try {
      const s = await api.inscriptions.stats();
      const ps = s.parStatut || {};
      document.getElementById('kpi-insc-total').textContent = s.total || 0;
      document.getElementById('kpi-insc-val').textContent   = ps['Validee'] || 0;
      document.getElementById('kpi-insc-att').textContent   = ps['En attente'] || 0;
      document.getElementById('kpi-insc-ann').textContent   = ps['Annulee'] || 0;
    } catch (e) { utils.showToast('Erreur stats : ' + e.message, 'error'); }
  }

  function rendreTable() {
    const tbody = document.getElementById('insc-tbody');
    if (etat.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" class="empty">Aucune inscription.</td></tr>';
      return;
    }
    tbody.innerHTML = etat.map(i => {
      const badge = i.statut === 'Validee'   ? 'badge badge-ok'
                  : i.statut === 'Annulee'   ? 'badge badge-bad'
                  : 'badge badge-warn';
      return '<tr>'
        + '<td><code>' + utils.escapeHtml(i.idInscription) + '</code></td>'
        + '<td>' + utils.escapeHtml(i.matricule) + '<br><span class="muted">' + utils.escapeHtml(utils.nomEtudiant(i.matricule)) + '</span></td>'
        + '<td>' + utils.escapeHtml(i.filiere) + '</td>'
        + '<td>' + utils.escapeHtml(i.niveau) + '</td>'
        + '<td>' + utils.escapeHtml(i.annee) + '</td>'
        + '<td><span class="' + badge + '">' + utils.escapeHtml(i.statut) + '</span></td>'
        + '<td>'
        +   (i.statut !== 'Validee' ? '<button class="btn btn-sm" data-action="valider" data-id="' + utils.escapeHtml(i.idInscription) + '">Valider</button> ' : '')
        +   (i.statut !== 'Annulee' ? '<button class="btn btn-sm btn-ghost" data-action="annuler" data-id="' + utils.escapeHtml(i.idInscription) + '">Annuler</button> ' : '')
        +   '<button class="btn btn-sm btn-danger" data-action="del-insc" data-id="' + utils.escapeHtml(i.idInscription) + '">Suppr.</button>'
        + '</td></tr>';
    }).join('');
  }

  async function chargerListe() {
    try {
      const statut = document.getElementById('insc-filter-statut').value;
      etat = await api.inscriptions.liste(statut || null) || [];
      rendreTable();
      await chargerKpis();
    } catch (e) { utils.showToast('Erreur : ' + e.message, 'error'); }
  }

  function ouvrirFormulaire() {
    const optEtu = (utils.refCache.etudiants || []).map(e =>
      '<option value="' + utils.escapeHtml(e.matricule) + '">' + utils.escapeHtml(e.matricule + ' — ' + e.prenom + ' ' + e.nom) + '</option>'
    ).join('');
    const filieres = Array.from(new Set((utils.refCache.etudiants || []).map(e => e.filiere))).sort();
    const optFil = filieres.map(f => '<option>' + utils.escapeHtml(f) + '</option>').join('');
    const html =
      '<form id="form-insc">'
      + '<div class="form-group"><label class="form-label">Étudiant</label>'
      +   '<select class="form-select" name="matricule" required>' + optEtu + '</select></div>'
      + '<div class="form-row">'
      +   '<div class="form-group"><label class="form-label">Filière</label>'
      +     '<select class="form-select" name="filiere">' + optFil + '</select></div>'
      +   '<div class="form-group"><label class="form-label">Niveau</label>'
      +     '<select class="form-select" name="niveau">'
      +     '<option>L1</option><option>L2</option><option>L3</option><option selected>M1</option><option>M2</option></select></div>'
      + '</div>'
      + '<div class="form-row">'
      +   '<div class="form-group"><label class="form-label">Année (YYYY-YYYY)</label><input class="form-input" name="annee" value="2025-2026" required></div>'
      +   '<div class="form-group"><label class="form-label">Statut</label>'
      +     '<select class="form-select" name="statut"><option>En attente</option><option>Validee</option><option>Annulee</option></select></div>'
      + '</div>'
      + '<div class="form-actions">'
      +   '<button type="button" class="btn btn-ghost" data-close-modal>Annuler</button>'
      +   '<button type="submit" class="btn btn-primary">Enregistrer</button>'
      + '</div></form>';
    utils.openModal('Nouvelle inscription', html);

    document.getElementById('form-insc').addEventListener('submit', async (ev) => {
      ev.preventDefault();
      const f = ev.target;
      const payload = {
        idInscription: 'auto',
        matricule: f.matricule.value, filiere: f.filiere.value,
        niveau: f.niveau.value, annee: f.annee.value, statut: f.statut.value
      };
      try {
        await api.inscriptions.creer(payload);
        utils.closeModal();
        utils.showToast('Inscription créée.', 'success');
        await chargerListe();
      } catch (e) { utils.showToast('Échec : ' + e.message, 'error'); }
    });
  }

  async function changerStatut(id, valeur) {
    try {
      await api.inscriptions.changerStatut(id, valeur);
      utils.showToast('Statut mis à jour.', 'success');
      await chargerListe();
    } catch (e) { utils.showToast('Échec : ' + e.message, 'error'); }
  }
  async function supprimer(id) {
    if (!confirm('Supprimer ' + id + ' ?')) return;
    try {
      await api.inscriptions.supprimer(id);
      utils.showToast('Supprimée.', 'success');
      await chargerListe();
    } catch (e) { utils.showToast('Échec : ' + e.message, 'error'); }
  }

  async function init() {
    document.getElementById('insc-filter-statut').addEventListener('change', chargerListe);
    document.getElementById('btn-new-insc').addEventListener('click', ouvrirFormulaire);
    document.getElementById('insc-tbody').addEventListener('click', (e) => {
      const v = e.target.closest('[data-action="valider"]');
      if (v) return changerStatut(v.dataset.id, 'Validee');
      const a = e.target.closest('[data-action="annuler"]');
      if (a) return changerStatut(a.dataset.id, 'Annulee');
      const d = e.target.closest('[data-action="del-insc"]');
      if (d) return supprimer(d.dataset.id);
    });
    await chargerListe();
  }

  window.inscriptionsModule = { init };
})();
