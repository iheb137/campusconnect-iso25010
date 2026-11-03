import { chevronBackOutline, chevronForwardOutline } from "ionicons/icons";

import Icon from "./Icon";

/** Pagination simple (pages numérotées à partir de 0 côté API). */
export default function Pagination({
  page,
  totalPages,
  onChange,
}: {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}) {
  if (totalPages <= 1) return null;
  return (
    <nav className="pagination" aria-label="Pagination">
      <button type="button" className="btn btn-sm" disabled={page === 0} onClick={() => onChange(page - 1)}>
        <Icon icon={chevronBackOutline} size={16} />
        Précédent
      </button>
      <span className="small muted">
        Page {page + 1} sur {totalPages}
      </span>
      <button type="button" className="btn btn-sm" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
        Suivant
        <Icon icon={chevronForwardOutline} size={16} />
      </button>
    </nav>
  );
}
