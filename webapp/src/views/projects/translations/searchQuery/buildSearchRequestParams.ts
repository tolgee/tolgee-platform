import { operations } from 'tg.service/apiSchema.generated';

import { parseSearchQuery, ScopedToken } from './parseSearchQuery';

type TranslationsQuery = operations['getTranslations']['parameters']['query'];

export type SearchRequestParams = Pick<
  TranslationsQuery,
  | 'search'
  | 'filterKeyPattern'
  | 'filterNoKeyPattern'
  | 'filterDescriptionPattern'
  | 'filterNoDescriptionPattern'
  | 'filterNamespacePattern'
  | 'filterNoNamespacePattern'
  | 'filterTranslationPattern'
  | 'filterNoTranslationPattern'
>;

const PATTERN_PARAMS = {
  key: ['filterKeyPattern', 'filterNoKeyPattern'],
  description: ['filterDescriptionPattern', 'filterNoDescriptionPattern'],
  namespace: ['filterNamespacePattern', 'filterNoNamespacePattern'],
} as const;

function paramValue(token: ScopedToken): string {
  if (token.qualifier === 'language') {
    return `${token.languageTag},${token.value}`;
  }
  if (token.qualifier === 'translation') {
    return `*,${token.value}`;
  }
  return token.value;
}

type PatternParamName = Exclude<keyof SearchRequestParams, 'search'>;

function paramName(token: ScopedToken): PatternParamName {
  if (token.qualifier === 'language' || token.qualifier === 'translation') {
    return token.negated
      ? 'filterNoTranslationPattern'
      : 'filterTranslationPattern';
  }
  return PATTERN_PARAMS[token.qualifier][token.negated ? 1 : 0];
}

/**
 * Language tags referenced by translation pattern params. The backend resolves
 * tags only within the requested languages, so these must be added to the
 * `languages` request param for the filter to apply.
 */
export function getReferencedLanguageTags(
  params: SearchRequestParams
): string[] {
  const values = [
    ...(params.filterTranslationPattern ?? []),
    ...(params.filterNoTranslationPattern ?? []),
  ];
  const tags = values
    .map((value) => value.slice(0, value.indexOf(',')))
    .filter((tag) => tag && tag !== '*');
  return [...new Set(tags)];
}

/**
 * Turns the raw search field input into request params. Without any recognized
 * scoped qualifier the input is passed through verbatim as `search`, exactly
 * matching the behavior before the query language existed.
 */
export function buildSearchRequestParams(
  input: string | undefined,
  languageTags: string[]
): SearchRequestParams {
  const parsed = parseSearchQuery(input ?? '', languageTags);
  if (!parsed.hasScopedTokens) {
    return { search: input || undefined };
  }
  const result: SearchRequestParams = {};
  const textParts: string[] = [];
  for (const token of parsed.tokens) {
    if (token.type === 'text') {
      textParts.push(token.value);
      continue;
    }
    const name = paramName(token);
    result[name] = [...(result[name] ?? []), paramValue(token)];
  }
  result.search = textParts.join(' ') || undefined;
  return result;
}
