// app.js — Bootstrap général : routing entre vues, init des modules

(function () {
  // Modules déjà initialisés (lazy init au premier clic sur la vue)
  const initialises = new Set();

  function initVue(target) {
    if (initialises.has(target)) return;
    initialises.add(target);
    try {
      switch (target) {
        case 'dashboard':    if (window.dashboardModule)    dashboardModule.init();    break;
        case 'etudiants':    if (window.etudiantsModule)    etudiantsModule.init();    break;
        case 'enseignants':  if (window.enseignantsModule)  enseignantsModule.init();  break;
        case 'formations':   if (window.formationsModule)   formationsModule.init();   break;
        case 'inscriptions': if (window.inscriptionsModule) inscriptionsModule.init(); break;
        case 'notes':        if (window.notesModule)        notesModule.init();        break;
        case 'absences':     if (window.absencesModule)     absencesModule.init();     break;
        case 'emplois':      if (window.emploisModule)      emploisModule.init();      break;
        case 'paiements':    if (window.paiementsModule)    paiementsModule.init();    break;
        case 'bigdata':      if (window.bigdataModule)      bigdataModule.init();      break;
      }
    } catch (e) {
      console.error('Erreur init ' + target, e);
      utils.showToast('Erreur initialisation ' + target + ' : ' + e.message, 'error');
    }
  }

  function changerVue(target) {
    document.querySelectorAll('.view').forEach(v => v.classList.remove('is-active'));
    const view = document.getElementById('view-' + target);
    if (view) view.classList.add('is-active');

    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('is-active'));
    const nav = document.querySelector('.nav-item[data-view="' + target + '"]');
    if (nav) nav.classList.add('is-active');

    initVue(target);
    // Scroll en haut
    document.querySelector('.main').scrollTo(0, 0);
  }

  function bindNav() {
    document.querySelectorAll('.nav-item').forEach(item => {
      item.addEventListener('click', () => changerVue(item.dataset.view));
    });
  }

  async function pingApi() {
    const el = document.getElementById('api-status');
    try {
      const m = await api.ref.matieres();
      if (Array.isArray(m)) {
        el.textContent = 'API connectée';
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
      // Initialiser la vue active (dashboard par défaut)
      const active = document.querySelector('.nav-item.is-active');
      const target = active ? active.dataset.view : 'dashboard';
      initVue(target);
      utils.showToast('Plateforme prête.', 'success');
    } catch (e) {
      utils.showToast('Erreur de démarrage : ' + e.message, 'error');
      console.error(e);
    }
  });
})();
