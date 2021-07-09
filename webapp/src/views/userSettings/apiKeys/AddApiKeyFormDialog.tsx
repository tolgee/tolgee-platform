import { default as React, FunctionComponent, useEffect } from 'react';
import {
  Box,
  Dialog,
  DialogContent,
  DialogTitle,
  MenuItem,
} from '@material-ui/core';
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
import { useRedirect } from 'tg.hooks/useRedirect';
import { MessageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';

type ApiKeyDTO = components['schemas']['ApiKeyDTO'];
type EditApiKeyDTO = components['schemas']['EditApiKeyDTO'];

interface Value {
  scopes: string[];
  projectId: number;
}

interface Props {
  editKey?: ApiKeyDTO;
  loading?: boolean;
}

const messageService = container.resolve(MessageService);

const setsIntersection = (set1: Set<unknown>, set2: Set<unknown>) =>
  new Set([...set1].filter((v) => set2.has(v)));

export const AddApiKeyFormDialog: FunctionComponent<Props> = (props) => {
  const onDialogClose = () => useRedirect(LINKS.USER_API_KEYS);

  const projects = useApiQuery({
    url: '/v2/projects',
    method: 'get',
    query: { size: 1000 },
  });
  const scopes = useApiQuery({
    url: '/api/apiKeys/availableScopes',
    method: 'get',
  });
  const editLoadable = useApiMutation({
    url: '/api/apiKeys/edit',
    method: 'post',
    invalidatePrefix: '/api/apiKeys',
  });
  const generateLoadable = useApiMutation({
    url: '/api/apiKeys',
    method: 'post',
    invalidatePrefix: '/api/apiKeys',
  });

  const getAvailableScopes = (projectId?: number): Set<string> => {
    const userPermissions = projects?.data?._embedded?.projects?.find(
      (r) => r.id === projectId
    )?.computedPermissions;
    if (!userPermissions || !scopes?.data) {
      return new Set();
    }
    return new Set(scopes.data[userPermissions]);
  };

  const handleEdit = (value) => {
    editLoadable.mutateAsync(
      {
        content: {
          'application/json': {
            id: props.editKey!.id,
            scopes: Array.from(value.scopes),
          } as EditApiKeyDTO,
        },
      },
      {
        onSuccess: () => {
          messageService.success(<T>api_key_successfully_edited</T>);
          useRedirect(LINKS.USER_API_KEYS);
        },
      }
    );
  };

  const handleAdd = (value) => {
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
        onSuccess() {
          messageService.success(<T>api_key_successfully_generated</T>);
          useRedirect(LINKS.USER_API_KEYS);
        },
      }
    );
  };

  const getInitialValues = () => {
    if (props.editKey) {
      return {
        projectId: props.editKey.projectId,
        //check all scopes by default
        scopes: new Set(props.editKey.scopes),
      };
    }

    const projectId = projects.data?._embedded?.projects?.[0]?.id;

    return {
      projectId: projectId,
      //check all scopes checked by default
      scopes: getAvailableScopes(projectId),
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
        {(projects.data && projects.data?._embedded?.projects?.length === 0 && (
          <T>cannot_add_api_key_without_project_message</T>
        )) || (
          <>
            {props.loading && <BoxLoading />}
            {projects.data && scopes.data && (
              <StandardForm
                onSubmit={props.editKey ? handleEdit : handleAdd}
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
                      {!props.editKey && (
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
