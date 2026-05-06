// enseignants.js — Module 2 : Gestion des enseignants
(function () {
  let etat = [];

  async function chargerKpis() {
    try {
      const s = await api.enseignants.stats();
      const top = (s.topVolume || [])[0];
      const dep = Object.keys(s.parDepartement || {}).length;
      const spec = new Set((utils.refCache.enseignants || []).map(e => e.specialite)).size;
      document.getElementById('kpi-ens-total').textContent  = s.total;
      document.getElementById('kpi-ens-dep').textContent    = dep;
      document.getElementById('kpi-ens-volume').textContent = top ? (top.heures + 'h') : '—';
      document.getElementById('kpi-ens-spec').textContent   = spec;
    } catch (e) {
      utils.showToast('Erreur stats : ' + e.message, 'error');
    }
  }

  function rendreTable() {
    const tbody = document.getElementById('ens-tbody');
    const recherche = (document.getElementById('ens-search').value || '').trim().toLowerCase();
    const dep = document.getElementById('ens-filter-dep').value;
    const lignes = etat.filter(e => {
      const okR = !recherche || (e.nom + ' ' + e.prenom).toLowerCase().includes(recherche)
                 || e.idEnseignant.toLowerCase().includes(recherche);
      const okD = !dep || e.departement === dep;
      return okR && okD;
    });
    if (lignes.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" class="empty">Aucun enseignant.</td></tr>';
      return;
    }
    tbody.innerHTML = lignes.map(e =>
      '<tr>'
      + '<td><code>' + utils.escapeHtml(e.idEnseignant) + '</code></td>'
      + '<td><strong>' + utils.escapeHtml(e.prenom + ' ' + e.nom) + '</strong></td>'
      + '<td>' + utils.escapeHtml(e.grade) + '</td>'
      + '<td>' + utils.escapeHtml(e.specialite) + '</td>'
      + '<td>' + utils.escapeHtml(e.departement) + '</td>'
      + '<td><span class="muted">' + utils.escapeHtml(e.email) + '</span></td>'
      + '<td><button class="btn btn-sm" data-action="cours-ens" data-id="' + utils.escapeHtml(e.idEnseignant) + '">Cours</button>'
      +     ' <button class="btn btn-sm btn-danger" data-action="del-ens" data-id="' + utils.escapeHtml(e.idEnseignant) + '">Suppr.</button></td>'
      + '</tr>'
    ).join('');
  }

  function remplirFiltreDep() {
    const sel = document.getElementById('ens-filter-dep');
    const deps = Array.from(new Set((utils.refCache.enseignants || []).map(e => e.departement))).sort();
    sel.innerHTML = '<option value="">Tous départements</option>'
      + deps.map(d => '<option>' + utils.escapeHtml(d) + '</option>').join('');
  }

  async function chargerListe() {
    try {
      etat = await api.enseignants.liste() || [];
      rendreTable();
      await chargerKpis();
    } catch (e) {
      utils.showToast('Erreur : ' + e.message, 'error');
    }
  }

  async function rendreVolume() {
    const wrap = document.getElementById('ens-volume-list');
    wrap.innerHTML = '<div class="muted">Calcul…</div>';
    try {
      const s = await api.enseignants.stats();
      const top = s.topVolume || [];
      if (top.length === 0) { wrap.innerHTML = '<div class="empty">—</div>'; return; }
      const max = Math.max(1, ...top.map(t => t.heures));
      wrap.innerHTML = top.map(t => {
        const pct = (t.heures / max) * 100;
        return '<div class="bar-row">'
          + '<div class="bar-label">' + utils.escapeHtml(utils.nomEnseignant(t.idEnseignant)) + '</div>'
          + '<div class="bar-track"><div class="bar-fill" style="width:' + pct.toFixed(1) + '%"></div></div>'
          + '<div class="bar-value">' + t.heures + ' h</div>'
          + '</div>';
      }).join('');
    } catch (e) {
      wrap.innerHTML = '<div class="empty">Erreur : ' + utils.escapeHtml(e.message) + '</div>';
    }
  }

  async function rendreParDepartement() {
    const wrap = document.getElementById('ens-dep-list');
    wrap.innerHTML = '<div class="muted">Calcul…</div>';
    try {
      const s = await api.enseignants.stats();
      const ent = Object.entries(s.parDepartement || {});
      if (ent.length === 0) { wrap.innerHTML = '<div class="empty">—</div>'; return; }
      const max = Math.max(1, ...ent.map(([_, n]) => n));
      wrap.innerHTML = ent.map(([dep, n]) => {
        const pct = (n / max) * 100;
        return '<div class="bar-row">'
          + '<div class="bar-label">' + utils.escapeHtml(dep) + '</div>'
          + '<div class="bar-track"><div class="bar-fill" style="width:' + pct.toFixed(1) + '%"></div></div>'
          + '<div class="bar-value">' + n + '</div>'
          + '</div>';
      }).join('');
    } catch (e) {
      wrap.innerHTML = '<div class="empty">Erreur : ' + utils.escapeHtml(e.message) + '</div>';
    }
  }

  function ouvrirFormulaire() {
    const html =
      '<form id="form-ens">'
      + '<div class="form-row">'
      +   '<div class="form-group"><label class="form-label">Nom</label><input class="form-input" name="nom" required></div>'
      +   '<div class="form-group"><label class="form-label">Prénom</label><input class="form-input" name="prenom" required></div>'
      + '</div>'
      + '<div class="form-row">'
      +   '<div class="form-group"><label class="form-label">Grade</label><input class="form-input" name="grade"></div>'
      +   '<div class="form-group"><label class="form-label">Spécialité</label><input class="form-input" name="specialite"></div>'
      + '</div>'
      + '<div class="form-row">'
      +   '<div class="form-group"><label class="form-label">Département</label><input class="form-input" name="departement"></div>'
      +   '<div class="form-group"><label class="form-label">Email</label><input class="form-input" type="email" name="email"></div>'
      + '</div>'
      + '<div class="form-group"><label class="form-label">ID Enseignant (auto si vide)</label>'
      +   '<input class="form-input" name="idEnseignant" placeholder="ENS006…"></div>'
      + '<div class="form-group"><label class="form-label">Téléphone</label><input class="form-input" name="telephone"></div>'
      + '<div class="form-actions">'
      +   '<button type="button" class="btn btn-ghost" data-close-modal>Annuler</button>'
      +   '<button type="submit" class="btn btn-primary">Enregistrer</button>'
      + '</div></form>';
    utils.openModal('Nouvel enseignant', html);

    document.getElementById('form-ens').addEventListener('submit', async (ev) => {
      ev.preventDefault();
      const f = ev.target;
      const idAuto = 'ENS' + String(Date.now()).slice(-3);
      const payload = {
        idEnseignant: f.idEnseignant.value || idAuto,
        nom: f.nom.value, prenom: f.prenom.value,
        grade: f.grade.value, specialite: f.specialite.value,
        departement: f.departement.value, email: f.email.value, telephone: f.telephone.value
      };
      try {
        await api.enseignants.creer(payload);
        utils.closeModal();
        utils.showToast('Enseignant créé.', 'success');
        await utils.loadRefs();
        await chargerListe();
      } catch (e) {
        utils.showToast('Échec : ' + e.message, 'error');
      }
    });
  }

  async function afficherCours(id) {
    try {
      const cours = await api.enseignants.cours(id) || [];
      if (cours.length === 0) {
        utils.openModal('Cours assurés', '<p class="muted">Aucun cours assuré par cet enseignant.</p>');
        return;
      }
      const html = '<div class="table-wrap"><table class="table"><thead><tr><th>ID</th><th>Matière</th><th>UE</th><th class="num">Coef</th><th class="num">Volume</th></tr></thead><tbody>'
        + cours.map(m =>
            '<tr>'
            + '<td><code>' + utils.escapeHtml(m.idMatiere) + '</code></td>'
            + '<td>' + utils.escapeHtml(m.nomMatiere) + '</td>'
            + '<td>' + utils.escapeHtml(m.ue) + '</td>'
            + '<td class="num">' + m.coefficient + '</td>'
            + '<td class="num">' + m.volumeHoraire + ' h</td>'
            + '</tr>'
          ).join('')
        + '</tbody></table></div>';
      utils.openModal('Cours assurés par ' + utils.nomEnseignant(id), html);
    } catch (e) {
      utils.showToast('Échec : ' + e.message, 'error');
    }
  }

  async function supprimer(id) {
    if (!confirm('Supprimer ' + id + ' ?')) return;
    try {
      await api.enseignants.supprimer(id);
      utils.showToast('Supprimé.', 'success');
      await utils.loadRefs();
      await chargerListe();
    } catch (e) {
      utils.showToast('Échec : ' + e.message, 'error');
    }
  }

  async function init() {
    utils.bindTabs('#view-enseignants');
    remplirFiltreDep();
    document.getElementById('ens-search').addEventListener('input', utils.debounce(rendreTable, 150));
    document.getElementById('ens-filter-dep').addEventListener('change', rendreTable);
    document.getElementById('btn-new-ens').addEventListener('click', ouvrirFormulaire);
    document.getElementById('ens-tbody').addEventListener('click', (e) => {
      const c = e.target.closest('[data-action="cours-ens"]');
      if (c) return afficherCours(c.dataset.id);
      const d = e.target.closest('[data-action="del-ens"]');
      if (d) return supprimer(d.dataset.id);
    });
    document.querySelectorAll('#view-enseignants .tab').forEach(t => {
      t.addEventListener('click', () => {
        if (t.dataset.tab === 'tab-ens-volume') rendreVolume();
        if (t.dataset.tab === 'tab-ens-departement') rendreParDepartement();
      });
    });
    await chargerListe();
    await rendreVolume();
    await rendreParDepartement();
  }

  window.enseignantsModule = { init };
})();
