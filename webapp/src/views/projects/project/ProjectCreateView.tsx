import { FunctionComponent } from 'react';
import { Box, Grid, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { FormikProps } from 'formik';
import { useHistory } from 'react-router-dom';
import { container } from 'tsyringe';

import { TextField } from 'tg.component/common/form/fields/TextField';
import { BaseFormView } from 'tg.component/layout/BaseFormView';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { LINKS, PARAMS } from 'tg.constants/links';
import { MessageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';

import { BaseLanguageSelect } from './components/BaseLanguageSelect';
import { CreateProjectLanguagesArrayField } from './components/CreateProjectLanguagesArrayField';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { OrganizationSwitch } from 'tg.component/organizationSwitch/OrganizationSwitch';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';

const messageService = container.resolve(MessageService);

export type CreateProjectValueType = components['schemas']['CreateProjectDTO'];

export const ProjectCreateView: FunctionComponent = () => {
  const history = useHistory();
  const { quickStartCompleteStep } = useGlobalActions();
  const createProjectLoadable = useApiMutation({
    url: '/v2/projects',
    method: 'post',
    fetchOptions: { disableErrorNotification: true },
    invalidatePrefix: '/v2/projects',
  });
  const { t } = useTranslate();
  const { preferredOrganization, updatePreferredOrganization } =
    usePreferredOrganization();

  const onSubmit = (values: CreateProjectValueType) => {
    values.languages = values.languages.filter((l) => !!l);
    createProjectLoadable.mutate(
      {
        content: {
          'application/json': values,
        },
      },
      {
        onSuccess(data) {
          updatePreferredOrganization(values.organizationId);
          messageService.success(<T keyName="project_created_message" />);
          history.push(
            LINKS.PROJECT_DASHBOARD.build({ [PARAMS.PROJECT_ID]: data.id })
          );
          quickStartCompleteStep('new_project');
        },
      }
    );
  };

  const organizationsLoadable = useApiQuery({
    url: '/v2/organizations',
    method: 'get',
    query: {
      size: 100,
      params: {
        filterCurrentUserOwner: true,
      },
    },
  });

  const initialValues: CreateProjectValueType = {
    name: '',
    languages: [
      { tag: 'en', name: 'English', originalName: 'English', flagEmoji: '🇬🇧' },
    ],
    organizationId: preferredOrganization.id,
    baseLanguageTag: 'en',
  };

  return (
    <DashboardPage>
      <BaseFormView
        maxWidth="narrow"
        windowTitle={t('create_project_view')}
        title={t('create_project_view')}
        initialValues={initialValues}
        loading={organizationsLoadable.isLoading}
        onSubmit={onSubmit}
        saveActionLoadable={createProjectLoadable}
        validationSchema={Validation.PROJECT_CREATION(t)}
        switcher={<OrganizationSwitch ownedOnly />}
      >
        {(props: FormikProps<CreateProjectValueType>) => {
          return (
            <Box mb={8}>
              <Grid container spacing={2}>
                <Grid item lg md sm xs={12}>
                  <TextField
                    variant="standard"
                    autoFocus
                    data-cy="project-name-field"
                    label={<T keyName="create_project_name_label" />}
                    name="name"
                    required={true}
                  />
                </Grid>
              </Grid>
              <Box mb={2}>
                <Typography variant="h6">
                  <T keyName="project_create_languages_title" />
                </Typography>
              </Box>
              <CreateProjectLanguagesArrayField />
              <Box mt={4}>
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
