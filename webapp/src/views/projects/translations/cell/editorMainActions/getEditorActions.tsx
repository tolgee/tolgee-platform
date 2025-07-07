import { SaveProps } from '../../useTranslationCell';
import { components } from 'tg.service/apiSchema.generated';
import { getPermissionTools } from 'tg.fixtures/getPermissionTools';
import { T } from '@tolgee/react';
import React from 'react';

type ProjectModel = components['schemas']['ProjectModel'];
type TaskModel = components['schemas']['KeyTaskViewModel'];
type TranslationViewModel = components['schemas']['TranslationViewModel'];

type Props = {
  onSave: (options?: SaveProps) => void;
  tasks: TaskModel[] | undefined;
  currentTask: number | undefined;
  translation: TranslationViewModel | undefined;
  languageId: number;
  project: ProjectModel;
};

type TranslationAction = {
  action: (props?: SaveProps) => void;
  label: React.ReactNode;
};

export function getEditorActions({
  onSave,
  tasks,
  currentTask,
  languageId,
  translation,
  project,
}: Props) {
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

  const actions: TranslationAction[] = [];

  const additional: TranslationAction[] = [];

  if (
    satisfiesLanguageAccess('translations.edit', languageId) &&
    (translation?.state !== 'REVIEWED' ||
      project.suggestionsMode !== 'ENFORCED' ||
      satisfiesLanguageAccess('translations.state-edit', languageId))
  ) {
    actions.push({
      action: (props) => onSave(props),
      label: displayTaskControls ? (
        <T keyName="translations_cell_save_and_done" />
      ) : (
        <T keyName="translations_cell_save" />
      ),
    });

    if (displayTaskControls) {
      additional.push({
        action: (props) => onSave({ ...props, preventTaskResolution: true }),
        label: <T keyName="translations_cell_save_only" />,
      });
    }
  }

  if (
    project.suggestionsMode !== 'DISABLED' &&
    satisfiesLanguageAccess('translations.suggest', languageId)
  ) {
    actions.push({
      action: (props) => onSave({ ...props, suggestionOnly: true }),
      label: displayTaskControls ? (
        <T keyName="translations_cell_suggest_and_done" />
      ) : (
        <T keyName="translations_cell_suggest" />
      ),
    });

    if (displayTaskControls) {
      additional.push({
        action: (props) =>
          onSave({
            ...props,
            suggestionOnly: true,
            preventTaskResolution: true,
          }),
        label: <T keyName="translations_cell_suggest_only" />,
      });
    }
  }

  return [...actions, ...additional];
}
