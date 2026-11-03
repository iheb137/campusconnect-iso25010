"use client";

import { addOutline, bookOutline, checkmarkOutline, closeOutline, createOutline, trashOutline } from "ionicons/icons";
import { useState } from "react";

import Field from "@/components/Field";
import Icon from "@/components/Icon";
import PageHead from "@/components/PageHead";
import { Alert, EmptyState, ErrorState, SkeletonList } from "@/components/States";
import { useApiData } from "@/components/useApiData";
import { api, ApiError, errorMessage } from "@/lib/api";
import { subjectIcon } from "@/lib/icons";
import type { Subject } from "@/lib/types";

function validName(value: string) {
  const name = value.trim();
  return name.length >= 2 && name.length <= 100;
}

export default function SubjectsAdmin() {
  const subjects = useApiData<Subject[]>("/api/subjects");
  const [name, setName] = useState("");
  const [nameError, setNameError] = useState<string | undefined>();
  const [editing, setEditing] = useState<{ id: number; name: string } | null>(null);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<{ kind: "error" | "success"; text: string } | null>(null);

  async function run(action: () => Promise<void>, success: string) {
    setBusy(true);
    setNotice(null);
    try {
      await action();
      subjects.reload();
      setNotice({ kind: "success", text: success });
      return true;
    } catch (error) {
      setNotice({ kind: "error", text: errorMessage(error) });
      return false;
    } finally {
      setBusy(false);
    }
  }

  async function create(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!validName(name)) {
      setNameError("Le nom doit contenir entre 2 et 100 caractères");
      return;
    }
    setNameError(undefined);
    const ok = await run(async () => {
      try {
        await api("/api/admin/subjects", { method: "POST", body: { name: name.trim() } });
      } catch (error) {
        if (error instanceof ApiError) setNameError(error.fieldErrors.name);
        throw error;
      }
    }, `Matière « ${name.trim()} » ajoutée.`);
    if (ok) setName("");
  }

  async function rename(subject: Subject) {
    if (!editing || !validName(editing.name)) {
      setNotice({ kind: "error", text: "Le nom doit contenir entre 2 et 100 caractères" });
      return;
    }
    const ok = await run(
      () => api(`/api/admin/subjects/${subject.id}`, { method: "PUT", body: { name: editing.name.trim() } }),
      "Matière renommée.",
    );
    if (ok) setEditing(null);
  }

  async function remove(subject: Subject) {
    if (!window.confirm(`Supprimer la matière « ${subject.name} » ?`)) return;
    await run(() => api(`/api/admin/subjects/${subject.id}`, { method: "DELETE" }), `Matière « ${subject.name} » supprimée.`);
  }

  return (
    <>
      <PageHead icon={bookOutline} title="Matières" subtitle="Les matières proposées aux étudiants. Une matière utilisée ne peut pas être supprimée." />

      <form className="card inline-form" onSubmit={create} noValidate>
        <Field label="Nouvelle matière" error={nameError} icon={bookOutline}>
          {(props) => (
            <input {...props} className="input" maxLength={100} placeholder="Exemple : Cybersécurité" value={name} onChange={(e) => setName(e.target.value)} />
          )}
        </Field>
        <button type="submit" className="btn btn-primary" disabled={busy}>
          <Icon icon={addOutline} size={18} />
          Ajouter
        </button>
      </form>

      {notice ? (
        <div style={{ marginTop: 16 }}>
          <Alert kind={notice.kind}>{notice.text}</Alert>
        </div>
      ) : null}

      <div style={{ marginTop: 20 }}>
        {subjects.loading && !subjects.data ? (
          <SkeletonList />
        ) : subjects.error || !subjects.data ? (
          <ErrorState message={subjects.error ?? "Chargement impossible"} onRetry={subjects.reload} />
        ) : subjects.data.length === 0 ? (
          <div className="card">
            <EmptyState icon={bookOutline} title="Aucune matière" />
          </div>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th scope="col">Matière</th>
                  <th scope="col" style={{ width: 1 }}>
                    <span className="visually-hidden">Actions</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {subjects.data.map((subject) => (
                  <tr key={subject.id}>
                    <td>
                      {editing?.id === subject.id ? (
                        <span className="row" style={{ flexWrap: "nowrap" }}>
                          <label className="visually-hidden" htmlFor={`subject-${subject.id}`}>
                            Nouveau nom de {subject.name}
                          </label>
                          <input
                            id={`subject-${subject.id}`}
                            className="input"
                            value={editing.name}
                            maxLength={100}
                            autoFocus
                            onChange={(e) => setEditing({ id: subject.id, name: e.target.value })}
                            onKeyDown={(e) => {
                              if (e.key === "Enter") void rename(subject);
                              if (e.key === "Escape") setEditing(null);
                            }}
                          />
                        </span>
                      ) : (
                        <span className="row">
                          <span className="icon-tile icon-tile-sm">
                            <Icon icon={subjectIcon(subject.name)} size={18} />
                          </span>
                          {subject.name}
                        </span>
                      )}
                    </td>
                    <td>
                      <span className="row" style={{ flexWrap: "nowrap", justifyContent: "flex-end" }}>
                        {editing?.id === subject.id ? (
                          <>
                            <button type="button" className="btn btn-primary btn-sm" disabled={busy} onClick={() => void rename(subject)}>
                              <Icon icon={checkmarkOutline} size={16} />
                              Enregistrer
                            </button>
                            <button type="button" className="btn btn-sm" onClick={() => setEditing(null)}>
                              <Icon icon={closeOutline} size={16} />
                              Annuler
                            </button>
                          </>
                        ) : (
                          <>
                            <button
                              type="button"
                              className="btn btn-sm"
                              disabled={busy}
                              aria-label={`Renommer ${subject.name}`}
                              onClick={() => setEditing({ id: subject.id, name: subject.name })}
                            >
                              <Icon icon={createOutline} size={16} />
                              Renommer
                            </button>
                            <button
                              type="button"
                              className="btn btn-ghost btn-sm"
                              disabled={busy}
                              aria-label={`Supprimer ${subject.name}`}
                              onClick={() => void remove(subject)}
                            >
                              <Icon icon={trashOutline} size={16} />
                              Supprimer
                            </button>
                          </>
                        )}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </>
  );
}
