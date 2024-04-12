import { Button, useMediaQuery } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { CompactView } from 'tg.component/layout/CompactView';
import { SPLIT_CONTENT_BREAK_POINT } from 'tg.component/security/SplitContent';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useHistory, useLocation } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';

export const SlackLoginView = () => {
  const { t } = useTranslate();
  const location = useLocation();
  const history = useHistory();

  const queryParameters = new URLSearchParams(location.search);
  const slackId = queryParameters.get('slackId');
  const slackChannelId = queryParameters.get('channelId');
  const workspaceId = queryParameters.get('workspaceId');

  const error = false;

  const slackMutation = useApiMutation({
    url: '/v2/slack/user-login',
    method: 'post',
  });

  if (!slackId || !slackChannelId || !workspaceId) {
    return <div>Invalid slack login parameters</div>;
  }

  const connectSlack = () => {
    slackMutation.mutate(
      {
        content: {
          'application/json': {
            slackId: slackId,
            channelId: slackChannelId,
            workspaceId: Number.parseInt(workspaceId),
          },
        },
      },
      {
        onSuccess: () => {
          // TODO: show success message
          history.push(LINKS.ROOT.build());
        },
      }
    );
  };

  const isSmall = useMediaQuery(SPLIT_CONTENT_BREAK_POINT);

  return (
    <CompactView
      maxWidth={isSmall ? 430 : 964}
      windowTitle={t('login_title')}
      title="Slack integration"
      content={
        <>
          <h1>Connect Slack to Tolgee</h1> {}
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
