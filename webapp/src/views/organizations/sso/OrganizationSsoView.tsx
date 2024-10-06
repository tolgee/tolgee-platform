import React, {FunctionComponent, useEffect, useRef, useState} from 'react';
import {useTranslate} from '@tolgee/react';
import {BaseOrganizationSettingsView} from '../components/BaseOrganizationSettingsView';
import {LINKS, PARAMS} from 'tg.constants/links';
import {useOrganization} from '../useOrganization';
import {CreateProviderSsoForm} from 'tg.views/organizations/sso/CreateProviderSsoForm';
import {useApiQuery} from 'tg.service/http/useQueryApi';
import {FormControlLabel, Switch} from '@mui/material';
import Box from '@mui/material/Box';

export const OrganizationSsoView: FunctionComponent = () => {
  const organization = useOrganization();
  const { t } = useTranslate();
  if (!organization) {
    return null;
  }

  const providersLoadable = useApiQuery({
    url: `/v2/{organizationId}/sso/providers`,
    method: 'get',
    path: {
      organizationId: organization.id,
    },
  });

  const credentialsRef = useRef({
    authorizationUri: '',
    clientId: '',
    clientSecret: '',
    redirectUri: '',
    tokenUri: '',
    jwkSetUri: '',
  });
  const [showForm, setShowForm] = useState(false);

  useEffect(() => {
    if (providersLoadable.data) {
      credentialsRef.current = {
        authorizationUri: providersLoadable.data.authorizationUri || '',
        clientId: providersLoadable.data.clientId || '',
        clientSecret: providersLoadable.data.clientSecret || '',
        redirectUri: providersLoadable.data.redirectUri || '',
        tokenUri: providersLoadable.data.tokenUri || '',
        jwkSetUri: providersLoadable.data.jwkSetUri || '',
      };

      setShowForm(providersLoadable.data.isEnabled);
    }
  }, [providersLoadable.data]);
  const handleSwitchChange = (event) => {
    setShowForm(event.target.checked);
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
      <FormControlLabel
        control={<Switch checked={showForm} onChange={handleSwitchChange} />}
        label={t('organization_sso_switch')}
      />
      <Box sx={{ marginTop: '16px' }}>
        <CreateProviderSsoForm
          credentialsRef={credentialsRef}
          disabled={!showForm}
        />
      </Box>
    </BaseOrganizationSettingsView>
  );
};
