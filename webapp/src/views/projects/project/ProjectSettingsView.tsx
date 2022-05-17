import { FunctionComponent, useState } from 'react';
import {
  Box,
  Button,
  styled,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Redirect } from 'react-router-dom';
import { container } from 'tsyringe';

import { SmallProjectAvatar } from 'tg.component/navigation/SmallProjectAvatar';
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
import { ProjectTransferModal } from 'tg.views/projects/project/components/ProjectTransferModal';
import { ProjectProfileAvatar } from './ProjectProfileAvatar';

const messageService = container.resolve(MessageService);

type ValueType = components['schemas']['EditProjectDTO'];

const StyledDangerZone = styled(Box)`
  border-radius: ${({ theme }) => theme.shape.borderRadius};
  border: 1px solid ${({ theme }) => theme.palette.error.dark};
`;

const StyledDangerZonePart = styled(Box)`
  display: flex;
  gap: ${({ theme }) => theme.spacing(2)};
`;

const StyledDangerButton = styled(Button)`
  white-space: nowrap;
  flex-shrink: 0;
`;

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

  const theme = useTheme();
  const isSmOrLower = useMediaQuery(theme.breakpoints.down('md'));

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
      lg={7}
      md={9}
      containerMaxWidth="lg"
      navigation={[
        [
          project.name,
          LINKS.PROJECT_DASHBOARD.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
          <SmallProjectAvatar key={0} project={project} />,
        ],
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
        >
          <TextField
            variant="standard"
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
        <StyledDangerZone p={2}>
          <StyledDangerZonePart
            alignItems={isSmOrLower ? 'start' : 'center'}
            flexDirection={isSmOrLower ? 'column' : 'row'}
          >
            <Box flexGrow={1} mr={1}>
              <Typography variant="body1">
                <T>this_will_delete_project_forever</T>
              </Typography>
            </Box>
            <StyledDangerButton variant="outlined" onClick={handleDelete}>
              <T>delete_project_button</T>
            </StyledDangerButton>
          </StyledDangerZonePart>
          <StyledDangerZonePart
            alignItems={isSmOrLower ? 'start' : 'center'}
            flexDirection={isSmOrLower ? 'column' : 'row'}
            mt={2}
          >
            <Box flexGrow={1} mr={1}>
              <Typography variant="body1">
                <T>this_will_transfer_project</T>
              </Typography>
            </Box>
            <StyledDangerButton
              data-cy="project-settings-transfer-button"
              variant="outlined"
              onClick={() => {
                setTransferDialogOpen(true);
              }}
            >
              <T>transfer_project_button</T>
            </StyledDangerButton>

            <ProjectTransferModal
              open={transferDialogOpen}
              onClose={() => setTransferDialogOpen(false)}
            />
          </StyledDangerZonePart>
        </StyledDangerZone>
      </Box>
    </BaseView>
  );
};
