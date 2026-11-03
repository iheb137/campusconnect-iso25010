"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";

import { useAuth } from "@/components/AuthProvider";
import { arrowForwardOutline, lockClosedOutline, mailOutline } from "ionicons/icons";

import Field from "@/components/Field";
import Icon from "@/components/Icon";
import { Alert } from "@/components/States";
import { api, ApiError, errorMessage } from "@/lib/api";
import { homeFor } from "@/lib/session";
import type { LoginResult } from "@/lib/types";

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { setUser } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const next = safeNext(searchParams.get("next"));

  async function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const found: Record<string, string> = {};
    if (!EMAIL.test(email.trim())) found.email = "Saisissez un email valide";
    if (!password) found.password = "Le mot de passe est obligatoire";
    setErrors(found);
    setFormError(null);
    if (Object.keys(found).length > 0) return;

    setSubmitting(true);
    try {
      const result = await api<LoginResult>("/api/auth/login", {
        method: "POST",
        body: { email: email.trim(), password },
      });
      setUser(result.user);
      const home = homeFor(result.user.role);
      router.replace(next ?? home);
    } catch (error) {
      setFormError(errorMessage(error));
      if (error instanceof ApiError) setErrors(error.fieldErrors);
      setSubmitting(false);
    }
  }

  return (
    <form className="form" onSubmit={onSubmit} noValidate>
      {searchParams.get("expired") ? <Alert kind="info">Votre session a expiré. Reconnectez-vous.</Alert> : null}
      {searchParams.get("registered") ? (
        <Alert kind="success">Compte créé. Vous pouvez vous connecter.</Alert>
      ) : null}
      {formError ? <Alert kind="error">{formError}</Alert> : null}

      <Field label="Email" error={errors.email} icon={mailOutline} required>
        {(props) => (
          <input
            {...props}
            className="input"
            type="email"
            autoComplete="email"
            placeholder="prenom.nom@exemple.com"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        )}
      </Field>
      <Field label="Mot de passe" error={errors.password} icon={lockClosedOutline} required>
        {(props) => (
          <input
            {...props}
            className="input"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        )}
      </Field>

      <button type="submit" className="btn btn-primary btn-lg btn-block" disabled={submitting}>
        {submitting ? "Connexion…" : "Se connecter"}
        {submitting ? null : <Icon icon={arrowForwardOutline} size={18} />}
      </button>
      <p className="small muted">
        Pas encore de compte ? <Link href="/register">Créer un compte</Link>
      </p>
    </form>
  );
}

/** N'accepte qu'un chemin interne, pour éviter les redirections vers un autre site. */
function safeNext(value: string | null): string | null {
  if (!value || !value.startsWith("/") || value.startsWith("//")) return null;
  return value;
}
