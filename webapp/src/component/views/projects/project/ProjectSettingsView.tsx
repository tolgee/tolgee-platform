import * as React from 'react';
import { FunctionComponent, useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { AppState } from '../../../../store';
import { container } from 'tsyringe';
import { ProjectActions } from '../../../../store/project/ProjectActions';
import { LINKS, PARAMS } from '../../../../constants/links';
import { Redirect } from 'react-router-dom';
import { TextField } from '../../../common/form/fields/TextField';
import { BaseFormView } from '../../../layout/BaseFormView';
import { useProject } from '../../../../hooks/useProject';
import { Button } from '@material-ui/core';
import { confirmation } from '../../../../hooks/confirmation';
import { T, useTranslate } from '@tolgee/react';
import { ConfirmationDialogProps } from '../../../common/ConfirmationDialog';
import { Validation } from '../../../../constants/GlobalValidationSchema';
import { Navigation } from '../../../navigation/Navigation';

const actions = container.resolve(ProjectActions);

type ValueType = {
  name: string;
};

export const ProjectSettingsView: FunctionComponent = () => {
  const loadable = useSelector(
    (state: AppState) => state.projects.loadables.editProject
  );
  const saveLoadable = useSelector(
    (state: AppState) => state.projects.loadables.editProject
  );

  let project = useProject();

  let confirm = (options: ConfirmationDialogProps) =>
    confirmation({ title: <T>delete_project_dialog_title</T>, ...options });

  const onSubmit = (values) => {
    actions.loadableActions.editProject.dispatch(project.id, values);
  };

  const t = useTranslate();

  useEffect(() => {
    if (saveLoadable.touched) {
      actions.loadableReset.project.dispatch();
    }
    return () => actions.loadableReset.editProject.dispatch();
  }, [saveLoadable.touched]);

  useEffect(() => {
    return () => {
      actions.loadableReset.deleteProject.dispatch();
    };
  }, []);

  const initialValues: ValueType = { name: project.name };

  const [cancelled, setCancelled] = useState(false);

  if (cancelled) {
    return <Redirect to={LINKS.PROJECTS.build()} />;
  }

  return (
    <BaseFormView
      lg={6}
      md={8}
      initialValues={initialValues}
      onSubmit={onSubmit}
      onCancel={() => setCancelled(true)}
      saveActionLoadable={loadable}
      validationSchema={Validation.REPOSITORY_SETTINGS}
      navigation={
        <Navigation
          path={[
            [
              project.name,
              LINKS.PROJECT.build({
                [PARAMS.PROJECT_ID]: project.id,
              }),
            ],
            [
              t('project_settings_title'),
              LINKS.PROJECT_TRANSLATIONS.build({
                [PARAMS.PROJECT_ID]: project.id,
              }),
            ],
          ]}
        />
      }
      customActions={
        <Button
          color="secondary"
          variant="outlined"
          onClick={() => {
            confirm({
              message: (
                <T parameters={{ name: project.name }}>
                  delete_project_confirmation_message
                </T>
              ),
              onConfirm: () =>
                actions.loadableActions.deleteProject.dispatch(project.id),
              hardModeText: project.name.toUpperCase(),
            });
          }}
        >
          <T>delete_project_button</T>
        </Button>
      }
    >
      <TextField
        label={<T>project_settings_name_label</T>}
        name="name"
        required={true}
      />
    </BaseFormView>
  );
};
