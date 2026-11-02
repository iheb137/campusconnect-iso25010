import type { Metadata } from "next";

import AuthShell from "@/components/AuthShell";

import RegisterForm from "./RegisterForm";

export const metadata: Metadata = { title: "Créer un compte" };

export default function RegisterPage() {
  return (
    <AuthShell title="Créer un compte" lead="Vous compléterez ensuite votre profil : spécialité, niveau et compétences.">
      <RegisterForm />
    </AuthShell>
  );
}
