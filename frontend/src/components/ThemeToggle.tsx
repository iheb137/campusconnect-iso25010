"use client";

import { moonOutline, sunnyOutline } from "ionicons/icons";
import { useSyncExternalStore } from "react";

import Icon from "./Icon";

type Theme = "light" | "dark";

const STORAGE_KEY = "cc-theme";
const EVENT = "cc-theme-change";
const DARK_QUERY = "(prefers-color-scheme: dark)";

/** Thème affiché : choix enregistré de l'utilisateur, sinon celui du système. */
function currentTheme(): Theme {
  const chosen = document.documentElement.dataset.theme;
  if (chosen === "light" || chosen === "dark") return chosen;
  return window.matchMedia(DARK_QUERY).matches ? "dark" : "light";
}

function subscribe(onChange: () => void) {
  const media = window.matchMedia(DARK_QUERY);
  media.addEventListener("change", onChange);
  window.addEventListener(EVENT, onChange);
  return () => {
    media.removeEventListener("change", onChange);
    window.removeEventListener(EVENT, onChange);
  };
}

export default function ThemeToggle() {
  // null côté serveur : le thème n'est connu que dans le navigateur.
  const theme = useSyncExternalStore<Theme | null>(subscribe, currentTheme, () => null);

  function toggle() {
    const next: Theme = currentTheme() === "dark" ? "light" : "dark";
    document.documentElement.dataset.theme = next;
    try {
      localStorage.setItem(STORAGE_KEY, next);
    } catch {
      // Stockage indisponible (navigation privée) : le choix vaut pour cette page seulement.
    }
    window.dispatchEvent(new Event(EVENT));
  }

  const label = theme === "dark" ? "Passer en mode clair" : "Passer en mode sombre";
  return (
    <button type="button" className="icon-btn" onClick={toggle} aria-label={label} title={label}>
      <Icon icon={theme === "dark" ? sunnyOutline : moonOutline} size={20} />
    </button>
  );
}
