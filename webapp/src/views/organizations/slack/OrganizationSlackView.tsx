import React, { FC, FunctionComponent } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { LINKS, PARAMS } from 'tg.constants/links';
import { BaseOrganizationSettingsView } from '../components/BaseOrganizationSettingsView';
import { Box, Button } from '@mui/material';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useOrganization } from '../useOrganization';
import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { OrganizationSlackSuccessHandler } from './OrganizationSlackSuccessHandler';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import LoadingButton from 'tg.component/common/form/LoadingButton';

export const OrganizationSlackView: FunctionComponent = () => {
  const { t } = useTranslate();

  const organization = useOrganization();

  if (!organization) return null;

  const getUrlMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/slack/get-connect-url',
    method: 'get',
    invalidatePrefix: '/v2/organizations/{organizationId}/',
  });

  const workspaces = useApiQuery({
    url: '/v2/organizations/{organizationId}/slack/workspaces',
    method: 'get',
    path: {
      organizationId: organization.id,
    },
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

      <PaginatedHateoasList
        loadable={workspaces}
        renderItem={(i) => (
          <Box key={i.id} p={4}>
            Team: {i.slackTeamName}
            <DisconnectButton workspaceId={i.id} />
          </Box>
        )}
      />

      <PrivateRoute path={LINKS.ORGANIZATION_SLACK.template}>
        <OrganizationSlackSuccessHandler />
      </PrivateRoute>
    </BaseOrganizationSettingsView>
  );
};

const DisconnectButton: FC<{ workspaceId: number }> = ({ workspaceId }) => {
  const organization = useOrganization();

  if (!organization) return null;

  const disconnectMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/slack/workspaces/{workspaceId}',
    method: 'delete',
    invalidatePrefix: '/v2/organizations/{organizationId}/slack',
  });

  const onDisconnect = () => {
    disconnectMutation.mutate({
      path: {
        organizationId: organization.id,
        workspaceId: workspaceId,
      },
    });
  };

  return (
    <LoadingButton
      loading={disconnectMutation.isLoading}
      variant="contained"
      onClick={onDisconnect}
    >
      <T keyName="organization_slack_remove_workspace_connection" />
    </LoadingButton>
  );
};
