import { Button, useMediaQuery } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { CompactView } from 'tg.component/layout/CompactView';
import { SPLIT_CONTENT_BREAK_POINT } from 'tg.component/security/SplitContent';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useHistory, useLocation } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';

export const SlackLoginView = () => {
  const { t } = useTranslate();
  const location = useLocation();
  const history = useHistory();

  const queryParameters = new URLSearchParams(location.search);
  const encryptedData = queryParameters.get('data');

  const error = false;

  const slackMutation = useApiMutation({
    url: '/v2/slack/user-login',
    method: 'post',
  });

  const connectionInfo = useApiQuery({
    // TODO: Don't know why tsc is unhappy, when it works and the schema is correct
    url: '/v2/slack/user-login-info',
    mehod: 'get',
    query: {
      data: encryptedData,
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
          // TODO: Maybe we can only print success message and tell
          //  them to continue in slack instead of redirecting them somewhere else
          history.push(LINKS.ROOT.build());
        },
      }
    );
  };

  const isSmall = useMediaQuery(SPLIT_CONTENT_BREAK_POINT);

  // TODO: We should keep the top bar, so the user can logout if they are logged to different account by accident
  return (
    <CompactView
      maxWidth={isSmall ? 430 : 964}
      windowTitle={t('login_title')}
      title="Slack integration"
      content={
        <>
          <h1>Connect Slack to Tolgee</h1>

          <ul>
            <li>Slack User Id: {connectionInfo.data?.slackId}</li>
            <li>Slack User Name: {connectionInfo.data?.slackName}</li>
            <li>Slack User Name: {connectionInfo.data?.slackRealName}</li>
          </ul>

          <Button
            disabled={error}
            onClick={connectSlack}
            size="medium"
            variant="outlined"
            style={{ marginBottom: '0.5rem', marginTop: '1rem' }}
            color="primary"
          >
            Connect to Slack
          </Button>
        </>
      }
    />
  );
};
