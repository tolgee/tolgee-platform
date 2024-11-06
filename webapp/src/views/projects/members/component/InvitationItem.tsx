import { T, useTranslate } from '@tolgee/react';
import { IconButton, styled, Tooltip } from '@mui/material';
import { XClose, Link02 } from '@untitled-ui/icons-react';

import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { LanguagePermissionSummary } from 'tg.component/PermissionsSettings/LanguagePermissionsSummary';
import { ScopesInfo } from 'tg.component/PermissionsSettings/ScopesInfo';
import { usePermissionTranslation } from 'tg.translationTools/usePermissionTranslation';
import { messageService } from 'tg.service/MessageService';

type UserAccountInProjectModel =
  components['schemas']['ProjectInvitationModel'];

const StyledListItem = styled('div')`
  display: flex;
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};

  &:last-child {
    border-bottom: 0;
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

const StyledPermissions = styled('div')`
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
  invitation: UserAccountInProjectModel;
};

export const InvitationItem: React.FC<Props> = ({ invitation }) => {
  const { t } = useTranslate();
  const languages = useProjectLanguages();
  const { satisfiesPermission } = useProjectPermissions();
  const canEditMembers = satisfiesPermission('members.edit');

  const translatePermission = usePermissionTranslation();

  const deleteInvitation = useApiMutation({
    url: '/v2/invitations/{invitationId}',
    method: 'delete',
    invalidatePrefix: '/v2/projects/{projectId}/invitations',
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
    <StyledListItem>
      <StyledItemText>
        {invitation.invitedUserName || invitation.invitedUserEmail}{' '}
      </StyledItemText>
      <StyledItemActions>
        <ScopesInfo scopes={invitation.permission.scopes} />

        <LanguagePermissionSummary
          permissions={invitation.permission}
          allLangs={languages}
        />

        <Tooltip
          title={translatePermission(invitation.type || 'granular', true)}
        >
          <StyledPermissions>
            {translatePermission(invitation.type || 'granular')}
          </StyledPermissions>
        </Tooltip>

        <Tooltip title={t('invite_user_invitation_copy_button')}>
          <IconButton size="small" onClick={handleGetLink}>
            <Link02 />
          </IconButton>
        </Tooltip>
        <Tooltip title={t('invite_user_invitation_cancel_button')}>
          <IconButton
            size="small"
            onClick={handleCancel}
            disabled={!canEditMembers}
          >
            <XClose />
          </IconButton>
        </Tooltip>
      </StyledItemActions>
    </StyledListItem>
  );
};
