import { FunctionComponent, useState } from 'react';
import { Box, Grid, Typography } from '@material-ui/core';
import { T, useTranslate } from '@tolgee/react';
import { FormikProps } from 'formik';
import { Redirect } from 'react-router-dom';
import { container } from 'tsyringe';

import { TextField } from 'tg.component/common/form/fields/TextField';
import { BaseFormView } from 'tg.component/layout/BaseFormView';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { LINKS } from 'tg.constants/links';
import { useConfig } from 'tg.hooks/useConfig';
import { MessageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';

import { BaseLanguageSelect } from './components/BaseLanguageSelect';
import { CreateProjectLanguagesArrayField } from './components/CreateProjectLanguagesArrayField';
import OwnerSelect from './components/OwnerSelect';

const messageService = container.resolve(MessageService);

export type CreateProjectValueType =
  components['schemas']['CreateProjectDTO'] & {
    owner: number;
  };

export const ProjectCreateView: FunctionComponent = () => {
  const createProjectLoadable = useApiMutation({
    url: '/v2/projects',
    method: 'post',
  });
  const t = useTranslate();

  const config = useConfig();

  const onSubmit = (values: CreateProjectValueType) => {
    const { owner, ...data } = {
      ...values,
    } as components['schemas']['CreateProjectDTO'] & { owner: number };
    if (values.owner !== 0) {
      data.organizationId = owner;
      data.languages = data.languages.filter((l) => !!l);
    }
    createProjectLoadable.mutate(
      {
        content: {
          'application/json': data,
        },
      },
      {
        onSuccess() {
          messageService.success(<T>project_created_message</T>);
        },
      }
    );
  };

  const initialValues: CreateProjectValueType = {
    name: '',
    languages: [
      { tag: 'en', name: 'English', originalName: 'English', flagEmoji: '🇬🇧' },
    ],
    owner: 0,
    baseLanguageTag: 'en',
  };

  const [cancelled, setCancelled] = useState(false);

  if (cancelled || createProjectLoadable.isSuccess) {
    return <Redirect to={LINKS.PROJECTS.build()} />;
  }

  return (
    <DashboardPage>
      <BaseFormView
        lg={6}
        md={8}
        title={<T>create_project_view</T>}
        initialValues={initialValues}
        onSubmit={onSubmit}
        onCancel={() => setCancelled(true)}
        saveActionLoadable={createProjectLoadable}
        validationSchema={Validation.PROJECT_CREATION(t)}
      >
        {(props: FormikProps<CreateProjectValueType>) => {
          return (
            <Box mb={8}>
              <Grid container spacing={2}>
                <Grid item lg md sm xs={12}>
                  <TextField
                    autoFocus
                    data-cy="project-name-field"
                    label={<T>create_project_name_label</T>}
                    name="name"
                    required={true}
                  />
                </Grid>
                {config.authentication && (
                  <Grid item lg md sm xs={12}>
                    <OwnerSelect />
                  </Grid>
                )}
              </Grid>
              <Box mb={2}>
                <Typography variant="h6">
                  <T>project_create_languages_title</T>
                </Typography>
              </Box>
              <CreateProjectLanguagesArrayField />
              <Box mt={4}>
                <Typography variant="h6">
                  <T>project_create_base_language_label</T>
                </Typography>
                <BaseLanguageSelect
                  valueKey="tag"
                  name="baseLanguageTag"
                  languages={props.values.languages}
                />
              </Box>
            </Box>
          );
        }}
      </BaseFormView>
    </DashboardPage>
  );
};
