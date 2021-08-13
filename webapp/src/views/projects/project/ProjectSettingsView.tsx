import { FunctionComponent, useState } from 'react';
import { Box, Button, Typography } from '@material-ui/core';
import { T, useTranslate } from '@tolgee/react';
import { Redirect } from 'react-router-dom';
import { container } from 'tsyringe';

import { ConfirmationDialogProps } from 'tg.component/common/ConfirmationDialog';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { BaseView } from 'tg.component/layout/BaseView';
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
import makeStyles from '@material-ui/core/styles/makeStyles';
import { ProjectTransferModal } from 'tg.views/projects/project/components/ProjectTransferModal';

const messageService = container.resolve(MessageService);

type ValueType = components['schemas']['EditProjectDTO'];

const useStyles = makeStyles((theme) => ({
  dangerZone: {
    borderRadius: theme.shape.borderRadius,
    border: `1px solid ${theme.palette.error.dark}`,
  },
}));

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

  const classes = useStyles();

  const [transferDialogOpen, setTransferDialogOpen] = useState(false);
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
      navigation={[
        [
          project.name,
          LINKS.PROJECT_TRANSLATIONS.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
        [
          t('project_settings_title'),
          LINKS.PROJECT_EDIT.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
      ]}
    >
      <StandardForm
        loading={deleteLoadable.isLoading}
        saveActionLoadable={updateLoadable}
        validationSchema={Validation.PROJECT_SETTINGS}
        onSubmit={handleEdit}
        onCancel={() => setCancelled(true)}
        initialValues={initialValues}
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
      <Box mt={2} mb={1}>
        <Typography variant={'h5'}>
          <T>project_settings_danger_zone_title</T>
        </Typography>
      </Box>
      <Box className={classes.dangerZone} p={2}>
        <Box display="flex" alignItems="center">
          <Box flexGrow={1}>
            <Typography variant="body1">
              <T>this_will_delete_project_forever</T>
            </Typography>
          </Box>
          <Button color="default" variant="outlined" onClick={handleDelete}>
            <T>delete_project_button</T>
          </Button>
        </Box>
        <Box display="flex" alignItems="center" mt={2}>
          <Box flexGrow={1}>
            <Typography variant="body1">
              <T>this_will_transfer_project</T>
            </Typography>
          </Box>
          <Button
            data-cy="project-settings-transfer-button"
            color="default"
            variant="outlined"
            onClick={() => {
              setTransferDialogOpen(true);
            }}
          >
            <T>transfer_project_button</T>
          </Button>

          <ProjectTransferModal
            open={transferDialogOpen}
            onClose={() => setTransferDialogOpen(false)}
          />
        </Box>
      </Box>
    </BaseView>
  );
};
