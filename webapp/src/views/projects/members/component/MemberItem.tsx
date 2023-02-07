import { useTranslate } from '@tolgee/react';
import { Chip, styled } from '@mui/material';

import { PermissionsMenu } from 'tg.views/projects/members/component/PermissionsMenu';
import { useProject } from 'tg.hooks/useProject';
import { useUser } from 'tg.globalContext/helpers';
import { components } from 'tg.service/apiSchema.generated';
import RevokePermissionsButton from './RevokePermissionsButton';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';

type UserAccountInProjectModel =
  components['schemas']['UserAccountInProjectModel'];

const StyledListItem = styled('div')`
  display: flex;
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider2.main};
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

type Props = {
  user: UserAccountInProjectModel;
};

export const MemberItem: React.FC<Props> = ({ user }) => {
  const project = useProject();
  const currentUser = useUser();
  const { t } = useTranslate();
  const { satisfiesPermission } = useProjectPermissions();
  const isAdmin = satisfiesPermission('admin');

  const isCurrentUser = currentUser?.id === user.id;
  const isOwner = user.organizationRole === 'OWNER';

  return (
    <StyledListItem data-cy="project-member-item">
      <StyledItemText>
        {user.name} ({user.username}){' '}
        {user.organizationRole && (
          <Chip size="small" label={project.organizationOwner?.name} />
        )}
      </StyledItemText>
      <StyledItemActions>
        <PermissionsMenu
          user={user}
          buttonTooltip={
            isOwner && !isCurrentUser
              ? t('user_is_owner_of_organization_tooltip')
              : isOwner
              ? t('cannot_change_your_own_access_tooltip')
              : undefined
          }
          buttonProps={{
            size: 'small',
            disabled: !isAdmin || isCurrentUser || isOwner,
          }}
          permissions={user.computedPermission}
        />
        <RevokePermissionsButton user={user} />
      </StyledItemActions>
    </StyledListItem>
  );
};
