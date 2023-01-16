import { useState } from 'react';
import { useHistory } from 'react-router-dom';
import { T } from '@tolgee/react';
import { container } from 'tsyringe';

import { LINKS } from 'tg.constants/links';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { MessageService } from 'tg.service/MessageService';
import { confirmation } from 'tg.hooks/confirmation';

const messaging = container.resolve(MessageService);

export const useLeaveProject = () => {
  const history = useHistory();
  const { refetchInitialData } = useGlobalActions();
  const [isLeaving, setIsLeaving] = useState(false);

  const leaveLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/leave',
    method: 'put',
    options: {
      onSuccess() {
        refetchInitialData();
        messaging.success(<T>project_successfully_left</T>);
        history.push(LINKS.PROJECTS.build());
      },
      onError(e) {
        switch (e.code) {
          case 'cannot_leave_project_with_organization_role':
            messaging.error(
              <T>cannot_leave_project_with_organization_role_error_message</T>
            );
            break;
          default:
            messaging.error(
              <T params={{ code: e.code }}>unexpected_error_message</T>
            );
        }
      },
      onSettled() {
        setIsLeaving(false);
      },
    },
    fetchOptions: {
      disableBadRequestHandling: true,
    },
  });

  const leave = (projectName: string, projectId: number) => {
    confirmation({
      title: <T>leave_project_confirmation_title</T>,
      message: <T>leave_project_confirmation_message</T>,
      hardModeText: projectName.toUpperCase(),
      onConfirm() {
        setIsLeaving(true);
        leaveLoadable.mutate({ path: { projectId } });
      },
    });
  };
  return { leave, isLeaving };
};
