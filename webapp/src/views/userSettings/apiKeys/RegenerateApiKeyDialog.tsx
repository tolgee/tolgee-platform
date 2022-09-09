import { default as React, FunctionComponent } from 'react';
import { Dialog, DialogContent, DialogTitle } from '@mui/material';
import { T } from '@tolgee/react';

import { BoxLoading } from 'tg.component/common/BoxLoading';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { LINKS, PARAMS } from 'tg.constants/links';
import { redirect } from 'tg.hooks/redirect';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useRouteMatch } from 'react-router-dom';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { ExpirationDateField } from 'tg.component/common/form/epirationField/ExpirationDateField';
import { useExpirationDateOptions } from 'tg.component/common/form/epirationField/useExpirationDateOptions';

interface Props {
  onGenerated: (data: components['schemas']['RevealedApiKeyModel']) => void;
}

export const RegenerateApiKeyDialog: FunctionComponent<Props> = (props) => {
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

  const regenerateMutation = useApiMutation({
    url: '/v2/api-keys/{apiKeyId}/regenerate',
    method: 'put',
    invalidatePrefix: '/v2/api-keys',
  });

  const handleRegenerate = (value) =>
    regenerateMutation.mutateAsync(
      {
        path: {
          apiKeyId: apiKeyLoadable!.data!.id,
        },
        content: {
          'application/json':
            value as components['schemas']['RegenerateApiKeyDto'],
        },
      },
      {
        onSuccess: (key) => {
          props.onGenerated(key);
          onDialogClose();
        },
      }
    );

  const getInitialValues = () => {
    return {
      expiresAt: apiKeyLoadable.data?.expiresAt,
    };
  };

  useGlobalLoading(apiKeyLoadable.isLoading);
  const expirationDateOptions = useExpirationDateOptions();

  return (
    <Dialog
      open={true}
      onClose={onDialogClose}
      fullWidth
      maxWidth={'xs'}
      data-cy="api-keys-create-edit-dialog"
    >
      <DialogTitle>
        <T>regenerate_api_key_title</T>
      </DialogTitle>
      <DialogContent>
        <>
          {apiKeyLoadable.isLoading && <BoxLoading />}
          {apiKeyLoadable.data && (
            <StandardForm
              onSubmit={handleRegenerate}
              saveActionLoadable={regenerateMutation}
              onCancel={() => onDialogClose()}
              initialValues={getInitialValues()}
              validationSchema={Validation.REGENERATE_API_KEY}
            >
              <>
                <ExpirationDateField options={expirationDateOptions} />
              </>
            </StandardForm>
          )}
        </>
      </DialogContent>
    </Dialog>
  );
};
