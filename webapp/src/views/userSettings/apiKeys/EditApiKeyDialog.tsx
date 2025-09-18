import { default as React, FunctionComponent } from 'react';
import { Box, Dialog, DialogContent, DialogTitle } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { BoxLoading } from 'tg.component/common/BoxLoading';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { CheckBoxGroupMultiSelect } from 'tg.component/PermissionsSettings/CheckBoxGroupMultiSelect';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { LINKS, PARAMS } from 'tg.constants/links';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useHistory, useRouteMatch } from 'react-router-dom';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { Scopes } from 'tg.fixtures/permissions';
import { messageService } from 'tg.service/MessageService';

type EditApiKeyDTO = components['schemas']['V2EditApiKeyDto'];

interface Props {
  loading?: boolean;
  onSaved?: (data: components['schemas']['ApiKeyModel']) => void;
}

export const EditApiKeyDialog: FunctionComponent<Props> = (props) => {
  const history = useHistory();
  const onDialogClose = () => {
    history.push(LINKS.USER_API_KEYS.build());
  };

  const id = useRouteMatch().params[PARAMS.API_KEY_ID];

  const apiKeyLoadable = useApiQuery({
    url: '/v2/api-keys/{keyId}',
    method: 'get',
    path: {
      keyId: id,
    },
    options: {
      cacheTime: 0,
      staleTime: 0,
    },
  });

  const projectLoadable = useApiQuery({
    url: '/v2/projects/{projectId}',
    method: 'get',
    path: {
      projectId: apiKeyLoadable?.data?.projectId as any,
    },
    options: {
      enabled: !!apiKeyLoadable.data,
    },
  });

  const editMutation = useApiMutation({
    url: '/v2/api-keys/{apiKeyId}',
    method: 'put',
    invalidatePrefix: '/v2/api-keys',
  });

  const { t } = useTranslate();

  const handleEdit = (value) =>
    editMutation.mutateAsync(
      {
        path: {
          apiKeyId: apiKeyLoadable!.data!.id,
        },
        content: {
          'application/json': {
            ...value,
            scopes: Array.from(value.scopes),
          } as EditApiKeyDTO,
        },
      },
      {
        onSuccess: () => {
          messageService.success(<T keyName="api_key_successfully_edited" />);
          history.push(LINKS.USER_API_KEYS.build());
        },
      }
    );

  const availableScopes = new Set(
    projectLoadable.data?.computedPermission?.scopes ?? []
  );

  const getInitialValues = () => {
    const scopes = apiKeyLoadable.data?.scopes as Scopes;
    const currentScopes = scopes?.filter((currentScope) =>
      availableScopes.has(currentScope)
    );

    const currentScopesSet = new Set(currentScopes);

    return {
      scopes: currentScopesSet,
      description: apiKeyLoadable.data?.description,
    };
  };

  return (
    <Dialog
      open={true}
      onClose={onDialogClose}
      fullWidth
      maxWidth={'xs'}
      data-cy="api-keys-create-edit-dialog"
    >
      <DialogTitle>
        <T keyName="edit_api_key_title" />
      </DialogTitle>
      <DialogContent>
        <>
          {(apiKeyLoadable.isLoading || projectLoadable.isLoading) && (
            <BoxLoading />
          )}
          {projectLoadable.data && apiKeyLoadable.data && (
            <StandardForm
              onSubmit={handleEdit}
              saveActionLoadable={editMutation}
              onCancel={() => onDialogClose()}
              initialValues={getInitialValues()}
              validationSchema={Validation.EDIT_API_KEY}
            >
              <>
                <TextField
                  inputProps={{
                    'data-cy': 'generate-api-key-dialog-description-input',
                  }}
                  autoFocus
                  name="description"
                  placeholder={t('api-key-description-placeholder')}
                  label={<T keyName="api-key-form-description" />}
                />

                <Box mt={2}>
                  <CheckBoxGroupMultiSelect
                    label="Scopes"
                    name="scopes"
                    options={availableScopes}
                  />
                </Box>
              </>
            </StandardForm>
          )}
        </>
      </DialogContent>
    </Dialog>
  );
};
