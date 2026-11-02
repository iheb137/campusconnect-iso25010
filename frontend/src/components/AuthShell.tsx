import { chatbubblesOutline, ribbonOutline, schoolOutline } from "ionicons/icons";

import Icon from "./Icon";
import Logo from "./Logo";

const POINTS = [
  { icon: schoolOutline, title: "Trouvez de l'aide", text: "Publiez votre besoin par matière et par niveau." },
  { icon: chatbubblesOutline, title: "Aidez les autres", text: "Proposez une séance sur les demandes de votre domaine." },
  { icon: ribbonOutline, title: "Gagnez en réputation", text: "Chaque séance terminée est évaluée par les participants." },
];

/** Mise en page des écrans de connexion et d'inscription : présentation à gauche, formulaire à droite. */
export default function AuthShell({ title, lead, children }: { title: string; lead: string; children: React.ReactNode }) {
  return (
    <div className="auth fade-in">
      <aside className="auth-aside">
        <span className="auth-brand">
          <Logo size={38} />
          CampusConnect
        </span>
        <div>
          <h2>L&apos;entraide entre étudiants, simplement.</h2>
          <p style={{ marginTop: 8 }}>Une plateforme pour demander de l&apos;aide, partager vos compétences et réviser ensemble.</p>
        </div>
        <ul>
          {POINTS.map((point) => (
            <li key={point.title}>
              <span className="icon-tile icon-tile-sm">
                <Icon icon={point.icon} size={20} />
              </span>
              <span>
                <strong>{point.title}</strong>
                <span>{point.text}</span>
              </span>
            </li>
          ))}
        </ul>
      </aside>
      <section className="auth-main">
        <h1>{title}</h1>
        <p className="lead">{lead}</p>
        {children}
      </section>
    </div>
  );
}
