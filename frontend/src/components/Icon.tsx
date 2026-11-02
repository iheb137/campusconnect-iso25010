/**
 * Icône Ionicons (https://ionic.io/ionicons) rendue en SVG intégré.
 * Les icônes viennent du paquet npm `ionicons` (import { addOutline } from "ionicons/icons").
 * Rendu côté serveur, couleur héritée du texte (currentColor), aucune requête réseau.
 */
type IconProps = {
  icon: string;
  size?: number;
  className?: string;
  /** Texte lu par les lecteurs d'écran. Sans label, l'icône est décorative. */
  label?: string;
};

const cache = new Map<string, string>();

function innerSvg(dataUrl: string): string {
  const cached = cache.get(dataUrl);
  if (cached !== undefined) return cached;
  const raw = dataUrl.startsWith("data:") ? decodeURIComponent(dataUrl.slice(dataUrl.indexOf(",") + 1)) : dataUrl;
  const start = raw.indexOf(">", raw.indexOf("<svg")) + 1;
  const end = raw.lastIndexOf("</svg>");
  const inner = start > 0 && end > start ? raw.slice(start, end) : "";
  cache.set(dataUrl, inner);
  return inner;
}

export default function Icon({ icon, size = 20, className, label }: IconProps) {
  return (
    <svg
      className={`icon${className ? ` ${className}` : ""}`}
      width={size}
      height={size}
      viewBox="0 0 512 512"
      role={label ? "img" : undefined}
      aria-label={label}
      aria-hidden={label ? undefined : true}
      focusable="false"
      dangerouslySetInnerHTML={{ __html: innerSvg(icon) }}
    />
  );
}
