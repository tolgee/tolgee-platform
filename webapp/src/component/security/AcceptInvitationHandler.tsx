import { useEffect } from 'react';
import { useHistory, useRouteMatch } from 'react-router-dom';
import { T } from '@tolgee/react';

import { LINKS, PARAMS } from 'tg.constants/links';
import { messageService } from 'tg.service/MessageService';
import { tokenService } from 'tg.service/TokenService';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';

import { FullPageLoading } from '../common/FullPageLoading';

const AcceptInvitationHandler: React.FC = () => {
  const history = useHistory();
  const match = useRouteMatch();

  const code = match.params[PARAMS.INVITATION_CODE];
  const { refetchInitialData, setInvitationCode } = useGlobalActions();

  const acceptCode = useApiMutation({
    url: '/v2/invitations/{code}/accept',
    method: 'get',
  });

  useEffect(() => {
    if (!tokenService.getToken()) {
      setInvitationCode(code);
      messageService.success(<T keyName="invitation_log_in_first" />);
      history.replace(LINKS.LOGIN.build());
    } else {
      acceptCode.mutate(
        { path: { code } },
        {
          onSuccess() {
            refetchInitialData();
            messageService.success(<T keyName="invitation_code_accepted" />);
          },
          onSettled() {
            history.replace(LINKS.PROJECTS.build());
          },
        }
      );
    }
  }, []);

  return <FullPageLoading />;
};
export default AcceptInvitationHandler;
