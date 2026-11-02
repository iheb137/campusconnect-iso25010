/** Logo CampusConnect : deux étudiants reliés par un arc (l'entraide), avec l'étincelle de la mise en relation. */
export default function Logo({ size = 32, title }: { size?: number; title?: string }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 48 48"
      xmlns="http://www.w3.org/2000/svg"
      role={title ? "img" : undefined}
      aria-label={title}
      aria-hidden={title ? undefined : true}
      focusable="false"
    >
      <rect width="48" height="48" rx="12" fill="#2f45d0" />
      <path d="M14 17.5C16.5 10 31.5 10 34 17.5" fill="none" stroke="#9fb0ff" strokeWidth="3" strokeLinecap="round" />
      <circle cx="24" cy="11.6" r="2.4" fill="#ffd166" />
      <circle cx="14.5" cy="23" r="5" fill="#fff" />
      <circle cx="33.5" cy="23" r="5" fill="#fff" />
      <path d="M6 39c0-4.9 3.8-8.6 8.5-8.6S23 34.1 23 39z" fill="#fff" />
      <path d="M25 39c0-4.9 3.8-8.6 8.5-8.6S42 34.1 42 39z" fill="#fff" />
    </svg>
  );
}
