import { checkmark, checkmarkDoneOutline, megaphoneOutline, peopleOutline } from "ionicons/icons";

import type { RequestStatus } from "@/lib/types";

import Icon from "./Icon";

const STEPS: { status: RequestStatus; title: string; desc: string; icon: string }[] = [
  { status: "OUVERTE", title: "Ouverte", desc: "La demande reçoit des propositions", icon: megaphoneOutline },
  { status: "AIDE_ACCEPTEE", title: "Aide acceptée", desc: "Un aidant a été choisi", icon: peopleOutline },
  { status: "TERMINEE", title: "Terminée", desc: "Séance réalisée, place à l'évaluation", icon: checkmarkDoneOutline },
];

/** Suivi visuel d'une demande : ouverte → aide acceptée → terminée. */
export default function StatusTimeline({ status }: { status: RequestStatus }) {
  const current = STEPS.findIndex((step) => step.status === status);
  return (
    <ol className="stepper" aria-label="Suivi de la demande">
      {STEPS.map((step, index) => {
        const state =
          index < current || status === "TERMINEE" ? "done" : index === current ? "current" : "todo";
        return (
          <li key={step.status} className="step" data-state={state} aria-current={index === current ? "step" : undefined}>
            <span className="step-dot">
              <Icon icon={state === "done" ? checkmark : step.icon} size={18} />
            </span>
            <span className="step-text">
              <span className="step-title">{step.title}</span>
              <span className="visually-hidden">
                {state === "done" ? " (terminé)" : state === "current" ? " (étape actuelle)" : " (à venir)"}
              </span>
              <span className="step-desc" style={{ display: "block" }}>
                {step.desc}
              </span>
            </span>
          </li>
        );
      })}
    </ol>
  );
}
