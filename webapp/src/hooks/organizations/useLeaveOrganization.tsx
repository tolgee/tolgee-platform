import { T } from '@tolgee/react';
import React, { ReactNode, useEffect } from 'react';
import { container } from 'tsyringe';
import { OrganizationActions } from '../../store/organization/OrganizationActions';
import { confirmation } from '../confirmation';
import { ResourceErrorComponent } from '../../component/common/form/ResourceErrorComponent';
import { Box } from '@material-ui/core';
import { parseErrorResponse } from '../../fixtures/errorFIxtures';
import { MessageService } from '../../service/MessageService';

const actions = container.resolve(OrganizationActions);
const messageService = container.resolve(MessageService);

export const useLeaveOrganization = () => {
  const leaveLoadable = actions.useSelector((state) => state.loadables.leave);

  useEffect(() => {
    if (leaveLoadable.error) {
      const parsed = parseErrorResponse(leaveLoadable.error);
      parsed.forEach((error) => messageService.error(<T>{error}</T>));
    }
  }, [leaveLoadable.error]);

  return (id) => {
    confirmation({
      message: <T>really_leave_organization_confirmation_message</T>,
      onConfirm: () => actions.loadableActions.leave.dispatch(id),
    });
  };
};
