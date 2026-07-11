import { operations } from 'tg.service/apiSchema.generated';

import {
  findLanguageTag,
  ParsedToken,
  parseSearchQuery,
  ScopedToken,
} from './parseSearchQuery';

type TranslationsQuery = operations['getTranslations']['parameters']['query'];

// these mirror the backend limits in WildcardLikeUtil and must stay in sync
const MAX_PATTERN_LENGTH = 500;
const MAX_WILDCARDS = 5;
const MAX_PATTERNS_PER_PARAM = 20;

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

type PatternParamName = Exclude<keyof SearchRequestParams, 'search'>;

const PATTERN_PARAMS = {
  key: { base: 'filterKeyPattern', negated: 'filterNoKeyPattern' },
  description: {
    base: 'filterDescriptionPattern',
    negated: 'filterNoDescriptionPattern',
  },
  namespace: {
    base: 'filterNamespacePattern',
    negated: 'filterNoNamespacePattern',
  },
} as const;

export function buildSearchRequestParams(
  input: string | undefined,
  languageTags: string[]
): SearchRequestParams {
  const parsed = parseSearchQuery(input ?? '', languageTags);
  const textPartOf = (token: ParsedToken) =>
    token.type === 'text' && isQualifierEscape(token, languageTags)
      ? token.value
      : token.raw;
  if (!parsed.hasRecognizedQualifiers) {
    if (parsed.tokens.some((token) => isQualifierEscape(token, languageTags))) {
      const search = parsed.tokens.map(textPartOf).join(' ');
      return { search: search || undefined };
    }
    return { search: input || undefined };
  }
  const result: SearchRequestParams = {};
  const textParts: string[] = [];
  for (const token of parsed.tokens) {
    if (token.type === 'text') {
      textParts.push(textPartOf(token));
      continue;
    }
    if (token.type === 'ignored') {
      continue;
    }
    const name = paramName(token);
    const values = result[name] ?? [];
    const value = paramValue(token, languageTags);
    // the backend validates only the pattern part after the language prefix
    const pattern = isTranslationScoped(token) ? patternOf(value) : value;
    if (exceedsLimits(pattern) || values.length >= MAX_PATTERNS_PER_PARAM) {
      textParts.push(token.raw);
      continue;
    }
    result[name] = [...values, value];
  }
  result.search = textParts.join(' ') || undefined;
  return result;
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
  const tags = values.map(languageTagOf).filter((tag) => tag && tag !== '*');
  return [...new Set(tags)];
}

function isTranslationScoped(token: ScopedToken): boolean {
  return token.qualifier === 'language' || token.qualifier === 'translation';
}

function paramName(token: ScopedToken): PatternParamName {
  if (isTranslationScoped(token)) {
    return token.negated
      ? 'filterNoTranslationPattern'
      : 'filterTranslationPattern';
  }
  const names = PATTERN_PARAMS[token.qualifier as keyof typeof PATTERN_PARAMS];
  return token.negated ? names.negated : names.base;
}

function paramValue(token: ScopedToken, languageTags: string[]): string {
  if (isTranslationScoped(token)) {
    return translationParamValue(token, languageTags);
  }
  return token.value;
}

function translationParamValue(
  token: ScopedToken,
  languageTags: string[]
): string {
  if (token.qualifier === 'language') {
    return `${token.languageTag},${token.value}`;
  }
  const separatorIndex = token.quoted ? -1 : token.value.indexOf(',');
  if (separatorIndex > 0) {
    const prefix = token.value.slice(0, separatorIndex);
    const rest = token.value.slice(separatorIndex + 1);
    if (prefix === '*' && rest) {
      return token.value;
    }
    const tag = findLanguageTag(languageTags, prefix);
    if (tag && rest) {
      return `${tag},${rest}`;
    }
  }
  return `*,${token.value}`;
}

function languageTagOf(paramValue: string): string {
  return paramValue.slice(0, paramValue.indexOf(','));
}

function patternOf(paramValue: string): string {
  return paramValue.slice(paramValue.indexOf(',') + 1);
}

function exceedsLimits(value: string): boolean {
  if (value.length > MAX_PATTERN_LENGTH) {
    return true;
  }
  return value.split('*').length - 1 > MAX_WILDCARDS;
}

/** a quoted token whose content would otherwise parse as a qualifier, e.g. `"key:foo"` */
function isQualifierEscape(token: ParsedToken, languageTags: string[]) {
  if (token.type !== 'text' || !token.raw.startsWith('"')) {
    return false;
  }
  return parseSearchQuery(token.value, languageTags).hasRecognizedQualifiers;
}
