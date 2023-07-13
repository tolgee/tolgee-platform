import { FunctionComponent, useEffect } from 'react';
import { T } from '@tolgee/react';
import { useRouteMatch } from 'react-router-dom';
import { container } from 'tsyringe';

import { LINKS, PARAMS } from 'tg.constants/links';
import { InvitationCodeService } from 'tg.service/InvitationCodeService';
import { MessageService } from 'tg.service/MessageService';
import { TokenService } from 'tg.service/TokenService';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { GlobalActions } from 'tg.store/global/GlobalActions';
import { RedirectionActions } from 'tg.store/global/RedirectionActions';

import { FullPageLoading } from '../common/FullPageLoading';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { TranslatedError } from 'tg.translationTools/TranslatedError';

interface AcceptInvitationHandlerProps {}

const globalActions = container.resolve(GlobalActions);
const redirectActions = container.resolve(RedirectionActions);
const messaging = container.resolve(MessageService);
const tokenService = container.resolve(TokenService);

const AcceptInvitationHandler: FunctionComponent<
  AcceptInvitationHandlerProps
> = () => {
  const match = useRouteMatch();

  const code = match.params[PARAMS.INVITATION_CODE];
  const { refetchInitialData } = useGlobalActions();

  const acceptCode = useApiMutation({
    url: '/v2/invitations/{code}/accept',
    method: 'get',
  });

  useEffect(() => {
    if (!tokenService.getToken()) {
      InvitationCodeService.setCode(code);
      globalActions.allowRegistration.dispatch();
      redirectActions.redirect.dispatch(LINKS.LOGIN.build());
      messaging.success(<T keyName="invitation_log_in_first" />);
    } else {
      acceptCode.mutate(
        { path: { code } },
        {
          onSuccess() {
            refetchInitialData();
            messaging.success(<T keyName="invitation_code_accepted" />);
          },
          onError(e) {
            messaging.error(<TranslatedError code={e.code} />);
          },
          onSettled() {
            redirectActions.redirect.dispatch(LINKS.PROJECTS.build());
          },
        }
      );
    }
  }, []);

  return <FullPageLoading />;
};
export default AcceptInvitationHandler;
