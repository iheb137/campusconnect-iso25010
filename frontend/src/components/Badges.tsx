import {
  banOutline,
  checkmarkCircle,
  checkmarkDoneOutline,
  closeCircle,
  handLeftOutline,
  hourglassOutline,
  peopleOutline,
  radioButtonOnOutline,
  star,
  starOutline,
} from "ionicons/icons";

import { OFFER_STATUS_LABEL, REPORT_STATUS_LABEL, REQUEST_STATUS_LABEL } from "@/lib/format";
import type { OfferStatus, ReportStatus, RequestStatus } from "@/lib/types";

import Icon from "./Icon";

const REQUEST_STYLE: Record<RequestStatus, [string, string]> = {
  OUVERTE: ["badge-info", radioButtonOnOutline],
  AIDE_ACCEPTEE: ["badge-warning", peopleOutline],
  TERMINEE: ["badge-success", checkmarkDoneOutline],
};

const OFFER_STYLE: Record<OfferStatus, [string, string]> = {
  EN_ATTENTE: ["badge-warning", hourglassOutline],
  ACCEPTEE: ["badge-success", checkmarkCircle],
  REFUSEE: ["badge-danger", closeCircle],
};

const REPORT_STYLE: Record<ReportStatus, [string, string]> = {
  EN_ATTENTE: ["badge-warning", handLeftOutline],
  TRAITE: ["badge-success", checkmarkCircle],
  REJETE: ["", banOutline],
};

function Badge({ tone, icon, label }: { tone: string; icon: string; label: string }) {
  return (
    <span className={`badge ${tone}`}>
      <Icon icon={icon} size={14} />
      {label}
    </span>
  );
}

export function RequestStatusBadge({ status }: { status: RequestStatus }) {
  const [tone, icon] = REQUEST_STYLE[status];
  return <Badge tone={tone} icon={icon} label={REQUEST_STATUS_LABEL[status]} />;
}

export function OfferStatusBadge({ status }: { status: OfferStatus }) {
  const [tone, icon] = OFFER_STYLE[status];
  return <Badge tone={tone} icon={icon} label={OFFER_STATUS_LABEL[status]} />;
}

export function ReportStatusBadge({ status }: { status: ReportStatus }) {
  const [tone, icon] = REPORT_STYLE[status];
  return <Badge tone={tone} icon={icon} label={REPORT_STATUS_LABEL[status]} />;
}

export function Stars({ score, max = 5, size = 16 }: { score: number; max?: number; size?: number }) {
  return (
    <span className="stars" role="img" aria-label={`${score} sur ${max}`}>
      {Array.from({ length: max }, (_, index) => (
        <span key={index} className={index < score ? undefined : "off"}>
          <Icon icon={index < score ? star : starOutline} size={size} />
        </span>
      ))}
    </span>
  );
}
