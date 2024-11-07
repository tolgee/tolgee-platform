import React, { FunctionComponent, useEffect, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { BaseOrganizationSettingsView } from '../components/BaseOrganizationSettingsView';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useOrganization } from '../useOrganization';
import { CreateProviderSsoForm } from 'tg.views/organizations/sso/CreateProviderSsoForm';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { FormControlLabel, Switch } from '@mui/material';
import Box from '@mui/material/Box';
import { useEnabledFeatures } from 'tg.globalContext/helpers';
import { PaidFeatureBanner } from 'tg.ee/common/PaidFeatureBanner';

export const OrganizationSsoView: FunctionComponent = () => {
  const organization = useOrganization();
  const { isEnabled } = useEnabledFeatures();
  const featureNotEnabled = !isEnabled('SSO');
  const { t } = useTranslate();
  if (!organization) {
    return null;
  }

  const providersLoadable = featureNotEnabled
    ? null
    : useApiQuery({
        url: `/v2/organizations/{organizationId}/sso`,
        method: 'get',
        path: {
          organizationId: organization.id,
        },
      });
  const [toggleFormState, setToggleFormState] = useState(false);

  useEffect(() => {
    setToggleFormState(providersLoadable?.data?.isEnabled || false);
  }, [providersLoadable?.data]);

  const handleSwitchChange = (event) => {
    setToggleFormState(event.target.checked);
  };

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
      {featureNotEnabled ? (
        <Box>
          <PaidFeatureBanner />
        </Box>
      ) : (
        <>
          <FormControlLabel
            control={
              <Switch checked={toggleFormState} onChange={handleSwitchChange} />
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
      )}
    </BaseOrganizationSettingsView>
  );
};
