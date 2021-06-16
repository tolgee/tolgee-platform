import { useRedirect } from '../../../../hooks/useRedirect';
import { LINKS } from '../../../../constants/links';
import {
  Box,
  Dialog,
  DialogContent,
  DialogTitle,
  MenuItem,
} from '@material-ui/core';
import { StandardForm } from '../../../common/form/StandardForm';
import { Select } from '../../../common/form/fields/Select';
import { default as React, FunctionComponent, useEffect } from 'react';
import { container } from 'tsyringe';
import { UserApiKeysActions } from '../../../../store/api_keys/UserApiKeysActions';
import { BoxLoading } from '../../../common/BoxLoading';
import { FormikProps } from 'formik';
import { CheckBoxGroupMultiSelect } from '../../../common/form/fields/CheckBoxGroupMultiSelect';
import { ApiKeyDTO } from '../../../../service/response.types';
import { EditApiKeyDTO } from '../../../../service/request.types';
import { Validation } from '../../../../constants/GlobalValidationSchema';
import { FullPageLoading } from '../../../common/FullPageLoading';
import { T } from '@tolgee/react';

interface Value {
  scopes: string[];
  projectId: number;
}

interface Props {
  editKey?: ApiKeyDTO;
  loading?: boolean;
}

const actions = container.resolve(UserApiKeysActions);

export const AddApiKeyFormDialog: FunctionComponent<Props> = (props) => {
  const onDialogClose = () => useRedirect(LINKS.USER_API_KEYS);

  const projects = actions.useSelector((s) => s.loadables.projects);
  const scopes = actions.useSelector((s) => s.loadables.scopes);
  const editLoadable = actions.useSelector((s) => s.loadables.edit);
  const generateLoadable = actions.useSelector(
    (s) => s.loadables.generateApiKey
  );

  useEffect(() => {
    actions.loadableActions.projects.dispatch({
      query: { pageable: { size: 1000 } },
    });
    actions.loadableActions.scopes.dispatch();
  }, []);

  const getAvailableScopes = (projectId?: number): Set<string> => {
    const userPermissions = projects?.data?._embedded?.projects?.find(
      (r) => r.id === projectId
    )?.computedPermissions;
    // eslint-disable-next-line no-console
    if (!userPermissions || !scopes?.data) {
      return new Set();
    }
    return new Set(scopes.data[userPermissions]);
  };

  const onSubmit = (value) => {
    if (props.editKey) {
      actions.loadableActions.edit.dispatch({
        id: props.editKey.id,
        scopes: Array.from(value.scopes),
      } as EditApiKeyDTO);
    } else {
      actions.loadableActions.generateApiKey.dispatch({
        ...value,
        scopes: Array.from(value.scopes),
      } as Value);
    }
  };

  useEffect(() => {
    if (editLoadable.loaded) {
      actions.loadableReset.edit.dispatch();
      actions.loadableReset.list.dispatch();
      useRedirect(LINKS.USER_API_KEYS);
    }
  }, [editLoadable.loaded]);

  useEffect(() => {
    if (generateLoadable.loaded) {
      actions.loadableReset.generateApiKey.dispatch();
      actions.loadableReset.list.dispatch();
      useRedirect(LINKS.USER_API_KEYS);
    }
  }, [generateLoadable.loaded]);

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

  if (projects.loading || scopes.loading) {
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
        {(projects.loaded &&
          projects.data?._embedded?.projects?.length === 0 && (
            <T>cannot_add_api_key_without_project_message</T>
          )) || (
          <>
            {(projects.loading || scopes.loading || props.loading) && (
              <BoxLoading />
            )}
            {projects.loaded && scopes.loaded && (
              <StandardForm
                onSubmit={onSubmit}
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
                      getAvailableScopes(formikProps.values.projectId)
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
