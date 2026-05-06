// bigdata.js — Module 10 : Big Data (exports CSV + analyses)
(function () {

  async function chargerKpis() {
    try {
      const m = await api.bigdata.manquantes();
      const promo = await api.bigdata.promotion();
      document.getElementById('kpi-bd-cc').textContent    = m['notes_sans_cc'] || 0;
      document.getElementById('kpi-bd-exam').textContent  = m['notes_sans_examen'] || 0;
      document.getElementById('kpi-bd-inv').textContent   = m['notes_invalides'] || 0;
      document.getElementById('kpi-bd-promo').textContent = Object.keys(promo || {}).length;
    } catch (e) { utils.showToast('Erreur Big Data : ' + e.message, 'error'); }
  }

  function rendreBars(elementId, data, suffix = '') {
    const wrap = document.getElementById(elementId);
    const ent = Object.entries(data || {});
    if (ent.length === 0) { wrap.innerHTML = '<div class="empty">Aucune donnée.</div>'; return; }
    const max = Math.max(1, ...ent.map(([_, v]) => v));
    wrap.innerHTML = ent.sort((a, b) => a[0].localeCompare(b[0])).map(([k, v]) => {
      const pct = (v / max) * 100;
      const valStr = typeof v === 'number' && !Number.isInteger(v) ? v.toFixed(2) : v;
      return '<div class="bar-row">'
        + '<div class="bar-label">' + utils.escapeHtml(k) + '</div>'
        + '<div class="bar-track"><div class="bar-fill" style="width:' + pct.toFixed(1) + '%"></div></div>'
        + '<div class="bar-value">' + valStr + suffix + '</div>'
        + '</div>';
    }).join('');
  }

  async function rendrePromo() {
    try {
      const data = await api.bigdata.promotion();
      rendreBars('bd-promo', data, ' /20');
    } catch (e) {
      document.getElementById('bd-promo').innerHTML = '<div class="empty">Erreur : ' + utils.escapeHtml(e.message) + '</div>';
    }
  }

  async function rendreAbsMois() {
    try {
      const data = await api.bigdata.absencesMois();
      rendreBars('bd-abs-mois', data, ' h');
    } catch (e) {
      document.getElementById('bd-abs-mois').innerHTML = '<div class="empty">Erreur : ' + utils.escapeHtml(e.message) + '</div>';
    }
  }

  async function rendrePaieMois() {
    try {
      const data = await api.bigdata.paiementsMois();
      const formatte = {};
      for (const [k, v] of Object.entries(data || {})) {
        formatte[k] = Math.round(v);
      }
      rendreBars('bd-paie-mois', formatte, ' F');
    } catch (e) {
      document.getElementById('bd-paie-mois').innerHTML = '<div class="empty">Erreur : ' + utils.escapeHtml(e.message) + '</div>';
    }
  }

  async function lancerExports() {
    const btn = document.getElementById('btn-bd-export');
    const orig = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<span>⌛</span> Export en cours…';
    try {
      const r = await api.bigdata.exporter();
      utils.showToast('Exports terminés.', 'success');

      const wrap = document.getElementById('bd-export-result');
      const list = document.getElementById('bd-export-list');
      wrap.style.display = 'block';
      list.innerHTML = ''
        + '<div class="export-item"><span class="export-label">Rapport académique</span><code class="export-path">' + utils.escapeHtml(r.rapportAcademique) + '</code></div>'
        + '<div class="export-item"><span class="export-label">Performances par matière</span><code class="export-path">' + utils.escapeHtml(r.performances) + '</code></div>'
        + '<div class="export-item"><span class="export-label">Indicateurs financiers</span><code class="export-path">' + utils.escapeHtml(r.indicateursFinanciers) + '</code></div>'
        + '<div class="muted" style="margin-top:12px">Les fichiers sont générés dans le dossier <code>./output/</code> du projet.</div>';
    } catch (e) {
      utils.showToast('Échec export : ' + e.message, 'error');
    } finally {
      btn.disabled = false;
      btn.innerHTML = orig;
    }
  }

  async function init() {
    document.getElementById('btn-bd-export').addEventListener('click', lancerExports);
    await chargerKpis();
    await rendrePromo();
    await rendreAbsMois();
    await rendrePaieMois();
  }

  window.bigdataModule = { init };
})();
