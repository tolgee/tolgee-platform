import React from 'react';
import {
  IconButton,
  TooltipLinkList,
  WithTooltip,
} from 'storybook/internal/components';
import {
  addons,
  useAddonState,
  useChannel,
  useGlobals,
  useParameter,
} from 'storybook/manager-api';
import { styled } from 'storybook/theming';
import { GlobeIcon } from '@storybook/icons';

import {
  TOOL_ID,
  GLOBAL_KEY,
  PARAM_KEY,
  PARAM_LANGUAGE_KEY,
  PARAM_DISABLE_KEY,
  LANGUAGES_CONFIG_EVENT,
} from './constants';
import type {
  TolgeeAddonParameters as Parameters,
  TolgeeAddonState,
} from './types';

type TolgeeAddonParameters = NonNullable<Parameters[typeof PARAM_KEY]>;

const IconButtonLabel = styled.div(({ theme }) => ({
  fontSize: theme.typography.size.s2 - 1,
}));

export const MenuSwitcher = React.memo(function MenuSwitcher() {
  const {
    [PARAM_LANGUAGE_KEY]: languageFromParameters,
    [PARAM_DISABLE_KEY]: disable,
  } = useParameter<TolgeeAddonParameters>(PARAM_KEY, {});
  const [
    { [GLOBAL_KEY]: languageFromGlobals },
    updateGlobals,
    globalsFromStory,
  ] = useGlobals();

  const priorAddonConfig = addons
    .getChannel()
    .last(LANGUAGES_CONFIG_EVENT)?.[0];
  const initAddonState = {
    language: priorAddonConfig?.language || undefined,
    locales: priorAddonConfig?.locales || {},
  } satisfies TolgeeAddonState;

  const [{ language: languageFromConfig, locales }, updateState] =
    useAddonState<TolgeeAddonState>(TOOL_ID, initAddonState);

  const isLocked = GLOBAL_KEY in globalsFromStory || !!languageFromParameters;

  useChannel({
    [LANGUAGES_CONFIG_EVENT]: ({ language, locales }) => {
      updateState((state) => ({
        ...state,
        language,
        locales,
      }));
    },
  });

  const language = languageFromGlobals || languageFromConfig;
  let label = '';
  if (isLocked) {
    label = 'Story override';
  } else if (locales[language]) {
    label = `${locales[language].name || language} ${locales[language].flag || ''}`;
  }

  if (disable) {
    return null;
  }

  if (Object.keys(locales).length === 2) {
    const languageAlternate = Object.keys(locales).find(
      (lang) => lang !== language,
    );
    return (
      <IconButton
        disabled={isLocked}
        key={TOOL_ID}
        active={!languageFromParameters}
        title="Language"
        onClick={() => {
          updateGlobals({ [GLOBAL_KEY]: languageAlternate });
        }}
      >
        <GlobeIcon />
        {label ? <IconButtonLabel>{label}</IconButtonLabel> : null}
      </IconButton>
    );
  }

  if (Object.keys(locales).length > 2) {
    return (
      <WithTooltip
        placement="top"
        trigger="click"
        closeOnOutsideClick
        tooltip={({ onHide }) => {
          return (
            <TooltipLinkList
              links={Object.keys(locales).map((lang) => ({
                id: lang,
                title: locales[lang].name || lang,
                right: locales[lang].flag,
                active: languageFromGlobals === lang,
                onClick: () => {
                  updateGlobals({ [GLOBAL_KEY]: lang });
                  onHide();
                },
              }))}
            />
          );
        }}
      >
        <IconButton
          key={TOOL_ID}
          active={!languageFromParameters}
          title="Tolgee Language"
          disabled={isLocked}
        >
          <GlobeIcon />
          {label && <IconButtonLabel>{label}</IconButtonLabel>}
        </IconButton>
      </WithTooltip>
    );
  }

  return null;
});
