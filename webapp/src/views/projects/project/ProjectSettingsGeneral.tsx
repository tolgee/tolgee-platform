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
import { useProjectNamespaces } from 'tg.hooks/useProjectNamespaces';
import { BaseNamespaceSelect } from './components/BaseNamespaceSelect';

type FormValues = {
  name: string;
  description: string | undefined;
  baseLanguageId: number | undefined;
  baseNamespaceId: number | undefined;
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

const NamespaceSelect = () => {
  const { namespaces } = useProjectNamespaces();
  return (
    <BaseNamespaceSelect
      label={<T keyName="project_settings_base_namespace" />}
      name="baseNamespaceId"
      namespaces={namespaces}
    />
  );
};

export const ProjectSettingsGeneral = () => {
  const project = useProject();
  const { baseNamespace, namespaceUpdate } = useProjectNamespaces();
  const { leave, isLeaving } = useLeaveProject();

  const initialValues = {
    name: project.name,
    baseLanguageId: project.baseLanguage?.id,
    description: project.description ?? '',
    baseNamespaceId: baseNamespace?.id ?? 0,
  } satisfies FormValues;

  const updateLoadable = useApiMutation({
    url: '/v2/projects/{projectId}',
    method: 'put',
    invalidatePrefix: '/v2/projects',
  });

  const updateProjectSettings = (
    values: Pick<FormValues, 'name' | 'description' | 'baseLanguageId'>
  ) => {
    const data = {
      ...values,
      description: values.description || undefined,
    };
    return updateLoadable.mutateAsync({
      path: { projectId: project.id },
      content: {
        'application/json': {
          ...data,
          icuPlaceholders: project.icuPlaceholders,
        },
      },
    });
  };

  const updateNamespaceSetting = async (
    values: Pick<FormValues, 'baseNamespaceId'>
  ) => {
    if (baseNamespace?.id) {
      const data = { base: false };
      await namespaceUpdate.mutateAsync({
        path: { projectId: project.id, id: baseNamespace?.id },
        content: { 'application/json': data },
      });
    }

    if (values.baseNamespaceId === 0) {
      return;
    }

    const data = { base: true };
    await namespaceUpdate.mutateAsync({
      path: { projectId: project.id, id: values.baseNamespaceId! },
      content: { 'application/json': data },
    });
  };

  const handleEdit = async (values: FormValues) => {
    await Promise.all([
      updateProjectSettings(values),
      updateNamespaceSetting(values),
    ]);
    messageService.success(<T keyName="project_successfully_edited_message" />);
  };

  return (
    <StyledContainer>
      <Box gridArea="avatar">
        <ProjectProfileAvatar />
      </Box>
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
        <Box gridArea="fields" display="grid" gap={2} mb={4}>
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
          <NamespaceSelect />
        </Box>
      </StandardForm>
    </StyledContainer>
  );
};
