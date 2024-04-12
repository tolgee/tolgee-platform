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
import { DefaultNamespaceSelect } from './components/DefaultNamespaceSelect';

type FormValues = {
  name: string;
  description: string | undefined;
  baseLanguageId: number | undefined;
  defaultNamespaceId: number | '';
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
  const { allNamespacesWithNone } = useProjectNamespaces();
  return (
    <DefaultNamespaceSelect
      label={<T keyName="project_settings_base_namespace" />}
      name="defaultNamespaceId"
      namespaces={allNamespacesWithNone}
    />
  );
};

export const ProjectSettingsGeneral = () => {
  const project = useProject();
  const { leave, isLeaving } = useLeaveProject();
  const { defaultNamespace } = useProjectNamespaces();

  const initialValues = {
    name: project.name,
    baseLanguageId: project.baseLanguage?.id,
    description: project.description ?? '',
    defaultNamespaceId: defaultNamespace?.id ?? '',
  } satisfies FormValues;

  const updateLoadable = useApiMutation({
    url: '/v2/projects/{projectId}',
    method: 'put',
    invalidatePrefix: '/v2/projects',
  });

  const updateProjectSettings = (values: FormValues) => {
    const data = {
      ...values,
      description: values.description || undefined,
      defaultNamespaceId:
        values.defaultNamespaceId === 0 ? undefined : values.defaultNamespaceId,
    };
    return updateLoadable.mutateAsync({
      path: { projectId: project.id },
      content: {
        'application/json': {
          ...data,
          defaultNamespaceId:
            data.defaultNamespaceId === ''
              ? undefined
              : data.defaultNamespaceId,
          icuPlaceholders: project.icuPlaceholders,
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
