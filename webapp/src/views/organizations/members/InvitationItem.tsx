import { T, useTranslate } from '@tolgee/react';
import { IconButton, styled, Tooltip } from '@mui/material';
import { Link02, XClose } from '@untitled-ui/icons-react';

import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useOrgRoleTranslation } from 'tg.translationTools/useOrgRoleTranslation';
import { messageService } from 'tg.service/MessageService';

type OrganizationInvitationModel =
  components['schemas']['OrganizationInvitationModel'];

const StyledListItem = styled('div')`
  display: flex;
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
  &:last-child {
    border-bottom: 0px;
  }
  position: relative;
  padding: ${({ theme }) => theme.spacing(1)};
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
`;

const StyledItemText = styled('div')`
  flex-grow: 1;
  padding: ${({ theme }) => theme.spacing(1)};
`;

const StyledItemActions = styled('div')`
  display: flex;
  gap: ${({ theme }) => theme.spacing(1)};
  align-items: center;
  flex-wrap: wrap;
`;

const StyledPermission = styled('div')`
  display: flex;
  padding: 3px 8px;
  align-items: center;
  justify-content: center;
  background: ${({ theme }) => theme.palette.emphasis[100]};
  height: 33px;
  border-radius: 3px;
  cursor: default;
`;

type Props = {
  invitation: OrganizationInvitationModel;
};

export const InvitationItem: React.FC<Props> = ({ invitation }) => {
  const { t } = useTranslate();
  const translateRole = useOrgRoleTranslation();

  const deleteInvitation = useApiMutation({
    url: '/v2/invitations/{invitationId}',
    method: 'delete',
    invalidatePrefix: '/v2/organizations/{organizationId}/invitations',
  });

  const handleCancel = () => {
    deleteInvitation.mutate({ path: { invitationId: invitation.id } });
  };

  const handleGetLink = () => {
    navigator.clipboard.writeText(
      LINKS.ACCEPT_INVITATION.buildWithOrigin({
        [PARAMS.INVITATION_CODE]: invitation.code,
      })
    );
    messageService.success(<T keyName="invite_user_invitation_copy_success" />);
  };

  return (
    <StyledListItem data-cy="organization-invitation-item">
      <StyledItemText>
        {invitation.invitedUserName || invitation.invitedUserEmail}{' '}
      </StyledItemText>
      <StyledItemActions>
        <Tooltip title={translateRole(invitation.type, true)}>
          <StyledPermission>{translateRole(invitation.type)}</StyledPermission>
        </Tooltip>

        <Tooltip title={t('invite_user_invitation_copy_button')}>
          <IconButton
            data-cy="organization-invitation-copy-button"
            size="small"
            onClick={handleGetLink}
          >
            <Link02 />
          </IconButton>
        </Tooltip>

        <Tooltip title={t('invite_user_invitation_cancel_button')}>
          <IconButton
            data-cy="organization-invitation-cancel-button"
            size="small"
            onClick={handleCancel}
          >
            <XClose />
          </IconButton>
        </Tooltip>
      </StyledItemActions>
    </StyledListItem>
  );
};
