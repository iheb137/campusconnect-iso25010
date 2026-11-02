import type { Level, OfferStatus, ReportStatus, RequestStatus } from "./types";

export const LEVELS: Level[] = ["L1", "L2", "L3", "M1", "M2"];

export const REQUEST_STATUS_LABEL: Record<RequestStatus, string> = {
  OUVERTE: "Ouverte",
  AIDE_ACCEPTEE: "Aide acceptée",
  TERMINEE: "Terminée",
};

export const OFFER_STATUS_LABEL: Record<OfferStatus, string> = {
  EN_ATTENTE: "En attente",
  ACCEPTEE: "Acceptée",
  REFUSEE: "Refusée",
};

export const REPORT_STATUS_LABEL: Record<ReportStatus, string> = {
  EN_ATTENTE: "En attente",
  TRAITE: "Traité",
  REJETE: "Rejeté",
};

const dateFormatter = new Intl.DateTimeFormat("fr-FR", { dateStyle: "medium" });
const dateTimeFormatter = new Intl.DateTimeFormat("fr-FR", { dateStyle: "medium", timeStyle: "short" });

export function formatDate(value: string | null | undefined): string {
  return value ? dateFormatter.format(new Date(value)) : "—";
}

export function formatDateTime(value: string | null | undefined): string {
  return value ? dateTimeFormatter.format(new Date(value)) : "—";
}
