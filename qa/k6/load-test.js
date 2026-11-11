/**
 * Test de charge (ISO 25010 — efficacité de performance, comportement temporel et capacité).
 * Exigence PERF-01 : avec 20 utilisateurs simultanés, 95 % des requêtes API < 500 ms et < 1 % d'erreurs.
 *
 * Exécution (voir qa/README.md) :
 *   docker run --rm -i --network host -v "$PWD/qa:/qa" -e BASE_URL=http://localhost:8380 \
 *     -e K6_WEB_DASHBOARD=true -e K6_WEB_DASHBOARD_EXPORT=/qa/reports/k6/dashboard.html \
 *     grafana/k6:0.57.0 run --summary-export=/qa/reports/k6/summary.json /qa/k6/load-test.js
 * Variables : BASE_URL (backend), VUS (défaut 20), DURATION (palier, défaut 2m).
 */
import http from "k6/http";
import { check, group, sleep } from "k6";
import { Trend } from "k6/metrics";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8280";
const VUS = Number(__ENV.VUS || 20);
const DURATION = __ENV.DURATION || "2m";
const PASSWORD = "MotDePasse123";

const searchDuration = new Trend("cc_search_duration", true);
const detailDuration = new Trend("cc_detail_duration", true);

export const options = {
  scenarios: {
    etudiants: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "30s", target: VUS },
        { duration: DURATION, target: VUS },
        { duration: "15s", target: 0 },
      ],
      gracefulRampDown: "10s",
    },
  },
  thresholds: {
    http_req_duration: ["p(95)<500"],
    http_req_failed: ["rate<0.01"],
    checks: ["rate>0.99"],
    cc_search_duration: ["p(95)<500"],
    cc_detail_duration: ["p(95)<500"],
  },
  summaryTrendStats: ["avg", "min", "med", "p(90)", "p(95)", "p(99)", "max"],
};

const json = { headers: { "Content-Type": "application/json" } };

/** Crée un compte étudiant par utilisateur virtuel (hors mesure). */
export function setup() {
  const health = http.get(`${BASE_URL}/api/health`);
  if (health.status !== 200) throw new Error(`Backend indisponible sur ${BASE_URL} (${health.status})`);
  const run = Date.now().toString(36);
  const tokens = [];
  for (let i = 0; i < VUS; i++) {
    const email = `k6.${run}.${i}@charge.campusconnect.local`;
    http.post(`${BASE_URL}/api/auth/register`, JSON.stringify({ email, password: PASSWORD, fullName: `Charge ${i}` }), json);
    const login = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({ email, password: PASSWORD }), json);
    tokens.push(login.json("token"));
  }
  const subjects = http.get(`${BASE_URL}/api/subjects`, { headers: { Authorization: `Bearer ${tokens[0]}` } }).json();
  return { tokens, subjectIds: subjects.map((s) => s.id) };
}

const WORDS = ["arbre", "sql", "graphe", "java", "réseau", "algo", "exercice", "examen"];
const LEVELS = ["L1", "L2", "L3", "M1", "M2"];
const pick = (list) => list[Math.floor(Math.random() * list.length)];

export default function (data) {
  const auth = { headers: { Authorization: `Bearer ${data.tokens[(__VU - 1) % data.tokens.length]}` } };

  group("parcourir les demandes", () => {
    const page = Math.floor(Math.random() * 5);
    const list = http.get(`${BASE_URL}/api/requests?page=${page}&size=10`, auth);
    check(list, { "liste 200": (r) => r.status === 200 });

    const filtered = http.get(`${BASE_URL}/api/requests?subjectId=${pick(data.subjectIds)}&level=${pick(LEVELS)}&status=OUVERTE`, auth);
    check(filtered, { "filtres 200": (r) => r.status === 200 });

    const search = http.get(`${BASE_URL}/api/requests?q=${encodeURIComponent(pick(WORDS))}&size=10`, auth);
    searchDuration.add(search.timings.duration);
    check(search, { "recherche 200": (r) => r.status === 200 });

    const content = list.json("content") || [];
    if (content.length > 0) {
      const detail = http.get(`${BASE_URL}/api/requests/${pick(content).id}`, auth);
      detailDuration.add(detail.timings.duration);
      check(detail, { "détail 200": (r) => r.status === 200 });
    }
  });

  group("espace personnel", () => {
    check(http.get(`${BASE_URL}/api/dashboard`, auth), { "tableau de bord 200": (r) => r.status === 200 });
    check(http.get(`${BASE_URL}/api/subjects`, auth), { "matières 200": (r) => r.status === 200 });
  });

  // 1 itération sur 10 publie une demande (écriture en base)
  if (Math.random() < 0.1) {
    const created = http.post(
      `${BASE_URL}/api/requests`,
      JSON.stringify({
        title: `Charge ${pick(WORDS)} ${Date.now()}`,
        description: "Demande créée par le test de charge k6.",
        availability: "Soir",
        subjectId: pick(data.subjectIds),
        level: pick(LEVELS),
      }),
      { headers: { ...auth.headers, "Content-Type": "application/json" } },
    );
    check(created, { "publication 201": (r) => r.status === 201 });
  }

  sleep(1 + Math.random()); // temps de réflexion d'un utilisateur
}
