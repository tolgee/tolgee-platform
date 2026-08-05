import { T, useTranslate } from '@tolgee/react';

import { useOrganization } from 'tg.views/organizations/useOrganization';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { AppRegisterDialogShell } from 'tg.component/apps/AppRegisterDialogShell';
import { AppRegisterUrlStep } from 'tg.component/apps/AppRegisterUrlStep';
import { AppRegisterConsentStep } from 'tg.component/apps/AppRegisterConsentStep';
import { useAppRegisterState } from 'tg.component/apps/useAppRegisterState';

type Props = {
  open: boolean;
  onClose: () => void;
};

export const RegisterAppDialog = ({ open, onClose }: Props) => {
  const organization = useOrganization();
  const { t } = useTranslate();

  const previewMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/apps/preview',
    method: 'post',
  });

  const registerMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/apps',
    method: 'post',
    invalidatePrefix: '/v2/organizations/{organizationId}/apps',
  });

  const state = useAppRegisterState(() => {
    previewMutation.reset();
    registerMutation.reset();
    onClose();
  });

  const handlePreview = () => {
    if (!organization) return;
    previewMutation.mutate(
      {
        path: { organizationId: organization.id },
        content: { 'application/json': { manifestUrl: state.manifestUrl } },
      },
      { onSuccess: state.setPreview }
    );
  };

  const handleRegister = () => {
    if (!organization) return;
    registerMutation.mutate(
      {
        path: { organizationId: organization.id },
        content: { 'application/json': { manifestUrl: state.manifestUrl } },
      },
      { onSuccess: () => state.close() }
    );
  };

  return (
    <AppRegisterDialogShell
      open={open}
      onClose={state.close}
      dataCy="organization-apps-register-dialog"
      title={
        <T
          keyName="organization_apps_register_dialog_title"
          defaultValue="Register app"
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
          fieldLabel={t(
            'organization_apps_register_manifest_url_label',
            'Manifest URL'
          )}
          fieldHelperText={t(
            'organization_apps_register_manifest_url_helper',
            'URL to the app manifest.json file.'
          )}
          submitLabel={
            <T
              keyName="organization_apps_register_continue"
              defaultValue="Continue"
            />
          }
          fieldDataCy="organization-apps-register-manifest-url"
          submitDataCy="organization-apps-register-continue"
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
              keyName="organization_apps_register_consent_intro"
              defaultValue="This app requests the following permissions:"
            />
          }
          noScopesLabel={
            <T
              keyName="organization_apps_register_consent_no_scopes"
              defaultValue="No scopes requested."
            />
          }
          backLabel={
            <T keyName="organization_apps_register_back" defaultValue="Back" />
          }
          submitLabel={
            <T
              keyName="organization_apps_register_submit"
              defaultValue="Approve & install"
            />
          }
          contentDataCy="organization-apps-register-consent"
          noScopesDataCy="organization-apps-register-consent-no-scopes"
          scopesDataCy="organization-apps-register-consent-scope"
          backDataCy="organization-apps-register-back"
          submitDataCy="organization-apps-register-submit"
        />
      )}
    </AppRegisterDialogShell>
  );
};
