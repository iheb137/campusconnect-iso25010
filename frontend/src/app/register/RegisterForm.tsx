"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";

import { arrowForwardOutline, lockClosedOutline, mailOutline, personOutline, shieldCheckmarkOutline } from "ionicons/icons";

import Field from "@/components/Field";
import Icon from "@/components/Icon";
import { Alert } from "@/components/States";
import { api, ApiError, errorMessage } from "@/lib/api";
import { useAuth } from "@/components/AuthProvider";
import type { LoginResult, User } from "@/lib/types";

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function RegisterForm() {
  const router = useRouter();
  const { setUser } = useAuth();
  const [form, setForm] = useState({ fullName: "", email: "", password: "", confirm: "" });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const update = (key: keyof typeof form) => (event: React.ChangeEvent<HTMLInputElement>) =>
    setForm((current) => ({ ...current, [key]: event.target.value }));

  function validate() {
    const found: Record<string, string> = {};
    const name = form.fullName.trim();
    if (name.length < 2 || name.length > 150) found.fullName = "Le nom doit contenir entre 2 et 150 caractères";
    if (!EMAIL.test(form.email.trim())) found.email = "Le format de l'email est invalide";
    if (form.password.length < 8 || form.password.length > 64)
      found.password = "Le mot de passe doit contenir entre 8 et 64 caractères";
    if (form.confirm !== form.password) found.confirm = "Les deux mots de passe ne correspondent pas";
    return found;
  }

  async function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const found = validate();
    setErrors(found);
    setFormError(null);
    if (Object.keys(found).length > 0) return;

    setSubmitting(true);
    const credentials = { email: form.email.trim(), password: form.password };
    try {
      await api<User>("/api/auth/register", {
        method: "POST",
        body: { ...credentials, fullName: form.fullName.trim() },
      });
    } catch (error) {
      setFormError(errorMessage(error));
      if (error instanceof ApiError) setErrors(error.fieldErrors);
      setSubmitting(false);
      return;
    }
    try {
      const result = await api<LoginResult>("/api/auth/login", { method: "POST", body: credentials });
      setUser(result.user);
      router.replace("/profile?welcome=1");
    } catch {
      router.replace("/login?registered=1");
    }
  }

  return (
    <form className="form" onSubmit={onSubmit} noValidate>
      {formError ? <Alert kind="error">{formError}</Alert> : null}
      <Field label="Nom complet" error={errors.fullName} icon={personOutline} required>
        {(props) => (
          <input
            {...props}
            className="input"
            autoComplete="name"
            placeholder="Prénom Nom"
            value={form.fullName}
            onChange={update("fullName")}
          />
        )}
      </Field>
      <Field label="Email" error={errors.email} icon={mailOutline} required>
        {(props) => (
          <input
            {...props}
            className="input"
            type="email"
            autoComplete="email"
            value={form.email}
            onChange={update("email")}
          />
        )}
      </Field>
      <Field label="Mot de passe" error={errors.password} hint="Entre 8 et 64 caractères" icon={lockClosedOutline} required>
        {(props) => (
          <input
            {...props}
            className="input"
            type="password"
            autoComplete="new-password"
            value={form.password}
            onChange={update("password")}
          />
        )}
      </Field>
      <Field label="Confirmer le mot de passe" error={errors.confirm} icon={shieldCheckmarkOutline} required>
        {(props) => (
          <input
            {...props}
            className="input"
            type="password"
            autoComplete="new-password"
            value={form.confirm}
            onChange={update("confirm")}
          />
        )}
      </Field>
      <button type="submit" className="btn btn-primary btn-lg btn-block" disabled={submitting}>
        {submitting ? "Création…" : "Créer mon compte"}
        {submitting ? null : <Icon icon={arrowForwardOutline} size={18} />}
      </button>
      <p className="small muted">
        Déjà inscrit ? <Link href="/login">Se connecter</Link>
      </p>
    </form>
  );
}
