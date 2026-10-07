import { IconButton, Tooltip } from '@mui/material';

import { confirmation } from 'tg.hooks/confirmation';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { messageService } from 'tg.service/MessageService';

type Props = {
  userId: number;
  dataCy: string;
  icon: React.ReactNode;
  tooltip: string;
  confirmMessage: React.ReactNode;
  successMessage: React.ReactNode;
  url:
    | '/v2/organizations/{organizationId}/users/{userId}/disable'
    | '/v2/organizations/{organizationId}/users/{userId}/enable';
};

export const UserAvailabilityButton = (props: Props) => {
  const organization = useOrganization();
  const submitLoadable = useApiMutation({
    url: props.url,
    method: 'put',
    invalidatePrefix: '/v2/organizations',
  });

  const confirmAndSubmit = () => {
    confirmation({
      message: props.confirmMessage,
      onConfirm: () =>
        submitLoadable.mutate(
          {
            path: {
              organizationId: organization!.id,
              userId: props.userId,
            },
          },
          {
            onSuccess: () => {
              messageService.success(props.successMessage);
            },
          }
        ),
    });
  };

  return (
    <Tooltip title={props.tooltip}>
      <IconButton
        data-cy={props.dataCy}
        onClick={confirmAndSubmit}
        size="small"
      >
        {props.icon}
      </IconButton>
    </Tooltip>
  );
};
