const PALETTE = ["#4f5bd5", "#0f9d8a", "#d9480f", "#7048e8", "#1c7ed6", "#c2255c", "#2b8a3e", "#e67700"];

function colorFor(name: string) {
  let hash = 0;
  for (const char of name) hash = (hash * 31 + char.charCodeAt(0)) >>> 0;
  return PALETTE[hash % PALETTE.length];
}

export function initials(name: string) {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  const letters = parts.length > 1 ? parts[0][0] + parts[parts.length - 1][0] : (parts[0] ?? "?").slice(0, 2);
  return letters.toUpperCase();
}

/** Pastille aux initiales, couleur stable dérivée du nom. */
export default function Avatar({ name, size = 36 }: { name: string; size?: number }) {
  return (
    <span
      className="avatar"
      style={{ width: size, height: size, fontSize: size * 0.38, background: colorFor(name) }}
      aria-hidden="true"
    >
      {initials(name)}
    </span>
  );
}
