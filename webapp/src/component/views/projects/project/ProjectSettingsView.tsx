import * as React from 'react';
import { FunctionComponent, useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { AppState } from '../../../../store';
import { container } from 'tsyringe';
import { ProjectActions } from '../../../../store/project/ProjectActions';
import { LINKS, PARAMS } from '../../../../constants/links';
import { Redirect } from 'react-router-dom';
import { TextField } from '../../../common/form/fields/TextField';
import { useProject } from '../../../../hooks/useProject';
import { Box, Button, Typography } from '@material-ui/core';
import { confirmation } from '../../../../hooks/confirmation';
import { T, useTranslate } from '@tolgee/react';
import { ConfirmationDialogProps } from '../../../common/ConfirmationDialog';
import { Validation } from '../../../../constants/GlobalValidationSchema';
import { Navigation } from '../../../navigation/Navigation';
import { BaseView } from '../../../layout/BaseView';
import { StandardForm } from '../../../common/form/StandardForm';
import { ProjectSettingsLanguages } from './components/ProjectSettingsLanguages';
import { ProjectLanguagesProvider } from '../../../../hooks/ProjectLanguagesProvider';
import { useProjectLanguages } from '../../../../hooks/useProjectLanguages';
import { BaseLanguageSelect } from './components/BaseLanguageSelect';
import { components } from '../../../../service/apiSchema.generated';

const actions = container.resolve(ProjectActions);

type ValueType = components['schemas']['EditProjectDTO'];

export const ProjectSettingsView: FunctionComponent = () => {
  const loadable = useSelector(
    (state: AppState) => state.projects.loadables.editProject
  );
  const saveLoadable = useSelector(
    (state: AppState) => state.projects.loadables.editProject
  );

  const deleteLoadable = useSelector(
    (state: AppState) => state.projects.loadables.deleteProject
  );

  const project = useProject();

  const confirm = (options: ConfirmationDialogProps) =>
    confirmation({ title: <T>delete_project_dialog_title</T>, ...options });

  const onSubmit = (values) => {
    actions.loadableActions.editProject.dispatch({
      path: {
        projectId: project.id,
      },
      content: {
        'application/json': values,
      },
    });
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

  const initialValues: ValueType = {
    name: project.name,
    baseLanguageId: project.baseLanguage?.id,
  };

  const [cancelled, setCancelled] = useState(false);

  if (cancelled || deleteLoadable.loaded) {
    return <Redirect to={LINKS.PROJECTS.build()} />;
  }

  const LanguageSelect = () => {
    const projectLanguages = useProjectLanguages();
    return (
      <BaseLanguageSelect name="baseLanguageId" languages={projectLanguages} />
    );
  };

  return (
    <BaseView
      lg={6}
      md={8}
      navigation={
        <Navigation
          path={[
            [
              project.name,
              LINKS.PROJECT_TRANSLATIONS.build({
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
    >
      <StandardForm
        saveActionLoadable={loadable}
        validationSchema={Validation.PROJECT_SETTINGS}
        onSubmit={onSubmit}
        onCancel={() => setCancelled(true)}
        initialValues={initialValues}
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
                  actions.loadableActions.deleteProject.dispatch({
                    path: { projectId: project.id },
                  }),
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
        <ProjectLanguagesProvider>
          <LanguageSelect />
        </ProjectLanguagesProvider>
      </StandardForm>
      <Box mt={4} mb={2}>
        <Typography variant="h5">
          <T>languages_title</T>
        </Typography>
      </Box>
      <ProjectSettingsLanguages />
    </BaseView>
  );
};
