import { Alert, Box, Button, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { CompactView } from 'tg.component/layout/CompactView';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { Link, useHistory, useLocation } from 'react-router-dom';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { Connection } from './Connection';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { useUser } from 'tg.globalContext/helpers';
import { UserAvatar } from 'tg.component/common/avatar/UserAvatar';
import { TolgeeLogo } from 'tg.component/common/icons/TolgeeLogo';
import { Slack } from 'tg.component/CustomIcons';
import { LINKS } from 'tg.constants/links';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import LoadingButton from 'tg.component/common/form/LoadingButton';

const StyledContainer = styled('div')`
  padding: 28px 4px 0px 4px;
  display: grid;
  justify-items: center;
`;

const StyledTitle = styled('h2')`
  color: ${({ theme }) => theme.palette.text.primary}
  font-size: 24px;
  font-style: normal;
  font-weight: 400;
  padding-top: 40px;
  margin-top: 0px;
  text-align: center;
`;

const StyledDescription = styled('div')`
  color: ${({ theme }) => theme.palette.text.primary}
  font-size: 15px;
  padding-bottom: 60px;
  max-width: 550px;
  text-align: center;
`;

const StyledImage = styled('img')`
  height: 40px;
  border-radius: 8px;
`;

function getUserName(name: string | undefined, realName: string | undefined) {
  if (name !== realName && name && realName) {
    return `${name} (${realName})`;
  } else {
    return name;
  }
}

export const SlackConnectView = () => {
  const { t } = useTranslate();
  const location = useLocation();
  const user = useUser();
  const history = useHistory();

  const queryParameters = new URLSearchParams(location.search);
  const encryptedData = queryParameters.get('data');

  const slackMutation = useApiMutation({
    url: '/v2/slack/user-login',
    method: 'post',
    fetchOptions: {
      disableErrorNotification: true,
    },
  });

  const connectionInfo = useApiQuery({
    url: '/v2/slack/user-login-info',
    method: 'get',
    query: {
      data: encryptedData!,
    },
    fetchOptions: {
      disableErrorNotification: true,
    },
  });

  if (!encryptedData) {
    return <div>Invalid slack login parameters</div>;
  }
  const connectSlack = () => {
    slackMutation.mutate(
      { query: { data: encryptedData } },
      {
        onSuccess: () => {
          history.replace(LINKS.SLACK_CONNECTED.build());
        },
      }
    );
  };

  const error = slackMutation.error || connectionInfo.error;

  return (
    <DashboardPage>
      <CompactView
        alerts={
          error?.code && (
            <Alert severity="error" sx={{ mb: 1 }}>
              <TranslatedError code={error.code} />
            </Alert>
          )
        }
        maxWidth={800}
        windowTitle={t('slack_connect_title')}
        primaryContent={
          <>
            {connectionInfo.isLoading ? (
              <BoxLoading />
            ) : (
              <StyledContainer>
                <Box display="flex" justifyContent="center">
                  <Connection
                    first={{
                      platformImage: (
                        <TolgeeLogo
                          fontSize="inherit"
                          style={{ fontSize: 22 }}
                        />
                      ),
                      image: <UserAvatar size={40} />,
                      name: user?.name ?? user?.username,
                      description: user?.username,
                    }}
                    second={{
                      platformImage: <Slack width={20} height={20} />,
                      image: (
                        <StyledImage src={connectionInfo.data?.slackAvatar} />
                      ),
                      name: getUserName(
                        connectionInfo.data?.slackName,
                        connectionInfo.data?.slackRealName
                      ),
                      description: connectionInfo.data?.teamName,
                    }}
                  />
                </Box>
                <StyledTitle>
                  <T keyName="slack_connect_main_title" />
                </StyledTitle>

                <StyledDescription>
                  <T keyName="slack_connect_description" />
                </StyledDescription>

                <Box display="flex" gap={2} mb={1.5}>
                  <Button
                    component={Link}
                    size="medium"
                    to={LINKS.PROJECTS.build()}
                  >
                    <T keyName="slack_connect_cancel" />
                  </Button>
                  <LoadingButton
                    disabled={Boolean(connectionInfo.error)}
                    loading={slackMutation.isLoading}
                    onClick={connectSlack}
                    size="medium"
                    variant="contained"
                    color="primary"
                  >
                    <T keyName="slack_connect_confirm" />
                  </LoadingButton>
                </Box>
              </StyledContainer>
            )}
          </>
        }
      />
    </DashboardPage>
  );
};

export default SlackConnectView;
