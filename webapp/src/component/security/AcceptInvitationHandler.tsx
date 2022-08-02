import { FunctionComponent, useEffect } from 'react';
import { T } from '@tolgee/react';
import { useRouteMatch } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import { invitationCodeService } from 'tg.service/InvitationCodeService';
import { messageService } from 'tg.service/MessageService';
import { tokenService } from 'tg.service/TokenService';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { globalActions } from 'tg.store/global/GlobalActions';
import { redirectionActions } from 'tg.store/global/RedirectionActions';

import { FullPageLoading } from '../common/FullPageLoading';
import { useGlobalDispatch } from 'tg.globalContext/GlobalContext';

interface AcceptInvitationHandlerProps {}

const AcceptInvitationHandler: FunctionComponent<AcceptInvitationHandlerProps> =
  () => {
    const match = useRouteMatch();

    const code = match.params[PARAMS.INVITATION_CODE];
    const globalDispatch = useGlobalDispatch();

    const acceptCode = useApiMutation({
      url: '/api/invitation/accept/{code}',
      method: 'get',
    });

    useEffect(() => {
      if (!tokenService.getToken()) {
        invitationCodeService.setCode(code);
        globalActions.allowRegistration.dispatch();
        redirectionActions.redirect.dispatch(LINKS.LOGIN.build());
        messageService.success(<T>invitation_log_in_first</T>);
      } else {
        acceptCode.mutate(
          { path: { code } },
          {
            onSuccess() {
              globalDispatch({ type: 'REFETCH_INITIAL_DATA' });
              messageService.success(<T>invitation_code_accepted</T>);
            },
            onError(e) {
              messageService.error(<T>{e.code}</T>);
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
