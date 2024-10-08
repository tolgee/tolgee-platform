import { T } from '@tolgee/react';
import { TolgeeFormat } from '@tginternal/editor';

import { useProject } from 'tg.hooks/useProject';
import { messageService } from 'tg.service/MessageService';

import {
  useDeleteTag,
  usePutKey,
  usePutTag,
  usePutTranslation,
} from 'tg.service/TranslationHooks';

import { useTranslationsService } from './useTranslationsService';
import { useRefsService } from './useRefsService';
import { AfterCommand, ChangeValue, SetEdit } from '../types';
import { useTaskService } from './useTaskService';
import { PrefilterType } from '../../prefilters/usePrefilter';
import { composeValue } from './utils';
import { usePositionService } from './usePositionService';

type Props = {
  positionService: ReturnType<typeof usePositionService>;
  translationService: ReturnType<typeof useTranslationsService>;
  viewRefs: ReturnType<typeof useRefsService>;
  taskService: ReturnType<typeof useTaskService>;
  prefilter: PrefilterType | undefined;
};

export const useEditService = ({
  positionService,
  translationService,
  taskService,
  prefilter,
}: Props) => {
  const {
    position,
    key,
    moveEditToDirection,
    updatePosition,
    setPositionAndFocus,
    getEditOldValue,
  } = positionService;

  const project = useProject();

  const putKey = usePutKey();
  const putTranslation = usePutTranslation();
  const putTag = usePutTag();
  const deleteTag = useDeleteTag();

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
        translationService.selectedLanguages
      );

      if (result?.translations) {
        Object.entries(result.translations).forEach(([lang, translation]) =>
          translationService.changeTranslations([
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
      translationService.updateTranslationKeys([
        { keyId, value: { keyName: value } },
      ]);
    }

    if (language && !data.preventTaskResolution && prefilter?.task) {
      const key = translationService.fixedTranslations?.find(
        (k) => k.keyId === keyId
      );
      const task = key?.tasks?.find((t) => t.languageTag === language);

      if (
        task &&
        prefilter.task === task.number &&
        !task.done &&
        task.userAssigned &&
        task.type === 'TRANSLATE'
      ) {
        await taskService.setTaskTranslationState({
          keyId: position.keyId,
          taskNumber: task.number,
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
    changeField,
    setEditValue,
    setEditValueString,
    isLoading:
      putKey.isLoading ||
      putTranslation.isLoading ||
      putTag.isLoading ||
      deleteTag.isLoading,
  };
};
