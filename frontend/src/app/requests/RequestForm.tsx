"use client";

import { useState } from "react";

import { bookOutline, checkmarkOutline, createOutline, schoolOutline, timeOutline } from "ionicons/icons";

import Field from "@/components/Field";
import Icon from "@/components/Icon";
import { Alert, ErrorState, Loading } from "@/components/States";
import { useApiData } from "@/components/useApiData";
import { ApiError, errorMessage } from "@/lib/api";
import { LEVELS } from "@/lib/format";
import type { HelpRequestInput, Level, Subject } from "@/lib/types";

type Props = {
  initial?: HelpRequestInput;
  submitLabel: string;
  onSubmit: (input: HelpRequestInput) => Promise<void>;
  onCancel?: () => void;
};

/** Formulaire partagé par la publication et la modification d'une demande (mêmes règles que le serveur). */
export default function RequestForm({ initial, submitLabel, onSubmit, onCancel }: Props) {
  const subjects = useApiData<Subject[]>("/api/subjects");
  const [title, setTitle] = useState(initial?.title ?? "");
  const [description, setDescription] = useState(initial?.description ?? "");
  const [availability, setAvailability] = useState(initial?.availability ?? "");
  const [subjectId, setSubjectId] = useState(initial ? String(initial.subjectId) : "");
  const [level, setLevel] = useState<Level | "">(initial?.level ?? "");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (subjects.loading) return <Loading label="Chargement des matières…" />;
  if (subjects.error || !subjects.data)
    return <ErrorState message={subjects.error ?? "Matières indisponibles"} onRetry={subjects.reload} />;

  function validate() {
    const found: Record<string, string> = {};
    const t = title.trim();
    const d = description.trim();
    const a = availability.trim();
    if (t.length < 5 || t.length > 100) found.title = "Le titre doit contenir entre 5 et 100 caractères";
    if (d.length < 10 || d.length > 2000) found.description = "La description doit contenir entre 10 et 2000 caractères";
    if (!a) found.availability = "La disponibilité est obligatoire";
    else if (a.length > 255) found.availability = "La disponibilité ne doit pas dépasser 255 caractères";
    if (!subjectId) found.subjectId = "La matière est obligatoire";
    if (!level) found.level = "Le niveau est obligatoire";
    return found;
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const found = validate();
    setErrors(found);
    setFormError(null);
    if (Object.keys(found).length > 0) return;

    setSubmitting(true);
    try {
      await onSubmit({
        title: title.trim(),
        description: description.trim(),
        availability: availability.trim(),
        subjectId: Number(subjectId),
        level: level as Level,
      });
    } catch (error) {
      setFormError(errorMessage(error));
      if (error instanceof ApiError) setErrors(error.fieldErrors);
      setSubmitting(false);
    }
  }

  return (
    <form className="card form fade-in" onSubmit={handleSubmit} noValidate style={{ maxWidth: 820 }}>
      {formError ? <Alert kind="error">{formError}</Alert> : null}
      <Field label="Titre" error={errors.title} hint="Entre 5 et 100 caractères" icon={createOutline} required>
        {(props) => (
          <input
            {...props}
            className="input"
            maxLength={100}
            placeholder="Exemple : Aide sur les arbres AVL"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
          />
        )}
      </Field>
      <Field
        label="Description"
        error={errors.description}
        hint={`Ce que vous ne comprenez pas, ce que vous avez déjà essayé (${description.trim().length}/2000).`}
        required
      >
        {(props) => (
          <textarea
            {...props}
            className="textarea"
            maxLength={2000}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
        )}
      </Field>
      <div className="form-row">
        <Field label="Matière" error={errors.subjectId} icon={bookOutline} required>
          {(props) => (
            <select {...props} className="select" value={subjectId} onChange={(e) => setSubjectId(e.target.value)}>
              <option value="">Choisir…</option>
              {subjects.data!.map((subject) => (
                <option key={subject.id} value={subject.id}>
                  {subject.name}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Niveau" error={errors.level} icon={schoolOutline} required>
          {(props) => (
            <select {...props} className="select" value={level} onChange={(e) => setLevel(e.target.value as Level | "")}>
              <option value="">Choisir…</option>
              {LEVELS.map((value) => (
                <option key={value} value={value}>
                  {value}
                </option>
              ))}
            </select>
          )}
        </Field>
      </div>
      <Field
        label="Disponibilité"
        error={errors.availability}
        hint="Exemple : mardi et jeudi après 17 h, en ligne"
        icon={timeOutline}
        required
      >
        {(props) => (
          <input
            {...props}
            className="input"
            maxLength={255}
            value={availability}
            onChange={(e) => setAvailability(e.target.value)}
          />
        )}
      </Field>
      <div className="form-actions">
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          <Icon icon={checkmarkOutline} size={18} />
          {submitting ? "Enregistrement…" : submitLabel}
        </button>
        {onCancel ? (
          <button type="button" className="btn" onClick={onCancel} disabled={submitting}>
            Annuler
          </button>
        ) : null}
      </div>
    </form>
  );
}
