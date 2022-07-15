import { useState } from 'react';
import { useHistory } from 'react-router-dom';
import { T } from '@tolgee/react';
import { container } from 'tsyringe';

import { LINKS } from 'tg.constants/links';
import { useGlobalDispatch } from 'tg.globalContext/GlobalContext';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { MessageService } from 'tg.service/MessageService';
import { confirmation } from 'tg.hooks/confirmation';

const messaging = container.resolve(MessageService);

export const useLeaveProject = () => {
  const history = useHistory();
  const initialDataDispatch = useGlobalDispatch();
  const [isLeaving, setIsLeaving] = useState(false);

  const leaveLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/leave',
    method: 'put',
    options: {
      onSuccess() {
        initialDataDispatch({ type: 'REFETCH_INITIAL_DATA' });
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
              <T parameters={{ code: e.code }}>unexpected_error_message</T>
            );
        }
      },
      onSettled() {
        setIsLeaving(false);
      },
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
