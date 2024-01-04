import { FunctionComponent, useEffect } from 'react';
import { T } from '@tolgee/react';
import { useRouteMatch } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import { messageService } from 'tg.service/MessageService';
import { signUpService } from 'tg.service/SignUpService';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { redirectionActions } from 'tg.store/global/RedirectionActions';

import { FullPageLoading } from 'tg.component/common/FullPageLoading';

interface OAuthRedirectionHandlerProps {}

export const EmailVerificationHandler: FunctionComponent<
  OAuthRedirectionHandlerProps
> = () => {
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
      messageService.success(<T keyName="email_verified_message" />);
      redirectionActions.redirect.dispatch(LINKS.AFTER_LOGIN.build());
    }
  }, [data]);

  return (
    <>
      <FullPageLoading />
    </>
  );
};
