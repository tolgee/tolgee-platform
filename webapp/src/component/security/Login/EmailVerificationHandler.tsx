import { FunctionComponent } from 'react';
import { T } from '@tolgee/react';
import { useHistory, useRouteMatch } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import { messageService } from 'tg.service/MessageService';
import { useApiQuery } from 'tg.service/http/useQueryApi';

import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';

interface OAuthRedirectionHandlerProps {}

export const EmailVerificationHandler: FunctionComponent<
  OAuthRedirectionHandlerProps
> = () => {
  const match = useRouteMatch();
  const history = useHistory();
  const { handleAfterLogin, refetchInitialData } = useGlobalActions();

  useApiQuery({
    url: '/api/public/verify_email/{userId}/{code}',
    method: 'get',
    path: {
      userId: match.params[PARAMS.USER_ID],
      code: match.params[PARAMS.VERIFICATION_CODE],
    },
    options: {
      onSuccess(data) {
        messageService.success(<T keyName="email_verified_message" />);
        refetchInitialData();
        handleAfterLogin(data);
      },
      onSettled() {
        history.replace(LINKS.AFTER_LOGIN.build());
      },
    },
  });

  return (
    <>
      <FullPageLoading />
    </>
  );
};
