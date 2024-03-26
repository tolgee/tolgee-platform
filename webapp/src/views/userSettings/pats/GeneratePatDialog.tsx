import { default as React, FunctionComponent } from 'react';
import { useHistory } from 'react-router-dom';
import { Dialog, DialogContent, DialogTitle } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { LINKS } from 'tg.constants/links';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { ExpirationDateField } from 'tg.component/common/form/epirationField/ExpirationDateField';
import { useExpirationDateOptions } from 'tg.component/common/form/epirationField/useExpirationDateOptions';

export const GeneratePatDialog: FunctionComponent<{
  onGenerated: (pat: components['schemas']['RevealedPatModel']) => void;
}> = (props) => {
  const expirationDateOptions = useExpirationDateOptions();
  const history = useHistory();

  const { t } = useTranslate();

  const onDialogClose = () => {
    history.push(LINKS.USER_PATS.build());
  };

  const generateMutation = useApiMutation({
    url: '/v2/pats',
    method: 'post',
    options: {
      onSuccess: (res) => {
        props.onGenerated(res);
        onDialogClose();
      },
    },
    invalidatePrefix: '/v2/pats',
  });

  return (
    <Dialog open={true} onClose={onDialogClose} fullWidth maxWidth={'xs'}>
      <DialogTitle data-cy="generate-pat-dialog-title">
        <T keyName="generate_pat_title" />
      </DialogTitle>
      <DialogContent data-cy="generate-pat-dialog-content">
        <StandardForm
          rootSx={{}}
          onSubmit={(values) => {
            generateMutation.mutate({
              content: { 'application/json': values },
            });
          }}
          saveActionLoadable={generateMutation}
          onCancel={() => onDialogClose()}
          submitButtonInner={<T keyName="pat-form-generate-submit-button" />}
          initialValues={
            {
              description: '',
              expiresAt: expirationDateOptions[0].time,
            } as components['schemas']['CreatePatDto']
          }
          validationSchema={Validation.CREATE_PAT}
        >
          <TextField
            inputProps={{
              'data-cy': 'generate-pat-dialog-description-input',
            }}
            autoFocus
            name="description"
            placeholder={t('pat-description-placeholder')}
            label={<T keyName="pat-form-description" />}
          />

          <ExpirationDateField options={expirationDateOptions} />
        </StandardForm>
      </DialogContent>
    </Dialog>
  );
};
