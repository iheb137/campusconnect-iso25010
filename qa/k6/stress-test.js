/**
 * Test de stress (informatif) : montée progressive jusqu'à 100 utilisateurs pour situer la limite.
 * Les seuils ne bloquent pas : on observe à partir de quelle charge le p95 dépasse 500 ms.
 */
import loadTest, { setup as baseSetup } from "./load-test.js";

export const options = {
  setupTimeout: "120s",
  scenarios: {
    montee: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "1m", target: 20 },
        { duration: "1m", target: 50 },
        { duration: "1m", target: 100 },
        { duration: "30s", target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_duration: [{ threshold: "p(95)<500", abortOnFail: false }],
    http_req_failed: [{ threshold: "rate<0.05", abortOnFail: false }],
  },
  summaryTrendStats: ["avg", "med", "p(90)", "p(95)", "p(99)", "max"],
};

export function setup() {
  return baseSetup();
}

export default loadTest;
