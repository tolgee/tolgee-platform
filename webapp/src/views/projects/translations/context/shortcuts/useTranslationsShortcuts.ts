import { useCallback, useRef } from 'react';

import { TRANSLATION_STATES } from 'tg.constants/translationStates';
import { getEventAction } from 'tg.fixtures/shortcuts';
import {
  ARROWS,
  getCurrentlyFocused,
  serializeElPosition,
  translationsNavigator,
} from './tools';
import {
  useTranslationsActions,
  useTranslationsSelector,
} from '../TranslationsContext';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { CellPosition } from '../types';
import { getMeta } from 'tg.fixtures/isMac';
import { isElementInput } from 'tg.fixtures/isElementInput';

export const KEY_MAP = {
  MOVE: ARROWS,
  EDIT: ['Enter'],
  CANCEL: ['Escape'],
  CHANGE_STATE: [`${getMeta()} + E`],
} as const;

export type ShortcutsArrayType = [
  action: string,
  shortcut: string[] | ReadonlyArray<string>
];

export const useTranslationsShortcuts = () => {
  const root = document.getElementById('root');
  const onKeyRef = useRef<(e: KeyboardEvent) => void>();
  const availableActions = useRef<() => ShortcutsArrayType[]>();
  const { setEdit, setTranslationState } = useTranslationsActions();
  const cursorKeyId = useTranslationsSelector((c) => c.cursor?.keyId);
  const cursorLanguage = useTranslationsSelector((c) => c.cursor?.language);
  const view = useTranslationsSelector((c) => c.view);
  const { satisfiesPermission, satisfiesLanguageAccess } =
    useProjectPermissions();
  const elementsRef = useTranslationsSelector((c) => c.elementsRef);
  const fixedTranslations = useTranslationsSelector((c) => c.translations);
  const allLanguages = useTranslationsSelector((c) => c.languages);
  const languages = useTranslationsSelector((c) => c.translationsLanguages);
  const list = useTranslationsSelector((c) => c.reactList);

  const hasCorrectTarget = (target: Element) =>
    target === document.body || root?.contains(target);

  const canEditKey = satisfiesPermission('keys.edit');

  const isTranslation = (position: CellPosition | undefined) =>
    position?.language;

  const getLanguageId = (langTag?: string) => {
    return allLanguages?.find((l) => l.tag === langTag)?.id;
  };

  const getCurrentKey = () => {
    const focused = getCurrentlyFocused(elementsRef.current);
    if (focused?.language && focused.keyId) {
      return fixedTranslations?.find((t) => t.keyId === focused.keyId);
    }
  };

  const getCurrentTranslation = () => {
    const focused = getCurrentlyFocused(elementsRef.current);
    if (focused?.language && focused.keyId) {
      return fixedTranslations?.find((t) => t.keyId === focused.keyId)
        ?.translations[focused.language];
    }
  };

  const getMoveHandler = () => {
    return (e: KeyboardEvent) => {
      e.preventDefault();
      const navigator = translationsNavigator(
        fixedTranslations,
        languages,
        elementsRef.current,
        view,
        list?.getVisibleRange.bind(list)()
      );
      const nextLocation = navigator.getNextLocation(e.key as any);
      if (nextLocation) {
        const el = elementsRef.current?.get(serializeElPosition(nextLocation));
        if (el !== document.activeElement) {
          el?.focus();
          el?.scrollIntoView({
            block: 'center',
            inline: 'center',
            behavior: 'smooth',
          });
        }
      }
    };
  };

  const getEnterHandler = () => {
    const focused = getCurrentlyFocused(elementsRef.current);
    if (focused) {
      const translation = getCurrentTranslation();
      const keyData = getCurrentKey();
      const firstTask = keyData?.tasks?.find(
        (t) => t.languageTag === focused.language
      );
      const canTranslate =
        satisfiesLanguageAccess(
          'translations.edit',
          getLanguageId(focused.language)
        ) ||
        (firstTask?.userAssigned && firstTask.type === 'TRANSLATE');
      if (
        (isTranslation(focused) && canTranslate) ||
        (!isTranslation(focused) && canEditKey)
      ) {
        if (translation?.state === 'DISABLED') {
          return;
        }
        return (e: KeyboardEvent) => {
          e.preventDefault();
          setEdit({
            keyId: focused.keyId,
            language: focused.language,
          });
        };
      }
    }
  };

  const getCancelHandler = () => {
    if (cursorKeyId) {
      setEdit(undefined);
      return;
    }
    const focused = getCurrentlyFocused(elementsRef.current);
    if (focused) {
      // @ts-ignore
      return (e: KeyboardEvent) => document.activeElement.blur?.();
    }
  };

  const getChangeStateHandler = () => {
    const focused = getCurrentlyFocused(elementsRef.current);
    const translation = fixedTranslations?.find(
      (t) => t.keyId === focused?.keyId
    )?.translations[focused?.language || ''];
    const keyData = getCurrentKey();
    const firstTask = keyData?.tasks?.find(
      (t) => t.languageTag === focused?.language
    );
    const canTranslate =
      satisfiesLanguageAccess(
        'translations.state-edit',
        getLanguageId(focused?.language)
      ) ||
      (firstTask?.userAssigned && firstTask.type === 'REVIEW');

    if (focused?.language && canTranslate) {
      const newState =
        translation?.state && TRANSLATION_STATES[translation.state]?.next;

      if (translation && newState) {
        return (e: KeyboardEvent) => {
          e.preventDefault();
          setTranslationState({
            keyId: focused.keyId,
            language: focused.language as string,
            translationId: translation.id,
            state: newState,
          });
        };
      }
    }

    // trigger 'preventDefault' even if handler is not active
    return true;
  };

  const getHandler = (
    action: keyof typeof KEY_MAP
  ): ((e: KeyboardEvent) => void) | undefined | true => {
    switch (action) {
      case 'MOVE':
        return getMoveHandler();
      case 'EDIT':
        return getEnterHandler();
      case 'CANCEL':
        return getCancelHandler();
      case 'CHANGE_STATE':
        return getChangeStateHandler();
    }
  };

  const isStateCorrect = () => {
    const activeElement = document.activeElement || document.body;

    // check if events are not coming from popup
    if (!hasCorrectTarget(activeElement)) {
      return false;
    }

    // if editor is open, don't apply shortcuts
    if (cursorKeyId) {
      const current = getCurrentlyFocused(elementsRef.current);
      if (
        current?.keyId === cursorKeyId &&
        current?.language === cursorLanguage
      ) {
        return false;
      }
    }

    // ignore events coming from inputs
    // as we don't want to influence them
    if (isElementInput(activeElement)) {
      return false;
    }

    return true;
  };

  onKeyRef.current = (e: KeyboardEvent) => {
    const action = getEventAction(e, KEY_MAP);

    if (action) {
      const handler = getHandler(action);
      if (handler === true) {
        e.preventDefault();
      } else if (typeof handler === 'function' && isStateCorrect()) {
        handler(e);
      }
    }
  };

  availableActions.current = () => {
    if (!isStateCorrect()) {
      return [];
    }
    return Object.entries(KEY_MAP).filter(
      ([action]) => typeof getHandler(action as any) === 'function'
    );
  };

  const getAvailableActions = useCallback(
    () => availableActions.current?.() || [],
    [availableActions]
  );

  const onKey = useCallback((e: KeyboardEvent) => onKeyRef.current?.(e), []);

  return { getAvailableActions, onKey };
};
