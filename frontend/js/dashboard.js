// dashboard.js — Module 9 : Tableau de bord académique
(function () {

  function fmtMoney(n) {
    if (n === null || n === undefined || isNaN(n)) return '—';
    return Number(n).toLocaleString('fr-FR') + ' F';
  }

  async function chargerSynthese() {
    try {
      const s = await api.dashboard.synthese();
      document.getElementById('dash-etu').textContent = s.nbEtudiants;
      document.getElementById('dash-ens').textContent = s.nbEnseignants;
      document.getElementById('dash-fil').textContent = s.nbFilieres;
      document.getElementById('dash-mat').textContent = s.nbMatieres;
      document.getElementById('dash-moy').textContent = (s.moyenneGlobale !== null && s.moyenneGlobale !== undefined)
        ? Number(s.moyenneGlobale).toFixed(2) + ' /20' : '—';
      document.getElementById('dash-reussite').textContent = (s.tauxReussiteGlobal * 100).toFixed(1) + ' %';
      document.getElementById('dash-recouv').textContent   = (s.tauxRecouvrement   * 100).toFixed(1) + ' %';
    } catch (e) { utils.showToast('Erreur synthèse : ' + e.message, 'error'); }
  }

  async function chargerRisque() {
    try {
      const data = await api.dashboard.risque(10) || [];
      document.getElementById('dash-risque').textContent = data.length;
      const list = document.getElementById('dash-risk-list');
      if (data.length === 0) {
        list.innerHTML = '<div class="empty">Aucun étudiant à risque — bravo.</div>';
        return;
      }
      list.innerHTML = data.slice(0, 8).map(r =>
        '<div class="risk-row">'
        + '<div class="risk-mat">' + utils.escapeHtml(r.matricule) + '</div>'
        + '<div class="risk-name">' + utils.escapeHtml(r.prenom + ' ' + r.nom)
            + '<br><span class="muted">' + utils.escapeHtml(r.filiere) + '</span></div>'
        + '<div class="risk-motifs">'
        +   r.motifs.map(m => '<span class="risk-motif">' + utils.escapeHtml(m) + '</span>').join('')
        + '</div></div>'
      ).join('');
    } catch (e) {
      document.getElementById('dash-risk-list').innerHTML = '<div class="empty">Erreur : ' + utils.escapeHtml(e.message) + '</div>';
    }
  }

  async function chargerTop5() {
    try {
      const r = await api.dashboard.reussite();
      const top = r.top5 || [];
      const list = document.getElementById('dash-top5');
      if (top.length === 0) { list.innerHTML = '<div class="empty">Aucun classement disponible.</div>'; return; }
      list.innerHTML = top.map((t, i) =>
        '<div class="rank-row">'
        + '<div class="rank-pos">#' + (i + 1) + '</div>'
        + '<div class="rank-mat"><strong>' + utils.escapeHtml(t.matricule) + '</strong>'
        +   '<br><span class="muted">' + utils.escapeHtml(utils.nomEtudiant(t.matricule)) + '</span></div>'
        + '<div class="rank-moy">' + utils.badgeMoyenne(t.moyenneGenerale) + '</div>'
        + '</div>'
      ).join('');
    } catch (e) {
      document.getElementById('dash-top5').innerHTML = '<div class="empty">Erreur : ' + utils.escapeHtml(e.message) + '</div>';
    }
  }

  async function chargerReussiteParFiliere() {
    const wrap = document.getElementById('dash-reussite-fil');
    try {
      const r = await api.dashboard.reussite();
      const ent = Object.entries(r.tauxReussiteParFiliere || {});
      if (ent.length === 0) { wrap.innerHTML = '<div class="empty">—</div>'; return; }
      wrap.innerHTML = ent.sort((a, b) => b[1] - a[1]).map(([fil, t]) => {
        const pct = (t * 100).toFixed(1);
        return '<div class="bar-row">'
          + '<div class="bar-label">' + utils.escapeHtml(fil) + '</div>'
          + '<div class="bar-track"><div class="bar-fill" style="width:' + pct + '%"></div></div>'
          + '<div class="bar-value">' + pct + ' %</div>'
          + '</div>';
      }).join('');
    } catch (e) {
      wrap.innerHTML = '<div class="empty">Erreur : ' + utils.escapeHtml(e.message) + '</div>';
    }
  }

  async function chargerFinanceParFiliere() {
    const wrap = document.getElementById('dash-fin-fil');
    try {
      const f = await api.dashboard.financier();
      const ent = Object.entries(f.parFiliere || {});
      if (ent.length === 0) { wrap.innerHTML = '<div class="empty">—</div>'; return; }
      wrap.innerHTML = ent.sort((a, b) => b[1].taux - a[1].taux).map(([fil, d]) => {
        const pct = (d.taux * 100).toFixed(1);
        return '<div class="bar-row">'
          + '<div class="bar-label">' + utils.escapeHtml(fil) + '</div>'
          + '<div class="bar-track"><div class="bar-fill" style="width:' + pct + '%"></div></div>'
          + '<div class="bar-value">' + pct + ' % • ' + fmtMoney(d.encaisse) + '</div>'
          + '</div>';
      }).join('');
    } catch (e) {
      wrap.innerHTML = '<div class="empty">Erreur : ' + utils.escapeHtml(e.message) + '</div>';
    }
  }

  async function init() {
    await chargerSynthese();
    await chargerTop5();
    await chargerRisque();
    await chargerReussiteParFiliere();
    await chargerFinanceParFiliere();
  }

  window.dashboardModule = { init };
})();
