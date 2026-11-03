"use client";

import { peopleOutline, searchOutline, shieldOutline } from "ionicons/icons";
import { useEffect, useState } from "react";

import { useAuth } from "@/components/AuthProvider";
import Avatar from "@/components/Avatar";
import Icon from "@/components/Icon";
import PageHead from "@/components/PageHead";
import Pagination from "@/components/Pagination";
import { Alert, EmptyState, ErrorState, SkeletonList } from "@/components/States";
import { useApiData } from "@/components/useApiData";
import { api, errorMessage } from "@/lib/api";
import { formatDate } from "@/lib/format";
import { ROLE_LABEL } from "@/lib/session";
import type { AdminUser, Page, Role } from "@/lib/types";

const ROLES = Object.keys(ROLE_LABEL) as Role[];

export default function UsersAdmin() {
  const { user: me } = useAuth();
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState("");
  const [role, setRole] = useState<Role | "">("");
  const [page, setPage] = useState(0);
  const [busy, setBusy] = useState<number | null>(null);
  const [notice, setNotice] = useState<{ kind: "error" | "success"; text: string } | null>(null);

  // Recherche déclenchée 300 ms après la dernière frappe.
  useEffect(() => {
    const timer = setTimeout(() => {
      setQuery(search.trim());
      setPage(0);
    }, 300);
    return () => clearTimeout(timer);
  }, [search]);

  const params = new URLSearchParams({ page: String(page), size: "15" });
  if (query) params.set("q", query);
  if (role) params.set("role", role);
  const users = useApiData<Page<AdminUser>>(`/api/admin/users?${params.toString()}`);

  async function update(target: AdminUser, path: "role" | "enabled", body: object, message: string) {
    setBusy(target.id);
    setNotice(null);
    try {
      const updated = await api<AdminUser>(`/api/admin/users/${target.id}/${path}`, { method: "PUT", body });
      if (users.data) {
        users.setData({ ...users.data, content: users.data.content.map((u) => (u.id === updated.id ? updated : u)) });
      }
      setNotice({ kind: "success", text: message });
    } catch (error) {
      setNotice({ kind: "error", text: errorMessage(error) });
    } finally {
      setBusy(null);
    }
  }

  return (
    <>
      <PageHead icon={peopleOutline} title="Comptes" subtitle="Rechercher un compte, changer son rôle, l'activer ou le désactiver." />

      <form className="card filters" role="search" aria-label="Rechercher un compte" onSubmit={(event) => event.preventDefault()}>
        <div className="field" style={{ gridColumn: "span 2" }}>
          <label htmlFor="user-search">Nom ou email</label>
          <div className="input-wrap">
            <Icon icon={searchOutline} size={18} />
            <input id="user-search" className="input" placeholder="Rechercher…" value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
        </div>
        <div className="field">
          <label htmlFor="user-role">Rôle</label>
          <div className="input-wrap">
            <Icon icon={shieldOutline} size={18} />
            <select
              id="user-role"
              className="select"
              value={role}
              onChange={(e) => {
                setRole(e.target.value as Role | "");
                setPage(0);
              }}
            >
              <option value="">Tous</option>
              {ROLES.map((value) => (
                <option key={value} value={value}>
                  {ROLE_LABEL[value]}
                </option>
              ))}
            </select>
          </div>
        </div>
      </form>

      {notice ? (
        <div style={{ marginTop: 16 }}>
          <Alert kind={notice.kind}>{notice.text}</Alert>
        </div>
      ) : null}

      <div style={{ marginTop: 20 }}>
        {users.loading && !users.data ? (
          <SkeletonList />
        ) : users.error || !users.data ? (
          <ErrorState message={users.error ?? "Chargement impossible"} onRetry={users.reload} />
        ) : users.data.content.length === 0 ? (
          <div className="card">
            <EmptyState icon={peopleOutline} title="Aucun compte ne correspond" />
          </div>
        ) : (
          <>
            <p className="small muted" style={{ marginBottom: 10 }} aria-live="polite">
              {users.data.totalElements} compte(s)
            </p>
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th scope="col">Compte</th>
                    <th scope="col">Rôle</th>
                    <th scope="col">Actif</th>
                    <th scope="col">Inscrit le</th>
                  </tr>
                </thead>
                <tbody>
                  {users.data.content.map((account) => {
                    const self = account.id === me?.id;
                    return (
                      <tr key={account.id}>
                        <td>
                          <span className="cell-user">
                            <Avatar name={account.fullName} size={34} />
                            <span>
                              {account.fullName}
                              {self ? " (vous)" : ""}
                              <small>{account.email}</small>
                            </span>
                          </span>
                        </td>
                        <td>
                          <label className="visually-hidden" htmlFor={`role-${account.id}`}>
                            Rôle de {account.fullName}
                          </label>
                          <select
                            id={`role-${account.id}`}
                            className="select"
                            value={account.role}
                            disabled={self || busy === account.id}
                            onChange={(e) =>
                              update(account, "role", { role: e.target.value }, `Rôle de ${account.fullName} : ${ROLE_LABEL[e.target.value as Role]}.`)
                            }
                          >
                            {ROLES.map((value) => (
                              <option key={value} value={value}>
                                {ROLE_LABEL[value]}
                              </option>
                            ))}
                          </select>
                        </td>
                        <td>
                          <button
                            type="button"
                            role="switch"
                            className="switch"
                            aria-checked={account.enabled}
                            aria-label={`${account.enabled ? "Désactiver" : "Activer"} le compte de ${account.fullName}`}
                            disabled={self || busy === account.id}
                            onClick={() =>
                              update(
                                account,
                                "enabled",
                                { enabled: !account.enabled },
                                `Compte de ${account.fullName} ${account.enabled ? "désactivé" : "activé"}.`,
                              )
                            }
                          />
                        </td>
                        <td className="small muted">{formatDate(account.createdAt)}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            <Pagination page={page} totalPages={users.data.totalPages} onChange={setPage} />
          </>
        )}
      </div>
    </>
  );
}
