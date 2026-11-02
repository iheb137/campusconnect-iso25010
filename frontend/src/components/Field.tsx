import { alertCircleOutline } from "ionicons/icons";
import { useId } from "react";

import Icon from "./Icon";

type FieldProps = {
  label: string;
  error?: string;
  hint?: string;
  required?: boolean;
  /** Icône Ionicons affichée dans le champ (à gauche). */
  icon?: string;
  children: (props: { id: string; "aria-invalid": boolean; "aria-describedby"?: string }) => React.ReactNode;
};

/** Champ de formulaire accessible : libellé relié, aide et erreur annoncées. */
export default function Field({ label, error, hint, required, icon, children }: FieldProps) {
  const id = useId();
  const hintId = hint ? `${id}-hint` : undefined;
  const errorId = error ? `${id}-error` : undefined;
  const describedBy = [hintId, errorId].filter(Boolean).join(" ") || undefined;
  const control = children({ id, "aria-invalid": Boolean(error), "aria-describedby": describedBy });

  return (
    <div className="field">
      <label htmlFor={id}>
        {label}
        {required ? <span aria-hidden="true"> *</span> : null}
      </label>
      {icon ? (
        <div className="input-wrap">
          <Icon icon={icon} size={18} />
          {control}
        </div>
      ) : (
        control
      )}
      {hint ? (
        <span id={hintId} className="hint">
          {hint}
        </span>
      ) : null}
      {error ? (
        <span id={errorId} className="error">
          <Icon icon={alertCircleOutline} size={16} />
          {error}
        </span>
      ) : null}
    </div>
  );
}
