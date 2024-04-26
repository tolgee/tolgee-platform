import { Box, Link, styled } from '@mui/material';
import { T } from '@tolgee/react';

import { LINKS } from 'tg.constants/links';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { OrganizationSlackSuccessHandler } from './OrganizationSlackSuccessHandler';
import { DisconnectButton } from './DisconnectButton';
import LoadingButton from 'tg.component/common/form/LoadingButton';

const StyledContainer = styled('div')`
  display: grid;
  border-radius: 4px;
  border: 1px solid ${({ theme }) => theme.palette.divider};
  background: ${({ theme }) => theme.palette.background.paper};
`;

const StyledAppSplitHeader = styled('div')`
  padding: 20px;
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: start;
`;

const StyledAppHeader = styled('div')`
  display: grid;
  gap: 4px;
`;

const StyledAppTitle = styled('div')`
  display: flex;
  align-items: center;
  gap: 8px;
`;

const StyledAppLogo = styled('img')`
  height: 20px;
`;

const StyledAppName = styled('div')`
  font-size: 16px;
`;

const StyledItem = styled('div')`
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid ${({ theme }) => theme.palette.divider};
  padding: 10px 20px;
`;

const StyledTeamId = styled('div')`
  color: ${({ theme }) => theme.palette.text.secondary};
`;

export const SlackApp = () => {
  const organization = useOrganization();

  if (!organization) {
    return null;
  }

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
    <StyledContainer>
      <StyledAppSplitHeader>
        <StyledAppHeader>
          <StyledAppTitle>
            <StyledAppLogo src="/images/slackLogo.svg" />
            <StyledAppName>Slack</StyledAppName>
          </StyledAppTitle>

          <div>
            <T keyName="slack_app_description" />
          </div>

          <Link>
            <T keyName="slack_app_docs_link" />
          </Link>
        </StyledAppHeader>
        <LoadingButton
          onClick={onConnect}
          color="primary"
          variant="contained"
          loading={getUrlMutation.isLoading}
        >
          <T keyName="organization_slack_connect_button" />
        </LoadingButton>
      </StyledAppSplitHeader>

      <div>
        {workspaces.data?._embedded?.workspaces?.map((item) => (
          <StyledItem key={item.id}>
            <Box display="grid" gap={0.2}>
              <div>{item.slackTeamName}</div>
              <StyledTeamId>{item.slackTeamId}</StyledTeamId>
            </Box>
            <DisconnectButton workspaceId={item.id} />
          </StyledItem>
        ))}
      </div>

      <PrivateRoute path={LINKS.ORGANIZATION_APPS_SLACK_OAUTH_SUCCESS.template}>
        <OrganizationSlackSuccessHandler />
      </PrivateRoute>
    </StyledContainer>
  );
};
