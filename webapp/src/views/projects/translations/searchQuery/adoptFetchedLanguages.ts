type Params = {
  fetchedTags: string[];
  currentSelection: string[] | undefined;
  /** tags added to the request only to satisfy a language-scoped search */
  searchInjectedTags: string[];
};

/**
 * Which language selection to adopt (and persist) from a fetched response.
 * Returns undefined when nothing must be adopted — a transient search term
 * must never narrow the stored selection the user did not make.
 */
export function adoptFetchedLanguages({
  fetchedTags,
  currentSelection,
  searchInjectedTags,
}: Params): string[] | undefined {
  if (currentSelection?.length) {
    return fetchedTags.filter((tag) => currentSelection.includes(tag));
  }
  if (searchInjectedTags.length > 0) {
    return undefined;
  }
  return fetchedTags;
}
