import { T } from '@tolgee/react';
import { Typography } from '@mui/material';

import { useApiMutation } from 'tg.service/http/useQueryApi';
import {
  AppRegisterDialog,
  AppRegisterDialogDataCy,
} from 'tg.component/apps/AppRegisterDialog';

const DATA_CY: AppRegisterDialogDataCy = {
  dialogDataCy: 'administration-apps-register-dialog',
  manifestUrlDataCy: 'administration-apps-register-manifest-url',
  continueDataCy: 'administration-apps-register-continue',
  consentDataCy: 'administration-apps-register-consent',
  noScopesDataCy: 'administration-apps-register-consent-no-scopes',
  scopesDataCy: 'administration-apps-register-consent-scope',
  backDataCy: 'administration-apps-register-back',
  submitDataCy: 'administration-apps-register-submit',
};

type Props = {
  open: boolean;
  onClose: () => void;
};

export const RegisterNativeAppDialog = ({ open, onClose }: Props) => {
  const previewMutation = useApiMutation({
    url: '/v2/administration/apps/preview',
    method: 'post',
  });

  const registerMutation = useApiMutation({
    url: '/v2/administration/apps',
    method: 'post',
    invalidatePrefix: '/v2/administration/apps',
  });

  const handleClose = () => {
    previewMutation.reset();
    registerMutation.reset();
    onClose();
  };

  return (
    <AppRegisterDialog
      open={open}
      onClose={handleClose}
      description={
        <Typography variant="body2" color="text.secondary">
          <T
            keyName="administration_apps_register_dialog_description"
            defaultValue="The app is registered at server level and belongs to no organization. Choose which organizations may use it afterwards."
          />
        </Typography>
      }
      previewLoading={previewMutation.isLoading}
      registerLoading={registerMutation.isLoading}
      onPreview={(manifestUrl, onSuccess) => {
        previewMutation.mutate(
          { content: { 'application/json': { manifestUrl } } },
          { onSuccess }
        );
      }}
      onRegister={(manifestUrl, onSuccess) => {
        registerMutation.mutate(
          { content: { 'application/json': { manifestUrl } } },
          { onSuccess: () => onSuccess() }
        );
      }}
      dataCy={DATA_CY}
    />
  );
};
