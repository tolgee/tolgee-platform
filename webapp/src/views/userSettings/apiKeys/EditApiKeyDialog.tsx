import { default as React, FunctionComponent } from 'react';
import { Box, Dialog, DialogContent, DialogTitle } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { container } from 'tsyringe';

import { BoxLoading } from 'tg.component/common/BoxLoading';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { CheckBoxGroupMultiSelect } from 'tg.component/common/form/fields/CheckBoxGroupMultiSelect';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { LINKS, PARAMS } from 'tg.constants/links';
import { redirect } from 'tg.hooks/redirect';
import { MessageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useRouteMatch } from 'react-router-dom';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { useGlobalLoading } from 'tg.component/GlobalLoading';

type EditApiKeyDTO = components['schemas']['V2EditApiKeyDto'];

interface Props {
  loading?: boolean;
  onSaved?: (data: components['schemas']['ApiKeyModel']) => void;
}

const messageService = container.resolve(MessageService);

export const EditApiKeyDialog: FunctionComponent<Props> = (props) => {
  const onDialogClose = () => {
    redirect(LINKS.USER_API_KEYS);
  };

  const id = useRouteMatch().params[PARAMS.API_KEY_ID];

  const apiKeyLoadable = useApiQuery({
    url: '/v2/api-keys/{keyId}',
    method: 'get',
    path: {
      keyId: id,
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

  const availableScopesLoadable = useApiQuery({
    url: '/v2/api-keys/availableScopes',
    method: 'get',
  });

  const editMutation = useApiMutation({
    url: '/v2/api-keys/{apiKeyId}',
    method: 'put',
    invalidatePrefix: '/v2/api-keys',
  });

  const { t } = useTranslate();

  const getAvailableScopes = (): Set<string> => {
    const userPermissionType = projectLoadable?.data?.computedPermissions?.type;
    if (!userPermissionType || !availableScopesLoadable?.data) {
      return new Set();
    }
    return new Set(availableScopesLoadable.data[userPermissionType]);
  };

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
          messageService.success(<T>api_key_successfully_edited</T>);
          redirect(LINKS.USER_API_KEYS);
        },
      }
    );

  const getInitialValues = () => {
    const availableScopes = getAvailableScopes();

    const currentScopes = apiKeyLoadable.data?.scopes.filter((currentScope) =>
      availableScopes.has(currentScope)
    );

    const currentScopesSet = new Set(currentScopes);

    return {
      scopes: currentScopesSet,
      description: apiKeyLoadable.data?.description,
    };
  };

  useGlobalLoading(
    availableScopesLoadable.isLoading ||
      apiKeyLoadable.isLoading ||
      projectLoadable.isLoading
  );

  return (
    <Dialog
      open={true}
      onClose={onDialogClose}
      fullWidth
      maxWidth={'xs'}
      data-cy="api-keys-create-edit-dialog"
    >
      <DialogTitle>
        <T>edit_api_key_title</T>
      </DialogTitle>
      <DialogContent>
        <>
          {(availableScopesLoadable.isLoading ||
            apiKeyLoadable.isLoading ||
            projectLoadable.isLoading) && <BoxLoading />}
          {projectLoadable.data &&
            availableScopesLoadable.data &&
            apiKeyLoadable.data && (
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
                      options={getAvailableScopes()}
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
