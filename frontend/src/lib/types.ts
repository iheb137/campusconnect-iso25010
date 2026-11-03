// Types des réponses de l'API Spring Boot (voir backend/src/main/java/.../dto).

export type Role = "STUDENT" | "MODERATOR" | "ADMIN";
export type Level = "L1" | "L2" | "L3" | "M1" | "M2";
export type RequestStatus = "OUVERTE" | "AIDE_ACCEPTEE" | "TERMINEE";
export type OfferStatus = "EN_ATTENTE" | "ACCEPTEE" | "REFUSEE";
export type ReportStatus = "EN_ATTENTE" | "TRAITE" | "REJETE";

export type Subject = { id: number; name: string };
export type Person = { id: number; fullName: string };

export type User = {
  id: number;
  email: string;
  fullName: string;
  role: Role;
  specialty: string | null;
  level: Level | null;
  skills: Subject[];
};

export type LoginResult = { user: User };

export type HelpRequest = {
  id: number;
  title: string;
  description: string;
  availability: string;
  level: Level;
  status: RequestStatus;
  createdAt: string;
  completedAt: string | null;
  subject: Subject;
  author: Person;
  ownedByCurrentUser: boolean;
};

export type HelpRequestInput = {
  title: string;
  description: string;
  availability: string;
  subjectId: number;
  level: Level;
};

export type Page<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type Offer = {
  id: number;
  requestId: number;
  message: string;
  status: OfferStatus;
  createdAt: string;
  helper: Person;
};

export type Rating = {
  id: number;
  requestId: number;
  score: number;
  comment: string | null;
  createdAt: string;
  rater: Person;
  rated: Person;
};

export type SessionRatings = { canRate: boolean; ratings: Rating[] };

export type Report = {
  id: number;
  requestId: number;
  reason: string;
  status: ReportStatus;
  createdAt: string;
  resolvedAt: string | null;
  reporter: Person;
};

export type ReportedRequest = {
  requestId: number;
  title: string;
  description: string;
  status: RequestStatus;
  hidden: boolean;
  createdAt: string;
  subject: Subject;
  author: Person;
  pendingReports: number;
  reports: Report[];
};

export type Dashboard = {
  stats: {
    publishedRequests: number;
    openRequests: number;
    offersMade: number;
    offersAccepted: number;
    completedSessions: number;
    ratingsReceived: number;
    averageRating: number | null;
  };
  myRequests: {
    id: number;
    title: string;
    subject: Subject;
    level: Level;
    status: RequestStatus;
    hidden: boolean;
    createdAt: string;
    completedAt: string | null;
    offersCount: number;
    pendingOffersCount: number;
  }[];
  myOffers: {
    id: number;
    message: string;
    status: OfferStatus;
    createdAt: string;
    requestId: number;
    requestTitle: string;
    requestStatus: RequestStatus;
    subject: Subject;
    requestAuthor: Person;
  }[];
  sessions: {
    requestId: number;
    title: string;
    subject: Subject;
    completedAt: string | null;
    role: "AUTEUR" | "AIDANT";
    partner: Person;
    ratedByMe: boolean;
  }[];
};

export type AdminUser = {
  id: number;
  email: string;
  fullName: string;
  role: Role;
  enabled: boolean;
  level: Level | null;
  specialty: string | null;
  createdAt: string;
};

export type AuditAction =
  | "USER_REGISTERED"
  | "LOGIN_SUCCEEDED"
  | "LOGIN_FAILED"
  | "USER_ROLE_CHANGED"
  | "USER_DISABLED"
  | "USER_ENABLED"
  | "REQUEST_DELETED"
  | "OFFER_ACCEPTED"
  | "OFFER_REJECTED"
  | "OFFER_WITHDRAWN"
  | "SESSION_COMPLETED"
  | "RATING_SUBMITTED"
  | "REQUEST_REPORTED"
  | "REQUEST_HIDDEN"
  | "REQUEST_RESTORED"
  | "REPORTS_DISMISSED"
  | "SUBJECT_CREATED"
  | "SUBJECT_UPDATED"
  | "SUBJECT_DELETED";

export type AuditEntry = {
  id: number;
  createdAt: string;
  actorId: number | null;
  actorEmail: string | null;
  action: AuditAction;
  targetType: string | null;
  targetId: number | null;
  details: string | null;
};

export type AdminStats = {
  from: string;
  to: string;
  subjectId: number | null;
  usersByRole: Record<Role, number>;
  requestsByStatus: Record<RequestStatus, number>;
  offersByStatus: Record<OfferStatus, number>;
  totalRequests: number;
  completedSessions: number;
  completionRate: number | null;
  averageRating: number | null;
  ratingsCount: number;
  pendingReports: number;
  bySubject: { subjectId: number; name: string; requests: number; completed: number }[];
  requestsPerDay: { day: string; count: number }[];
  topHelpers: { userId: number; fullName: string; sessions: number }[];
};
