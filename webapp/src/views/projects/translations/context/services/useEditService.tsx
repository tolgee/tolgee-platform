import { useEffect, useState } from 'react';
import { container } from 'tsyringe';
import { T } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import {
  useDeleteTag,
  usePutKey,
  usePutTag,
  usePutTranslation,
} from 'tg.service/TranslationHooks';
import { useTranslationsService } from './useTranslationsService';
import { useRefsService } from './useRefsService';
import { confirmation } from 'tg.hooks/confirmation';
import { MessageService } from 'tg.service/MessageService';
import { AfterCommand, ChangeValue, Direction, Edit, SetEdit } from '../types';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { useProject } from 'tg.hooks/useProject';
import { TranslatedError } from 'tg.translationTools/TranslatedError';

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
          options: { block: 'center', behavior: 'smooth' },
        });
      }
    });
  }, [position?.keyId, position?.language]);

  const updatePosition = (newPos: Partial<Edit>) =>
    setPosition((pos) => (pos ? { ...pos, ...newPos } : pos));

  const getTranslation = (keyId: number) =>
    translations!.fixedTranslations!.find((t) => t.keyId === keyId)!;

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
    const { keyName, keyNamespace } = getTranslation(keyId);

    const newVal =
      payload.value !== getEditOldValue()
        ? await putTranslation.mutateAsync({
            path: { projectId: project.id },
            content: {
              'application/json': {
                key: keyName,
                namespace: keyNamespace,
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
        setPositionAndFocus({ ...position, mode: 'editor' });
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
            translations.changeTranslations([
              { keyId, language: lang, value: translation },
            ])
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
        translations.updateTranslationKeys([
          { keyId, value: { keyName: value } },
        ]);
      }
      doAfterCommand(data.after);
      data.onSuccess?.();
    } catch (e) {
      const parsed = parseErrorResponse(e);
      parsed.forEach((error) =>
        messaging.error(<TranslatedError code={error} />)
      );
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
    changeField,
    confirmUnsavedChanges,
    isLoading:
      putKey.isLoading ||
      putTranslation.isLoading ||
      putTag.isLoading ||
      deleteTag.isLoading,
    moveEditToDirection,
  };
};
