import type { AuditAction } from "./types";

/** Libellés lisibles des actions du journal d'audit. */
export const AUDIT_LABEL: Record<AuditAction, string> = {
  USER_REGISTERED: "Inscription",
  LOGIN_SUCCEEDED: "Connexion réussie",
  LOGIN_FAILED: "Connexion refusée",
  USER_ROLE_CHANGED: "Rôle modifié",
  USER_DISABLED: "Compte désactivé",
  USER_ENABLED: "Compte réactivé",
  REQUEST_DELETED: "Demande supprimée",
  OFFER_ACCEPTED: "Proposition acceptée",
  OFFER_REJECTED: "Proposition refusée",
  OFFER_WITHDRAWN: "Proposition retirée",
  SESSION_COMPLETED: "Séance terminée",
  RATING_SUBMITTED: "Évaluation envoyée",
  REQUEST_REPORTED: "Publication signalée",
  REQUEST_HIDDEN: "Publication masquée",
  REQUEST_RESTORED: "Publication rétablie",
  REPORTS_DISMISSED: "Signalements rejetés",
  SUBJECT_CREATED: "Matière créée",
  SUBJECT_UPDATED: "Matière modifiée",
  SUBJECT_DELETED: "Matière supprimée",
};

/** Tonalité du badge selon la sensibilité de l'action. */
export function auditTone(action: AuditAction): string {
  if (action === "LOGIN_FAILED" || action === "USER_DISABLED" || action.endsWith("DELETED") || action === "REQUEST_HIDDEN") {
    return "badge-danger";
  }
  if (action === "USER_ROLE_CHANGED" || action === "REQUEST_REPORTED" || action === "REPORTS_DISMISSED") return "badge-warning";
  if (action === "LOGIN_SUCCEEDED" || action === "USER_REGISTERED") return "badge-info";
  return "badge-success";
}
