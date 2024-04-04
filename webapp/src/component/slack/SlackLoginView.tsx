import {Button, useMediaQuery} from '@mui/material';
import {T, useTranslate} from '@tolgee/react';
import {CompactView} from 'tg.component/layout/CompactView';
import {SPLIT_CONTENT_BREAK_POINT} from 'tg.component/security/SplitContent';
import {useApiMutation} from 'tg.service/http/useQueryApi';
import {useHistory, useLocation} from 'react-router-dom';
import {useUser} from 'tg.globalContext/helpers';
import {LINKS} from 'tg.constants/links';
import {useMessage} from 'tg.hooks/useSuccessMessage';

export const SlackLoginView = () => {
  const { t } = useTranslate();
  const location = useLocation();
  const messaging = useMessage();
  const history = useHistory();

  const queryParameters = new URLSearchParams(location.search);
  const slackId = queryParameters.get('slackId');
  const slackChannelId = queryParameters.get('channelId');
  const slackNickName = queryParameters.get('nickName');

  const user = useUser();
  const error = !slackId || !slackChannelId || !user;

  const validSlackId = slackId ?? 'defaultSlackId';
  const validChannelId = slackChannelId ?? 'defaultSlackId';
  const validUserAccountId = user?.id?.toString() ?? 'defaultAccountId';
  const validSlackNickName = slackNickName ?? '';

  const slackMutation = useApiMutation({
    url: '/v2/slack/connect',
    method: 'post',
  });

  const connectSlack = () => {
    slackMutation.mutate(
      {
        content: {
          'application/json': {
            slackId: validSlackId,
            userAccountId: validUserAccountId,
            channelId: validChannelId,
            slackNickName: validSlackNickName,
          },
        },
      },
      {
        onSuccess: () => {
          messaging.success(<T keyName="" />);
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
