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
import { useGlobalDispatch } from 'tg.globalContext/GlobalContext';

interface AcceptInvitationHandlerProps {}

const globalActions = container.resolve(GlobalActions);
const redirectActions = container.resolve(RedirectionActions);
const messaging = container.resolve(MessageService);
const invitationCodeService = container.resolve(InvitationCodeService);
const tokenService = container.resolve(TokenService);

const AcceptInvitationHandler: FunctionComponent<AcceptInvitationHandlerProps> =
  () => {
    const match = useRouteMatch();

    const code = match.params[PARAMS.INVITATION_CODE];
    const initialDataDispatch = useGlobalDispatch();

    const acceptCode = useApiMutation({
      url: '/api/invitation/accept/{code}',
      method: 'get',
    });

    useEffect(() => {
      if (!tokenService.getToken()) {
        invitationCodeService.setCode(code);
        globalActions.allowRegistration.dispatch();
        redirectActions.redirect.dispatch(LINKS.LOGIN.build());
        messaging.success(<T>invitation_log_in_first</T>);
      } else {
        acceptCode.mutate(
          { path: { code } },
          {
            onSuccess() {
              initialDataDispatch({ type: 'REFETCH_INITIAL_DATA' });
              messaging.success(<T>invitation_code_accepted</T>);
            },
            onError(e) {
              messaging.error(<T>{e.code}</T>);
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
