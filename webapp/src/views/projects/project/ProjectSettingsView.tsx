import { FunctionComponent, useState } from 'react';
import { Box, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Redirect } from 'react-router-dom';
import { container } from 'tsyringe';

import { ConfirmationDialogProps } from 'tg.component/common/ConfirmationDialog';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { LINKS, PARAMS } from 'tg.constants/links';
import { ProjectLanguagesProvider } from 'tg.hooks/ProjectLanguagesProvider';
import { confirmation } from 'tg.hooks/confirmation';
import { useProject } from 'tg.hooks/useProject';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { MessageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';

import { BaseLanguageSelect } from './components/BaseLanguageSelect';
import { ProjectTransferModal } from 'tg.views/projects/project/components/ProjectTransferModal';
import { DangerZone } from 'tg.component/DangerZone/DangerZone';
import { ProjectProfileAvatar } from './ProjectProfileAvatar';
import { BaseProjectView } from '../BaseProjectView';
import { useLeaveProject } from '../useLeaveProject';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { DangerButton } from 'tg.component/DangerZone/DangerButton';

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

  const [transferDialogOpen, setTransferDialogOpen] = useState(false);
  const confirm = (options: ConfirmationDialogProps) =>
    confirmation({
      title: <T keyName="delete_project_dialog_title" />,
      ...options,
    });

  const handleEdit = (values: ValueType) => {
    const data = {
      ...values,
      description: values.description || undefined,
    };
    updateLoadable.mutate(
      {
        path: { projectId: project.id },
        content: { 'application/json': data },
      },
      {
        onSuccess() {
          messageService.success(
            <T keyName="project_successfully_edited_message" />
          );
        },
      }
    );
  };

  const handleDelete = () => {
    confirm({
      message: (
        <T
          keyName="delete_project_confirmation_message"
          params={{ name: project.name }}
        />
      ),
      onConfirm: () =>
        deleteLoadable.mutate(
          { path: { projectId: project.id } },
          {
            onSuccess() {
              messageService.success(<T keyName="project_deleted_message" />);
            },
          }
        ),
      hardModeText: project.name.toUpperCase(),
    });
  };

  const { leave, isLeaving } = useLeaveProject();

  const { t } = useTranslate();

  const initialValues: ValueType = {
    name: project.name,
    baseLanguageId: project.baseLanguage?.id,
    description: project.description,
  };

  const [cancelled, setCancelled] = useState(false);

  if (cancelled || deleteLoadable.isSuccess) {
    return <Redirect to={LINKS.PROJECTS.build()} />;
  }

  const LanguageSelect = () => {
    const projectLanguages = useProjectLanguages();
    return (
      <BaseLanguageSelect
        label={<T keyName="project_settings_base_language" />}
        name="baseLanguageId"
        languages={projectLanguages}
      />
    );
  };

  return (
    <BaseProjectView
      maxWidth="narrow"
      windowTitle={t('project_settings_title')}
      navigation={[
        [
          t('project_settings_title'),
          LINKS.PROJECT_EDIT.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
      ]}
    >
      <Box data-cy="project-settings">
        <ProjectProfileAvatar />
        <StandardForm
          loading={deleteLoadable.isLoading}
          saveActionLoadable={updateLoadable}
          validationSchema={Validation.PROJECT_SETTINGS}
          onSubmit={handleEdit}
          onCancel={() => setCancelled(true)}
          initialValues={initialValues}
          customActions={
            <LoadingButton
              data-cy="project-delete-button"
              color="secondary"
              variant="outlined"
              onClick={() => leave(project.name, project.id)}
              loading={isLeaving}
            >
              <T keyName="project_leave_button" />
            </LoadingButton>
          }
        >
          <TextField
            variant="standard"
            label={<T keyName="project_settings_name_label" />}
            name="name"
            required={true}
            data-cy="project-settings-name"
          />
          <TextField
            variant="standard"
            minRows={1}
            multiline
            label={<T keyName="project_settings_description_label" />}
            name="description"
            data-cy="project-settings-description"
          />
          <ProjectLanguagesProvider>
            <LanguageSelect />
          </ProjectLanguagesProvider>
        </StandardForm>
        <Box mt={2} mb={1}>
          <Typography variant={'h5'}>
            <T keyName="project_settings_danger_zone_title" />
          </Typography>
        </Box>
        <DangerZone
          actions={[
            {
              description: <T keyName="this_will_delete_project_forever" />,
              button: (
                <DangerButton
                  onClick={handleDelete}
                  data-cy="project-settings-delete-button"
                >
                  <T keyName="delete_project_button" />
                </DangerButton>
              ),
            },
            {
              description: <T keyName="this_will_transfer_project" />,
              button: (
                <DangerButton
                  onClick={() => setTransferDialogOpen(true)}
                  data-cy="project-settings-transfer-button"
                >
                  <T keyName="transfer_project_button" />
                </DangerButton>
              ),
            },
          ]}
        />

        <ProjectTransferModal
          open={transferDialogOpen}
          onClose={() => setTransferDialogOpen(false)}
        />
      </Box>
    </BaseProjectView>
  );
};
