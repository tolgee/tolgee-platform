import { useEffect, useMemo, useState } from 'react';
import { Direction, Edit, EditorProps } from '../types';
import {
  generateCurrentValue,
  serializeVariants,
  updateReactListSizes,
} from './utils';
import { useTranslationsService } from './useTranslationsService';
import { useRefsService } from './useRefsService';
import { useDebounce } from 'use-debounce';
import { components } from 'tg.service/apiSchema.generated';
import { getTolgeeFormat } from '@tginternal/editor';
import { useProject } from 'tg.hooks/useProject';
import { confirmation } from 'tg.hooks/confirmation';
import { T } from '@tolgee/react';

type KeyWithTranslationsModelType =
  components['schemas']['KeyWithTranslationsModel'];

type Props = {
  translations: ReturnType<typeof useTranslationsService>;
  viewRefs: ReturnType<typeof useRefsService>;
};

export function usePositionService({ translations, viewRefs }: Props) {
  const project = useProject();
  const [position, setPosition] = useState<Edit | undefined>(undefined);
  const currentIndex = useMemo(() => {
    return translations.fixedTranslations?.findIndex(
      (i) => i.keyId === position?.keyId
    );
  }, [position?.keyId]);

  const key = useMemo(() => {
    if (position?.keyId) {
      return translations?.fixedTranslations?.find(
        (t) => t.keyId === position?.keyId
      );
    }
  }, [position?.keyId, translations]);

  useEffect(() => {
    if (viewRefs.reactList && currentIndex !== undefined) {
      updateReactListSizes(viewRefs.reactList, currentIndex);
    }
  }, [position, currentIndex]);

  const [debouncedPosition] = useDebounce(position, 100, { maxWait: 100 });

  useEffect(() => {
    if (!debouncedPosition) {
      return;
    }
    const newValue = serializeVariants(debouncedPosition.value.variants);

    const isChanged = newValue !== originalValue;

    if (isChanged !== debouncedPosition.changed) {
      setPosition(() => ({
        ...debouncedPosition,
        changed: isChanged,
      }));
    }
  }, [debouncedPosition]);

  useEffect(() => {
    // field is also focused, which breaks the scrolling
    // so we need to make it async
    setTimeout(() => {
      // avoiding scrolling to keys when edited
      // as they are edited in dialog
      if (position && position.language) {
        viewRefs.scrollToElement({
          keyId: position.keyId,
          language: position.language,
          options: { block: 'center', inline: 'center', behavior: 'smooth' },
        });
      }
    });
  }, [position?.keyId, position?.language]);

  const updatePosition = (newPos: Partial<Edit>) => {
    setPosition((pos) => (pos ? { ...pos, ...newPos } : pos));
  };

  const moveEditToDirection = (direction: Direction | undefined) => {
    const currentIndex =
      translations.fixedTranslations?.findIndex(
        (k) => k.keyId === position?.keyId
      ) || 0;
    if (currentIndex === -1 || !direction) {
      clearPosition();
      return;
    }
    let nextKey = undefined as KeyWithTranslationsModelType | undefined;
    if (direction === 'DOWN') {
      nextKey = translations.fixedTranslations?.[currentIndex + 1];
    } else if (direction === 'UP') {
      nextKey = translations.fixedTranslations?.[currentIndex - 1];
    }
    setPositionAndFocus(
      nextKey
        ? {
            keyId: nextKey.keyId,
            language: position?.language,
          }
        : undefined
    );
  };

  const originalValue = useMemo(() => {
    const value = position?.language
      ? key?.translations?.[position.language]?.text
      : key?.keyName;

    return serializeVariants(
      getTolgeeFormat(
        value ?? '',
        Boolean(key?.keyIsPlural),
        !project.icuPlaceholders
      )?.variants
    );
  }, [key, position?.language, Boolean(position?.value.parameter)]);

  const setPositionAndFocus = (pos: EditorProps | undefined) => {
    if (!pos) {
      clearPosition();
    } else {
      const key = translations.fixedTranslations?.find(
        (key) => key.keyId === pos.keyId
      );

      const textValue = pos.language
        ? key?.translations[pos.language]?.text
        : key?.keyName;

      setPosition(() =>
        generateCurrentValue(pos, textValue, key, !project.icuPlaceholders)
      );
    }
    // make it async if someone is stealing focus
    setTimeout(() => {
      // focus cell when closing editor
      if (pos === undefined && position) {
        const newPosition = {
          keyId: position.keyId,
          language: position.language,
        };
        viewRefs.focusCell(newPosition);
      }
    });
  };

  function clearPosition() {
    setPosition(undefined);
  }

  const getEditOldValue = (): string | undefined => {
    const key = translations.fixedTranslations?.find(
      (k) => k.keyId === position?.keyId
    );
    if (key) {
      return (
        (position?.language
          ? key.translations[position.language]?.text
          : key.keyName) || ''
      );
    }
  };

  const confirmUnsavedChanges = (newPosition?: Partial<Edit>) => {
    return new Promise<boolean>((resolve) => {
      const fieldIsDifferent =
        newPosition?.keyId !== undefined &&
        (newPosition?.keyId !== position?.keyId ||
          newPosition?.language !== position?.language);

      if (
        position?.changed &&
        position.keyId !== undefined &&
        (!newPosition || fieldIsDifferent)
      ) {
        confirmation({
          title: <T keyName="translations_discard_unsaved_title" />,
          message: <T keyName="translations_discard_unsaved_message" />,
          cancelButtonText: <T keyName="back_to_editing" />,
          confirmButtonText: (
            <T keyName="translations_discard_button_confirm" />
          ),
          onConfirm() {
            resolve(true);
          },
          onCancel() {
            resolve(false);
          },
        });
      } else {
        resolve(true);
      }
    });
  };

  return {
    key,
    position,
    updatePosition,
    moveEditToDirection,
    setPositionAndFocus,
    clearPosition,
    getEditOldValue,
    confirmUnsavedChanges,
  };
}
