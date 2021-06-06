import * as React from 'react';
import { FunctionComponent, useState } from 'react';
import { useSelector } from 'react-redux';
import { AppState } from '../../../../store';
import { container } from 'tsyringe';
import { ProjectActions } from '../../../../store/project/ProjectActions';
import { LanguageDTO } from '../../../../service/response.types';
import { LINKS } from '../../../../constants/links';
import { Redirect } from 'react-router-dom';
import { TextField } from '../../../common/form/fields/TextField';
import { BaseFormView } from '../../../layout/BaseFormView';
import { FieldArray } from '../../../common/form/fields/FieldArray';
import { Validation } from '../../../../constants/GlobalValidationSchema';
import { DashboardPage } from '../../../layout/DashboardPage';
import { T } from '@tolgee/react';
import { Grid } from '@material-ui/core';
import OwnerSelect from './components/OwnerSelect';
import { useConfig } from '../../../../hooks/useConfig';
import { components } from '../../../../service/apiSchema.generated';

const actions = container.resolve(ProjectActions);

type ValueType = {
  name: string;
  languages: Partial<LanguageDTO>[];
  owner: number;
};

export const ProjectCreateView: FunctionComponent = () => {
  const loadable = useSelector(
    (state: AppState) => state.projects.loadables.createProject
  );

  const config = useConfig();

  const onSubmit = (values: ValueType) => {
    const data = { ...values } as
      | components['schemas']['CreateProjectDTO']
      | any;
    if (values.owner !== 0) {
      data.organizationId = values.owner;
    }
    delete data.owner;
    actions.loadableActions.createProject.dispatch(data);
  };

  const initialValues: ValueType = {
    name: '',
    languages: [{ abbreviation: '', name: '' }],
    owner: 0,
  };

  const [cancelled, setCancelled] = useState(false);

  if (cancelled) {
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
        saveActionLoadable={loadable}
        validationSchema={Validation.REPOSITORY_CREATION}
      >
        <>
          <Grid container spacing={2}>
            {config.authentication && (
              <Grid item lg={3} md={4} sm={12} xs={12}>
                <OwnerSelect />
              </Grid>
            )}

            <Grid item lg md sm xs>
              <TextField
                data-cy="project-name-field"
                label={<T>create_project_name_label</T>}
                name="name"
                required={true}
              />
            </Grid>
          </Grid>
          <FieldArray name="languages">
            {(n) => (
              <>
                <TextField
                  data-cy="project-language-name-field"
                  fullWidth={false}
                  label={<T>create_project_language_name_label</T>}
                  name={n('name')}
                  required={true}
                />
                <TextField
                  data-cy="project-language-abbreviation-field"
                  fullWidth={false}
                  label={<T>create_project_language_abbreviation_label</T>}
                  name={n('abbreviation')}
                  required={true}
                />
              </>
            )}
          </FieldArray>
        </>
      </BaseFormView>
    </DashboardPage>
  );
};
