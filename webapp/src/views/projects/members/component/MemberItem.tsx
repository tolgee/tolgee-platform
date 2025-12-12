import { T, useTranslate } from '@tolgee/react';
import { Chip, styled, Tooltip } from '@mui/material';

import { AgencyLabel } from 'tg.ee';
import { PermissionsMenu } from 'tg.component/PermissionsSettings/PermissionsMenu';
import { useProject } from 'tg.hooks/useProject';
import { useUser } from 'tg.globalContext/helpers';
import { components } from 'tg.service/apiSchema.generated';
import RevokePermissionsButton from './RevokePermissionsButton';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useUpdatePermissions } from './useUpdatePermissions';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { PermissionSettingsState } from 'tg.component/PermissionsSettings/types';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { LanguagePermissionSummary } from 'tg.component/PermissionsSettings/LanguagePermissionsSummary';
import { ScopesInfo } from 'tg.component/PermissionsSettings/ScopesInfo';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { MfaBadge } from '@tginternal/library/components/MfaBadge';

type UserAccountInProjectModel =
  components['schemas']['UserAccountInProjectModel'];

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
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
`;

const StyledItemActions = styled('div')`
  display: flex;
  gap: ${({ theme }) => theme.spacing(1)};
  align-items: center;
  flex-wrap: wrap;
`;

const StyledItemUser = styled('div')`
  display: flex;
  margin-left: 8px;
  flex-grow: 1;
  align-items: center;
`;

type Props = {
  user: UserAccountInProjectModel;
};

export const MemberItem: React.FC<Props> = ({ user }) => {
  const project = useProject();
  const currentUser = useUser();
  const { t } = useTranslate();
  const { satisfiesPermission } = useProjectPermissions();
  const canEditMembers = satisfiesPermission('members.edit');
  const allLangs = useProjectLanguages();

  const isCurrentUser = currentUser?.id === user.id;
  const isOwner = user.organizationRole === 'OWNER';

  const messages = useMessage();

  const { updatePermissions, setByOrganization } = useUpdatePermissions({
    userId: user.id,
    projectId: project.id,
  });

  async function handleSubmit(data: PermissionSettingsState) {
    await updatePermissions(data);
    messages.success(<T keyName="permissions_set_message" />);
  }

  const isOrganzationMember = Boolean(user.organizationRole);
  const hasDirectPermissions = Boolean(user.directPermission);

  async function handleResetToOrganization() {
    await setByOrganization();
    messages.success(<T keyName="permissions_reset_message" />);
  }

  return (
    <StyledListItem data-cy="project-member-item">
      <StyledItemUser>
        <AvatarImg owner={{ ...user, type: 'USER' }} size={24} />
        <StyledItemText>
          {user.name} ({user.username}){' '}
          {user.organizationRole && (
            <Chip size="small" label={project.organizationOwner?.name} />
          )}
          {user.directPermission?.agency && (
            <Tooltip title={t('member_item_agency_tooltip')}>
              <span>
                <AgencyLabel agency={user.directPermission.agency} />
              </span>
            </Tooltip>
          )}
        </StyledItemText>
      </StyledItemUser>
      <StyledItemActions>
        <MfaBadge enabled={user.mfaEnabled} />
        <ScopesInfo scopes={user.computedPermission.scopes} />
        <LanguagePermissionSummary
          permissions={user.computedPermission}
          allLangs={allLangs}
        />
        <PermissionsMenu
          buttonTooltip={
            isOwner && !isCurrentUser
              ? t('user_is_owner_of_organization_tooltip')
              : isOwner
              ? t('cannot_change_your_own_access_tooltip')
              : undefined
          }
          buttonProps={{
            size: 'small',
            disabled: !canEditMembers || isCurrentUser || isOwner,
          }}
          modalProps={{
            allLangs,
            title: user.name || user.username,
            permissions: user.computedPermission,
            onSubmit: handleSubmit,
            isInheritedFromOrganization:
              !hasDirectPermissions && isOrganzationMember,
            onResetToOrganization:
              hasDirectPermissions && isOrganzationMember
                ? handleResetToOrganization
                : undefined,
          }}
        />
        <RevokePermissionsButton user={user} />
      </StyledItemActions>
    </StyledListItem>
  );
};
