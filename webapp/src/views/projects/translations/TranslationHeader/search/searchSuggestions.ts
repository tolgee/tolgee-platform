import {
  canLanguageScope,
  findLanguageTag,
  isReservedQualifier,
  ParsedToken,
  parseSearchQuery,
  RESERVED_QUALIFIERS,
} from '../../searchQuery/parseSearchQuery';

export type SuggestionItem = {
  insert: string;
  kind: 'field' | 'language';
};

export type SuggestionState = {
  items: SuggestionItem[];
  replaceFrom: number;
  replaceTo: number;
};

function exactQualifierState(
  token: ParsedToken,
  caret: number,
  languageTags: string[]
): SuggestionState | undefined {
  if (token.type !== 'ignored' || caret !== token.to) {
    return undefined;
  }
  const negated = token.raw.startsWith('-');
  const name = token.raw.slice(negated ? 1 : 0, -1).toLowerCase();
  const replaceFrom = negated ? token.from + 1 : token.from;
  if (isReservedQualifier(name)) {
    return {
      items: [{ insert: `${name}:`, kind: 'field' }],
      replaceFrom,
      replaceTo: caret,
    };
  }
  const tag = findLanguageTag(languageTags, name);
  if (!tag) {
    return undefined;
  }
  return {
    items: [{ insert: `${tag}:`, kind: 'language' }],
    replaceFrom,
    replaceTo: caret,
  };
}

export function getSuggestions(
  value: string,
  caret: number,
  languageTags: string[]
): SuggestionState | undefined {
  const { tokens } = parseSearchQuery(value, languageTags);
  const token = tokens.find((t) => t.from <= caret && caret <= t.to);
  let replaceFrom = caret;
  let match = '';
  if (token) {
    const tokenBlocksSuggestions =
      token.type !== 'text' ||
      token.raw.includes(':') ||
      token.raw.includes('"');
    if (tokenBlocksSuggestions) {
      return exactQualifierState(token, caret, languageTags);
    }
    replaceFrom = token.raw.startsWith('-') ? token.from + 1 : token.from;
    match = value.slice(replaceFrom, caret).toLowerCase();
  }
  const fields: SuggestionItem[] = RESERVED_QUALIFIERS.filter((qualifier) =>
    qualifier.startsWith(match)
  ).map((qualifier) => ({ insert: `${qualifier}:`, kind: 'field' }));
  const languages: SuggestionItem[] = languageTags
    .filter(
      (tag) => canLanguageScope(tag) && tag.toLowerCase().startsWith(match)
    )
    .map((tag) => ({ insert: `${tag}:`, kind: 'language' }));
  const items = [...fields, ...languages];
  if (items.length === 0) {
    return undefined;
  }
  return { items, replaceFrom, replaceTo: caret };
}
