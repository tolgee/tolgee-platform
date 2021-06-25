import { FunctionComponent, useEffect } from 'react';
import { T } from '@tolgee/react';
import { useRouteMatch } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { FullPageLoading } from '../common/FullPageLoading';
import { container } from 'tsyringe';
import { RedirectionActions } from 'tg.store/global/RedirectionActions';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { MessageService } from 'tg.service/MessageService';
import { SignUpService } from 'tg.service/SignUpService';

interface OAuthRedirectionHandlerProps {}

const messageService = container.resolve(MessageService);
const redirectionActions = container.resolve(RedirectionActions);
const signUpService = container.resolve(SignUpService);

export const EmailVerificationHandler: FunctionComponent<OAuthRedirectionHandlerProps> =
  () => {
    const match = useRouteMatch();

    const { data } = useApiQuery({
      url: '/api/public/verify_email/{userId}/{code}',
      method: 'get',
      path: {
        userId: match.params[PARAMS.USER_ID],
        code: match.params[PARAMS.VERIFICATION_CODE],
      },
    });

    useEffect(() => {
      if (data) {
        signUpService.verifyEmail(data.accessToken);
        messageService.success(<T>email_verified_message</T>);
        redirectionActions.redirect.dispatch(LINKS.AFTER_LOGIN.build());
      }
    }, [data]);

    return (
      <>
        <FullPageLoading />
      </>
    );
  };
