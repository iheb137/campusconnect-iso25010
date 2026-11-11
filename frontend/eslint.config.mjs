import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";
import security from "eslint-plugin-security";

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  // Règles de sécurité (injection, regex dangereuses, eval…) : ISO 25010 — sécurité
  security.configs.recommended,
  {
    rules: {
      // Faux positifs sur les tables indexées par des types énumérés TypeScript
      // (ex. REQUEST_STATUS_LABEL[status]) : la clé est contrôlée par le typage, pas par l'utilisateur.
      "security/detect-object-injection": "off",
    },
  },
  {
    // Tests Playwright : la fonction « use » des fixtures n'est pas un hook React
    files: ["e2e/**/*.ts"],
    rules: { "react-hooks/rules-of-hooks": "off" },
  },
  // Override default ignores of eslint-config-next.
  globalIgnores([
    // Default ignores of eslint-config-next:
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
    "playwright-report/**",
    "test-results/**",
  ]),
]);

export default eslintConfig;
