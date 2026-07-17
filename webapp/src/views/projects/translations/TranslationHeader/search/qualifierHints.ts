import { canLanguageScope } from '../../searchQuery/parseSearchQuery';

export const QUALIFIER_HINTS = {
  key: {
    // @tolgee-key translations_search_help_key
    keyName: 'translations_search_help_key',
    defaultValue: 'Search only in key names',
  },
  description: {
    // @tolgee-key translations_search_help_description
    keyName: 'translations_search_help_description',
    defaultValue: 'Search only in key descriptions',
  },
  namespace: {
    // @tolgee-key translations_search_help_namespace
    keyName: 'translations_search_help_namespace',
    defaultValue: 'Search only in namespaces',
  },
  translation: {
    // @tolgee-key translations_search_help_translation
    keyName: 'translations_search_help_translation',
    defaultValue: 'Search only in translation texts',
  },
  language: {
    // @tolgee-key translations_search_help_language
    keyName: 'translations_search_help_language',
    defaultValue: 'Search only in the "{tag}" language',
  },
} as const;

export const pickLanguageExampleTag = (languageTags: string[]): string =>
  languageTags.find(canLanguageScope) ?? 'de';
