import { FunctionComponent, useEffect } from 'react';
import { useRouteMatch } from 'react-router-dom';
import { container } from 'tsyringe';
import { T } from '@tolgee/react';

import { PARAMS } from '../../constants/links';
import { FullPageLoading } from '../common/FullPageLoading';

import { GlobalActions } from '../../store/global/GlobalActions';
import { RedirectionActions } from '../../store/global/RedirectionActions';
import { MessageService } from '../../service/MessageService';
import { InvitationCodeService } from '../../service/InvitationCodeService';
import { TokenService } from '../../service/TokenService';
import { LINKS } from '../../constants/links';
import { useApiQuery } from '../../service/http/useQueryApi';

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

    const { isSuccess, error } = useApiQuery({
      url: '/api/invitation/accept/{code}',
      method: 'get',
      path: { code },
    });

    useEffect(() => {
      if (!tokenService.getToken()) {
        invitationCodeService.setCode(code);
        //circular dependency
        globalActions.allowRegistration.dispatch();
        redirectActions.redirect.dispatch(LINKS.LOGIN.build());
        return;
      }

      if (isSuccess || error) {
        if (isSuccess) {
          messaging.success(<T>invitation_code_accepted</T>);
        } else {
          messaging.error(<T>{error.code}</T>);
        }
        redirectActions.redirect.dispatch(LINKS.PROJECTS.build());
      }
    }, [isSuccess, error]);

    return <FullPageLoading />;
  };
export default AcceptInvitationHandler;
