export const RESERVED_QUALIFIERS = [
  'key',
  'description',
  'namespace',
  'translation',
] as const;

export type ReservedQualifier = (typeof RESERVED_QUALIFIERS)[number];

const QUALIFIER_NAME_CHARS = '[A-Za-z0-9_@-]';

export const QUALIFIER_NAME_REGEX = new RegExp(`^${QUALIFIER_NAME_CHARS}+$`);

const QUALIFIER_TOKEN_REGEX = new RegExp(
  `^(-?)(${QUALIFIER_NAME_CHARS}+):(.*)$`,
  // `s` so a quoted value that slipped a newline past the field is still captured whole
  's'
);

export function isReservedQualifier(name: string): boolean {
  return (RESERVED_QUALIFIERS as readonly string[]).includes(name);
}

export function findLanguageTag(
  languageTags: string[],
  name: string
): string | undefined {
  const lower = name.toLowerCase();
  return languageTags.find((tag) => tag.toLowerCase() === lower);
}

/** a tag that is a legal qualifier name and not shadowed by a reserved qualifier */
export function canLanguageScope(tag: string): boolean {
  return (
    QUALIFIER_NAME_REGEX.test(tag) && !isReservedQualifier(tag.toLowerCase())
  );
}

type TokenPosition = {
  from: number;
  /** end offset in the input (exclusive) */
  to: number;
};

export type TextToken = TokenPosition & {
  type: 'text';
  raw: string;
  value: string;
};

export type IgnoredToken = TokenPosition & {
  type: 'ignored';
  raw: string;
};

export type ScopedToken = TokenPosition & {
  type: 'scoped';
  raw: string;
  /** the value was quoted — it is a literal phrase, never shorthand syntax */
  quoted: boolean;
  qualifier: ReservedQualifier | 'language';
  /** canonical project language tag, set when qualifier === 'language' */
  languageTag?: string;
  negated: boolean;
  value: string;
};

export type ParsedToken = TextToken | IgnoredToken | ScopedToken;

export type ParsedSearch = {
  tokens: ParsedToken[];
  hasRecognizedQualifiers: boolean;
};

type RawToken = TokenPosition & { raw: string };

/**
 * Splits the input into whitespace-separated raw tokens. A `"` starts a quoted
 * run in which whitespace does not split; an unterminated quote runs to the end
 * of the input.
 */
function splitRawTokens(input: string): RawToken[] {
  const tokens: RawToken[] = [];
  let current = '';
  let from = 0;
  let inQuote = false;
  for (let i = 0; i < input.length; i++) {
    const char = input[i];
    if (char === '"') {
      inQuote = !inQuote;
      current += char;
      continue;
    }
    if (!inQuote && /\s/.test(char)) {
      if (current) {
        tokens.push({ raw: current, from, to: i });
        current = '';
      }
      from = i + 1;
      continue;
    }
    if (!current) {
      from = i;
    }
    current += char;
  }
  if (current) {
    tokens.push({ raw: current, from, to: input.length });
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

function parseToken(token: RawToken, languageTags: string[]): ParsedToken {
  const { raw, from, to } = token;
  const textToken: TextToken = {
    type: 'text',
    raw,
    value: unquote(raw),
    from,
    to,
  };
  const match = QUALIFIER_TOKEN_REGEX.exec(raw);
  if (!match) {
    return textToken;
  }
  const [, minus, qualifierRaw, valueRaw] = match;
  const quoted = valueRaw.startsWith('"');
  const value = unquote(valueRaw);
  const qualifier = qualifierRaw.toLowerCase();
  const isReserved = isReservedQualifier(qualifier);
  const languageTag = isReserved
    ? undefined
    : findLanguageTag(languageTags, qualifier);
  if (!isReserved && languageTag === undefined) {
    return textToken;
  }
  if (!value.trim()) {
    return { type: 'ignored', raw, from, to };
  }
  return {
    type: 'scoped',
    raw,
    quoted,
    qualifier: isReserved ? (qualifier as ReservedQualifier) : 'language',
    languageTag,
    negated: minus === '-',
    value,
    from,
    to,
  };
}

export function parseSearchQuery(
  input: string,
  languageTags: string[]
): ParsedSearch {
  const tokens = splitRawTokens(input).map((token) =>
    parseToken(token, languageTags)
  );
  return {
    tokens,
    hasRecognizedQualifiers: tokens.some((token) => token.type !== 'text'),
  };
}
