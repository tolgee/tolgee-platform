import { addons } from 'storybook/preview-api';
import type { StoryContext } from 'storybook/internal/types';
import type {
  TolgeePlugin,
  TolgeeChainer,
  TolgeeInstance,
  TolgeeOptions,
} from '@tolgee/core';
import { FormatIcu } from '@tolgee/format-icu';

import {
  GLOBAL_KEY,
  PARAM_KEY,
  PARAM_LANGUAGE_KEY,
  LANGUAGES_CONFIG_EVENT,
} from '../constants';
import type { Locales, TolgeeAddonState } from '../types';

/** Make `language` required and remove `defaultLanguage`. */
export type RequireLanguageNoDefault<T extends TolgeeOptions> = Omit<
  T,
  'language' | 'defaultLanguage'
> & {
  language: NonNullable<T['language']>;
};

export interface SharedAddonOptions {
  /** Adds friendly names and flags to `availableLanguages` in Storybook Tolgee addon. */
  locales?: Locales;
  /** Translated string format: either `'simple'` or `'icu'`. Default is `'simple'`. */
  messageFormat?: 'simple' | 'icu';
}

function arrayToObject(arr: string[]) {
  return Object.fromEntries(arr.map((lang) => [lang, { name: lang }]));
}

function availableLocales(config: TolgeeOptions, locales?: Locales): Locales {
  if (locales) {
    return locales;
  } else if (config.availableLanguages) {
    return arrayToObject(config.availableLanguages);
  } else if (config.staticData) {
    const languagesFromStaticData = Object.keys(config.staticData).map(
      (key) => key.split(':')[0],
    );
    return arrayToObject(Array.from(new Set(languagesFromStaticData)));
  }
  return {};
}

export function initMenuSwitcher({
  config,
  locales,
}: {
  config: RequireLanguageNoDefault<TolgeeOptions>;
  locales?: Locales;
}) {
  addons.getChannel().emit(LANGUAGES_CONFIG_EVENT, {
    language: config.language,
    locales: availableLocales(config, locales),
  } satisfies TolgeeAddonState);
}

export function initTolgee({
  config,
  messageFormat,
  Tolgee,
  DevTools,
  FormatSimple,
}: {
  config: RequireLanguageNoDefault<TolgeeOptions>;
  messageFormat?: SharedAddonOptions['messageFormat'];
  Tolgee: () => TolgeeChainer;
  DevTools: () => TolgeePlugin;
  FormatSimple: () => TolgeePlugin;
}) {
  return Tolgee()
    .use(DevTools())
    .use(messageFormat === 'icu' ? FormatIcu() : FormatSimple())
    .init(config);
}

export function changeLanguage(
  tolgee: TolgeeInstance,
  context: StoryContext,
  config: RequireLanguageNoDefault<TolgeeOptions>,
) {
  const languageFromParameters =
    context.parameters[PARAM_KEY]?.[PARAM_LANGUAGE_KEY];
  const languageFromGlobals = context.globals[GLOBAL_KEY];
  const languageFromConfig = config.language;
  const language =
    languageFromParameters || languageFromGlobals || languageFromConfig;

  tolgee.changeLanguage(language);
}
