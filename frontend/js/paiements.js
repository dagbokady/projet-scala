// paiements.js — Module 8 : Gestion des paiements
(function () {
  let etat = [];

  function fmtMoney(n) {
    if (n === null || n === undefined || isNaN(n)) return '—';
    return Number(n).toLocaleString('fr-FR') + ' F';
  }

  async function chargerKpis() {
    try {
      const s = await api.paiements.synthese();
      document.getElementById('kpi-paie-attendu').textContent  = fmtMoney(s.totalAttendu);
      document.getElementById('kpi-paie-encaisse').textContent = fmtMoney(s.totalEncaisse);
      document.getElementById('kpi-paie-restant').textContent  = fmtMoney(s.totalRestant);
      document.getElementById('kpi-paie-taux').textContent     = (s.tauxRecouvrement * 100).toFixed(1) + ' %';
    } catch (e) { utils.showToast('Erreur synthèse : ' + e.message, 'error'); }
  }

  function rendreTable() {
    const tbody = document.getElementById('paie-tbody');
    if (etat.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8" class="empty">Aucun paiement.</td></tr>';
      return;
    }
    tbody.innerHTML = etat.map(p => {
      const reste = p.reste;
      const badge = reste <= 0 ? 'badge badge-ok' : reste < p.montantTotal * 0.5 ? 'badge badge-warn' : 'badge badge-bad';
      return '<tr>'
        + '<td><code>' + utils.escapeHtml(p.idPaiement) + '</code></td>'
        + '<td>' + utils.escapeHtml(p.matricule) + '<br><span class="muted">' + utils.escapeHtml(utils.nomEtudiant(p.matricule)) + '</span></td>'
        + '<td class="num">' + fmtMoney(p.montantTotal) + '</td>'
        + '<td class="num">' + fmtMoney(p.montantPaye) + '</td>'
        + '<td class="num"><span class="' + badge + '">' + fmtMoney(reste) + '</span></td>'
        + '<td>' + utils.escapeHtml(p.mode) + '</td>'
        + '<td>' + utils.formatDate(p.datePaiement) + '</td>'
        + '<td><button class="btn btn-sm btn-danger" data-action="del-paie" data-id="' + utils.escapeHtml(p.idPaiement) + '">Suppr.</button></td>'
        + '</tr>';
    }).join('');
  }

  async function chargerListe() {
    try {
      etat = await api.paiements.liste() || [];
      rendreTable();
      await chargerKpis();
    } catch (e) { utils.showToast('Erreur : ' + e.message, 'error'); }
  }

  async function rendreDette() {
    const tbody = document.getElementById('dette-tbody');
    tbody.innerHTML = '<tr><td colspan="3" class="empty">Calcul…</td></tr>';
    try {
      const data = await api.paiements.dette() || [];
      if (data.length === 0) {
        tbody.innerHTML = '<tr><td colspan="3" class="empty">Aucune dette en cours.</td></tr>';
        return;
      }
      tbody.innerHTML = data.map(d =>
        '<tr>'
        + '<td><code>' + utils.escapeHtml(d.matricule) + '</code></td>'
        + '<td>' + utils.escapeHtml(utils.nomEtudiant(d.matricule)) + '</td>'
        + '<td class="num"><span class="badge badge-bad">' + fmtMoney(d.dette) + '</span></td>'
        + '</tr>'
      ).join('');
    } catch (e) {
      tbody.innerHTML = '<tr><td colspan="3" class="empty">Erreur : ' + utils.escapeHtml(e.message) + '</td></tr>';
    }
  }

  async function rendreSyntheseFiliere() {
    const wrap = document.getElementById('paie-synth');
    wrap.innerHTML = '<div class="muted">Calcul…</div>';
    try {
      const s = await api.paiements.synthese();
      const ent = Object.entries(s.parFiliere || {});
      if (ent.length === 0) { wrap.innerHTML = '<div class="empty">—</div>'; return; }
      wrap.innerHTML = ent.map(([fil, f]) => {
        const pct = (f.taux * 100).toFixed(1);
        return '<div class="bar-row">'
          + '<div class="bar-label">' + utils.escapeHtml(fil) + '</div>'
          + '<div class="bar-track"><div class="bar-fill" style="width:' + pct + '%"></div></div>'
          + '<div class="bar-value">' + pct + ' % (' + fmtMoney(f.encaisse) + ')</div>'
          + '</div>';
      }).join('');
    } catch (e) {
      wrap.innerHTML = '<div class="empty">Erreur : ' + utils.escapeHtml(e.message) + '</div>';
    }
  }

  function ouvrirFormulaire() {
    const optEtu = (utils.refCache.etudiants || []).map(e =>
      '<option value="' + utils.escapeHtml(e.matricule) + '">' + utils.escapeHtml(e.matricule + ' — ' + e.prenom + ' ' + e.nom) + '</option>'
    ).join('');
    const aujourdhui = new Date().toISOString().substring(0, 10);
    const html =
      '<form id="form-paie">'
      + '<div class="form-group"><label class="form-label">Étudiant</label>'
      +   '<select class="form-select" name="matricule" required>' + optEtu + '</select></div>'
      + '<div class="form-row">'
      +   '<div class="form-group"><label class="form-label">Montant total (frais)</label>'
      +     '<input class="form-input" type="number" name="montantTotal" min="0" step="1000" value="1000000" required></div>'
      +   '<div class="form-group"><label class="form-label">Montant payé</label>'
      +     '<input class="form-input" type="number" name="montantPaye" min="0" step="1000" value="0" required></div>'
      + '</div>'
      + '<div class="form-row">'
      +   '<div class="form-group"><label class="form-label">Date</label>'
      +     '<input class="form-input" type="date" name="datePaiement" value="' + aujourdhui + '"></div>'
      +   '<div class="form-group"><label class="form-label">Mode</label>'
      +     '<select class="form-select" name="mode"><option>Mobile Money</option><option>Banque</option><option>Especes</option><option>Cheque</option></select></div>'
      + '</div>'
      + '<div class="form-actions">'
      +   '<button type="button" class="btn btn-ghost" data-close-modal>Annuler</button>'
      +   '<button type="submit" class="btn btn-primary">Enregistrer</button>'
      + '</div></form>';
    utils.openModal('Nouveau paiement', html);

    document.getElementById('form-paie').addEventListener('submit', async (ev) => {
      ev.preventDefault();
      const f = ev.target;
      const payload = {
        idPaiement: 'auto',
        matricule: f.matricule.value,
        montantTotal: Number(f.montantTotal.value),
        montantPaye:  Number(f.montantPaye.value),
        datePaiement: f.datePaiement.value || null,
        mode: f.mode.value
      };
      try {
        await api.paiements.creer(payload);
        utils.closeModal();
        utils.showToast('Paiement enregistré.', 'success');
        await chargerListe();
        await rendreDette();
        await rendreSyntheseFiliere();
      } catch (e) { utils.showToast('Échec : ' + e.message, 'error'); }
    });
  }

  async function supprimer(id) {
    if (!confirm('Supprimer le paiement ' + id + ' ?')) return;
    try {
      await api.paiements.supprimer(id);
      utils.showToast('Supprimé.', 'success');
      await chargerListe();
      await rendreDette();
    } catch (e) { utils.showToast('Échec : ' + e.message, 'error'); }
  }

  async function init() {
    utils.bindTabs('#view-paiements');
    document.getElementById('btn-new-paie').addEventListener('click', ouvrirFormulaire);
    document.getElementById('paie-tbody').addEventListener('click', (e) => {
      const b = e.target.closest('[data-action="del-paie"]');
      if (b) supprimer(b.dataset.id);
    });
    document.querySelectorAll('#view-paiements .tab').forEach(t => {
      t.addEventListener('click', () => {
        if (t.dataset.tab === 'tab-paie-dette') rendreDette();
        if (t.dataset.tab === 'tab-paie-synth') rendreSyntheseFiliere();
      });
    });
    await chargerListe();
    await rendreDette();
    await rendreSyntheseFiliere();
  }

  window.paiementsModule = { init };
})();
