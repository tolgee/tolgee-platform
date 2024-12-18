import { default as React, FunctionComponent } from 'react';
import { Dialog, DialogContent, DialogTitle } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { useHistory, useRouteMatch } from 'react-router-dom';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { useMessage } from 'tg.hooks/useSuccessMessage';

export const EditPatDialog: FunctionComponent = () => {
  const history = useHistory();
  const onDialogClose = () => {
    history.push(LINKS.USER_PATS.build());
  };

  const id = useRouteMatch().params[PARAMS.PAT_ID];

  const message = useMessage();

  const patLoadable = useApiQuery({
    url: '/v2/pats/{id}',
    method: 'get',
    path: { id: id },
  });

  const editMutable = useApiMutation({
    url: '/v2/pats/{id}',
    method: 'put',
    options: {
      onSuccess: (r) => {
        message.success(<T keyName="pat-token-edited" />);
        onDialogClose();
      },
    },
    invalidatePrefix: '/v2/pats',
  });

  const { t } = useTranslate();

  if (!patLoadable.data) {
    return null;
  }

  return (
    <Dialog
      open={true}
      onClose={onDialogClose}
      fullWidth
      maxWidth={'xs'}
      data-cy="api-keys-create-edit-dialog"
    >
      <DialogTitle data-cy="edit-pat-dialog-title">
        <T keyName="edit_pat_title" />
      </DialogTitle>
      <DialogContent data-cy="edit-pat-dialog-content">
        <StandardForm
          onSubmit={(values) => {
            editMutable.mutate({
              path: {
                id: patLoadable.data.id,
              },
              content: { 'application/json': values },
            });
          }}
          saveActionLoadable={editMutable}
          onCancel={() => onDialogClose()}
          initialValues={
            {
              description: patLoadable.data.description,
            } as components['schemas']['UpdatePatDto']
          }
        >
          <TextField
            inputProps={{
              'data-cy': 'edit-pat-dialog-description-input',
            }}
            autoFocus
            name="description"
            placeholder={t('pat-description-placeholder')}
            label={<T keyName="pat-form-description" />}
          />
        </StandardForm>
      </DialogContent>
    </Dialog>
  );
};
