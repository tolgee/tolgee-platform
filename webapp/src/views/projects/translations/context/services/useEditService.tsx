import { T } from '@tolgee/react';
import { TolgeeFormat } from '@tginternal/editor';

import { useProject } from 'tg.hooks/useProject';
import { messageService } from 'tg.service/MessageService';

import {
  useDeleteTag,
  usePostTranslationSuggestion,
  usePutKey,
  usePutTag,
  usePutTranslation,
} from 'tg.service/TranslationHooks';
import { components } from 'tg.service/apiSchema.generated';

import { useTranslationsService } from './useTranslationsService';
import { useRefsService } from './useRefsService';
import { AfterCommand, ChangeValue, SetEdit } from '../types';
import { useTaskService } from './useTaskService';
import { composeValue, taskEditControlsShouldBeVisible } from './utils';
import { usePositionService } from './usePositionService';
import { TranslationViewModel } from '../../ToolsPanel/common/types';

type LanguageModel = components['schemas']['LanguageModel'];

type Props = {
  positionService: ReturnType<typeof usePositionService>;
  translationService: ReturnType<typeof useTranslationsService>;
  viewRefs: ReturnType<typeof useRefsService>;
  taskService: ReturnType<typeof useTaskService>;
  allLanguages: LanguageModel[];
};

export const useEditService = ({
  positionService,
  translationService,
  taskService,
  allLanguages,
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
  const postSuggestion = usePostTranslationSuggestion();

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

  const createSuggestion = async (payload: SetEdit) => {
    const languageId = allLanguages.find((l) => l.tag === payload.language)?.id;
    if (languageId !== undefined && payload.value !== getEditOldValue()) {
      return await postSuggestion.mutateAsync({
        path: {
          projectId: project.id,
          keyId: payload.keyId,
          languageId: languageId,
        },
        content: {
          'application/json': {
            translation: payload.value,
          },
        },
      });
    }
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

    if (language && data.suggestionOnly) {
      const result = await createSuggestion({
        ...position,
        value,
      });

      const lang = allLanguages.find((lang) => lang.id === result?.languageId);

      if (result && lang) {
        translationService.updateTranslation({
          keyId: result.keyId,
          lang: lang.tag,
          data(value) {
            return {
              suggestions: [result],
              activeSuggestionCount: (value.activeSuggestionCount ?? 0) + 1,
              totalSuggestionCount: (value.totalSuggestionCount ?? 0) + 1,
            } satisfies Partial<TranslationViewModel>;
          },
        });
      }
    } else if (language) {
      // update translation
      const result = await mutateTranslation(
        {
          ...data,
          value,
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

    if (language && !data.preventTaskResolution) {
      const key = translationService.fixedTranslations?.find(
        (k) => k.keyId === keyId
      );
      const firstTask = key?.tasks?.find((t) => t.languageTag === language);

      if (firstTask && taskEditControlsShouldBeVisible(firstTask)) {
        await taskService.setTaskTranslationState({
          keyId: position.keyId,
          taskNumber: firstTask.number,
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

  const appendEditValueString = (value: string) => {
    if (!position) {
      return;
    }

    const activeVariant = position.activeVariant ?? 'other';
    const currentValue = position.value.variants[activeVariant] ?? '';
    setEditValueString(currentValue + value);
  };

  return {
    changeField,
    setEditValue,
    setEditValueString,
    appendEditValueString,
    isLoading:
      putKey.isLoading ||
      putTranslation.isLoading ||
      putTag.isLoading ||
      deleteTag.isLoading,
  };
};
