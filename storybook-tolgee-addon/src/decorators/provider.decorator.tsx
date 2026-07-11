import React from 'react';
import type {
  DecoratorFunction,
  PartialStoryFn,
  Renderer,
  StoryContext,
} from 'storybook/internal/types';
import {
  DevTools,
  FormatSimple,
  Tolgee,
  type TolgeeOptions,
  TolgeeProvider,
} from '@tolgee/react';

import {
  initMenuSwitcher,
  initTolgee,
  SharedAddonOptions,
  changeLanguage,
  type RequireLanguageNoDefault,
} from './helpers';

export interface ProviderOptions extends SharedAddonOptions {
  /** Good old Tolgee configuration options. `language` is required. */
  tolgee: RequireLanguageNoDefault<TolgeeOptions>;
  /** LocalizationProvider to decorate the story. */
  LocalizationProvider?: React.ComponentType<
    React.PropsWithChildren<Record<string, unknown>>
  >;
}

export const withTolgeeProvider = <TRenderer extends Renderer>({
  tolgee: config,
  locales,
  messageFormat = 'simple',
  LocalizationProvider,
}: ProviderOptions): DecoratorFunction<TRenderer> => {
  initMenuSwitcher({ config, locales });

  return function TolgeeDecorator(
    storyFn: PartialStoryFn<Renderer>,
    context: StoryContext,
  ) {
    const tolgee = initTolgee({
      config,
      messageFormat,
      Tolgee,
      DevTools,
      FormatSimple,
    });

    changeLanguage(tolgee, context, config);

    return (
      <TolgeeProvider tolgee={tolgee} fallback="Loading...">
        {LocalizationProvider ? (
          <LocalizationProvider>{storyFn(context)}</LocalizationProvider>
        ) : (
          storyFn(context)
        )}
      </TolgeeProvider>
    );
  };
};
