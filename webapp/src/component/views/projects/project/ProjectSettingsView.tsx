import { FunctionComponent, useState } from 'react';
import { container } from 'tsyringe';
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
import { useApiMutation } from '../../../../service/http/useQueryApi';
import { MessageService } from '../../../../service/MessageService';

const messageService = container.resolve(MessageService);

type ValueType = components['schemas']['EditProjectDTO'];

export const ProjectSettingsView: FunctionComponent = () => {
  const project = useProject();
  const updateLoadable = useApiMutation({
    url: '/v2/projects/{projectId}',
    method: 'put',
    invalidatePrefix: '/v2/projects',
  });
  const deleteLoadable = useApiMutation({
    url: '/v2/projects/{projectId}',
    method: 'delete',
  });

  const confirm = (options: ConfirmationDialogProps) =>
    confirmation({ title: <T>delete_project_dialog_title</T>, ...options });

  const handleEdit = (values) => {
    updateLoadable.mutate(
      {
        path: { projectId: project.id },
        content: { 'application/json': values },
      },
      {
        onSuccess() {
          messageService.success(<T>project_successfully_edited_message</T>);
        },
      }
    );
  };

  const handleDelete = () => {
    confirm({
      message: (
        <T parameters={{ name: project.name }}>
          delete_project_confirmation_message
        </T>
      ),
      onConfirm: () =>
        deleteLoadable.mutate(
          { path: { projectId: project.id } },
          {
            onSuccess() {
              messageService.success(<T>project_deleted_message</T>);
            },
          }
        ),
      hardModeText: project.name.toUpperCase(),
    });
  };

  const t = useTranslate();

  const initialValues: ValueType = {
    name: project.name,
    baseLanguageId: project.baseLanguage?.id,
  };

  const [cancelled, setCancelled] = useState(false);

  if (cancelled || deleteLoadable.isSuccess) {
    return <Redirect to={LINKS.PROJECTS.build()} />;
  }

  const LanguageSelect = () => {
    const projectLanguages = useProjectLanguages();
    return (
      <BaseLanguageSelect
        label={<T>project_settings_base_language</T>}
        name="baseLanguageId"
        languages={projectLanguages}
      />
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
        loading={updateLoadable.isLoading || deleteLoadable.isLoading}
        validationSchema={Validation.PROJECT_SETTINGS}
        onSubmit={handleEdit}
        onCancel={() => setCancelled(true)}
        initialValues={initialValues}
        customActions={
          <Button color="secondary" variant="outlined" onClick={handleDelete}>
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
