import type { Metadata } from "next";
import { GeistMono } from "geist/font/mono";
import { GeistSans } from "geist/font/sans";

import { AuthProvider } from "@/components/AuthProvider";
import Logo from "@/components/Logo";
import SiteHeader from "@/components/SiteHeader";

import "./globals.css";

// Applique le thème enregistré avant l'affichage, pour éviter un flash du mauvais thème.
const THEME_SCRIPT = `try{var t=localStorage.getItem("cc-theme");if(t==="light"||t==="dark")document.documentElement.dataset.theme=t}catch(e){}`;

export const metadata: Metadata = {
  title: { default: "CampusConnect", template: "%s · CampusConnect" },
  description: "Plateforme d'entraide entre étudiants : demandes d'aide, propositions, séances et évaluations.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="fr" className={`${GeistSans.variable} ${GeistMono.variable}`} suppressHydrationWarning>
      <head>
        <script dangerouslySetInnerHTML={{ __html: THEME_SCRIPT }} />
      </head>
      <body>
        <a href="#contenu" className="skip-link">
          Aller au contenu
        </a>
        <AuthProvider>
          <SiteHeader />
          <main id="contenu" tabIndex={-1}>
            <div className="container">{children}</div>
          </main>
        </AuthProvider>
        <footer className="site-footer">
          <div className="container">
            <span className="brand">
              <Logo size={26} />
              CampusConnect
            </span>
            <span>Entraide entre étudiants · Projet universitaire TEK-UP</span>
          </div>
        </footer>
      </body>
    </html>
  );
}
