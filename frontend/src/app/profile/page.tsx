import { personCircleOutline } from "ionicons/icons";
import type { Metadata } from "next";
import { Suspense } from "react";

import PageHead from "@/components/PageHead";
import { Loading } from "@/components/States";

import ProfileForm from "./ProfileForm";

export const metadata: Metadata = { title: "Mon profil" };

export default function ProfilePage() {
  return (
    <>
      <PageHead
        icon={personCircleOutline}
        title="Mon profil"
        subtitle="Votre spécialité, votre niveau et vos compétences aident les autres étudiants à vous trouver."
      />
      <Suspense fallback={<Loading />}>
        <ProfileForm />
      </Suspense>
    </>
  );
}
