import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { ProjectProfileAvatar } from './ProjectProfileAvatar';
import { BaseLanguageSelect } from './components/BaseLanguageSelect';
import { T } from '@tolgee/react';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { messageService } from 'tg.service/MessageService';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useLeaveProject } from '../useLeaveProject';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { FieldLabel } from 'tg.component/FormField';
import { Box, styled } from '@mui/material';
import { ProjectLanguagesProvider } from 'tg.hooks/ProjectLanguagesProvider';

type FormValues = {
  name: string;
  description: string | undefined;
  baseLanguageId: number | undefined;
};

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
  const { leave, isLeaving } = useLeaveProject();

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
          ...values,
          description: values.description || undefined,
        },
      },
    });
  };

  const handleEdit = async (values: FormValues) => {
    await updateProjectSettings(values);
    messageService.success(<T keyName="project_successfully_edited_message" />);
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
    </StyledContainer>
  );
};
