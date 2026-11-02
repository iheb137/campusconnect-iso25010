import Icon from "./Icon";

/** En-tête de page : icône, titre, sous-titre et actions éventuelles. */
export default function PageHead({
  icon,
  title,
  subtitle,
  children,
}: {
  icon: string;
  title: string;
  subtitle?: string;
  children?: React.ReactNode;
}) {
  return (
    <div className="page-head fade-in">
      <div className="page-head-title">
        <span className="icon-tile">
          <Icon icon={icon} size={24} />
        </span>
        <div style={{ minWidth: 0 }}>
          <h1>{title}</h1>
          {subtitle ? <p>{subtitle}</p> : null}
        </div>
      </div>
      {children ? <div className="row">{children}</div> : null}
    </div>
  );
}
