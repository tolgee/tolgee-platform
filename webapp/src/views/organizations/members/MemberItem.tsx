import { useTranslate } from '@tolgee/react';
import { IconButton, styled, Tooltip } from '@mui/material';
import { Clear } from '@mui/icons-material';
import { useUser } from 'tg.globalContext/helpers';

import { components } from 'tg.service/apiSchema.generated';
import { RemoveUserButton } from './RemoveUserButton';
import { UpdateRoleButton } from './UpdateRoleButton';
import { useLeaveOrganization } from '../useLeaveOrganization';

type UserAccountWithOrganizationRoleModel =
  components['schemas']['UserAccountWithOrganizationRoleModel'];

const StyledListItem = styled('div')`
  display: flex;
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider2.main};
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

type Props = {
  user: UserAccountWithOrganizationRoleModel;
  organizationId: number;
};

export const MemberItem: React.FC<Props> = ({ user, organizationId }) => {
  const t = useTranslate();
  const currentUser = useUser();
  const leaveOrganization = useLeaveOrganization();

  return (
    <StyledListItem data-cy="organization-member-item">
      <StyledItemText>
        {user.name} ({user.username}){' '}
      </StyledItemText>
      <StyledItemActions>
        <UpdateRoleButton user={user} />

        {currentUser?.id === user.id ? (
          <Tooltip title={t('organization_users_leave')}>
            <IconButton
              size="small"
              onClick={() => leaveOrganization(organizationId)}
              data-cy="organization-member-leave-button"
            >
              <Clear />
            </IconButton>
          </Tooltip>
        ) : (
          <RemoveUserButton userId={user.id} userName={user.username} />
        )}
      </StyledItemActions>
    </StyledListItem>
  );
};
