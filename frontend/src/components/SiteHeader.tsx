"use client";

import {
  addCircleOutline,
  closeOutline,
  flagOutline,
  gridOutline,
  logInOutline,
  logOutOutline,
  menuOutline,
  personCircleOutline,
  searchOutline,
} from "ionicons/icons";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";

import { useAuth } from "./AuthProvider";
import Avatar from "./Avatar";
import Icon from "./Icon";
import Logo from "./Logo";
import ThemeToggle from "./ThemeToggle";

type NavItem = { href: string; label: string; icon: string };

const STUDENT_NAV: NavItem[] = [
  { href: "/dashboard", label: "Tableau de bord", icon: gridOutline },
  { href: "/requests", label: "Demandes", icon: searchOutline },
  { href: "/requests/new", label: "Publier", icon: addCircleOutline },
  { href: "/profile", label: "Profil", icon: personCircleOutline },
];

const ADMIN_NAV: NavItem[] = [
  { href: "/admin", label: "Signalements", icon: flagOutline },
  { href: "/requests", label: "Demandes", icon: searchOutline },
];

export default function SiteHeader() {
  const { user, loading, logout } = useAuth();
  const pathname = usePathname();
  const [open, setOpen] = useState(false);

  const items = !user ? [] : user.role === "ADMIN" ? ADMIN_NAV : STUDENT_NAV;
  const isCurrent = (href: string) =>
    href === "/requests" ? pathname === "/requests" || /^\/requests\/\d+/.test(pathname) : pathname === href;
  const close = () => setOpen(false);

  return (
    <header className="site-header">
      <div className="container">
        <Link href={user ? (user.role === "ADMIN" ? "/admin" : "/dashboard") : "/"} className="brand" onClick={close}>
          <Logo size={34} />
          CampusConnect
        </Link>

        <div className="header-actions">
          <ThemeToggle />
          <button
            type="button"
            className="icon-btn menu-toggle"
            aria-expanded={open}
            aria-controls="main-nav"
            aria-label="Menu"
            onClick={() => setOpen((value) => !value)}
          >
            <Icon icon={open ? closeOutline : menuOutline} size={24} />
          </button>
        </div>

        <nav id="main-nav" className="nav" aria-label="Navigation principale" data-open={open}>
          {items.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className="nav-link"
              aria-current={isCurrent(item.href) ? "page" : undefined}
              onClick={close}
            >
              <Icon icon={item.icon} size={19} />
              {item.label}
            </Link>
          ))}
          {loading ? null : user ? (
            <>
              <span className="nav-sep" aria-hidden="true" />
              <span className="user-chip" title={user.email}>
                <Avatar name={user.fullName} size={34} />
                <span className="user-chip-text">
                  <span className="user-chip-name">{user.fullName}</span>
                  <span className="user-chip-role">{user.role === "ADMIN" ? "Administrateur" : "Étudiant"}</span>
                </span>
              </span>
              <button type="button" className="nav-link" onClick={() => void logout()}>
                <Icon icon={logOutOutline} size={19} />
                Se déconnecter
              </button>
            </>
          ) : (
            <>
              <Link
                href="/login"
                className="nav-link"
                aria-current={pathname === "/login" ? "page" : undefined}
                onClick={close}
              >
                <Icon icon={logInOutline} size={19} />
                Se connecter
              </Link>
              <Link href="/register" className="btn btn-primary btn-sm" onClick={close}>
                Créer un compte
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
