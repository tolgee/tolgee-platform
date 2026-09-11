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
  React.PropsWithChildren<OAuthRedirectionHandlerProps>
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
      // the code is one-time-use and gets consumed by the first GET that hits it,
      // so a remount/refetch of this same query must not re-issue the request
      staleTime: Infinity,
      cacheTime: Infinity,
      async onSuccess(data) {
        messageService.success(<T keyName="email_verified_message" />);
        // handleAfterLogin puts the token in place; initial data must not be
        // fetched before that or it comes back as the unauthenticated payload
        await handleAfterLogin(data);
        refetchInitialData();
      },
      onError(error) {
        if (error.code === 'email_already_verified') {
          messageService.success(<T keyName="email_verified_message" />);
        } else {
          error.handleError?.();
        }
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
