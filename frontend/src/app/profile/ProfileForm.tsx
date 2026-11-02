"use client";

import { useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";

import { useAuth } from "@/components/AuthProvider";
import { checkmark, mailOutline, personOutline, ribbonOutline, saveOutline, schoolOutline } from "ionicons/icons";

import Avatar from "@/components/Avatar";
import Field from "@/components/Field";
import Icon from "@/components/Icon";
import { subjectIcon } from "@/lib/icons";
import { Alert, ErrorState, Loading } from "@/components/States";
import { useApiData } from "@/components/useApiData";
import { api, ApiError, errorMessage } from "@/lib/api";
import { LEVELS } from "@/lib/format";
import type { Level, Subject, User } from "@/lib/types";

const MAX_SKILLS = 20;

export default function ProfileForm() {
  const searchParams = useSearchParams();
  const { setUser } = useAuth();
  const me = useApiData<User>("/api/users/me");
  const subjects = useApiData<Subject[]>("/api/subjects");

  const [fullName, setFullName] = useState("");
  const [specialty, setSpecialty] = useState("");
  const [level, setLevel] = useState<Level | "">("");
  const [skillIds, setSkillIds] = useState<number[]>([]);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [message, setMessage] = useState<{ kind: "error" | "success"; text: string } | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!me.data) return;
    // Remplit le formulaire avec le profil chargé.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setFullName(me.data.fullName);
    setSpecialty(me.data.specialty ?? "");
    setLevel(me.data.level ?? "");
    setSkillIds(me.data.skills.map((skill) => skill.id));
  }, [me.data]);

  if (me.loading || subjects.loading) return <Loading label="Chargement du profil…" />;
  if (me.error || !me.data) return <ErrorState message={me.error ?? "Profil introuvable"} onRetry={me.reload} />;
  if (subjects.error || !subjects.data)
    return <ErrorState message={subjects.error ?? "Matières indisponibles"} onRetry={subjects.reload} />;

  const isAdmin = me.data.role === "ADMIN";

  function toggleSkill(id: number) {
    setSkillIds((current) => (current.includes(id) ? current.filter((value) => value !== id) : [...current, id]));
  }

  async function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const found: Record<string, string> = {};
    const name = fullName.trim();
    if (name.length < 2 || name.length > 150) found.fullName = "Le nom doit contenir entre 2 et 150 caractères";
    if (specialty.trim().length > 150) found.specialty = "La spécialité ne doit pas dépasser 150 caractères";
    if (skillIds.length > MAX_SKILLS) found.skillIds = `${MAX_SKILLS} compétences maximum`;
    setErrors(found);
    setMessage(null);
    if (Object.keys(found).length > 0) return;

    setSubmitting(true);
    try {
      const updated = await api<User>("/api/users/me", {
        method: "PUT",
        body: {
          fullName: name,
          specialty: specialty.trim() || null,
          level: level || null,
          skillIds,
        },
      });
      me.setData(updated);
      setUser(updated);
      setMessage({ kind: "success", text: "Profil enregistré." });
    } catch (error) {
      setMessage({ kind: "error", text: errorMessage(error) });
      if (error instanceof ApiError) setErrors(error.fieldErrors);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="grid-2">
    <form className="card form fade-in" onSubmit={onSubmit} noValidate>
      {searchParams.get("welcome") ? (
        <Alert kind="info">Bienvenue ! Complétez votre profil pour commencer.</Alert>
      ) : null}
      {message ? <Alert kind={message.kind}>{message.text}</Alert> : null}

      <Field label="Nom complet" error={errors.fullName} icon={personOutline} required>
        {(props) => (
          <input {...props} className="input" value={fullName} onChange={(event) => setFullName(event.target.value)} />
        )}
      </Field>

      {isAdmin ? null : (
        <>
          <div className="form-row">
            <Field label="Spécialité" error={errors.specialty} hint="Exemple : Génie logiciel" icon={ribbonOutline}>
              {(props) => (
                <input
                  {...props}
                  className="input"
                  value={specialty}
                  onChange={(event) => setSpecialty(event.target.value)}
                />
              )}
            </Field>
            <Field label="Niveau" error={errors.level} icon={schoolOutline}>
              {(props) => (
                <select
                  {...props}
                  className="select"
                  value={level}
                  onChange={(event) => setLevel(event.target.value as Level | "")}
                >
                  <option value="">Non précisé</option>
                  {LEVELS.map((value) => (
                    <option key={value} value={value}>
                      {value}
                    </option>
                  ))}
                </select>
              )}
            </Field>
          </div>

          <fieldset className="field" aria-describedby="skills-hint">
            <legend>Compétences</legend>
            <span id="skills-hint" className="hint">
              Les matières dans lesquelles vous pouvez aider ({skillIds.length}/{MAX_SKILLS}).
            </span>
            <div className="checkbox-grid">
              {subjects.data.map((subject) => (
                <label key={subject.id} className="checkbox">
                  <input
                    type="checkbox"
                    checked={skillIds.includes(subject.id)}
                    onChange={() => toggleSkill(subject.id)}
                  />
                  <span className="check" aria-hidden="true">
                    <Icon icon={checkmark} size={14} />
                  </span>
                  <Icon icon={subjectIcon(subject.name)} size={18} />
                  {subject.name}
                </label>
              ))}
            </div>
            {errors.skillIds ? <span className="error">{errors.skillIds}</span> : null}
          </fieldset>
        </>
      )}

      <div className="form-actions">
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          <Icon icon={saveOutline} size={18} />
          {submitting ? "Enregistrement…" : "Enregistrer"}
        </button>
      </div>
    </form>

    <aside className="card fade-in" aria-label="Aperçu du profil">
      <div className="stack" style={{ alignItems: "center", textAlign: "center" }}>
        <Avatar name={fullName.trim() || me.data.fullName} size={84} />
        <div>
          <h2>{fullName.trim() || me.data.fullName}</h2>
          <p className="muted small" style={{ display: "inline-flex", alignItems: "center", gap: 6 }}>
            <Icon icon={mailOutline} size={15} />
            {me.data.email}
          </p>
        </div>
        <div className="row" style={{ justifyContent: "center" }}>
          <span className="chip">{isAdmin ? "Administrateur" : "Étudiant"}</span>
          {level ? <span className="chip chip-neutral">Niveau {level}</span> : null}
          {specialty.trim() ? <span className="chip chip-neutral">{specialty.trim()}</span> : null}
        </div>
      </div>
      {isAdmin ? null : (
        <>
          <hr style={{ border: 0, borderTop: "1px solid var(--border)", margin: "18px 0" }} />
          <h3 className="small muted" style={{ marginBottom: 10 }}>
            Compétences ({skillIds.length})
          </h3>
          {skillIds.length === 0 ? (
            <p className="small muted">Aucune compétence sélectionnée.</p>
          ) : (
            <div className="row">
              {subjects.data
                .filter((subject) => skillIds.includes(subject.id))
                .map((subject) => (
                  <span key={subject.id} className="chip">
                    <Icon icon={subjectIcon(subject.name)} size={14} />
                    {subject.name}
                  </span>
                ))}
            </div>
          )}
        </>
      )}
    </aside>
    </div>
  );
}
