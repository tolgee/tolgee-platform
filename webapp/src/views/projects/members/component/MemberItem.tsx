import { useTranslate } from '@tolgee/react';
import { Chip, styled } from '@mui/material';

import { PermissionsMenu } from 'tg.component/PermissionsSettings/PermissionsMenu';
import { useProject } from 'tg.hooks/useProject';
import { useUser } from 'tg.globalContext/helpers';
import { components } from 'tg.service/apiSchema.generated';
import RevokePermissionsButton from './RevokePermissionsButton';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useUpdatePermissions } from './useUpdatePermissions';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { T } from '@tolgee/react';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { PermissionSettingsState } from 'tg.component/PermissionsSettings/types';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';

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
  const allLangs = useProjectLanguages();

  const isCurrentUser = currentUser?.id === user.id;
  const isOwner = user.organizationRole === 'OWNER';

  const messages = useMessage();

  const { updatePermissions } = useUpdatePermissions({
    userId: user.id,
    projectId: project.id,
  });

  function handleSubmit(data: PermissionSettingsState) {
    return updatePermissions(data)
      .then(() => {
        messages.success(<T>permissions_set_message</T>);
      })
      .catch((e) => {
        parseErrorResponse(e).forEach((err) => messages.error(<T>{err}</T>));
      });
  }

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
          allLangs={allLangs}
          nameInTitle={user.name}
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
          onSubmit={handleSubmit}
        />
        <RevokePermissionsButton user={user} />
      </StyledItemActions>
    </StyledListItem>
  );
};
