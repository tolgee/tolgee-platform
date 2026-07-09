export type UrlTemplateVars = {
  keyName?: string;
  keyNamespace?: string;
  keyId?: number;
  projectId?: number;
  languageTag?: string;
  languageId?: number;
  translationId?: number;
};

const TOKEN_PATTERN = /\{([a-zA-Z]+)\}/g;
const KNOWN_TOKENS: ReadonlyArray<keyof UrlTemplateVars> = [
  'keyName',
  'keyNamespace',
  'keyId',
  'projectId',
  'languageTag',
  'languageId',
  'translationId',
];
const warnedTemplates = new Set<string>();

export function substituteUrlTemplate(
  template: string,
  vars: UrlTemplateVars
): string {
  return template.replace(TOKEN_PATTERN, (match, token: string) => {
    if (!(KNOWN_TOKENS as readonly string[]).includes(token)) {
      if (!warnedTemplates.has(template)) {
        warnedTemplates.add(template);
        // eslint-disable-next-line no-console
        console.warn(
          `[tolgee-apps] unknown URL template token "${token}" in "${template}"; leaving as-is`
        );
      }
      return match;
    }
    const value = vars[token as keyof UrlTemplateVars];
    if (value === undefined || value === null) return match;
    return encodeURIComponent(String(value));
  });
}
