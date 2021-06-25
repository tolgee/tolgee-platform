import { T } from '@tolgee/react';
import { container } from 'tsyringe';

import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { confirmation } from 'tg.hooks/confirmation';
import { MessageService } from 'tg.service/MessageService';
import { useApiMutation } from 'tg.service/http/useQueryApi';

const messageService = container.resolve(MessageService);

export const useLeaveOrganization = () => {
  const leaveLoadable = useApiMutation({
    url: '/v2/organizations/{id}/leave',
    method: 'put',
  });

  return (id: number) => {
    confirmation({
      message: <T>really_leave_organization_confirmation_message</T>,
      onConfirm: () =>
        leaveLoadable.mutate(
          { path: { id } },
          {
            onSuccess() {
              messageService.success(<T>organization_left_message</T>);
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
