"""Génère la collection Postman (source unique, lisible) : python build_collection.py"""
import json, uuid

items = []
def req(name, method, path, token=None, body=None, tests=(), prereq=None):
    headers = [{"key": "Content-Type", "value": "application/json"}] if body is not None else []
    if token:
        headers.append({"key": "Authorization", "value": "Bearer {{" + token + "}}"})
    item = {
        "name": name,
        "request": {
            "method": method,
            "header": headers,
            "url": {"raw": "{{baseUrl}}" + path, "host": ["{{baseUrl}}"], "path": [p for p in path.split("?")[0].split("/") if p],
                    **({"query": [{"key": kv.split("=")[0], "value": kv.split("=", 1)[1]} for kv in path.split("?")[1].split("&")]} if "?" in path else {})},
        },
        "event": [{"listen": "test", "script": {"type": "text/javascript", "exec": list(tests)}}],
    }
    if body is not None:
        item["request"]["body"] = {"mode": "raw", "raw": json.dumps(body, ensure_ascii=False)}
    if prereq:
        item["event"].append({"listen": "prerequest", "script": {"type": "text/javascript", "exec": prereq}})
    return item

def status(code, label=None):
    return f'pm.test("{label or "Statut " + str(code)}", () => pm.response.to.have.status({code}));'

def fast(ms=500):
    return f'pm.test("Temps de réponse < {ms} ms", () => pm.expect(pm.response.responseTime).to.be.below({ms}));'

def folder(name, children):
    return {"name": name, "item": children}

PWD = "MotDePasse123"

items.append(folder("00 - Disponibilité", [
    req("Santé de l'API et de la base", "GET", "/api/health", tests=[status(200), fast(),
        'pm.test("Base de données joignable", () => pm.expect(pm.response.json().database).to.eql("up"));']),
]))

items.append(folder("01 - Comptes et authentification (RG-08, RG-10, RG-13)", [
    req("Inscription de l'auteur", "POST", "/api/auth/register",
        body={"email": "{{authorEmail}}", "password": PWD, "fullName": "Auteur Newman"},
        prereq=['const id = Date.now().toString(36) + Math.random().toString(36).slice(2, 6);',
                'pm.collectionVariables.set("runId", id);',
                'pm.collectionVariables.set("authorEmail", `auteur.${id}@newman.campusconnect.local`);',
                'pm.collectionVariables.set("helperEmail", `aidant.${id}@newman.campusconnect.local`);'],
        tests=[status(201), fast(1000),
               'pm.test("Le mot de passe n\'est jamais renvoyé", () => pm.expect(pm.response.text()).not.to.match(/password/i));',
               'pm.test("Rôle étudiant par défaut", () => pm.expect(pm.response.json().role).to.eql("STUDENT"));']),
    req("Inscription en double (email en majuscules) → 409", "POST", "/api/auth/register",
        body={"email": "{{authorEmail}}", "password": PWD, "fullName": "Doublon"},
        prereq=['pm.request.body.raw = pm.request.body.raw.replace(pm.collectionVariables.get("authorEmail"), pm.collectionVariables.get("authorEmail").toUpperCase());'],
        tests=[status(409)]),
    req("Inscription invalide → 400 avec erreurs par champ", "POST", "/api/auth/register",
        body={"email": "pas-un-email", "password": "123", "fullName": ""},
        tests=[status(400), 'pm.test("Erreurs par champ", () => pm.expect(Object.keys(pm.response.json().fieldErrors)).to.include.members(["email", "password", "fullName"]));']),
    req("Inscription de l'aidant", "POST", "/api/auth/register",
        body={"email": "{{helperEmail}}", "password": PWD, "fullName": "Aidant Newman"}, tests=[status(201)]),
    req("Connexion de l'auteur", "POST", "/api/auth/login", body={"email": "{{authorEmail}}", "password": PWD},
        tests=[status(200), fast(1000), 'pm.collectionVariables.set("authorToken", pm.response.json().token);',
               'pm.test("Jeton Bearer", () => pm.expect(pm.response.json().tokenType).to.eql("Bearer"));']),
    req("Connexion de l'aidant", "POST", "/api/auth/login", body={"email": "{{helperEmail}}", "password": PWD},
        tests=[status(200), 'pm.collectionVariables.set("helperToken", pm.response.json().token);']),
    req("Mauvais mot de passe → 401 générique", "POST", "/api/auth/login", body={"email": "{{authorEmail}}", "password": "faux"},
        tests=[status(401), 'pm.test("Message générique", () => pm.expect(pm.response.json().message).to.eql("Email ou mot de passe incorrect"));']),
    req("Connexion administrateur", "POST", "/api/auth/login", body={"email": "{{adminEmail}}", "password": "{{adminPassword}}"},
        tests=[status(200), 'pm.collectionVariables.set("adminToken", pm.response.json().token);',
               'pm.test("Rôle ADMIN", () => pm.expect(pm.response.json().user.role).to.eql("ADMIN"));']),
    req("Connexion modérateur", "POST", "/api/auth/login", body={"email": "{{moderatorEmail}}", "password": "{{moderatorPassword}}"},
        tests=[status(200), 'pm.collectionVariables.set("moderatorToken", pm.response.json().token);',
               'pm.test("Rôle MODERATOR", () => pm.expect(pm.response.json().user.role).to.eql("MODERATOR"));']),
    req("Profil sans jeton → 401", "GET", "/api/users/me", tests=[status(401)]),
    req("Profil de l'auteur", "GET", "/api/users/me", token="authorToken",
        tests=[status(200), fast(), 'pm.collectionVariables.set("authorId", pm.response.json().id);']),
]))

