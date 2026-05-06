// api.js — Wrapper fetch pour l'API REST du backend Akka HTTP
// Le frontend est servi par le même serveur que l'API, donc URL relative.

const BASE_URL = '/api';

async function request(method, path, body) {
  const opts = {
    method,
    headers: { 'Accept': 'application/json' }
  };
  if (body !== undefined && body !== null) {
    opts.headers['Content-Type'] = 'application/json';
    opts.body = JSON.stringify(body);
  }
  let resp;
  try {
    resp = await fetch(BASE_URL + path, opts);
  } catch (e) {
    throw new Error('Connexion impossible au serveur : ' + e.message);
  }
  const text = await resp.text();
  let data = null;
  if (text) {
    try { data = JSON.parse(text); }
    catch (_) { data = text; }
  }
  if (!resp.ok) {
    const msg = (data && data.message) ? data.message
              : (typeof data === 'string' && data) ? data
              : ('Erreur HTTP ' + resp.status);
    const err = new Error(msg);
    err.status = resp.status;
    err.data = data;
    throw err;
  }
  return data;
}

const api = {
  get:    (path)        => request('GET', path),
  post:   (path, body)  => request('POST', path, body),
  put:    (path, body)  => request('PUT', path, body),
  patch:  (path, body)  => request('PATCH', path, body),
  delete: (path)        => request('DELETE', path),

  // ----- Notes -----
  notes: {
    liste:        ()              => api.get('/notes'),
    parEtudiant:  (matricule)     => api.get('/notes/etudiant/' + encodeURIComponent(matricule)),
    parMatiere:   (idMatiere)     => api.get('/notes/matiere/' + encodeURIComponent(idMatiere)),
    moyenne:      (matricule)     => api.get('/notes/moyenne/' + encodeURIComponent(matricule)),
    releve:       (matricule)     => api.get('/notes/releve/' + encodeURIComponent(matricule)),
    classement:   ()              => api.get('/notes/classement'),
    echec:        ()              => api.get('/notes/echec'),
    incompletes:  ()              => api.get('/notes/incompletes'),
    invalides:    ()              => api.get('/notes/invalides'),
    statsMatiere: ()              => api.get('/notes/stats/parmatiere'),
    creer:        (note)          => api.post('/notes', note),
    supprimer:    (id)            => api.delete('/notes/' + encodeURIComponent(id))
  },

  // ----- Absences -----
  absences: {
    liste:        ()              => api.get('/absences'),
    parEtudiant:  (matricule)     => api.get('/absences/etudiant/' + encodeURIComponent(matricule)),
    parMatiere:   (idMatiere)     => api.get('/absences/matiere/' + encodeURIComponent(idMatiere)),
    nonJustifiees:()              => api.get('/absences/non-justifiees'),
    alerte:       (seuil = 10)    => api.get('/absences/alerte?seuil=' + encodeURIComponent(seuil)),
    statsFiliere: ()              => api.get('/absences/stats/filiere'),
    statsMatiere: ()              => api.get('/absences/stats/matiere'),
    creer:        (abs)           => api.post('/absences', abs),
    justifier:    (id)            => api.patch('/absences/' + encodeURIComponent(id) + '/justifier', {}),
    supprimer:    (id)            => api.delete('/absences/' + encodeURIComponent(id))
  },

  // ----- Séances / emplois du temps -----
  seances: {
    liste:        ()              => api.get('/seances'),
    parClasse:    (filiere, niveau) =>
      api.get('/seances/classe?filiere=' + encodeURIComponent(filiere)
                 + '&niveau=' + encodeURIComponent(niveau)),
    parEnseignant:(idEns)         => api.get('/seances/enseignant/' + encodeURIComponent(idEns)),
    parSalle:     (idSalle)       => api.get('/seances/salle/' + encodeURIComponent(idSalle)),
    conflits:     ()              => api.get('/seances/conflits'),
    verifier:     (seance)        => api.post('/seances/verifier', seance),
    creer:        (seance)        => api.post('/seances', seance),
    modifier:     (id, seance)    => api.put('/seances/' + encodeURIComponent(id), seance),
    supprimer:    (id)            => api.delete('/seances/' + encodeURIComponent(id))
  },

  // ----- Référentiels -----
  ref: {
    etudiants:   () => api.get('/ref/etudiants'),
    matieres:    () => api.get('/ref/matieres'),
    salles:      () => api.get('/ref/salles'),
    enseignants: () => api.get('/ref/enseignants'),
    filieres:    () => api.get('/ref/filieres')
  },

  // ----- Étudiants (Module 1) -----
  etudiants: {
    liste:        (filtres = {}) => {
      const q = Object.entries(filtres).filter(([_, v]) => v).map(([k, v]) => `${k}=${encodeURIComponent(v)}`).join('&');
      return api.get('/etudiants' + (q ? '?' + q : ''));
    },
    trouver:      (m)         => api.get('/etudiants/' + encodeURIComponent(m)),
    stats:        ()          => api.get('/etudiants/stats'),
    creer:        (e)         => api.post('/etudiants', e),
    modifier:     (m, e)      => api.put('/etudiants/' + encodeURIComponent(m), e),
    supprimer:    (m)         => api.delete('/etudiants/' + encodeURIComponent(m)),
    changerStatut:(m, valeur) =>
      api.patch('/etudiants/' + encodeURIComponent(m) + '/statut?valeur=' + encodeURIComponent(valeur), {})
  },

  // ----- Enseignants (Module 2) -----
  enseignants: {
    liste:        (departement) =>
      api.get('/enseignants' + (departement ? '?departement=' + encodeURIComponent(departement) : '')),
    trouver:      (id)        => api.get('/enseignants/' + encodeURIComponent(id)),
    stats:        ()          => api.get('/enseignants/stats'),
    cours:        (id)        => api.get('/enseignants/' + encodeURIComponent(id) + '/cours'),
    creer:        (e)         => api.post('/enseignants', e),
    modifier:     (id, e)     => api.put('/enseignants/' + encodeURIComponent(id), e),
    supprimer:    (id)        => api.delete('/enseignants/' + encodeURIComponent(id))
  },

  // ----- Filières (Module 3) -----
  filieres: {
    liste:        ()          => api.get('/filieres'),
    creer:        (f)         => api.post('/filieres', f),
    modifier:     (id, f)     => api.put('/filieres/' + encodeURIComponent(id), f),
    supprimer:    (id)        => api.delete('/filieres/' + encodeURIComponent(id))
  },

  // ----- Matières (Module 3) -----
  matieres: {
    liste:        ()          => api.get('/matieres'),
    parUe:        ()          => api.get('/matieres/par-ue'),
    stats:        ()          => api.get('/matieres/stats'),
    creer:        (m)         => api.post('/matieres', m),
    modifier:     (id, m)     => api.put('/matieres/' + encodeURIComponent(id), m),
    supprimer:    (id)        => api.delete('/matieres/' + encodeURIComponent(id))
  },

  // ----- Inscriptions (Module 4) -----
  inscriptions: {
    liste:        (statut)    => api.get('/inscriptions' + (statut ? '?statut=' + encodeURIComponent(statut) : '')),
    parEtudiant:  (m)         => api.get('/inscriptions?matricule=' + encodeURIComponent(m)),
    stats:        ()          => api.get('/inscriptions/stats'),
    creer:        (i)         => api.post('/inscriptions', i),
    changerStatut:(id, valeur)=>
      api.patch('/inscriptions/' + encodeURIComponent(id) + '/statut?valeur=' + encodeURIComponent(valeur), {}),
    supprimer:    (id)        => api.delete('/inscriptions/' + encodeURIComponent(id))
  },

  // ----- Paiements (Module 8) -----
  paiements: {
    liste:        (matricule) => api.get('/paiements' + (matricule ? '?matricule=' + encodeURIComponent(matricule) : '')),
    dette:        ()          => api.get('/paiements/dette'),
    synthese:     ()          => api.get('/paiements/synthese'),
    reste:        (m)         => api.get('/paiements/etudiant/' + encodeURIComponent(m) + '/reste'),
    creer:        (p)         => api.post('/paiements', p),
    supprimer:    (id)        => api.delete('/paiements/' + encodeURIComponent(id))
  },

  // ----- Tableau de bord (Module 9) -----
  dashboard: {
    synthese:     ()          => api.get('/dashboard/synthese'),
    etudiants:    ()          => api.get('/dashboard/etudiants'),
    reussite:     ()          => api.get('/dashboard/reussite'),
    absenteisme:  ()          => api.get('/dashboard/absenteisme'),
    financier:    ()          => api.get('/dashboard/financier'),
    risque:       (seuil = 10)=> api.get('/dashboard/risque?seuil=' + seuil),
    topEnseignants:(n = 5)    => api.get('/dashboard/topEnseignants?n=' + n)
  },

  // ----- Big Data (Module 10) -----
  bigdata: {
    exporter:     ()          => api.post('/bigdata/export', {}),
    manquantes:   ()          => api.get('/bigdata/manquantes'),
    promotion:    ()          => api.get('/bigdata/promotion'),
    absencesMois: ()          => api.get('/bigdata/absences-mois'),
    paiementsMois:()          => api.get('/bigdata/paiements-mois')
  }
};

window.api = api;
