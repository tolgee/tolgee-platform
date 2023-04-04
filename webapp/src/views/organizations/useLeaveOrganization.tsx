import { T } from '@tolgee/react';
import { container } from 'tsyringe';

import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { confirmation } from 'tg.hooks/confirmation';
import { MessageService } from 'tg.service/MessageService';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useHistory } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { TranslatedError } from 'tg.translationTools/TranslatedError';

const messageService = container.resolve(MessageService);

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
      onError(e) {
        const parsed = parseErrorResponse(e);
        parsed.forEach((error) =>
          messageService.error(<TranslatedError code={error} />)
        );
      },
    },
    fetchOptions: {
      disableBadRequestHandling: true,
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
