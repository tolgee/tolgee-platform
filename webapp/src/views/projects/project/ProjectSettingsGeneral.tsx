import { useState } from 'react';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { ProjectProfileAvatar } from './ProjectProfileAvatar';
import { BaseLanguageSelect } from './components/BaseLanguageSelect';
import { T, useTranslate } from '@tolgee/react';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { messageService } from 'tg.service/MessageService';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useLeaveProject } from '../useLeaveProject';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { FieldLabel } from 'tg.component/FormField';
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Typography,
  styled,
} from '@mui/material';
import { ProjectLanguagesProvider } from 'tg.hooks/ProjectLanguagesProvider';

type FormValues = {
  name: string;
  description: string | undefined;
  baseLanguageId: number | undefined;
};

type TmConflict = { id: number; name: string };

const StyledContainer = styled('div')`
  display: grid;
  grid-template: 'fields avatar';
  grid-template-columns: 1fr auto;
  gap: 24px;
  margin-top: 24px;

  ${({ theme }) => theme.breakpoints.down('md')} {
    grid-template:
      'avatar'
      'fields';
    grid-template-columns: 1fr;
  }
`;

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

export const ProjectSettingsGeneral = () => {
  const project = useProject();
  const { t } = useTranslate();
  const { leave, isLeaving } = useLeaveProject();

  const [conflictDialog, setConflictDialog] = useState<{
    values: FormValues;
    conflicts: TmConflict[];
  } | null>(null);
  const [unassigning, setUnassigning] = useState(false);

  const initialValues = {
    name: project.name,
    baseLanguageId: project.baseLanguage?.id,
    description: project.description ?? '',
  } satisfies FormValues;

  const updateLoadable = useApiMutation({
    url: '/v2/projects/{projectId}',
    method: 'put',
    invalidatePrefix: '/v2/projects',
    fetchOptions: {
      disableErrorNotification: true,
    },
  });

  const unassignMutation = useApiMutation({
    url: '/v2/projects/{projectId}/translation-memories/{translationMemoryId}',
    method: 'delete',
  });

  const updateProjectSettings = (values: FormValues) => {
    return updateLoadable.mutateAsync({
      path: { projectId: project.id },
      content: {
        'application/json': {
          icuPlaceholders: project.icuPlaceholders,
          suggestionsMode: project.suggestionsMode,
          translationProtection: project.translationProtection,
          defaultNamespaceId: project.defaultNamespace?.id,
          useNamespaces: project.useNamespaces,
          useBranching: project.useBranching,
          ...values,
          description: values.description || undefined,
        },
      },
    });
  };

  const handleEdit = async (values: FormValues) => {
    try {
      await updateProjectSettings(values);
      messageService.success(
        <T keyName="project_successfully_edited_message" />
      );
    } catch (error: any) {
      if (
        error?.code === 'cannot_change_project_base_language_tm_conflict' &&
        Array.isArray(error.params)
      ) {
        setConflictDialog({
          values,
          conflicts: error.params as TmConflict[],
        });
        // Reset loadable so StandardForm doesn't render a lingering error banner.
        updateLoadable.reset();
        return;
      }
      throw error;
    }
  };

  const confirmUnassignAndSave = async () => {
    if (!conflictDialog) return;
    setUnassigning(true);
    try {
      for (const conflict of conflictDialog.conflicts) {
        await unassignMutation.mutateAsync({
          path: { projectId: project.id, translationMemoryId: conflict.id },
          query: { keepData: false },
        });
      }
      await updateProjectSettings(conflictDialog.values);
      messageService.success(
        <T keyName="project_successfully_edited_message" />
      );
      setConflictDialog(null);
    } catch {
      messageService.error(
        <T
          keyName="project_base_language_tm_conflict_retry_failed"
          defaultValue="Failed to save after unassigning. Please try again."
        />
      );
    } finally {
      setUnassigning(false);
    }
  };

  return (
    <StyledContainer>
      <Box gridArea="avatar">
        <ProjectProfileAvatar />
      </Box>
      <Box gridArea="fields">
        <StandardForm
          validationSchema={Validation.PROJECT_SETTINGS}
          initialValues={initialValues}
          onSubmit={handleEdit}
          saveActionLoadable={updateLoadable}
          hideCancel
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
          <Box display="grid" gap={2} mb={4}>
            <Box>
              <FieldLabel>
                <T keyName="project_settings_name_label" />
              </FieldLabel>
              <TextField
                size="small"
                name="name"
                required={true}
                data-cy="project-settings-name"
                sx={{ mt: 0 }}
              />
            </Box>
            <Box>
              <FieldLabel>
                <T keyName="project_settings_description_label" />
              </FieldLabel>
              <TextField
                size="small"
                minRows={2}
                multiline
                name="description"
                data-cy="project-settings-description"
                sx={{ mt: 0 }}
              />
            </Box>
            <ProjectLanguagesProvider>
              <LanguageSelect />
            </ProjectLanguagesProvider>
          </Box>
        </StandardForm>
      </Box>
      <Dialog
        open={conflictDialog !== null}
        onClose={() => setConflictDialog(null)}
        data-cy="project-base-language-tm-conflict-dialog"
      >
        <DialogTitle>
          <T
            keyName="project_base_language_tm_conflict_title"
            defaultValue="Conflicting translation memories"
          />
        </DialogTitle>
        <DialogContent>
          <DialogContentText>
            <T
              keyName="project_base_language_tm_conflict_message"
              defaultValue="The following shared translation memories are assigned to this project but use a different source language. Change the base language by unassigning them."
            />
          </DialogContentText>
          <Box
            component="ul"
            sx={{ mt: 1.5, mb: 0, pl: 3 }}
            data-cy="project-base-language-tm-conflict-list"
          >
            {conflictDialog?.conflicts.map((c) => (
              <Typography component="li" variant="body2" key={c.id}>
                {c.name}
              </Typography>
            ))}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => setConflictDialog(null)}
            disabled={unassigning}
          >
            <T keyName="global_cancel_button" defaultValue="Cancel" />
          </Button>
          <LoadingButton
            variant="contained"
            color="primary"
            onClick={confirmUnassignAndSave}
            loading={unassigning}
            data-cy="project-base-language-tm-conflict-confirm"
          >
            {t(
              'project_base_language_tm_conflict_confirm',
              'Unassign and save'
            )}
          </LoadingButton>
        </DialogActions>
      </Dialog>
    </StyledContainer>
  );
};
