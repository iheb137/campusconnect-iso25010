import type { Metadata } from "next";
import { Suspense } from "react";

import AuthShell from "@/components/AuthShell";
import { Loading } from "@/components/States";

import LoginForm from "./LoginForm";

export const metadata: Metadata = { title: "Connexion" };

export default function LoginPage() {
  return (
    <AuthShell title="Connexion" lead="Heureux de vous revoir ! Accédez à vos demandes et à vos séances.">
      <Suspense fallback={<Loading />}>
        <LoginForm />
      </Suspense>
    </AuthShell>
  );
}
