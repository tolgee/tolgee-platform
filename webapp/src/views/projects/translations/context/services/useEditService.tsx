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

import type { useTranslationsService } from './useTranslationsService';
import type { useRefsService } from './useRefsService';
import { AfterCommand, ChangeValue, SetEdit } from '../types';
import type { useTaskService } from './useTaskService';
import { composeValue, taskEditControlsShouldBeVisible } from './utils';
import type { usePositionService } from './usePositionService';
import { TranslationViewModel } from '../../ToolsPanel/common/types';
import { getTranslationPermissions } from '../../cell/editorMainActions/getEditorActions';
import { applyQaReplacement, QaReplacementParams } from 'tg.fixtures/qaUtils';

type LanguageModel = components['schemas']['LanguageModel'];
type TranslationModel = components['schemas']['TranslationViewModel'];

type Props = {
  positionService: ReturnType<typeof usePositionService>;
  translationService: ReturnType<typeof useTranslationsService>;
  viewRefs: ReturnType<typeof useRefsService>;
  taskService: ReturnType<typeof useTaskService>;
  branchName?: string;
  allLanguages: LanguageModel[];
};

export type CorrectTranslationParams = {
  translationId: number;
  translationText: string;
  issue: QaReplacementParams;
};

function findTranslationInList(
  translations:
    | ReturnType<typeof useTranslationsService>['fixedTranslations']
    | undefined,
  translationId: number
) {
  for (const key of translations ?? []) {
    for (const [langTag, translation] of Object.entries(key.translations) as [
      string,
      TranslationModel
    ][]) {
      if (translation.id === translationId) {
        return {
          keyId: key.keyId,
          keyName: key.keyName,
          keyNamespace: key.keyNamespace,
          languageTag: langTag,
          translation,
          tasks: key.tasks,
        };
      }
    }
  }
  return null;
}

export const useEditService = ({
  positionService,
  translationService,
  taskService,
  branchName,
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

  /**
   * Saves a translation via the API and updates the local state.
   * Marks QA checks as stale after save.
   */
  const saveTranslationValue = async (params: {
    keyId: number;
    keyName: string;
    keyNamespace?: string;
    language: string;
    value: string;
  }) => {
    const result = await putTranslation.mutateAsync({
      path: { projectId: project.id },
      content: {
        'application/json': {
          key: params.keyName,
          namespace: params.keyNamespace,
          branch: branchName,
          translations: {
            [params.language]: params.value,
          },
          languagesToReturn: translationService.selectedLanguages,
        },
      },
    });

    if (result?.translations) {
      Object.entries(result.translations).forEach(([lang, translation]) =>
        translationService.changeTranslations([
          { keyId: params.keyId, language: lang, value: translation },
        ])
      );
      // Mark QA checks as stale and clear old issues, since the translation was updated
      translationService.changeTranslations([
        {
          keyId: params.keyId,
          language: params.language,
          value: { qaChecksStale: true, qaIssues: [] },
        },
      ]);
    }

    return result;
  };

  /**
   * Creates a translation suggestion via the API and updates the local state.
   */
  const suggestTranslationValue = async (params: {
    keyId: number;
    language: string;
    value: string;
  }) => {
    const languageId = allLanguages.find((l) => l.tag === params.language)?.id;
    if (languageId === undefined) return;

    const result = await postSuggestion.mutateAsync({
      path: {
        projectId: project.id,
        keyId: params.keyId,
        languageId: languageId,
      },
      content: {
        'application/json': {
          translation: params.value,
        },
      },
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
      if (value !== getEditOldValue()) {
        await suggestTranslationValue({ keyId, language, value });
      }
    } else if (language) {
      if (value !== getEditOldValue()) {
        await saveTranslationValue({
          keyId,
          keyName: key!.keyName,
          keyNamespace: key!.keyNamespace,
          language,
          value,
        });
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

  /**
   * Returns whether the user can save or suggest a given translation.
   */
  const canEditTranslation = (translationId: number) => {
    const found = findTranslationInList(
      translationService.fixedTranslations,
      translationId
    );
    if (!found) return false;

    const languageId = allLanguages.find(
      (l) => l.tag === found.languageTag
    )?.id;
    if (!languageId) return false;

    const { canSave, canSuggest } = getTranslationPermissions({
      project,
      languageId,
      translationState: found.translation.state,
      tasks: found.tasks,
    });

    return canSave || canSuggest;
  };

  /**
   * Applies a QA correction and saves (or suggests) the translation directly,
   * without opening the editor.
   */
  const correctTranslation = async (params: CorrectTranslationParams) => {
    const found = findTranslationInList(
      translationService.fixedTranslations,
      params.translationId
    );
    if (!found) return;

    const corrected = applyQaReplacement(params.translationText, params.issue);

    const languageId = allLanguages.find(
      (l) => l.tag === found.languageTag
    )?.id;
    if (!languageId) return;

    const { canSave, canSuggest } = getTranslationPermissions({
      project,
      languageId,
      translationState: found.translation.state,
      tasks: found.tasks,
    });

    if (canSave) {
      await saveTranslationValue({
        keyId: found.keyId,
        keyName: found.keyName,
        keyNamespace: found.keyNamespace,
        language: found.languageTag,
        value: corrected,
      });
    } else if (canSuggest) {
      await suggestTranslationValue({
        keyId: found.keyId,
        language: found.languageTag,
        value: corrected,
      });
    }
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
    canEditTranslation,
    correctTranslation,
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
