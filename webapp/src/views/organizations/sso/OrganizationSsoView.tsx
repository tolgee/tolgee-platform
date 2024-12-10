import React, { FunctionComponent, useEffect, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { BaseOrganizationSettingsView } from '../components/BaseOrganizationSettingsView';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useOrganization } from '../useOrganization';
import { CreateProviderSsoForm } from 'tg.views/organizations/sso/CreateProviderSsoForm';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { Alert, AlertTitle, FormControlLabel, Switch } from '@mui/material';
import Box from '@mui/material/Box';
import { useEnabledFeatures } from 'tg.globalContext/helpers';
import { DisabledFeatureBanner } from 'tg.component/common/DisabledFeatureBanner';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

export const OrganizationSsoView: FunctionComponent = () => {
  const organization = useOrganization();
  if (!organization) {
    return null;
  }
  const { isEnabled } = useEnabledFeatures();
  const featureEnabled = isEnabled('SSO');
  const organizationsSsoEnabled = useGlobalContext(
    (c) =>
      c.initialData.serverConfiguration.authMethods?.ssoOrganizations.enabled
  );
  const { t } = useTranslate();

  const providersLoadable =
    featureEnabled && organizationsSsoEnabled
      ? useApiQuery({
          url: `/v2/organizations/{organizationId}/sso`,
          method: 'get',
          path: {
            organizationId: organization.id,
          },
        })
      : null;
  const [toggleFormState, setToggleFormState] = useState(false);

  useEffect(() => {
    setToggleFormState(providersLoadable?.data?.enabled || false);
  }, [providersLoadable?.data]);

  const handleSwitchChange = (event) => {
    setToggleFormState(event.target.checked);
  };

  function renderBody() {
    switch (true) {
      case !featureEnabled:
        return (
          <Box>
            <DisabledFeatureBanner />
          </Box>
        );
      case !organizationsSsoEnabled:
        return (
          <Box>
            <Alert severity="info">
              <AlertTitle sx={{ pb: 0 }}>
                {t('organization_sso_disabled_title')}
              </AlertTitle>
              <Box>{t('organization_sso_disabled')}</Box>
            </Alert>
          </Box>
        );
      default:
        return (
          <>
            <FormControlLabel
              control={
                <Switch
                  checked={toggleFormState}
                  onChange={handleSwitchChange}
                />
              }
              label={t('organization_sso_switch')}
            />
            <Box sx={{ marginTop: '16px' }}>
              <CreateProviderSsoForm
                data={providersLoadable?.data}
                disabled={!toggleFormState}
              />
            </Box>
          </>
        );
    }
  }

  return (
    <BaseOrganizationSettingsView
      windowTitle={t('organization_sso_title')}
      link={LINKS.ORGANIZATION_SSO}
      title={t('organization_sso_title')}
      navigation={[
        [
          t('edit_organization_title'),
          LINKS.ORGANIZATION_SSO.build({
            [PARAMS.ORGANIZATION_SLUG]: organization.slug,
          }),
        ],
      ]}
      hideChildrenOnLoading={false}
      maxWidth="normal"
    >
      {renderBody()}
    </BaseOrganizationSettingsView>
  );
};
