import { useEffect, useMemo, useState } from 'react';
import { T } from '@tolgee/react';
import { useDebounce } from 'use-debounce';
import ReactList from 'react-list';
import {
  TolgeeFormat,
  getTolgeeFormat,
  tolgeeFormatGenerateIcu,
} from '@tginternal/editor';

import { useProject } from 'tg.hooks/useProject';
import { messageService } from 'tg.service/MessageService';

import { components } from 'tg.service/apiSchema.generated';
import {
  useDeleteTag,
  usePutKey,
  usePutTag,
  usePutTranslation,
} from 'tg.service/TranslationHooks';
import { confirmation } from 'tg.hooks/confirmation';

import { useTranslationsService } from './useTranslationsService';
import { useRefsService } from './useRefsService';
import {
  AfterCommand,
  ChangeValue,
  DeletableKeyWithTranslationsModelType,
  Direction,
  Edit,
  EditorProps,
  SetEdit,
} from '../types';
import { getPluralVariants } from '@tginternal/editor';
import { useTaskService } from './useTaskService';

/**
 * Kinda hacky way how to update react-list size cache, when editor gets open
 */
function updateListSizes(list: ReactList, currentIndex: number) {
  // @ts-ignore
  const cache = list.cache as Record<number, number>;
  // @ts-ignore
  const from = list.state.from as number;
  // @ts-ignore
  const itemEls = list.items.children;
  const elementIndex = currentIndex - from;
  const previousSize = cache[currentIndex];
  const currentSize = itemEls[elementIndex]?.['offsetHeight'];
  // console.log({ previousSize, currentSize });
  if (currentSize !== previousSize && typeof currentSize === 'number') {
    cache[currentIndex] = currentSize;
    // @ts-ignore
    list.updateFrameAndClearCache();
    list.setState((state) => ({ ...state }));
  }
}

type KeyWithTranslationsModelType =
  components['schemas']['KeyWithTranslationsModel'];

type Props = {
  translations: ReturnType<typeof useTranslationsService>;
  viewRefs: ReturnType<typeof useRefsService>;
  taskService: ReturnType<typeof useTaskService>;
};

function generateCurrentValue(
  position: EditorProps,
  textValue: string | undefined,
  key: DeletableKeyWithTranslationsModelType | undefined,
  raw: boolean
): Edit {
  const result: Edit = {
    ...position,
    activeVariant: position.activeVariant ?? 'other',
    value: { variants: { other: textValue } },
  };
  if (position.language && key?.keyIsPlural) {
    const format = getTolgeeFormat(textValue ?? '', key.keyIsPlural, raw);
    const variants = getPluralVariants(position.language);
    if (!position.activeVariant) {
      result.activeVariant = variants[0];
    }
    result.value = format;
    result.value.parameter = key.keyPluralArgName ?? 'value';
  }
  return result;
}

function composeValue(position: Edit, raw: boolean) {
  if (position.value) {
    return tolgeeFormatGenerateIcu(position.value, raw);
  }
  return position.value;
}

function serializeVariants(
  variants: Record<string, string | undefined> | undefined
) {
  if (!variants) {
    return '';
  }
  return Object.entries(variants)
    .sort(([keyA], [keyB]) => keyA.localeCompare(keyB))
    .map(([_, value]) => value)
    .filter((value) => Boolean(value))
    .join('<%>');
}

export const useEditService = ({
  translations,
  viewRefs,
  taskService,
}: Props) => {
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
      updateListSizes(viewRefs.reactList, currentIndex);
    }
  }, [position, currentIndex]);

  const project = useProject();

  const putKey = usePutKey();
  const putTranslation = usePutTranslation();
  const putTag = usePutTag();
  const deleteTag = useDeleteTag();

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
    const { language, value } = payload;
    const { keyName, keyNamespace } = key!;

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

  const changeField = async (data: ChangeValue) => {
    if (!position) {
      return;
    }
    const { keyId, language } = position;
    const value = composeValue(position, !project.icuPlaceholders);
    if (!language && !value) {
      // key can't be empty
      return messageService.error(<T keyName="global_empty_value" />);
    }

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

      if (result?.translations) {
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

    if (language && !data.preventTaskResolution) {
      const key = translations.fixedTranslations?.find(
        (k) => k.keyId === keyId
      );
      const translation = key?.translations[language];
      const task = translation?.tasks?.[0];

      if (
        task &&
        !task.done &&
        task.userAssigned &&
        task.type === 'TRANSLATE'
      ) {
        await taskService.setTaskTranslationState({
          keyId: position.keyId,
          taskId: task.id,
          done: true,
        });
      }
    }

    data.onSuccess?.();
    doAfterCommand(data.after);
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

  function clearPosition() {
    setPosition(undefined);
  }

  const setEditValue = (newValue: TolgeeFormat) => {
    updatePosition({
      value: newValue,
    });
  };

  const setEditValueString = (value: string) => {
    if (position) {
      setEditValue({
        ...position.value,
        variants: {
          ...position.value.variants,
          [position.activeVariant ?? 'other']: value,
        },
      });
    }
  };

  return {
    position,
    clearPosition,
    updatePosition,
    setPositionAndFocus,
    changeField,
    confirmUnsavedChanges,
    setEditValue,
    setEditValueString,
    isLoading:
      putKey.isLoading ||
      putTranslation.isLoading ||
      putTag.isLoading ||
      deleteTag.isLoading,
    moveEditToDirection,
  };
};
