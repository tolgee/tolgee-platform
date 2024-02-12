import {Alert, Button, useMediaQuery} from "@mui/material";
import {T, useTranslate} from '@tolgee/react';
import {useSelector} from "react-redux";
import {CompactView} from "tg.component/layout/CompactView";
import {SPLIT_CONTENT_BREAK_POINT} from "tg.component/security/SplitContent";
import {AppState} from "tg.store/index";
import {TranslatedError} from "tg.translationTools/TranslatedError";
import {useApiMutation} from 'tg.service/http/useQueryApi';
import {useHistory, useLocation} from "react-router-dom"
import {useUser} from 'tg.globalContext/helpers';
import {LINKS} from "tg.constants/links";
import {useMessage} from "tg.hooks/useSuccessMessage";

export const SlackLoginView = () => {
    const { t } = useTranslate();
    const location = useLocation()
    const messaging = useMessage();
    const history = useHistory();
    
    const queryParameters = new URLSearchParams(location.search)
    const slackId = queryParameters.get('slackId')
    const slackChannelId = queryParameters.get('channelId')
    const user = useUser()
    const error = !slackId || !slackChannelId || !user;

    const validSlackId = slackId ?? 'defaultSlackId'; 
    const validChannelId = slackChannelId ?? 'defaultSlackId'; 
    const validUserAccountId = user?.id?.toString() ?? 'defaultAccountId';

    const slackMutation = useApiMutation({
        url: '/v2/slack/events/connect', 
        method: 'post',
    });

    const connectSlack = () => {
        slackMutation.mutate({
            content: {
                'application/json': {
                    slackId: validSlackId, 
                    userAccountId: validUserAccountId,
                    channelId: validChannelId
                }
            },
        }, {
            onSuccess: () => {
                messaging.success(
                    <T keyName="" />
                  );
                history.push(LINKS.ROOT.build());
            },
        });
    };
  
    const security = useSelector((state: AppState) => state.global.security);
  
    const isSmall = useMediaQuery(SPLIT_CONTENT_BREAK_POINT);

      return (
        <CompactView
            maxWidth={isSmall ? 430 : 964}
            windowTitle={t('login_title')}
            title='Slack integration'
            alerts={
                <>
                    {security.loginErrorCode && (
                        <Alert severity="error">
                            <TranslatedError code={security.loginErrorCode} />
                        </Alert>
                    )}
                    {error && (
                        <Alert severity="warning">
                            Please make sure all required fields (Slack ID, Slack Channel ID, User) are provided.
                        </Alert>
                    )}
                </>
            }
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
}