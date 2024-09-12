import React, { FunctionComponent, useRef } from 'react';
import { useTranslate } from '@tolgee/react';
import { BaseOrganizationSettingsView } from '../components/BaseOrganizationSettingsView';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useOrganization } from '../useOrganization';
import { CreateProviderSsoForm } from 'tg.views/organizations/sso/CreateProviderSsoForm';

export const OrganizationSsoView: FunctionComponent = () => {
  const organization = useOrganization();
  const { t } = useTranslate();
  if (!organization) {
    return null;
  }
  const credentialsRef = useRef({
    authorizationUri: '',
    clientId: '',
    clientSecret: '',
    redirectUri: '',
    tokenUri: '',
  });

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
