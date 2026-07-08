export const RESERVED_QUALIFIERS = [
  'key',
  'description',
  'namespace',
  'translation',
] as const;

export type ReservedQualifier = (typeof RESERVED_QUALIFIERS)[number];

export type TextToken = {
  type: 'text';
  /** verbatim as typed, including any quotes */
  raw: string;
  /** unquoted value */
  value: string;
};

/** a recognized qualifier with an empty value — contributes nothing */
export type IgnoredToken = {
  type: 'ignored';
  raw: string;
};

export type ScopedToken = {
  type: 'scoped';
  qualifier: ReservedQualifier | 'language';
  /** canonical project language tag, set when qualifier === 'language' */
  languageTag?: string;
  negated: boolean;
  value: string;
};

export type ParsedToken = TextToken | ScopedToken | IgnoredToken;

export type ParsedSearch = {
  tokens: ParsedToken[];
  /** true when any token was recognized as query syntax (scoped or ignored) */
  hasScopedTokens: boolean;
};

const QUALIFIER_REGEX = /^(-?)([A-Za-z0-9_@-]+):(.*)$/;

/**
 * Splits the input into whitespace-separated raw tokens. A `"` starts a quoted
 * run in which whitespace does not split; an unterminated quote runs to the end
 * of the input.
 */
function splitRawTokens(input: string): string[] {
  const tokens: string[] = [];
  let current = '';
  let inQuote = false;
  for (const char of input) {
    if (char === '"') {
      inQuote = !inQuote;
      current += char;
      continue;
    }
    if (!inQuote && /\s/.test(char)) {
      if (current) {
        tokens.push(current);
        current = '';
      }
      continue;
    }
    current += char;
  }
  if (current) {
    tokens.push(current);
  }
  return tokens;
}

function unquote(value: string): string {
  if (!value.startsWith('"')) {
    return value;
  }
  if (value.length > 1 && value.endsWith('"')) {
    return value.slice(1, -1);
  }
  return value.slice(1);
}

function parseToken(raw: string, languageTags: string[]): ParsedToken {
  const textToken: TextToken = { type: 'text', raw, value: unquote(raw) };
  const match = QUALIFIER_REGEX.exec(raw);
  if (!match) {
    return textToken;
  }
  const [, minus, qualifierRaw, valueRaw] = match;
  const value = unquote(valueRaw);
  const qualifier = qualifierRaw.toLowerCase();
  const isReserved = (RESERVED_QUALIFIERS as readonly string[]).includes(
    qualifier
  );
  const languageTag = isReserved
    ? undefined
    : languageTags.find((tag) => tag.toLowerCase() === qualifier);
  if (!isReserved && languageTag === undefined) {
    return textToken;
  }
  // a qualifier the user is still typing the value for must not filter anything
  if (!value.trim()) {
    return { type: 'ignored', raw };
  }
  if (isReserved) {
    return {
      type: 'scoped',
      qualifier: qualifier as ReservedQualifier,
      negated: minus === '-',
      value,
    };
  }
  return {
    type: 'scoped',
    qualifier: 'language',
    languageTag,
    negated: minus === '-',
    value,
  };
}

/**
 * Parses a translations search query with GitHub-style scoped qualifiers
 * (`key:cart*`, `description:"legal text"`, `de:Warenkorb`, `-namespace:web`).
 * Anything not recognized as a qualifier stays a plain text token.
 */
export function parseSearchQuery(
  input: string,
  languageTags: string[]
): ParsedSearch {
  const tokens = splitRawTokens(input).map((raw) =>
    parseToken(raw, languageTags)
  );
  return {
    tokens,
    hasScopedTokens: tokens.some((token) => token.type !== 'text'),
  };
}