items.append(folder("02 - Demandes : CRUD, recherche, filtres", [
    req("Liste des matières", "GET", "/api/subjects", token="authorToken",
        tests=[status(200), fast(), 'const s = pm.response.json(); pm.test("Au moins 2 matières", () => pm.expect(s.length).to.be.at.least(2));',
               'pm.collectionVariables.set("subjectId", s[0].id); pm.collectionVariables.set("otherSubjectId", s[1].id);']),
    req("Publier une demande", "POST", "/api/requests", token="authorToken",
        body={"title": "Newman {{runId}} arbres AVL", "description": "Rotations doubles lors de l'insertion.", "availability": "Mardi soir", "subjectId": "{{subjectId}}", "level": "L2"},
        prereq=['pm.request.body.raw = pm.request.body.raw.replace(\'"{{subjectId}}"\', pm.collectionVariables.get("subjectId"));'],
        tests=[status(201), fast(), 'const r = pm.response.json(); pm.collectionVariables.set("requestId", r.id);',
               'pm.test("Statut initial OUVERTE", () => pm.expect(r.status).to.eql("OUVERTE"));']),
    req("Publier une demande invalide → 400", "POST", "/api/requests", token="authorToken",
        body={"title": "x", "description": "", "availability": "", "subjectId": 999999, "level": "L9"}, tests=[status(400)]),
    req("Un administrateur ne publie pas → 403", "POST", "/api/requests", token="adminToken",
        body={"title": "Demande admin", "description": "Description suffisante.", "availability": "Jamais", "subjectId": 1, "level": "L2"}, tests=[status(403)]),
    req("Lire la demande", "GET", "/api/requests/{{requestId}}", token="helperToken", tests=[status(200), fast()]),
    req("Recherche plein texte (insensible à la casse)", "GET", "/api/requests?q=NEWMAN%20{{runId}}&size=10", token="helperToken",
        tests=[status(200), fast(), 'pm.test("Exactement 1 résultat", () => pm.expect(pm.response.json().totalElements).to.eql(1));']),
    req("Recherche + filtre matière (autre matière) → 0", "GET", "/api/requests?q={{runId}}&subjectId={{otherSubjectId}}", token="helperToken",
        tests=[status(200), 'pm.test("Aucun résultat", () => pm.expect(pm.response.json().totalElements).to.eql(0));']),
    req("Pagination plafonnée à 50", "GET", "/api/requests?size=500", token="helperToken",
        tests=[status(200), fast(), 'pm.test("Taille de page ≤ 50", () => pm.expect(pm.response.json().size).to.be.at.most(50));']),
    req("Filtre invalide → 400", "GET", "/api/requests?level=Z9", token="helperToken", tests=[status(400)]),
    req("Modifier par un autre étudiant → 403", "PUT", "/api/requests/{{requestId}}", token="helperToken",
        body={"title": "Piratage", "description": "Tentative de modification.", "availability": "Jamais", "subjectId": 1, "level": "L2"}, tests=[status(403)]),
]))

