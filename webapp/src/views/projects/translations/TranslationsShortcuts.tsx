import React, { useEffect, useState } from 'react';
import { styled, keyframes, Typography } from '@mui/material';
import { alpha } from '@mui/material/styles';
import { useDebouncedCallback } from 'use-debounce';
import { Close, Help } from '@mui/icons-material';
import { T } from '@tolgee/react';

import { stopBubble } from 'tg.fixtures/eventHandler';
import { formatShortcut } from 'tg.fixtures/shortcuts';
import { useHideShortcuts } from 'tg.hooks/useHideShortcuts';
import {
  KEY_MAP,
  ShortcutsArrayType,
  useTranslationsShortcuts,
} from './context/shortcuts/useTranslationsShortcuts';
import clsx from 'clsx';

import { useTranslationsSelector } from './context/TranslationsContext';
import { getMetaName, IS_MAC } from 'tg.fixtures/isMac';
import { TRANSLATION_STATES } from 'tg.constants/translationStates';
import { getCurrentlyFocused } from './context/shortcuts/tools';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';

const StyledContainer = styled('div')`
  position: absolute;
  display: flex;
  bottom: 0px;
  right: 0px;
  width: 100%;
  transition: all 300ms ease-in-out;
  flex-shrink: 1;

  &.collapsed {
    background: transparent;
    width: 50px;
    text-overflow: clip;
    pointer-events: none;
  }
`;

const StyledItems = styled('div')`
  display: flex;
  flex-shrink: 1;
  opacity: 0.5;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  transition: all 300ms ease-in-out;
  & > * + * {
    margin-left: ${({ theme }) => theme.spacing(2)};
  }
`;

const easeIn = keyframes`
  0% {
    opacity: 0;
  }
`;

const StyledContent = styled('div')`
  display: flex;
  align-items: center;
  box-sizing: border-box;
  transition: background-color 300ms ease-in-out, visibility 0ms;
  padding: ${({ theme }) => theme.spacing(0, 1, 0, 2)};
  pointer-events: all;
  border-radius: 6px;
  height: 40px;
  max-width: 100%;
  background-color: ${({ theme }) =>
    alpha(theme.palette.background.paper, 0.9)};

  @supports (backdrop-filter: blur()) or (-webkit-backdrop-filter: blur()) or
    (-moz-backdrop-filter: blur()) {
    background-color: ${({ theme }) =>
      alpha(theme.palette.background.paper, 0.5)};
    -webkit-backdrop-filter: blur(7px);
    -moz-backdrop-filter: blur(7px);
    backdrop-filter: blur(7px);
  }
  -webkit-box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.25);
  box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.25);

  .icon {
    opacity: 0.5;
    cursor: pointer;
    margin-left: ${({ theme }) => theme.spacing(1)};
    transition: all 300ms ease-in-out;
    animation: ${easeIn} 0.2s ease-in;
    pointer-events: all;
  }

  .hoverHidden {
    opacity: 0.2;
  }

  &:hover .icon {
    opacity: 1;
  }

  &:hover .items {
    opacity: 1;
  }

  &:hover .hoverHidden {
    opacity: 1;
  }

  &.contentEmpty {
    opacity: 0;
  }

  &.contentCollapsed {
    -webkit-backdrop-filter: none;
    backdrop-filter: none;
    background: transparent;
    -webkit-box-shadow: none;
    box-shadow: none;
    pointer-events: none;
  }
`;

const StyledItem = styled('span')`
  display: flex;
  align-items: center;
  gap: 2px;
  animation: ${easeIn} 0.2s ease-in;
  & > * + * {
    margin-left: 0.4em;
  }
`;

const StyledItemContent = styled(Typography)`
  font-size: 15px;
`;

const StyledHelp = styled(Help)`
  opacity: 0.5;
  cursor: pointer;
  margin-left: ${({ theme }) => theme.spacing(1)};
  transition: all 300ms ease-in-out;
  animation: ${easeIn} 0.2s ease-in;
  pointer-events: all;
`;

const StyledClose = styled(Close)`
  opacity: 0.5;
  cursor: pointer;
  margin-left: ${({ theme }) => theme.spacing(1)};
  transition: all 300ms ease-in-out;
  animation: ${easeIn} 0.2s ease-in;
  pointer-events: all;

  .hoverHidden {
    opacity: 0.2;
  }
`;

