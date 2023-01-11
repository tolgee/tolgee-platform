import { default as React, FunctionComponent, useEffect } from 'react';
import {
  Box,
  Dialog,
  DialogContent,
  DialogTitle,
  MenuItem,
} from '@mui/material';
import { T } from '@tolgee/react';
import { FormikProps } from 'formik';
import { container } from 'tsyringe';

import { BoxLoading } from 'tg.component/common/BoxLoading';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { CheckBoxGroupMultiSelect } from 'tg.component/common/form/fields/CheckBoxGroupMultiSelect';
import { Select } from 'tg.component/common/form/fields/Select';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { LINKS } from 'tg.constants/links';
import { redirect } from 'tg.hooks/redirect';
import { MessageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';

type ApiKeyModel = components['schemas']['ApiKeyModel'];
type EditApiKeyDTO = components['schemas']['V2EditApiKeyDto'];

interface Value {
  scopes: string[];
  projectId: number;
}

interface Props {
  editKey?: ApiKeyModel;
  loading?: boolean;
  onClose?: () => void;
  onSaved?: (data: components['schemas']['ApiKeyModel']) => void;
  project?: components['schemas']['ProjectModel'];
}

const messageService = container.resolve(MessageService);

const setsIntersection = (set1: Set<unknown>, set2: Set<unknown>) =>
  new Set([...set1].filter((v) => set2.has(v)));

export const ApiKeyFormDialog: FunctionComponent<Props> = (props) => {
  const onDialogClose = () => {
    if (props.onClose) {
      props.onClose();
      return;
    }
    redirect(LINKS.USER_API_KEYS);
  };

  const projects = useApiQuery({
    url: '/v2/projects',
    method: 'get',
    query: { size: 1000 },
    options: {
      enabled: !props.project,
    },
  });

  const scopes = useApiQuery({
    url: '/v2/api-keys/availableScopes',
    method: 'get',
  });

  const editLoadable = useApiMutation({
    url: '/v2/api-keys/{apiKeyId}',
    method: 'put',
    invalidatePrefix: '/v2/api-keys',
  });

  const generateLoadable = useApiMutation({
    url: '/v2/api-keys',
    method: 'post',
    invalidatePrefix: '/v2/api-keys',
  });

  const getAvailableScopes = (projectId?: number): Set<string> => {
    const userPermissionType =
      projects?.data?._embedded?.projects?.find((r) => r.id === projectId)
        ?.computedPermissions.type || props.project?.computedPermissions.type;
    if (!userPermissionType || !scopes?.data) {
      return new Set();
    }
    return new Set(scopes.data[userPermissionType]);
  };

  const handleEdit = (value) =>
    editLoadable.mutateAsync(
      {
        path: {
          apiKeyId: props.editKey!.id,
        },
        content: {
          'application/json': {
            scopes: Array.from(value.scopes),
          } as EditApiKeyDTO,
        },
      },
      {
        onSuccess: () => {
          messageService.success(<T>api_key_successfully_edited</T>);
          redirect(LINKS.USER_API_KEYS);
        },
      }
    );

  const handleAdd = (value) => {
    if (props.project) {
      value.projectId = props.project.id;
    }
    generateLoadable.mutate(
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
          messageService.success(<T>api_key_successfully_generated</T>);
          if (props.onSaved) {
            props.onSaved(data);
            return;
          }
          redirect(LINKS.USER_API_KEYS);
        },
      }
    );
  };

  const getInitialValues = () => {
    const projectId =
      projects.data?._embedded?.projects?.[0]?.id || props.project?.id;

    const availableScopes = getAvailableScopes(projectId);

    if (props.editKey) {
      const currentScopes = props.editKey.scopes.filter((currentScope) =>
        availableScopes.has(currentScope)
      );

      const currentScopesSet = new Set(currentScopes);

      return {
        projectId: props.editKey.projectId,
        //check all scopes by default
        scopes: currentScopesSet,
      };
    }

    return {
      projectId: projectId,
      //check all scopes checked by default
      scopes: availableScopes,
    };
  };

  if (projects.isLoading || scopes.isLoading) {
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
        {props.editKey ? (
          <T>edit_api_key_title</T>
        ) : (
          <T>generate_api_key_title</T>
        )}
      </DialogTitle>
      <DialogContent>
        {(projects.data && projects.data?._embedded === undefined && (
          <T>cannot_add_api_key_without_project_message</T>
        )) || (
          <>
            {props.loading && <BoxLoading />}
            {(projects.data || props.project) && scopes.data && (
              <StandardForm
                onSubmit={props.editKey ? handleEdit : handleAdd}
                saveActionLoadable={
                  props.editKey ? editLoadable : generateLoadable
                }
                onCancel={() => onDialogClose()}
                initialValues={getInitialValues()}
                validationSchema={
                  props.editKey && props.editKey.projectId
                    ? Validation.EDIT_API_KEY
                    : Validation.CREATE_API_KEY
                }
              >
                {(formikProps: FormikProps<Value>) => {
                  useEffect(() => {
                    formikProps.setFieldValue(
                      'scopes',
                      setsIntersection(
                        getAvailableScopes(formikProps.values.projectId),
                        formikProps.values.scopes as any
                      )
                    );
                  }, [formikProps.values.projectId]);

                  return (
                    <>
                      {!props.editKey && !props.project && (
                        <Select
                          fullWidth
                          name="projectId"
                          label="Project"
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
                      <Box mt={2}>
                        <CheckBoxGroupMultiSelect
                          label="Scopes"
                          name="scopes"
                          options={getAvailableScopes(
                            formikProps.values.projectId
                          )}
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