items.append(folder("03 - Workflow ouverte → aide acceptée → terminée (RG-01 à RG-05)", [
    req("L'auteur ne propose pas sur sa demande → 403", "POST", "/api/requests/{{requestId}}/offers", token="authorToken",
        body={"message": "Je m'aide moi-même"}, tests=[status(403)]),
    req("Proposition de l'aidant", "POST", "/api/requests/{{requestId}}/offers", token="helperToken",
        body={"message": "Je peux t'expliquer avec des schémas."},
        tests=[status(201), 'pm.collectionVariables.set("offerId", pm.response.json().id);',
               'pm.test("EN_ATTENTE", () => pm.expect(pm.response.json().status).to.eql("EN_ATTENTE"));']),
    req("Seconde proposition du même aidant → 409", "POST", "/api/requests/{{requestId}}/offers", token="helperToken",
        body={"message": "Encore moi"}, tests=[status(409)]),
    req("Seul l'auteur voit les propositions (aidant → 403)", "GET", "/api/requests/{{requestId}}/offers", token="helperToken", tests=[status(403)]),
    req("Clôturer avant acceptation → 409", "POST", "/api/requests/{{requestId}}/complete", token="authorToken", tests=[status(409)]),
    req("L'aidant ne peut pas accepter → 403", "POST", "/api/offers/{{offerId}}/accept", token="helperToken", tests=[status(403)]),
    req("Accepter la proposition", "POST", "/api/offers/{{offerId}}/accept", token="authorToken",
        tests=[status(200), 'pm.test("ACCEPTEE", () => pm.expect(pm.response.json().status).to.eql("ACCEPTEE"));']),
    req("La demande est passée en AIDE_ACCEPTEE", "GET", "/api/requests/{{requestId}}", token="authorToken",
        tests=[status(200), 'pm.test("AIDE_ACCEPTEE", () => pm.expect(pm.response.json().status).to.eql("AIDE_ACCEPTEE"));']),
    req("Retirer une proposition acceptée → 409", "DELETE", "/api/offers/{{offerId}}", token="helperToken", tests=[status(409)]),
    req("Évaluer avant clôture → 409", "POST", "/api/requests/{{requestId}}/ratings", token="authorToken", body={"score": 5}, tests=[status(409)]),
    req("Clôturer la séance", "POST", "/api/requests/{{requestId}}/complete", token="authorToken",
        tests=[status(200), 'pm.test("TERMINEE", () => pm.expect(pm.response.json().status).to.eql("TERMINEE"));']),
    req("Note hors limites → 400", "POST", "/api/requests/{{requestId}}/ratings", token="authorToken", body={"score": 7}, tests=[status(400)]),
    req("Évaluation par l'auteur", "POST", "/api/requests/{{requestId}}/ratings", token="authorToken",
        body={"score": 5, "comment": "Très clair"}, tests=[status(201)]),
    req("Double évaluation → 409", "POST", "/api/requests/{{requestId}}/ratings", token="authorToken", body={"score": 4}, tests=[status(409)]),
    req("Évaluation par l'aidant", "POST", "/api/requests/{{requestId}}/ratings", token="helperToken", body={"score": 4}, tests=[status(201)]),
    req("Tableau de bord de l'auteur", "GET", "/api/dashboard", token="authorToken", tests=[status(200), fast()]),
    req("Supprimer une demande terminée → 409", "DELETE", "/api/requests/{{requestId}}", token="authorToken", tests=[status(409)]),
]))