export const TranslationsShortcuts = () => {
  const [collapsed, setCollapsed] = useHideShortcuts();

  const toggleCollapse = () => {
    setCollapsed(!collapsed);
  };

  const cursorKeyId = useTranslationsSelector((c) => c.cursor?.keyId);
  const cursorLanguage = useTranslationsSelector((c) => c.cursor?.language);
  const cursorMode = useTranslationsSelector((c) => c.cursor?.mode);

  const translations = useTranslationsSelector((c) => c.translations);
  const baseLanguage = useTranslationsSelector((c) =>
    c.languages?.find((l) => l.base)
  );

  const elementsRef = useTranslationsSelector((c) => c.elementsRef);

  const [availableActions, setAvailableActions] = useState<
    ShortcutsArrayType[]
  >([]);
  const { getAvailableActions } = useTranslationsShortcuts();
  const { satisfiesLanguageAccess } = useProjectPermissions();

  const onFocusChange = useDebouncedCallback(
    () => {
      setAvailableActions(getAvailableActions());
    },
    100,
    { maxWait: 200 }
  );

  useEffect(() => {
    onFocusChange();
    document.body.addEventListener('focus', onFocusChange, true);
    document.body.addEventListener('blur', onFocusChange, true);
    return () => {
      document.body.removeEventListener('focus', onFocusChange, true);
      document.body.removeEventListener('blur', onFocusChange, true);
    };
  }, [useDebouncedCallback]);

  const getCellNextState = (keyId: number, language: string | undefined) => {
    if (language) {
      const state = translations?.find((t) => t.keyId === keyId)?.translations[
        language
      ]?.state;
      return state && TRANSLATION_STATES[state]?.next;
    }
  };

  const getActionTranslation = (action: keyof typeof KEY_MAP) => {
    switch (action) {
      case 'CHANGE_STATE': {
        const focusedCell = getCurrentlyFocused(elementsRef.current);
        const nextState =
          focusedCell &&
          getCellNextState(focusedCell.keyId, focusedCell.language);
        return nextState && TRANSLATION_STATES[nextState]?.translation;
      }
      case 'MOVE':
        return <T keyName="translations_shortcuts_move" />;
      case 'EDIT':
        return <T keyName="translations_cell_edit" />;
    }
  };

  const cursorKeyIdNextState =
    cursorKeyId && getCellNextState(cursorKeyId, cursorLanguage);

  const getEditorShortcuts = () => [
    {
      name: <T keyName="translations_cell_save" />,
      formula: formatShortcut('Enter'),
    },
    {
      name: <T keyName="translations_cell_save_and_continue" />,
      formula: formatShortcut(`${getMetaName()} + Enter`),
    },
    {
      name:
        cursorKeyIdNextState &&
        TRANSLATION_STATES[cursorKeyIdNextState]?.translation,
      formula: formatShortcut(`${getMetaName()} + E`),
    },
    {
      name: cursorLanguage != baseLanguage?.tag &&
        satisfiesLanguageAccess('translations.view', baseLanguage?.id) && (
          <T keyName="translations_cell_insert_base" />
        ),
      formula: IS_MAC
        ? formatShortcut(`${getMetaName()} + Shift + S`)
        : formatShortcut(`${getMetaName()} + Insert`),
    },
  ];

  const editorIsActive =
    cursorMode === 'editor' &&
    cursorLanguage &&
    document.activeElement?.className === 'CodeMirror-code';

  const items = (
    (editorIsActive
      ? getEditorShortcuts()
      : availableActions.map(([action, keys]) => ({
          name: getActionTranslation(action as any),
          formula: keys.map((f, i) => (
            <React.Fragment key={i}>{formatShortcut(f)}</React.Fragment>
          )),
        }))) as any[]
  ).filter((i) => i.name);

  return (
    <StyledContainer className={clsx({ collapsed })} onMouseDown={stopBubble()}>
      <StyledContent
        className={clsx({
          contentCollapsed: collapsed,
          contentEmpty: !collapsed && !items.length,
        })}
      >
        <StyledItems className="items">
          {!collapsed &&
            items.map((item, i) => {
              return (
                <StyledItem key={i}>
                  <StyledItemContent
                    variant="inherit"
                    data-cy="translations-shortcuts-command"
                  >
                    {item.name}
                  </StyledItemContent>
                  <StyledItemContent variant="inherit">
                    {item.formula}
                  </StyledItemContent>
                </StyledItem>
              );
            })}
        </StyledItems>
        {!collapsed ? (
          <StyledClose
            className="hoverHidden"
            onClick={stopBubble(toggleCollapse)}
          />
        ) : (
          <StyledHelp onClick={stopBubble(toggleCollapse)} />
        )}
      </StyledContent>
    </StyledContainer>
  );
};
