/**
 * The backend resolves language-scoped search only within the requested
 * languages and rejects unresolvable tags, so the searched languages must
 * always be part of the request — even when no selection is made yet and the
 * backend would otherwise fall back to an implicit subset.
 */
export const addSearchedIfMissing = (
  languages: string[] | undefined,
  searched: string[],
  baseLang: string
): string[] | undefined => {
  if (searched.length === 0) {
    return languages;
  }
  if (!languages || languages.length === 0) {
    return [...new Set([baseLang, ...searched])];
  }
  const missing = searched.filter((tag) => !languages.includes(tag));
  if (missing.length === 0) {
    return languages;
  }
  return [...languages, ...missing];
};
