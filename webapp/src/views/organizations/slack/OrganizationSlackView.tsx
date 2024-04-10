import React, { FunctionComponent } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { LINKS, PARAMS } from 'tg.constants/links';
import { BaseOrganizationSettingsView } from '../components/BaseOrganizationSettingsView';
import { Button } from '@mui/material';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useOrganization } from '../useOrganization';
import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { OrganizationSlackSuccessHandler } from './OrganizationSlackSuccessHandler';

export const OrganizationSlackView: FunctionComponent = () => {
  const { t } = useTranslate();

  const organization = useOrganization();

  if (!organization) return null;

  const getUrlMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/slack/get-connect-url',
    method: 'get',
  });

  const onConnect = () => {
    getUrlMutation.mutate(
      {
        path: {
          organizationId: organization.id,
        },
      },
      {
        onSuccess(data) {
          window.location.href = data.url;
        },
      }
    );
  };

  return (
    <BaseOrganizationSettingsView
      windowTitle={t('organization_slack_title')}
      link={LINKS.ORGANIZATION_SLACK}
      title={t('organization_slack_title')}
      navigation={[
        [
          t('edit_organization_title'),
          LINKS.ORGANIZATION_SLACK.build({
            [PARAMS.ORGANIZATION_SLUG]: organization.slug,
          }),
        ],
      ]}
      hideChildrenOnLoading={false}
      maxWidth="normal"
    >
      <>
        <Button onClick={onConnect}>
          <T keyName="organization_slack_connect_button" />
        </Button>
      </>

      <PrivateRoute path={LINKS.ORGANIZATION_SLACK.template}>
        <OrganizationSlackSuccessHandler />
      </PrivateRoute>
    </BaseOrganizationSettingsView>
  );
};
