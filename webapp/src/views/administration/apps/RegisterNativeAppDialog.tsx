import { T, useTranslate } from '@tolgee/react';
import { Typography } from '@mui/material';

import { useApiMutation } from 'tg.service/http/useQueryApi';
import { AppRegisterDialogShell } from 'tg.component/apps/AppRegisterDialogShell';
import { AppRegisterUrlStep } from 'tg.component/apps/AppRegisterUrlStep';
import { AppRegisterConsentStep } from 'tg.component/apps/AppRegisterConsentStep';
import { useAppRegisterState } from 'tg.component/apps/useAppRegisterState';

type Props = {
  open: boolean;
  onClose: () => void;
};

export const RegisterNativeAppDialog = ({ open, onClose }: Props) => {
  const { t } = useTranslate();

  const previewMutation = useApiMutation({
    url: '/v2/administration/apps/preview',
    method: 'post',
  });

  const registerMutation = useApiMutation({
    url: '/v2/administration/apps',
    method: 'post',
    invalidatePrefix: '/v2/administration/apps',
  });

  const state = useAppRegisterState(() => {
    previewMutation.reset();
    registerMutation.reset();
    onClose();
  });

  const handlePreview = () => {
    previewMutation.mutate(
      { content: { 'application/json': { manifestUrl: state.manifestUrl } } },
      { onSuccess: state.setPreview }
    );
  };

  const handleRegister = () => {
    registerMutation.mutate(
      { content: { 'application/json': { manifestUrl: state.manifestUrl } } },
      { onSuccess: () => state.close() }
    );
  };

  return (
    <AppRegisterDialogShell
      open={open}
      onClose={state.close}
      dataCy="administration-apps-register-dialog"
      title={
        <T
          keyName="administration_apps_register_dialog_title"
          defaultValue="Register server app"
        />
      }
    >
      {state.step === 'url' && (
        <AppRegisterUrlStep
          value={state.manifestUrl}
          onChange={state.setManifestUrl}
          onSubmit={handlePreview}
          onCancel={state.close}
          loading={previewMutation.isLoading}
          description={
            <Typography variant="body2" color="text.secondary">
              <T
                keyName="administration_apps_register_dialog_description"
                defaultValue="The app is registered at server level and belongs to no organization. Choose which organizations may use it afterwards."
              />
            </Typography>
          }
          fieldLabel={t(
            'administration_apps_register_manifest_url_label',
            'Manifest URL'
          )}
          fieldHelperText={t(
            'administration_apps_register_manifest_url_helper',
            'URL to the app manifest.json file.'
          )}
          submitLabel={
            <T
              keyName="administration_apps_register_continue"
              defaultValue="Continue"
            />
          }
          fieldDataCy="administration-apps-register-manifest-url"
          submitDataCy="administration-apps-register-continue"
        />
      )}

      {state.step === 'consent' && state.preview && (
        <AppRegisterConsentStep
          preview={state.preview}
          loading={registerMutation.isLoading}
          onBack={state.back}
          onSubmit={handleRegister}
          intro={
            <T
              keyName="administration_apps_register_consent_intro"
              defaultValue="This app requests the following permissions in every project it is enabled for:"
            />
          }
          noScopesLabel={
            <T
              keyName="administration_apps_register_consent_no_scopes"
              defaultValue="No scopes requested."
            />
          }
          backLabel={
            <T
              keyName="administration_apps_register_back"
              defaultValue="Back"
            />
          }
          submitLabel={
            <T
              keyName="administration_apps_register_submit"
              defaultValue="Approve & register"
            />
          }
          contentDataCy="administration-apps-register-consent"
          noScopesDataCy="administration-apps-register-consent-no-scopes"
          scopesDataCy="administration-apps-register-consent-scope"
          backDataCy="administration-apps-register-back"
          submitDataCy="administration-apps-register-submit"
        />
      )}
    </AppRegisterDialogShell>
  );
};
