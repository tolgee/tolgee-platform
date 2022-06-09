import { useEffect, useState } from 'react';
import { container } from 'tsyringe';
import { useTranslate, T } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import {
  usePutKey,
  usePutTranslation,
  usePutTag,
  useDeleteTag,
} from 'tg.service/TranslationHooks';
import { useTranslationsService } from './useTranslationsService';
import { useRefsService } from './useRefsService';
import { confirmation } from 'tg.hooks/confirmation';
import { MessageService } from 'tg.service/MessageService';
import { AfterCommand, ChangeValue, Direction, Edit, SetEdit } from '../types';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { useProject } from 'tg.hooks/useProject';

type KeyWithTranslationsModelType =
  components['schemas']['KeyWithTranslationsModel'];

type Props = {
  translations: ReturnType<typeof useTranslationsService>;
  viewRefs: ReturnType<typeof useRefsService>;
};

const messaging = container.resolve(MessageService);

export const useEditService = ({ translations, viewRefs }: Props) => {
  const [position, setPosition] = useState<Edit | undefined>(undefined);

  const project = useProject();

  const putKey = usePutKey();
  const putTranslation = usePutTranslation();
  const putTag = usePutTag();
  const deleteTag = useDeleteTag();
  const t = useTranslate();

  useEffect(() => {
    // field is also focused, which breaks the scrolling
    // so we need to make it async
    setTimeout(() => {
      if (position) {
        viewRefs.scrollToElement({
          keyId: position.keyId,
          language: position.language,
          options: { block: 'center', behavior: 'smooth' },
        });
      }
    });
  }, [position?.keyId, position?.language]);

  const updatePosition = (newPos: Partial<Edit>) =>
    setPosition((pos) => (pos ? { ...pos, ...newPos } : pos));

  const getTranslationKeyName = (keyId: number) =>
    translations!.fixedTranslations!.find((t) => t.keyId === keyId)!.keyName;

  const moveEditToDirection = (direction: Direction | undefined) => {
    const currentIndex =
      translations.fixedTranslations?.findIndex(
        (k) => k.keyId === position?.keyId
      ) || 0;
    if (currentIndex === -1 || !direction) {
      setPosition(undefined);
      return;
    }
    let nextKey = undefined as KeyWithTranslationsModelType | undefined;
    if (direction === 'DOWN') {
      nextKey = translations.fixedTranslations?.[currentIndex + 1];
    } else if (direction === 'UP') {
      nextKey = translations.fixedTranslations?.[currentIndex - 1];
    }
    setPosition(
      nextKey
        ? {
            keyId: nextKey.keyId,
            language: position?.language,
            mode: 'editor',
          }
        : undefined
    );
  };

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

  const mutateTranslationKey = async (payload: SetEdit) => {
    if (payload.value !== getEditOldValue()) {
      await putKey.mutateAsync({
        path: { projectId: project.id, id: payload.keyId },
        content: {
          'application/json': {
            name: payload.value,
          },
        },
      });
    }
  };

  const mutateTranslation = async (
    payload: SetEdit,
    languagesToReturn?: string[]
  ) => {
    const { language, value, keyId } = payload;
    const keyName = getTranslationKeyName(keyId);

    const newVal =
      payload.value !== getEditOldValue()
        ? await putTranslation.mutateAsync({
            path: { projectId: project.id },
            content: {
              'application/json': {
                key: keyName,
                translations: {
                  [language!]: value,
                },
                languagesToReturn,
              },
            },
          })
        : null;
    return newVal;
  };

  const setPositionAndFocus = (pos: Edit | undefined) => {
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
      setPosition(pos);
    });
  };

  const setEdit = (newPosition: Edit | undefined) => {
    if (position?.changed) {
      setPositionAndFocus({ ...position, mode: 'editor' });
      confirmation({
        title: t('translations_unsaved_changes_confirmation_title'),
        message: t('translations_unsaved_changes_confirmation'),
        cancelButtonText: t('back_to_editing'),
        confirmButtonText: t('translations_cell_save'),
        onConfirm: () => {
          changeField({
            onSuccess() {
              setPositionAndFocus(newPosition);
            },
          });
        },
      });
    } else {
      setPositionAndFocus(newPosition);
    }
  };

  const changeField = async (data: ChangeValue) => {
    if (!position) {
      return;
    }
    const { keyId, language, value } = position;
    if (!language && !value) {
      // key can't be empty
      return messaging.error(<T keyName="global_empty_value" />);
    }
    try {
      if (language) {
        // update translation
        const result = await mutateTranslation(
          {
            ...data,
            value: value as string,
            keyId,
            language,
          },
          translations.selectedLanguages
        );

        if (result) {
          Object.entries(result.translations).forEach(([lang, translation]) =>
            translations.changeTranslation(keyId, lang, translation)
          );
        }
      } else {
        // update key
        await mutateTranslationKey({
          ...data,
          value: value as string,
          keyId,
          language,
        });
        translations.updateTranslationKey(keyId, { keyName: value });
      }
      doAfterCommand(data.after);
      data.onSuccess?.();
    } catch (e) {
      const parsed = parseErrorResponse(e);
      parsed.forEach((error) => messaging.error(<T>{error}</T>));
    }
    return;
  };

  const doAfterCommand = (command?: AfterCommand) => {
    switch (command) {
      case 'EDIT_NEXT':
        moveEditToDirection('DOWN');
        return;

      default:
        setPositionAndFocus(undefined);
    }
  };

  return {
    position,
    setPosition,
    updatePosition,
    setPositionAndFocus,
    setEdit,
    changeField,
    isLoading:
      putKey.isLoading ||
      putTranslation.isLoading ||
      putTag.isLoading ||
      deleteTag.isLoading,
    moveEditToDirection,
  };
};
