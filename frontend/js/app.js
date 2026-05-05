// app.js — Bootstrap général : routing entre vues, init des modules

(function () {
  function changerVue(target) {
    document.querySelectorAll('.view').forEach(v => v.classList.remove('is-active'));
    const view = document.getElementById('view-' + target);
    if (view) view.classList.add('is-active');

    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('is-active'));
    const nav = document.querySelector('.nav-item[data-view="' + target + '"]');
    if (nav) nav.classList.add('is-active');
  }

  function bindNav() {
    document.querySelectorAll('.nav-item').forEach(item => {
      item.addEventListener('click', () => changerVue(item.dataset.view));
    });
  }

  async function pingApi() {
    const el = document.getElementById('api-status');
    try {
      // Le simple chargement du référentiel sert de health-check.
      const m = await api.ref.matieres();
      if (Array.isArray(m)) {
        el.textContent = 'API connectée (' + m.length + ' matières)';
        el.classList.add('ok');
      }
    } catch (e) {
      el.textContent = 'API déconnectée';
      el.classList.add('ko');
      throw e;
    }
  }

  document.addEventListener('DOMContentLoaded', async () => {
    bindNav();
    try {
      await pingApi();
      await utils.loadRefs();
      await Promise.all([
        notesModule.init(),
        absencesModule.init(),
        emploisModule.init()
      ]);
      utils.showToast('Plateforme prête.', 'success');
    } catch (e) {
      utils.showToast('Erreur de démarrage : ' + e.message, 'error');
      console.error(e);
    }
  });
})();
