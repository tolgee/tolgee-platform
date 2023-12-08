import { T } from '@tolgee/react';
import { useHistory } from 'react-router-dom';

import { confirmation } from 'tg.hooks/confirmation';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { LINKS } from 'tg.constants/links';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { messageService } from 'tg.service/MessageService';

export const useLeaveOrganization = () => {
  const leaveLoadable = useApiMutation({
    url: '/v2/organizations/{id}/leave',
    method: 'put',
    options: {
      onSuccess() {
        messageService.success(<T keyName="organization_left_message" />);
        history.push(LINKS.PROJECTS.build());
        refetchInitialData();
      },
    },
  });
  const history = useHistory();
  const { refetchInitialData } = useGlobalActions();

  return (id: number) => {
    confirmation({
      message: <T keyName="really_leave_organization_confirmation_message" />,
      onConfirm: () => leaveLoadable.mutate({ path: { id } }),
    });
  };
};
