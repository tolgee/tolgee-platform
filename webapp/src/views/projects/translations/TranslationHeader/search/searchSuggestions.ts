import { RESERVED_QUALIFIERS } from '../../searchQuery/parseSearchQuery';

export type SuggestionItem = {
  /** text inserted into the field, e.g. `description:` */
  insert: string;
  kind: 'field' | 'language';
  languageTag?: string;
};

export type SuggestionState = {
  items: SuggestionItem[];
  /** replace range in the input value */
  replaceFrom: number;
  replaceTo: number;
};

/**
 * Qualifier suggestions for the token under the caret. Returns undefined when
 * the caret is not in a completable position (token already has a qualifier,
 * or is quoted).
 */
export function getSuggestions(
  value: string,
  caret: number,
  languageTags: string[]
): SuggestionState | undefined {
  const beforeCaret = value.slice(0, caret);
  if ((beforeCaret.match(/"/g)?.length ?? 0) % 2 === 1) {
    return undefined;
  }
  const tokenStart = beforeCaret.search(/\S*$/);
  let token = beforeCaret.slice(tokenStart);
  let replaceFrom = tokenStart;
  if (token.startsWith('-')) {
    token = token.slice(1);
    replaceFrom += 1;
  }
  if (token.includes(':') || token.includes('"')) {
    return undefined;
  }
  const match = token.toLowerCase();
  const fields: SuggestionItem[] = RESERVED_QUALIFIERS.filter((qualifier) =>
    qualifier.startsWith(match)
  ).map((qualifier) => ({ insert: `${qualifier}:`, kind: 'field' }));
  const languages: SuggestionItem[] = languageTags
    .filter((tag) => tag.toLowerCase().startsWith(match))
    .map((tag) => ({ insert: `${tag}:`, kind: 'language', languageTag: tag }));
  const items = [...fields, ...languages];
  if (items.length === 0) {
    return undefined;
  }
  return { items, replaceFrom, replaceTo: caret };
}
