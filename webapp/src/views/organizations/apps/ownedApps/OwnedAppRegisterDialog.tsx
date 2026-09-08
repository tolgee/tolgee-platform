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
import { components } from 'tg.service/apiSchema.generated';
import { AppRegisterDialogShell } from 'tg.component/apps/AppRegisterDialogShell';
import { AppRegisterUrlStep } from 'tg.component/apps/AppRegisterUrlStep';
import { AppRegisterConsentStep } from 'tg.component/apps/AppRegisterConsentStep';
import { AppCredentialsDisclosure } from 'tg.component/apps/AppCredentialsDisclosure';
import { AppRegisterError } from 'tg.component/apps/AppRegisterError';
import { useAppRegisterState } from 'tg.component/apps/useAppRegisterState';

type AppRegisteredModel = components['schemas']['AppRegisteredModel'];

type Props = {
  open: boolean;
  onClose: () => void;
};

const INVALIDATE_PREFIXES = [
  '/v2/organizations/{organizationId}/apps',
  '/v2/organizations/{organizationId}/owned-apps',
] as const;

/**
 * Registers an app the organization will own. The manifest is previewed for consent, then
 * registered and installed in one step — the response is the only place the app-level
 * credentials are ever shown. The previewed manifestHash is passed back so the server can
 * reject a manifest whose scopes changed between consent and register.
 */
export const OwnedAppRegisterDialog = ({ open, onClose }: Props) => {
  const organization = useOrganization();
  const { t } = useTranslate();
  const [issuedApp, setIssuedApp] = useState<AppRegisteredModel | null>(null);

  const previewMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/apps/preview',
    method: 'post',
    fetchOptions: { disableAutoErrorHandle: true },
  });

  const registerMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/owned-apps',
    method: 'post',
    fetchOptions: { disableAutoErrorHandle: true },
    invalidatePrefix: [...INVALIDATE_PREFIXES],
  });

  const state = useAppRegisterState(() => {
    previewMutation.reset();
    registerMutation.reset();
    setIssuedApp(null);
    onClose();
  });

  const handlePreview = () => {
    if (!organization) return;
    previewMutation.mutate(
      {
        path: { organizationId: organization.id },
        content: {
          'application/json': { manifestUrl: state.manifestUrl, install: true },
        },
      },
      { onSuccess: state.setPreview }
    );
  };

  const handleRegister = () => {
    if (!organization || !state.preview) return;
    registerMutation.mutate(
      {
        path: { organizationId: organization.id },
        content: {
          'application/json': {
            manifestUrl: state.manifestUrl,
            manifestHash: state.preview.manifestHash,
            install: true,
          },
        },
      },
      { onSuccess: (data) => setIssuedApp(data) }
    );
  };

  const screen = issuedApp ? ('credentials' as const) : state.step;

  return (
    <AppRegisterDialogShell
      open={open}
      onClose={state.close}
      dataCy="owned-apps-register-dialog"
      title={
        <T
          keyName="owned_apps_register_dialog_title"
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
          errorNode={<AppRegisterError error={previewMutation.error} />}
          fieldLabel={t(
            'owned_apps_register_manifest_url_label',
            'Manifest URL'
          )}
          fieldHelperText={t(
            'owned_apps_register_manifest_url_helper',
            'URL to the app manifest.json file.'
          )}
          submitLabel={
            <T keyName="owned_apps_register_continue" defaultValue="Continue" />
          }
          fieldDataCy="owned-apps-register-manifest-url"
          submitDataCy="owned-apps-register-continue"
        />
      )}

      {screen === 'consent' && state.preview && (
        <AppRegisterConsentStep
          preview={state.preview}
          loading={registerMutation.isLoading}
          errorNode={<AppRegisterError error={registerMutation.error} />}
          onBack={state.back}
          onSubmit={handleRegister}
          intro={
            <T
              keyName="owned_apps_register_consent_intro"
              defaultValue="Registering it makes this organization the app's owner: you get its app-level credentials, you can rotate them, and you can remove the app from every organization that installs it. It requests the following permissions"
            />
          }
          noScopesLabel={
            <T
              keyName="owned_apps_register_consent_no_scopes"
              defaultValue="No scopes requested."
            />
          }
          backLabel={
            <T keyName="owned_apps_register_back" defaultValue="Back" />
          }
          submitLabel={
            <T
              keyName="owned_apps_register_submit"
              defaultValue="Register & install"
            />
          }
          contentDataCy="owned-apps-register-consent"
          scopesDataCy="owned-apps-register-consent-scope"
          backDataCy="owned-apps-register-back"
          submitDataCy="owned-apps-register-submit"
        />
      )}

      {screen === 'credentials' && issuedApp && (
        <>
          <DialogContent>
            <Typography variant="body2" mb={2}>
              <T
                keyName="owned_apps_register_credentials_intro"
                defaultValue="{name} is registered and installed. Tolgee also sends these credentials to the app's base URL."
                params={{ name: issuedApp.name }}
              />
            </Typography>
            <AppCredentialsDisclosure
              clientId={issuedApp.clientId}
              clientSecret={issuedApp.clientSecret}
              webhookSecret={issuedApp.webhookSecret}
              delivery={issuedApp.delivery}
              dataCy="owned-apps-register-credentials"
            />
          </DialogContent>
          <DialogActions>
            <Button
              data-cy="owned-apps-register-credentials-close"
              variant="contained"
              color="primary"
              onClick={state.close}
            >
              {issuedApp.delivery?.delivered ? (
                <T
                  keyName="owned_apps_register_credentials_ok"
                  defaultValue="OK"
                />
              ) : (
                <T
                  keyName="owned_apps_register_credentials_close"
                  defaultValue="I saved them"
                />
              )}
            </Button>
          </DialogActions>
        </>
      )}
    </AppRegisterDialogShell>
  );
};
