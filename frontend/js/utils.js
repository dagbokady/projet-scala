// utils.js — Helpers UI partagés

// ---------- Toast ----------
let toastTimer = null;
function showToast(message, kind = 'info') {
  const el = document.getElementById('toast');
  if (!el) return;
  el.textContent = message;
  // Retirer les classes précédentes
  el.classList.remove('show', 'toast-success', 'toast-error', 'toast-info');
  // Ajouter la bonne couleur
  if (kind === 'success') el.classList.add('toast-success');
  else if (kind === 'error') el.classList.add('toast-error');
  el.classList.add('show');
  el.setAttribute('aria-hidden', 'false');
  if (toastTimer) clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    el.classList.remove('show');
    el.setAttribute('aria-hidden', 'true');
  }, 3200);
}

// ---------- Modal ----------
function openModal(title, htmlBody) {
  const modal = document.getElementById('modal');
  document.getElementById('modal-title').textContent = title;
  document.getElementById('modal-body').innerHTML = htmlBody;
  modal.setAttribute('aria-hidden', 'false');
  modal.classList.add('is-open');
}
function closeModal() {
  const modal = document.getElementById('modal');
  modal.setAttribute('aria-hidden', 'true');
  modal.classList.remove('is-open');
  document.getElementById('modal-body').innerHTML = '';
}
// Fermer au clic sur le backdrop ou sur les éléments [data-close-modal]
document.addEventListener('click', (e) => {
  if (e.target.matches('[data-close-modal]')) closeModal();
  if (e.target.id === 'modal') closeModal();
});
document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') closeModal();
});

// ---------- Formatage ----------
function formatDate(s) {
  if (!s) return '—';
  // s: "2025-10-12" ou ISO
  const d = new Date(s);
  if (isNaN(d.getTime())) return s;
  return d.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' });
}
function formatHeure(s) {
  if (!s) return '—';
  // s: "08:00" ou "08:00:00"
  return s.length >= 5 ? s.substring(0, 5) : s;
}
function formatNum(n, dec = 2) {
  if (n === null || n === undefined || Number.isNaN(n)) return '—';
  return Number(n).toFixed(dec);
}
function escapeHtml(s) {
  if (s === null || s === undefined) return '';
  return String(s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

// ---------- Debounce ----------
function debounce(fn, ms = 200) {
  let t = null;
  return function (...args) {
    clearTimeout(t);
    t = setTimeout(() => fn.apply(this, args), ms);
  };
}

// ---------- Badges ----------
function badgeClassFromDecision(decision) {
  switch ((decision || '').toLowerCase()) {
    case 'admis':       return 'badge badge-ok';
    case 'ajourne':
    case 'ajourné':     return 'badge badge-warn';
    case 'redoublement':return 'badge badge-bad';
    default:            return 'badge badge-info';
  }
}
function badgeMoyenne(m) {
  if (m === null || m === undefined) return '<span class="badge badge-info">—</span>';
  if (m >= 14) return '<span class="badge badge-ok">' + formatNum(m) + '</span>';
  if (m >= 10) return '<span class="badge badge-info">' + formatNum(m) + '</span>';
  if (m >= 8)  return '<span class="badge badge-warn">' + formatNum(m) + '</span>';
  return '<span class="badge badge-bad">' + formatNum(m) + '</span>';
}

// ---------- Tabs (générique) ----------
function bindTabs(rootSelector) {
  const root = document.querySelector(rootSelector);
  if (!root) return;
  root.querySelectorAll('.tab').forEach(tab => {
    tab.addEventListener('click', () => {
      const target = tab.dataset.tab;
      // boutons
      root.querySelectorAll('.tab').forEach(t => t.classList.remove('is-active'));
      tab.classList.add('is-active');
      // panneaux
      root.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('is-active'));
      const panel = document.getElementById(target);
      if (panel) panel.classList.add('is-active');
    });
  });
}

// ---------- Caches référentiels (chargés une fois) ----------
const refCache = {
  etudiants: null,
  matieres: null,
  salles: null,
  enseignants: null
};
async function loadRefs() {
  const [etu, mat, sal, ens] = await Promise.all([
    api.ref.etudiants(),
    api.ref.matieres(),
    api.ref.salles(),
    api.ref.enseignants()
  ]);
  refCache.etudiants   = etu  || [];
  refCache.matieres    = mat  || [];
  refCache.salles      = sal  || [];
  refCache.enseignants = ens  || [];
  return refCache;
}
function nomEtudiant(matricule) {
  const e = (refCache.etudiants || []).find(x => x.matricule === matricule);
  return e ? (e.prenom + ' ' + e.nom) : matricule;
}
function nomMatiere(idMatiere) {
  const m = (refCache.matieres || []).find(x => x.idMatiere === idMatiere);
  return m ? m.nomMatiere : idMatiere;
}
function nomSalle(idSalle) {
  const s = (refCache.salles || []).find(x => x.idSalle === idSalle);
  return s ? s.nomSalle : idSalle;
}
function nomEnseignant(idEns) {
  const e = (refCache.enseignants || []).find(x => x.idEnseignant === idEns);
  return e ? (e.prenom + ' ' + e.nom) : idEns;
}

window.utils = {
  showToast, openModal, closeModal,
  formatDate, formatHeure, formatNum, escapeHtml, debounce,
  badgeClassFromDecision, badgeMoyenne,
  bindTabs, loadRefs, refCache,
  nomEtudiant, nomMatiere, nomSalle, nomEnseignant
};