items.append(folder("04 - Modération et administration (RG-06, RG-07, RG-09, RG-12)", [
    req("Seconde demande à signaler", "POST", "/api/requests", token="authorToken",
        body={"title": "Newman {{runId}} à signaler", "description": "Contenu à modérer pour le test.", "availability": "Lundi", "subjectId": "{{subjectId}}", "level": "L1"},
        prereq=['pm.request.body.raw = pm.request.body.raw.replace(\'"{{subjectId}}"\', pm.collectionVariables.get("subjectId"));'],
        tests=[status(201), 'pm.collectionVariables.set("reportedId", pm.response.json().id);']),
    req("Signaler sa propre demande → 403", "POST", "/api/requests/{{reportedId}}/reports", token="authorToken", body={"reason": "Je me signale"}, tests=[status(403)]),
    req("Signalement par l'aidant", "POST", "/api/requests/{{reportedId}}/reports", token="helperToken", body={"reason": "Hors sujet {{runId}}"}, tests=[status(201)]),
    req("Étudiant → espace modération 403", "GET", "/api/admin/reports", token="helperToken", tests=[status(403)]),
    req("Modérateur → liste des signalements", "GET", "/api/admin/reports", token="moderatorToken", tests=[status(200), fast()]),
    req("Modérateur → espace administrateur 403", "GET", "/api/admin/users", token="moderatorToken", tests=[status(403)]),
    req("Modérateur masque la publication", "POST", "/api/admin/requests/{{reportedId}}/hide", token="moderatorToken", tests=[status(200)]),
    req("Publication masquée → 404 pour les étudiants", "GET", "/api/requests/{{reportedId}}", token="helperToken", tests=[status(404)]),
    req("Recherche de comptes (admin)", "GET", "/api/admin/users?q={{runId}}&size=10", token="adminToken",
        tests=[status(200), fast(), 'pm.test("2 comptes du run", () => pm.expect(pm.response.json().totalElements).to.eql(2));']),
    req("Statistiques (admin)", "GET", "/api/admin/stats", token="adminToken",
        tests=[status(200), fast(), 'pm.test("Indicateurs présents", () => pm.expect(pm.response.json()).to.include.keys("requestsByStatus", "usersByRole", "requestsPerDay"));']),
    req("Période de statistiques invalide → 400", "GET", "/api/admin/stats?from=2026-12-01&to=2026-01-01", token="adminToken", tests=[status(400)]),
    req("Journal d'audit filtré (masquage)", "GET", "/api/admin/audit?action=REQUEST_HIDDEN&size=20", token="adminToken",
        tests=[status(200), fast(), 'pm.test("Le masquage est tracé", () => pm.expect(pm.response.json().content.some(e => String(e.targetId) === String(pm.collectionVariables.get("reportedId")))).to.be.true);']),
    req("Journal d'audit (échecs de connexion)", "GET", "/api/admin/audit?action=LOGIN_FAILED&size=50", token="adminToken",
        tests=[status(200), 'pm.test("Échec de connexion tracé avec l\'email", () => pm.expect(pm.response.json().content.some(e => e.actorEmail === pm.collectionVariables.get("authorEmail"))).to.be.true);']),
]))

collection = {
    "info": {"_postman_id": str(uuid.uuid5(uuid.NAMESPACE_URL, "campusconnect-iso25010")), "name": "CampusConnect — API (ISO 25010)",
             "description": "Tests fonctionnels de l'API REST. Exécution : npx newman run (voir qa/README.md).",
             "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"},
    "item": items,
    "variable": [{"key": k, "value": v} for k, v in [("baseUrl", "http://localhost:8280"), ("adminEmail", "admin@campusconnect.local"),
        ("adminPassword", "Admin12345"), ("moderatorEmail", "moderateur@campusconnect.local"), ("moderatorPassword", "Moderateur123")]],
}
json.dump(collection, open("campusconnect.postman_collection.json", "w", encoding="utf-8"), ensure_ascii=False, indent=2)
print(sum(len(f["item"]) for f in items), "requêtes")
