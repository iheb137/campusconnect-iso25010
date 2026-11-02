import {
  bookOutline,
  calculatorOutline,
  cafeOutline,
  codeSlashOutline,
  constructOutline,
  gitNetworkOutline,
  globeOutline,
  hardwareChipOutline,
  languageOutline,
  serverOutline,
  sparklesOutline,
  wifiOutline,
} from "ionicons/icons";

const SUBJECT_ICONS: [RegExp, string][] = [
  [/math/i, calculatorOutline],
  [/algo/i, gitNetworkOutline],
  [/java/i, cafeOutline],
  [/donn[ée]es|sql/i, serverOutline],
  [/web/i, globeOutline],
  [/r[ée]seau/i, wifiOutline],
  [/syst[èe]me/i, hardwareChipOutline],
  [/g[ée]nie/i, constructOutline],
  [/intelligence|\bia\b/i, sparklesOutline],
  [/anglais|langue/i, languageOutline],
  [/prog/i, codeSlashOutline],
];

/** Icône représentant une matière (repli : un livre). */
export function subjectIcon(name: string): string {
  return SUBJECT_ICONS.find(([pattern]) => pattern.test(name))?.[1] ?? bookOutline;
}
