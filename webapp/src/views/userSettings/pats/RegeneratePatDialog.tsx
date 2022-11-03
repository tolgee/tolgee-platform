import { default as React, FunctionComponent } from 'react';
import {
  Box,
  Dialog,
  DialogContent,
  DialogTitle,
  Typography,
} from '@mui/material';
import { T } from '@tolgee/react';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { LINKS, PARAMS } from 'tg.constants/links';
import { redirect } from 'tg.hooks/redirect';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { ExpirationDateField } from 'tg.component/common/form/epirationField/ExpirationDateField';
import { useExpirationDateOptions } from 'tg.component/common/form/epirationField/useExpirationDateOptions';
import { useRouteMatch } from 'react-router-dom';
import { useGlobalLoading } from 'tg.component/GlobalLoading';

type RevealedPatModel = components['schemas']['RevealedPatModel'];
type RegeneratePatDto = components['schemas']['RegeneratePatDto'];

export const RegeneratePatDialog: FunctionComponent<{
  onGenerated: (pat: RevealedPatModel) => void;
}> = (props) => {
  const expirationDateOptions = useExpirationDateOptions();

  const onDialogClose = () => {
    redirect(LINKS.USER_PATS);
  };

  const id = useRouteMatch().params[PARAMS.PAT_ID];

  const patLoadable = useApiQuery({
    url: '/v2/pats/{id}',
    method: 'get',
    path: { id: id },
  });

  useGlobalLoading(patLoadable.isLoading);

  const regenerateMutation = useApiMutation({
    url: '/v2/pats/{id}/regenerate',
    method: 'put',
    options: {
      onSuccess: (r) => {
        props.onGenerated(r);
        onDialogClose();
      },
    },
    invalidatePrefix: '/v2/pats',
  });

  if (!patLoadable.data) {
    return <></>;
  }

  let initialExpiresAt = patLoadable.data.expiresAt;
  if (initialExpiresAt && initialExpiresAt < new Date().getTime()) {
    initialExpiresAt = expirationDateOptions[0].time!;
  }

  return (
    <Dialog
      open={true}
      onClose={onDialogClose}
      fullWidth
      maxWidth={'xs'}
      data-cy="api-keys-create-edit-dialog"
    >
      <DialogTitle data-cy="regenerate-pat-dialog-title">
        <T>regenerate_pat_title</T>
      </DialogTitle>
      <DialogContent data-cy="regenerate-pat-dialog-content">
        <StandardForm
          onSubmit={(values) => {
            regenerateMutation.mutate({
              path: {
                id: patLoadable.data.id,
              },
              content: { 'application/json': values },
            });
          }}
          saveActionLoadable={regenerateMutation}
          onCancel={() => onDialogClose()}
          submitButtonInner={<T keyName="pat-form-regenerate-submit-button" />}
          initialValues={
            {
              expiresAt: initialExpiresAt,
            } as RegeneratePatDto
          }
        >
          <ExpirationDateField options={expirationDateOptions} />

          <Box my={4}>
            <Typography variant="body2">
              <T keyName="token-regenerate-message" />
            </Typography>
          </Box>
        </StandardForm>
      </DialogContent>
    </Dialog>
  );
};
