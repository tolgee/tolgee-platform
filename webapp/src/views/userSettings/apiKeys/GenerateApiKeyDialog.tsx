import { default as React, FunctionComponent, useEffect } from 'react';
import {
  Box,
  Dialog,
  DialogContent,
  DialogTitle,
  MenuItem,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { FormikProps } from 'formik';

import { BoxLoading } from 'tg.component/common/BoxLoading';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { CheckBoxGroupMultiSelect } from 'tg.component/common/form/fields/CheckBoxGroupMultiSelect';
import { Select } from 'tg.component/common/form/fields/Select';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { LINKS } from 'tg.constants/links';
import { messageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { ExpirationDateField } from 'tg.component/common/form/epirationField/ExpirationDateField';
import { useExpirationDateOptions } from 'tg.component/common/form/epirationField/useExpirationDateOptions';
import { ProjectModel } from 'tg.fixtures/permissions';
import { useHistory } from 'react-router-dom';

type Scope = components['schemas']['ComputedPermissionModel']['scopes'][number];

interface Value {
  scopes: string[];
  projectId: number;
}

interface Props {
  loading?: boolean;
  onClose?: () => void;
  onGenerated: (data: components['schemas']['RevealedApiKeyModel']) => void;
  project?: components['schemas']['ProjectModel'];
  initialDescriptionValue?: string;
}

const setsIntersection = (set1: Set<unknown>, set2: Set<unknown>) =>
  new Set([...set1].filter((v) => set2.has(v)));

export const GenerateApiKeyDialog: FunctionComponent<Props> = (props) => {
  const history = useHistory();
  const onDialogClose = () => {
    if (props.onClose) {
      props.onClose();
      return;
    }
    history.push(LINKS.USER_API_KEYS.build());
  };

  const { t } = useTranslate();

  const projects = useApiQuery({
    url: '/v2/projects',
    method: 'get',
    query: { size: 1000 },
    options: {
      enabled: !props.project,
    },
  });

  const generateMutation = useApiMutation({
    url: '/v2/api-keys',
    method: 'post',
    invalidatePrefix: '/v2/api-keys',
  });

  const handleGenerate = (value) => {
    if (props.project) {
      value.projectId = props.project.id;
    }
    generateMutation.mutate(
      {
        content: {
          'application/json': {
            ...value,
            scopes: Array.from(value.scopes),
          } as Value,
        },
      },
      {
        onSuccess(data) {
          messageService.success(
            <T keyName="api_key_successfully_generated" />
          );
          props.onGenerated(data);
          onDialogClose();
        },
      }
    );
  };

  const expirationDateOptions = useExpirationDateOptions();

  const getProject = (projectId?: number) => {
    return (
      projects.data?._embedded?.projects?.find(
        (p) => p.id === projectId || projectId === undefined
      ) || props.project
    );
  };

  const getInitialValues = (project: ProjectModel) => {
    const projectScoopes = new Set(project.computedPermission.scopes);
    const preselectedScopes = new Set<Scope>([
      'keys.view',
      'keys.create',
      'keys.edit',
      'translations.view',
      'translations.edit',
      'translations.state-edit',
      'screenshots.view',
      'screenshots.upload',
      'screenshots.delete',
    ]);

    preselectedScopes.forEach((value) => {
      if (!projectScoopes.has(value)) {
        preselectedScopes.delete(value);
      }
    });

    return {
      projectId: project.id,
      //all scopes checked by default
      scopes: preselectedScopes,
      description: props.initialDescriptionValue || '',
      expiresAt: expirationDateOptions[0].time,
    };
  };

  if (projects.isLoading) {
    return <FullPageLoading />;
  }

  return (
    <Dialog
      open={true}
      onClose={onDialogClose}
      fullWidth
      maxWidth={'xs'}
      data-cy="api-keys-create-edit-dialog"
    >
      <DialogTitle>
        <T keyName="generate_api_key_title" />
      </DialogTitle>
      <DialogContent>
        {(projects.data && projects.data?._embedded === undefined && (
          <T keyName="cannot_add_api_key_without_project_message" />
        )) || (
          <>
            {props.loading && <BoxLoading />}
            {(projects.data || props.project) && (
              <StandardForm<any>
                onSubmit={handleGenerate}
                saveActionLoadable={generateMutation}
                onCancel={() => onDialogClose()}
                initialValues={getInitialValues(getProject()!)}
                validationSchema={Validation.CREATE_API_KEY}
              >
                {(formikProps: FormikProps<Value>) => {
                  const project = getProject(formikProps.values.projectId)!;

                  const availableScopes = new Set(
                    project.computedPermission.scopes ?? []
                  );

                  useEffect(() => {
                    formikProps.setFieldValue(
                      'scopes',
                      setsIntersection(
                        availableScopes,
                        new Set(formikProps.values.scopes)
                      )
                    );
                  }, [project]);

                  return (
                    <>
                      {!props.project && (
                        <Select
                          fullWidth
                          name="projectId"
                          label="Project"
                          minHeight={false}
                          renderValue={(v) =>
                            projects.data?._embedded?.projects?.find(
                              (r) => r.id === v
                            )?.name
                          }
                        >
                          {projects.data?._embedded?.projects?.map((r) => (
                            <MenuItem
                              data-cy="api-keys-project-select-item"
                              key={r.id}
                              value={r.id}
                            >
                              {r.name}
                            </MenuItem>
                          ))}
                        </Select>
                      )}
                      <TextField
                        inputProps={{
                          'data-cy':
                            'generate-api-key-dialog-description-input',
                        }}
                        autoFocus
                        name="description"
                        placeholder={t('api-key-description-placeholder')}
                        label={<T keyName="api-key-form-description" />}
                      />

                      <ExpirationDateField options={expirationDateOptions} />
                      <Box mt={2}>
                        <CheckBoxGroupMultiSelect
                          label="Scopes"
                          name="scopes"
                          options={availableScopes}
                        />
                      </Box>
                    </>
                  );
                }}
              </StandardForm>
            )}
          </>
        )}
      </DialogContent>
    </Dialog>
  );
};
