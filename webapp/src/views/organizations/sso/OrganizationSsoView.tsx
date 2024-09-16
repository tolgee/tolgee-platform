import React, { FunctionComponent, useEffect, useRef } from 'react';
import { useTranslate } from '@tolgee/react';
import { BaseOrganizationSettingsView } from '../components/BaseOrganizationSettingsView';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useOrganization } from '../useOrganization';
import { CreateProviderSsoForm } from 'tg.views/organizations/sso/CreateProviderSsoForm';
import { useApiQuery } from 'tg.service/http/useQueryApi';

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
  });

  useEffect(() => {
    if (providersLoadable.data) {
      credentialsRef.current = {
        authorizationUri: providersLoadable.data.authorizationUri || '',
        clientId: providersLoadable.data.clientId || '',
        clientSecret: providersLoadable.data.clientSecret || '',
        redirectUri: providersLoadable.data.redirectUri || '',
        tokenUri: providersLoadable.data.tokenUri || '',
      };
    }
  }, [providersLoadable.data]);

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
      <CreateProviderSsoForm credentialsRef={credentialsRef} />
    </BaseOrganizationSettingsView>
  );
};
