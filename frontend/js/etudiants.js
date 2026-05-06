// etudiants.js — Module 1 : Gestion des étudiants
(function () {
  let etat = [];

  async function chargerKpis() {
    try {
      const s = await api.etudiants.stats();
      document.getElementById('kpi-etu-total').textContent  = s.total;
      document.getElementById('kpi-etu-actifs').textContent = s.actifs;
      document.getElementById('kpi-etu-susp').textContent   = s.suspendus;
      document.getElementById('kpi-etu-dipl').textContent   = s.diplomes;
    } catch (e) {
      utils.showToast('Erreur stats étudiants : ' + e.message, 'error');
    }
  }

  function rendreTable() {
    const tbody = document.getElementById('etu-tbody');
    const recherche = (document.getElementById('etu-search').value || '').trim().toLowerCase();
    const lignes = etat.filter(e => {
      if (!recherche) return true;
      return e.matricule.toLowerCase().includes(recherche)
        || (e.nom + ' ' + e.prenom).toLowerCase().includes(recherche);
    });
    if (lignes.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" class="empty">Aucun étudiant.</td></tr>';
      return;
    }
    tbody.innerHTML = lignes.map(e => {
      const badge = e.statut === 'Actif' ? 'badge badge-ok'
        : e.statut === 'Suspendu' ? 'badge badge-bad'
        : e.statut === 'Diplome'  ? 'badge badge-info'
        : 'badge';
      return '<tr>'
        + '<td><code>' + utils.escapeHtml(e.matricule) + '</code></td>'
        + '<td><strong>' + utils.escapeHtml(e.prenom + ' ' + e.nom) + '</strong></td>'
        + '<td>' + utils.escapeHtml(e.filiere) + '</td>'
        + '<td>' + utils.escapeHtml(e.niveau) + '</td>'
        + '<td><span class="muted">' + utils.escapeHtml(e.email) + '</span></td>'
        + '<td><span class="' + badge + '">' + utils.escapeHtml(e.statut) + '</span></td>'
        + '<td><button class="btn btn-sm btn-danger" data-action="del-etu" data-id="'
            + utils.escapeHtml(e.matricule) + '">Suppr.</button></td>'
        + '</tr>';
    }).join('');
  }

  async function chargerListe() {
    try {
      const filiere = document.getElementById('etu-filter-filiere').value;
      const niveau  = document.getElementById('etu-filter-niveau').value;
      const statut  = document.getElementById('etu-filter-statut').value;
      etat = await api.etudiants.liste({ filiere, niveau, statut }) || [];
      rendreTable();
    } catch (e) {
      utils.showToast('Erreur chargement étudiants : ' + e.message, 'error');
    }
  }

  function remplirFiltreFiliere() {
    const sel = document.getElementById('etu-filter-filiere');
    const filieres = Array.from(new Set((utils.refCache.etudiants || []).map(x => x.filiere))).sort();
    sel.innerHTML = '<option value="">Toutes filières</option>'
      + filieres.map(f => '<option value="' + utils.escapeHtml(f) + '">' + utils.escapeHtml(f) + '</option>').join('');
  }

  function ouvrirFormulaire() {
    const filieres = Array.from(new Set((utils.refCache.etudiants || []).map(x => x.filiere))).sort();
    const optFil = filieres.map(f => '<option>' + utils.escapeHtml(f) + '</option>').join('');
    const html =
      '<form id="form-etu">'
      + '<div class="form-row">'
      +   '<div class="form-group"><label class="form-label">Nom</label><input class="form-input" name="nom" required></div>'
      +   '<div class="form-group"><label class="form-label">Prénom</label><input class="form-input" name="prenom" required></div>'
      + '</div>'
      + '<div class="form-row">'
      +   '<div class="form-group"><label class="form-label">Sexe</label>'
      +     '<select class="form-select" name="sexe"><option>M</option><option>F</option></select></div>'
      +   '<div class="form-group"><label class="form-label">Date de naissance</label>'
      +     '<input class="form-input" type="date" name="dateNaissance"></div>'
      + '</div>'
      + '<div class="form-row">'
      +   '<div class="form-group"><label class="form-label">Email</label><input class="form-input" type="email" name="email"></div>'
      +   '<div class="form-group"><label class="form-label">Téléphone</label><input class="form-input" name="telephone"></div>'
      + '</div>'
      + '<div class="form-row">'
      +   '<div class="form-group"><label class="form-label">Filière</label>'
      +     '<select class="form-select" name="filiere">' + optFil + '</select></div>'
      +   '<div class="form-group"><label class="form-label">Niveau</label>'
      +     '<select class="form-select" name="niveau">'
      +     '<option>L1</option><option>L2</option><option>L3</option><option>M1</option><option>M2</option></select></div>'
      + '</div>'
      + '<div class="form-row">'
      +   '<div class="form-group"><label class="form-label">Année</label>'
      +     '<input class="form-input" name="annee" value="2025-2026" required></div>'
      +   '<div class="form-group"><label class="form-label">Statut</label>'
      +     '<select class="form-select" name="statut"><option>Actif</option><option>Suspendu</option><option>Diplome</option></select></div>'
      + '</div>'
      + '<div class="form-actions">'
      +   '<button type="button" class="btn btn-ghost" data-close-modal>Annuler</button>'
      +   '<button type="submit" class="btn btn-primary">Enregistrer</button>'
      + '</div></form>';

    utils.openModal('Nouvel étudiant', html);
    document.getElementById('form-etu').addEventListener('submit', async (ev) => {
      ev.preventDefault();
      const f = ev.target;
      const payload = {
        matricule: 'auto',
        nom: f.nom.value, prenom: f.prenom.value,
        sexe: f.sexe.value,
        dateNaissance: f.dateNaissance.value || null,
        email: f.email.value, telephone: f.telephone.value,
        filiere: f.filiere.value, niveau: f.niveau.value,
        annee: f.annee.value, statut: f.statut.value
      };
      try {
        await api.etudiants.creer(payload);
        utils.closeModal();
        utils.showToast('Étudiant créé.', 'success');
        await chargerListe();
        await chargerKpis();
      } catch (e) {
        utils.showToast('Échec : ' + e.message, 'error');
      }
    });
  }

  async function supprimer(matricule) {
    if (!confirm('Supprimer définitivement l\'étudiant ' + matricule + ' ?')) return;
    try {
      await api.etudiants.supprimer(matricule);
      utils.showToast('Étudiant supprimé.', 'success');
      await chargerListe();
      await chargerKpis();
    } catch (e) {
      utils.showToast('Échec : ' + e.message, 'error');
    }
  }

  async function init() {
    remplirFiltreFiliere();
    document.getElementById('etu-search').addEventListener('input', utils.debounce(rendreTable, 150));
    document.getElementById('etu-filter-filiere').addEventListener('change', chargerListe);
    document.getElementById('etu-filter-niveau').addEventListener('change', chargerListe);
    document.getElementById('etu-filter-statut').addEventListener('change', chargerListe);
    document.getElementById('btn-new-etu').addEventListener('click', ouvrirFormulaire);
    document.getElementById('etu-tbody').addEventListener('click', (e) => {
      const btn = e.target.closest('[data-action="del-etu"]');
      if (btn) supprimer(btn.dataset.id);
    });
    await chargerListe();
    await chargerKpis();
  }

  window.etudiantsModule = { init };
})();
