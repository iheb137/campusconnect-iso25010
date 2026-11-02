import Link from "next/link";
import {
  alertCircleOutline,
  checkmarkCircleOutline,
  cloudOfflineOutline,
  fileTrayOutline,
  informationCircleOutline,
  refreshOutline,
} from "ionicons/icons";

import Icon from "./Icon";

export function Loading({ label = "Chargement…" }: { label?: string }) {
  return (
    <div className="loading" role="status" aria-live="polite">
      <span className="spinner" aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}

export function SkeletonList({ count = 3 }: { count?: number }) {
  return (
    <div className="list" role="status" aria-label="Chargement">
      {Array.from({ length: count }, (_, index) => (
        <div key={index} className="skeleton" />
      ))}
    </div>
  );
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="state" role="alert">
      <span className="state-icon tone-danger">
        <Icon icon={cloudOfflineOutline} size={30} />
      </span>
      <strong>Une erreur est survenue</strong>
      <span>{message}</span>
      {onRetry ? (
        <button type="button" className="btn" onClick={onRetry}>
          <Icon icon={refreshOutline} size={18} />
          Réessayer
        </button>
      ) : null}
    </div>
  );
}

export function EmptyState({
  title,
  text,
  icon = fileTrayOutline,
  action,
}: {
  title: string;
  text?: string;
  icon?: string;
  action?: { href: string; label: string; icon?: string };
}) {
  return (
    <div className="state">
      <span className="state-icon">
        <Icon icon={icon} size={30} />
      </span>
      <strong>{title}</strong>
      {text ? <span>{text}</span> : null}
      {action ? (
        <Link href={action.href} className="btn btn-primary">
          {action.icon ? <Icon icon={action.icon} size={18} /> : null}
          {action.label}
        </Link>
      ) : null}
    </div>
  );
}

const ALERT_ICON = {
  error: alertCircleOutline,
  success: checkmarkCircleOutline,
  info: informationCircleOutline,
};

export function Alert({ kind, children }: { kind: "error" | "success" | "info"; children: React.ReactNode }) {
  return (
    <div className={`alert alert-${kind} fade-in`} role={kind === "error" ? "alert" : "status"}>
      <Icon icon={ALERT_ICON[kind]} size={20} />
      <div>{children}</div>
    </div>
  );
}
