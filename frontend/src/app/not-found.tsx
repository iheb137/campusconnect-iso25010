import { compassOutline, homeOutline } from "ionicons/icons";

import { EmptyState } from "@/components/States";

export default function NotFound() {
  return (
    <div className="card">
      <EmptyState
        icon={compassOutline}
        title="Page introuvable"
        text="Cette page n'existe pas ou a été déplacée."
        action={{ href: "/", label: "Retour à l'accueil", icon: homeOutline }}
      />
    </div>
  );
}
