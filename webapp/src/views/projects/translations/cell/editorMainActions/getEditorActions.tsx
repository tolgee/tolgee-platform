import { SaveProps } from '../../useTranslationCell';
import { components } from 'tg.service/apiSchema.generated';
import { getPermissionTools } from 'tg.fixtures/getPermissionTools';
import { T } from '@tolgee/react';
import React from 'react';
import { isEmpty, isUnchanged } from 'tg.fixtures/plurals';
import { TolgeeFormat } from '@tginternal/editor';

type ProjectModel = components['schemas']['ProjectModel'];
type TaskModel = components['schemas']['KeyTaskViewModel'];
type TranslationViewModel = components['schemas']['TranslationViewModel'];

type Props = {
  onSave?: (options?: SaveProps) => void;
  tasks: TaskModel[] | undefined;
  currentTask: number | undefined;
  translation: TranslationViewModel | undefined;
  languageId: number;
  project: ProjectModel;
  value: TolgeeFormat | undefined;
};

type TranslationAction = {
  action: (props?: SaveProps) => void;
  label: React.ReactNode;
  disabled?: boolean;
};

/**
 * Returns possible actions based on user permissions
 * assigned tasks and project settings (suggestion mode)
 *
 * If it returns empty array, it means we shouldn't open the editor
 */
export function getEditorActions({
  onSave,
  tasks,
  currentTask,
  languageId,
  translation,
  project,
  value,
}: Props) {
  const editorEmpty = isEmpty(value);
  const editorUnchanged = isUnchanged(value, translation?.text);
  const disabled = editorEmpty || editorUnchanged;
  const { satisfiesLanguageAccess } = getPermissionTools(
    project.computedPermission
  );
  const task = tasks?.[0];
  const displayTaskControls =
    (currentTask === undefined || currentTask === task?.number) &&
    task &&
    task.userAssigned &&
    !task.done &&
    task.type === 'TRANSLATE';

  const assignedTask = tasks?.find(
    (t) => t.languageId === languageId && t.userAssigned
  );
  const actions: TranslationAction[] = [];

  const additional: TranslationAction[] = [];

  if (
    (satisfiesLanguageAccess('translations.edit', languageId) &&
      (translation?.state !== 'REVIEWED' ||
        project.translationProtection !== 'PROTECT_REVIEWED' ||
        satisfiesLanguageAccess('translations.state-edit', languageId))) ||
    (assignedTask?.userAssigned && assignedTask.type === 'TRANSLATE')
  ) {
    actions.push({
      action: (props) => onSave?.(props),
      label: displayTaskControls ? (
        <T keyName="translations_cell_save_and_done" />
      ) : (
        <T keyName="translations_cell_save" />
      ),
    });

    if (displayTaskControls) {
      additional.push({
        action: (props) => onSave?.({ ...props, preventTaskResolution: true }),
        label: <T keyName="translations_cell_save_only" />,
      });
    }
  }

  if (
    project.suggestionsMode !== 'DISABLED' &&
    satisfiesLanguageAccess('translations.suggest', languageId)
  ) {
    actions.push({
      action: (props) => onSave?.({ ...props, suggestionOnly: true }),
      label: displayTaskControls ? (
        <T keyName="translations_cell_suggest_and_done" />
      ) : (
        <T keyName="translations_cell_suggest" />
      ),
      disabled,
    });

    if (displayTaskControls) {
      additional.push({
        action: (props) =>
          onSave?.({
            ...props,
            suggestionOnly: true,
            preventTaskResolution: true,
          }),
        label: <T keyName="translations_cell_suggest_only" />,
        disabled,
      });
    }
  }

  return [...actions, ...additional];
}
