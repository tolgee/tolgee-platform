import { T } from '@tolgee/react';
import { container } from 'tsyringe';

import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { confirmation } from 'tg.hooks/confirmation';
import { MessageService } from 'tg.service/MessageService';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useHistory } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';
import { useGlobalDispatch } from 'tg.globalContext/GlobalContext';

const messageService = container.resolve(MessageService);

export const useLeaveOrganization = () => {
  const leaveLoadable = useApiMutation({
    url: '/v2/organizations/{id}/leave',
    method: 'put',
  });
  const history = useHistory();
  const initialDataDispatch = useGlobalDispatch();

  return (id: number) => {
    confirmation({
      message: <T>really_leave_organization_confirmation_message</T>,
      onConfirm: () =>
        leaveLoadable.mutate(
          { path: { id } },
          {
            onSuccess() {
              messageService.success(<T>organization_left_message</T>);
              history.push(LINKS.PROJECTS.build());
              initialDataDispatch({ type: 'REFETCH_INITIAL_DATA' });
            },
            onError(e) {
              const parsed = parseErrorResponse(e);
              parsed.forEach((error) => messageService.error(<T>{error}</T>));
            },
          }
        ),
    });
  };
};
