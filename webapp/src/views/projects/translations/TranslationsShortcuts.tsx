import React, { useEffect, useState } from 'react';
import { makeStyles, Typography } from '@material-ui/core';
import { alpha } from '@material-ui/core/styles/colorManipulator';
import { useDebouncedCallback } from 'use-debounce/lib';
import { Close, Help } from '@material-ui/icons';
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
import { getMetaName } from 'tg.fixtures/isMac';
import { translationStates } from 'tg.constants/translationStates';
import { getCurrentlyFocused } from './context/shortcuts/tools';

const useStyles = makeStyles((theme) => ({
  '@keyframes easeIn': {
    '0%': {
      opacity: 0,
    },
  },
  container: {
    position: 'absolute',
    display: 'flex',
    bottom: 0,
    right: 0,
    width: '100%',
    transition: 'all 300ms ease-in-out',
    flexShrink: 1,
  },
  containerCollapsed: {
    background: 'transparent',
    width: 50,
    textOverflow: 'clip',
    pointerEvents: 'none',
  },
  content: {
    display: 'flex',
    alignItems: 'center',
    boxSizing: 'border-box',
    transition: 'background 300ms ease-in-out, visibility 0ms',
    padding: theme.spacing(0, 1, 0, 2),
    pointerEvents: 'all',
    borderRadius: 6,
    height: 40,
    maxWidth: '100%',
    background: alpha(theme.palette.extraLightBackground.main, 0.9),
    '@supports (backdrop-filter: blur()) or (-webkit-backdrop-filter: blur()) or (-moz-backdrop-filter: blur())':
      {
        background: alpha(theme.palette.extraLightBackground.main, 0.5),
        '-webkit-backdrop-filter': 'blur(7px)',
        '-moz-backdrop-filter': 'blur(7px)',
        backdropFilter: 'blur(7px)',
      },
    '-webkit-box-shadow': '2px 2px 5px rgba(0, 0, 0, 0.25)',
    'box-shadow': '2px 2px 5px rgba(0, 0, 0, 0.25)',
    '&:hover $icon': {
      opacity: 1,
    },
    '&:hover $items': {
      opacity: 1,
    },
    '&:hover $hoverHidden': {
      opacity: 1,
    },
  },
  contentEmpty: {
    opacity: 0,
  },
  contentCollapsed: {
    '-webkit-backdrop-filter': 'none',
    backdropFilter: 'none',
    background: 'transparent',
    '-webkit-box-shadow': 'none',
    'box-shadow': 'none',
    pointerEvents: 'none',
  },
  items: {
    flexShrink: 1,
    opacity: 0.5,
    overflow: 'hidden',
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis',
    transition: 'all 300ms ease-in-out',
    height: 22,
    '& > * + *': {
      marginLeft: theme.spacing(2),
    },
  },
  item: {
    display: 'inline',
    animationName: '$easeIn',
    animationDuration: '0.2s',
    animationTimingFunction: 'ease-in',
    '& > * + *': {
      marginLeft: '0.4em',
    },
  },
  icon: {
    opacity: 0.5,
    cursor: 'pointer',
    marginLeft: theme.spacing(1),
    transition: 'all 300ms ease-in-out',
    animationName: '$easeIn',
    animationDuration: '0.5s',
    animationTimingFunction: 'ease-in',
    pointerEvents: 'all',
  },
  hoverHidden: {
    opacity: 0.2,
  },
}));

export const TranslationsShortcuts = () => {
  const classes = useStyles();
  const [collapsed, setCollapsed] = useHideShortcuts();

  const toggleCollapse = () => {
    setCollapsed(!collapsed);
  };

  const cursorKeyId = useTranslationsSelector((c) => c.cursor?.keyId);
  const cursorLanguage = useTranslationsSelector((c) => c.cursor?.language);
  const cursorMode = useTranslationsSelector((c) => c.cursor?.mode);

  const translations = useTranslationsSelector((c) => c.translations);

  const elementsRef = useTranslationsSelector((c) => c.elementsRef);

  const [availableActions, setAvailableActions] = useState<
    ShortcutsArrayType[]
  >([]);
  const { getAvailableActions } = useTranslationsShortcuts();

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
      return (state && translationStates[state]?.next) || 'TRANSLATED';
    }
  };

  const getActionTranslation = (action: keyof typeof KEY_MAP) => {
    switch (action) {
      case 'CHANGE_STATE': {
        const focusedCell = getCurrentlyFocused(elementsRef.current);
        const nextState =
          focusedCell &&
          getCellNextState(focusedCell.keyId, focusedCell.language);
        return (
          nextState &&
          translationStates[nextState] && (
            <T>{translationStates[nextState].translationKey}</T>
          )
        );
      }
      case 'MOVE':
        return <T>translations_shortcuts_move</T>;
      case 'EDIT':
        return <T>translations_cell_edit</T>;
    }
  };

  const cursorKeyIdNextState =
    cursorKeyId && getCellNextState(cursorKeyId, cursorLanguage);

  const getEditorShortcuts = () => [
    {
      name: <T>translations_cell_save</T>,
      formula: formatShortcut('Enter'),
    },
    {
      name: <T>translations_cell_save_and_continue</T>,
      formula: formatShortcut(`${getMetaName()} + Enter`),
    },
    {
      name: cursorKeyIdNextState && translationStates[cursorKeyIdNextState] && (
        <T>{translationStates[cursorKeyIdNextState].translationKey}</T>
      ),
      formula: formatShortcut(`${getMetaName()} + E`),
    },
  ];

  const editorIsActive =
    cursorMode === 'editor' &&
    document.activeElement?.className === 'CodeMirror-code';

  const items = (
    editorIsActive
      ? getEditorShortcuts()
      : availableActions.map(([action, keys]) => ({
          name: getActionTranslation(action as any),
          formula: keys.map((f, i) => (
            <React.Fragment key={i}>{formatShortcut(f)}</React.Fragment>
          )),
        }))
  ).filter((i) => i.name);

  return (
    <div
      className={clsx(
        classes.container,
        collapsed ? classes.containerCollapsed : undefined
      )}
      onMouseDown={stopBubble()}
    >
      <div
        className={clsx(
          classes.content,
          collapsed
            ? classes.contentCollapsed
            : !items.length
            ? classes.contentEmpty
            : undefined
        )}
      >
        <div className={classes.items}>
          {!collapsed &&
            items.map((item, i) => {
              return (
                <span className={classes.item} key={i}>
                  <Typography
                    variant="inherit"
                    data-cy="translations-shortcuts-command"
                  >
                    {item.name}
                  </Typography>
                  <Typography variant="inherit">{item.formula}</Typography>
                </span>
              );
            })}
        </div>
        {!collapsed ? (
          <Close
            className={clsx(classes.icon, classes.hoverHidden)}
            onClick={stopBubble(toggleCollapse)}
          />
        ) : (
          <Help className={classes.icon} onClick={stopBubble(toggleCollapse)} />
        )}
      </div>
    </div>
  );
};
