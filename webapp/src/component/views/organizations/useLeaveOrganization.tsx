import { T } from '@tolgee/react';
import { container } from 'tsyringe';
import { confirmation } from '../../../hooks/confirmation';
import { parseErrorResponse } from '../../../fixtures/errorFIxtures';
import { MessageService } from '../../../service/MessageService';
import { usePutOrganizationLeave } from '../../../service/hooks/Organization';

const messageService = container.resolve(MessageService);

export const useLeaveOrganization = () => {
  const leaveLoadable = usePutOrganizationLeave();

  return (id: number) => {
    confirmation({
      message: <T>really_leave_organization_confirmation_message</T>,
      onConfirm: () =>
        leaveLoadable.mutate(id, {
          onSuccess() {
            messageService.success(<T>organization_left_message</T>);
          },
          onError(e) {
            const parsed = parseErrorResponse(e);
            parsed.forEach((error) => messageService.error(<T>{error}</T>));
          },
        }),
    });
  };
};
