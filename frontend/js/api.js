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
    enseignants: () => api.get('/ref/enseignants')
  }
};

window.api = api;
