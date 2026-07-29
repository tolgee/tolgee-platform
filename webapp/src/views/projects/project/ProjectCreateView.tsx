import { FunctionComponent } from 'react';
import { Box, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { FormikProps } from 'formik';
import { useHistory } from 'react-router-dom';

import { TextField } from 'tg.component/common/form/fields/TextField';
import { BaseFormView } from 'tg.component/layout/BaseFormView';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { CreateProjectFormValues } from 'tg.views/projects/project/types';
import {
  useCanCreateProject,
  usePreferredOrganization,
} from 'tg.globalContext/helpers';
import { OrganizationSwitch } from 'tg.component/organizationSwitch/OrganizationSwitch';
import { messageService } from 'tg.service/MessageService';

import { BaseLanguageSelect } from 'tg.views/projects/project/components/BaseLanguageSelect';
import { CreateProjectLanguagesArrayField } from 'tg.views/projects/project/components/CreateProjectLanguagesArrayField';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';

const RefusalMessage: FunctionComponent<
  React.PropsWithChildren<{ dataCy: string }>
> = ({ dataCy, children }) => (
  <Typography variant="body2" color="error" data-cy={dataCy}>
    {children}
  </Typography>
);

export const ProjectCreateView: FunctionComponent<
  React.PropsWithChildren<unknown>
> = () => {
  const history = useHistory();
  const { quickStartCompleteStep } = useGlobalActions();
  const createProjectLoadable = useApiMutation({
    url: '/v2/projects',
    method: 'post',
    fetchOptions: { disableErrorNotification: true },
    invalidatePrefix: '/v2/projects',
  });
  const { t } = useTranslate();
  const { preferredOrganization } = usePreferredOrganization();
  const { canCreateProject, isFetching } = useCanCreateProject();

  const onSubmit = (values: CreateProjectFormValues) => {
    if (!preferredOrganization) {
      return;
    }
    createProjectLoadable.mutate(
      {
        content: {
          'application/json': {
            ...values,
            name: values.name.trim(),
            languages: values.languages.filter((l) => !!l),
            organizationId: preferredOrganization.id,
          },
        },
      },
      {
        onSuccess(data) {
          messageService.success(<T keyName="project_created_message" />);
          history.push(
            LINKS.PROJECT_DASHBOARD.build({ [PARAMS.PROJECT_ID]: data.id })
          );
          quickStartCompleteStep('new_project');
        },
      }
    );
  };

  const initialValues: CreateProjectFormValues = {
    name: '',
    languages: [
      { tag: 'en', name: 'English', originalName: 'English', flagEmoji: '🇬🇧' },
    ],
    baseLanguageTag: 'en',
    icuPlaceholders: true,
  };

  const refusalMessage = preferredOrganization ? (
    <RefusalMessage dataCy="project-create-no-permission-message">
      <T
        keyName="project_create_no_permission_message"
        defaultValue="You don't have permission to create a project in this organization."
      />
    </RefusalMessage>
  ) : (
    <RefusalMessage dataCy="project-create-no-organization-message">
      <T
        keyName="project_create_no_organization_message"
        defaultValue="You are not a member of any organization."
      />
    </RefusalMessage>
  );

  return (
    <DashboardPage>
      <BaseFormView
        maxWidth="narrow"
        windowTitle={t('create_project_view')}
        title={t('create_project_view')}
        initialValues={initialValues}
        onSubmit={onSubmit}
        saveActionLoadable={createProjectLoadable}
        validationSchema={Validation.PROJECT_CREATION(t)}
        disabled={isFetching}
        submitDisabledReason={!canCreateProject ? refusalMessage : undefined}
        switcher={<OrganizationSwitch />}
      >
        {(props: FormikProps<CreateProjectFormValues>) => {
          return (
            <Box>
              <Box sx={{ mb: 1 }}>
                <Typography variant="h6">
                  <T keyName="create_project_name_label" />
                </Typography>
                <TextField
                  size="small"
                  autoFocus
                  data-cy="project-name-field"
                  name="name"
                  required={true}
                />
              </Box>
              <Box mb={2}>
                <Typography variant="h6">
                  <T keyName="project_create_languages_title" />
                </Typography>
              </Box>
              <CreateProjectLanguagesArrayField />
              <Box mt={4} mb={4} maxWidth={200}>
                <Typography variant="h6">
                  <T keyName="project_create_base_language_label" />
                </Typography>
                <BaseLanguageSelect
                  valueKey="tag"
                  name="baseLanguageTag"
                  languages={props.values.languages!}
                />
              </Box>
            </Box>
          );
        }}
      </BaseFormView>
    </DashboardPage>
  );
};
