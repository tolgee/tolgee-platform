import { FunctionComponent, useEffect } from 'react';
import { T } from '@tolgee/react';
import { useRouteMatch } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import { InvitationCodeService } from 'tg.service/InvitationCodeService';
import { messageService } from 'tg.service/MessageService';
import { tokenService } from 'tg.service/TokenService';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { globalActions } from 'tg.store/global/GlobalActions';
import { redirectionActions } from 'tg.store/global/RedirectionActions';

import { FullPageLoading } from '../common/FullPageLoading';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';

interface AcceptInvitationHandlerProps {}

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
      redirectionActions.redirect.dispatch(LINKS.LOGIN.build());
      messageService.success(<T keyName="invitation_log_in_first" />);
    } else {
      acceptCode.mutate(
        { path: { code } },
        {
          onSuccess() {
            refetchInitialData();
            messageService.success(<T keyName="invitation_code_accepted" />);
          },

          onSettled() {
            redirectionActions.redirect.dispatch(LINKS.PROJECTS.build());
          },
        }
      );
    }
  }, []);

  return <FullPageLoading />;
};
export default AcceptInvitationHandler;
