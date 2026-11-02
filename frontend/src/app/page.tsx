import {
  arrowForwardOutline,
  checkmarkCircle,
  checkmarkDoneOutline,
  flagOutline,
  gridOutline,
  megaphoneOutline,
  peopleOutline,
  personAddOutline,
  shieldCheckmarkOutline,
  sparkles,
  starOutline,
  timeOutline,
} from "ionicons/icons";
import Link from "next/link";

import Avatar from "@/components/Avatar";
import BackendStatus from "@/components/BackendStatus";
import { RequestStatusBadge, Stars } from "@/components/Badges";
import Icon from "@/components/Icon";

const FEATURES = [
  {
    icon: megaphoneOutline,
    tone: "",
    title: "Demandez de l'aide",
    text: "Publiez votre besoin avec la matière, le niveau et vos disponibilités.",
  },
  {
    icon: peopleOutline,
    tone: "tone-success",
    title: "Proposez vos compétences",
    text: "Repérez les demandes de votre domaine et proposez une séance.",
  },
  {
    icon: checkmarkDoneOutline,
    tone: "tone-info",
    title: "Suivez chaque séance",
    text: "Ouverte, aide acceptée, terminée : chaque étape est visible.",
  },
  {
    icon: starOutline,
    tone: "tone-accent",
    title: "Évaluez",
    text: "Après la séance, chaque participant évalue l'autre.",
  },
  {
    icon: gridOutline,
    tone: "tone-info",
    title: "Tableau de bord",
    text: "Vos demandes, vos aides proposées et vos séances en un coup d'œil.",
  },
  {
    icon: flagOutline,
    tone: "tone-danger",
    title: "Communauté modérée",
    text: "Les publications inappropriées sont signalées puis traitées par un administrateur.",
  },
];

const HOW = [
  { title: "Publiez", text: "Décrivez ce qui vous bloque et quand vous êtes disponible." },
  { title: "Choisissez", text: "Recevez des propositions et acceptez celle qui vous convient." },
  { title: "Révisez et évaluez", text: "Clôturez la séance puis évaluez-vous mutuellement." },
];

export default function HomePage() {
  return (
    <>
      <div className="hero-wrap">
        <section className="hero container" aria-labelledby="hero-title" style={{ padding: 0 }}>
          <div className="fade-in">
            <span className="hero-badge">
              <span className="chip">
                <Icon icon={sparkles} size={13} />
                Nouveau
              </span>
              Suivi des séances et évaluations
            </span>
            <h1 id="hero-title">
              L&apos;entraide entre étudiants, <span className="gradient-text">simplement.</span>
            </h1>
            <p className="hero-lead">
              CampusConnect met en relation les étudiants qui ont besoin d&apos;aide et ceux qui peuvent en apporter,
              pour organiser des séances de révision efficaces.
            </p>
            <div className="hero-cta">
              <Link href="/register" className="btn btn-primary btn-lg">
                <Icon icon={personAddOutline} size={20} />
                Créer un compte
              </Link>
              <Link href="/login" className="btn btn-lg">
                J&apos;ai déjà un compte
              </Link>
            </div>
            <ul className="hero-points">
              <li>
                <Icon icon={checkmarkCircle} size={18} />
                Gratuit pour les étudiants
              </li>
              <li>
                <Icon icon={checkmarkCircle} size={18} />
                Données conservées
              </li>
              <li>
                <Icon icon={checkmarkCircle} size={18} />
                Mode sombre
              </li>
            </ul>
          </div>

          <div className="preview fade-in" aria-hidden="true">
            <div className="card request-card">
              <span className="icon-tile">
                <Icon icon={peopleOutline} size={24} />
              </span>
              <div>
                <div className="row-between">
                  <span className="card-title">Aide sur les arbres AVL</span>
                  <RequestStatusBadge status="OUVERTE" />
                </div>
                <p className="excerpt">Je ne comprends pas les rotations doubles lors de l&apos;insertion…</p>
                <div className="request-card-foot">
                  <span className="author">
                    <Avatar name="Alice Martin" size={26} />
                    <strong>Alice Martin</strong>
                  </span>
                  <span className="meta">
                    <span className="chip">Algorithmique</span>
                    <span className="meta-item">
                      <Icon icon={timeOutline} size={15} />
                      Mardi 17 h
                    </span>
                  </span>
                </div>
              </div>
            </div>
            <div className="card preview-float">
              <div className="row" style={{ gap: 10 }}>
                <Avatar name="Bob Trabelsi" size={32} />
                <div>
                  <div className="small" style={{ fontWeight: 600 }}>
                    Séance terminée
                  </div>
                  <Stars score={5} size={14} />
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>

      <section className="section" aria-labelledby="features-title">
        <div className="section-head">
          <span className="eyebrow">Fonctionnalités</span>
          <h2 id="features-title" style={{ fontSize: "clamp(1.4rem, 2vw + 1rem, 2rem)" }}>
            Tout pour s&apos;entraider efficacement
          </h2>
          <p>De la demande d&apos;aide à l&apos;évaluation, chaque étape est simple et suivie.</p>
        </div>
        <div className="features">
          {FEATURES.map((feature) => (
            <div key={feature.title} className="card feature">
              <span className={`icon-tile ${feature.tone}`}>
                <Icon icon={feature.icon} size={24} />
              </span>
              <h3>{feature.title}</h3>
              <p>{feature.text}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="section" aria-labelledby="how-title">
        <div className="section-head">
          <span className="eyebrow">Comment ça marche</span>
          <h2 id="how-title" style={{ fontSize: "clamp(1.4rem, 2vw + 1rem, 2rem)" }}>
            Trois étapes, une séance réussie
          </h2>
        </div>
        <ol className="steps">
          {HOW.map((step, index) => (
            <li key={step.title} className="card how">
              <span className="how-num" aria-hidden="true">
                {index + 1}
              </span>
              <h3>{step.title}</h3>
              <p className="muted" style={{ marginTop: 4 }}>
                {step.text}
              </p>
            </li>
          ))}
        </ol>
      </section>

      <section className="cta-band" aria-labelledby="cta-title">
        <Icon icon={shieldCheckmarkOutline} size={34} />
        <h2 id="cta-title" style={{ marginTop: 8 }}>
          Prêt à réviser autrement ?
        </h2>
        <p>Rejoignez CampusConnect et trouvez de l&apos;aide dès aujourd&apos;hui.</p>
        <Link href="/register" className="btn btn-lg">
          Commencer maintenant
          <Icon icon={arrowForwardOutline} size={18} />
        </Link>
      </section>

      <div style={{ marginTop: 28, textAlign: "center" }}>
        <BackendStatus />
      </div>
    </>
  );
}
