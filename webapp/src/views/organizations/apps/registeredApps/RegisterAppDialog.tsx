import { useState } from 'react';
import {
  Button,
  DialogActions,
  DialogContent,
  Typography,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { useOrganization } from 'tg.views/organizations/useOrganization';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { components } from 'tg.service/apiSchema.generated';
import { AppRegisterDialogShell } from 'tg.component/apps/AppRegisterDialogShell';
import { AppRegisterUrlStep } from 'tg.component/apps/AppRegisterUrlStep';
import { AppRegisterConsentStep } from 'tg.component/apps/AppRegisterConsentStep';
import { AppCredentialsDisclosure } from 'tg.component/apps/AppCredentialsDisclosure';
import { useAppRegisterState } from 'tg.component/apps/useAppRegisterState';

type AppModel = components['schemas']['AppModel'];

type Props = {
  open: boolean;
  onClose: () => void;
  /** Pre-fills the manifest URL; used when installing an app from the available list. */
  initialManifestUrl?: string;
};

const INVALIDATE_PREFIXES = [
  '/v2/organizations/{organizationId}/apps',
  '/v2/organizations/{organizationId}/owned-apps',
] as const;

export const RegisterAppDialog = ({
  open,
  onClose,
  initialManifestUrl,
}: Props) => {
  const organization = useOrganization();
  const { t } = useTranslate();
  const [notRegistered, setNotRegistered] = useState(false);
  const [issuedApp, setIssuedApp] = useState<AppModel | null>(null);

  const previewMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/apps/preview',
    method: 'post',
  });

  const installMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/apps',
    method: 'post',
    invalidatePrefix: [...INVALIDATE_PREFIXES],
  });

  const registerMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/apps/register',
    method: 'post',
    invalidatePrefix: [...INVALIDATE_PREFIXES],
  });

  const state = useAppRegisterState(() => {
    previewMutation.reset();
    installMutation.reset();
    registerMutation.reset();
    setNotRegistered(false);
    setIssuedApp(null);
    onClose();
  }, initialManifestUrl);

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

  const handleInstall = () => {
    if (!organization) return;
    installMutation.mutate(
      {
        path: { organizationId: organization.id },
        content: { 'application/json': { manifestUrl: state.manifestUrl } },
      },
      {
        onSuccess: () => state.close(),
        onError: (error) => {
          if (error.code === 'app_not_registered') {
            setNotRegistered(true);
            return;
          }
          error.handleError?.();
        },
      }
    );
  };

  const handleRegister = () => {
    if (!organization) return;
    registerMutation.mutate(
      {
        path: { organizationId: organization.id },
        content: { 'application/json': { manifestUrl: state.manifestUrl } },
      },
      {
        onSuccess: (data) => {
          if (data.app?.clientSecret) {
            setIssuedApp(data.app);
            return;
          }
          state.close();
        },
      }
    );
  };

  const getScreen = () => {
    if (issuedApp) return 'credentials' as const;
    if (notRegistered) return 'not-registered' as const;
    return state.step;
  };

  const screen = getScreen();

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
      {screen === 'url' && (
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

      {screen === 'consent' && state.preview && (
        <AppRegisterConsentStep
          preview={state.preview}
          loading={installMutation.isLoading}
          onBack={state.back}
          onSubmit={handleInstall}
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

      {screen === 'not-registered' && (
        <>
          <DialogContent data-cy="organization-apps-not-registered">
            <Typography variant="body1" mb={1}>
              <T
                keyName="organization_apps_not_registered_message"
                defaultValue="{name} is not registered on this Tolgee server yet, so it cannot be installed."
                params={{ name: state.preview?.name ?? state.manifestUrl }}
              />
            </Typography>
            <Typography variant="body2" color="text.secondary">
              <T
                keyName="organization_apps_not_registered_explanation"
                defaultValue="Registering it makes this organization the app's owner: you get its app-level credentials, you can rotate them, and you can remove the app from every organization that installs it. It is then installed here as well."
              />
            </Typography>
          </DialogContent>
          <DialogActions>
            <Button
              data-cy="organization-apps-not-registered-cancel"
              onClick={state.close}
              disabled={registerMutation.isLoading}
            >
              <T keyName="global_cancel_button" defaultValue="Cancel" />
            </Button>
            <LoadingButton
              data-cy="organization-apps-not-registered-register"
              variant="contained"
              color="primary"
              loading={registerMutation.isLoading}
              onClick={handleRegister}
            >
              <T
                keyName="organization_apps_not_registered_register"
                defaultValue="Register now"
              />
            </LoadingButton>
          </DialogActions>
        </>
      )}

      {screen === 'credentials' && issuedApp && (
        <>
          <DialogContent>
            <Typography variant="body2" mb={2}>
              <T
                keyName="organization_apps_registered_credentials_intro"
                defaultValue="{name} is registered and installed. Tolgee also sends these credentials to the app's base URL."
                params={{ name: issuedApp.name }}
              />
            </Typography>
            <AppCredentialsDisclosure
              clientId={issuedApp.clientId}
              clientSecret={issuedApp.clientSecret}
              webhookSecret={issuedApp.webhookSecret}
              delivery={issuedApp.delivery}
              dataCy="organization-apps-registered-credentials"
            />
          </DialogContent>
          <DialogActions>
            <Button
              data-cy="organization-apps-registered-credentials-close"
              variant="contained"
              color="primary"
              onClick={state.close}
            >
              <T
                keyName="organization_apps_registered_credentials_close"
                defaultValue="I saved them"
              />
            </Button>
          </DialogActions>
        </>
      )}
    </AppRegisterDialogShell>
  );
};
