import { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import {
  Chip,
  IconButton,
  styled,
  Tooltip,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  Link as MuiLink,
} from '@mui/material';
import { XClose, InfoCircle } from '@untitled-ui/icons-react';
import { useIsAdmin, useUser } from 'tg.globalContext/helpers';
import { Link } from 'react-router-dom';

import { components } from 'tg.service/apiSchema.generated';
import { RemoveUserButton } from 'tg.views/organizations/members/RemoveUserButton';
import {
  DisableUserButton,
  EnableUserButton,
} from 'tg.views/organizations/members/DisableEnableUserButton';
import { UpdateRoleButton } from 'tg.views/organizations/members/UpdateRoleButton';
import { useLeaveOrganization } from 'tg.views/organizations/useLeaveOrganization';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { LINKS, PARAMS } from 'tg.constants/links';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { MfaBadge } from '@tginternal/library/components/MfaBadge';

const DISABLED_ROW_OPACITY = 0.6;

type UserAccountWithOrganizationRoleModel =
  components['schemas']['UserAccountWithOrganizationRoleModel'];

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

const StyledMfaBadgeWrapper = styled('div')`
  padding: ${({ theme }) => theme.spacing(1)};
`;

const StyledItemActions = styled('div')`
  display: flex;
  gap: ${({ theme }) => theme.spacing(1)};
  align-items: center;
  flex-wrap: wrap;
`;

const StyledInfo = styled(InfoCircle)`
  opacity: 0.5;
`;

const StyledItemUser = styled('div')`
  display: flex;
  margin-left: 8px;
  flex-grow: 1;
  align-items: center;
`;

type Props = {
  user: UserAccountWithOrganizationRoleModel;
};

export const MemberItem: React.FC<React.PropsWithChildren<Props>> = ({
  user,
}) => {
  const { t } = useTranslate();
  const currentUser = useUser();
  const isAdmin = useIsAdmin();
  const organization = useOrganization();
  const leaveOrganization = useLeaveOrganization();

  const [projectsOpen, setProjectsOpen] = useState(false);

  // A SUPPORTER's bypass is read-only (OrganizationAuthorizationInterceptor.canBypass), so these
  // PUTs would 403 for them.
  const canManageMembers = organization?.currentUserRole === 'OWNER' || isAdmin;

  const renderMemberAction = () => {
    if (currentUser?.id === user.id) {
      if (user.managed) {
        return null;
      }
      return (
        <Tooltip title={t('organization_users_leave')}>
          <IconButton
            size="small"
            onClick={() => leaveOrganization(organization!.id)}
            data-cy="organization-member-leave-button"
          >
            <XClose />
          </IconButton>
        </Tooltip>
      );
    }
    if (!canManageMembers) {
      return null;
    }
    if (user.managed) {
      return user.disabled ? (
        <EnableUserButton userId={user.id} userName={user.username} />
      ) : (
        <DisableUserButton userId={user.id} userName={user.username} />
      );
    }
    return <RemoveUserButton userId={user.id} userName={user.username} />;
  };

  return (
    <StyledListItem
      data-cy="organization-member-item"
      data-cy-username={user.username}
    >
      <StyledItemUser
        sx={{ opacity: user.disabled ? DISABLED_ROW_OPACITY : 1 }}
      >
        <AvatarImg owner={{ ...user, type: 'USER' }} size={24} />
        <StyledItemText>
          {user.name} ({user.username})
          {user.disabled && (
            <>
              {' '}
              <Chip
                size="small"
                data-cy="organization-member-disabled-label"
                label={
                  <T
                    keyName="organization_member_disabled_label"
                    defaultValue="Disabled"
                  />
                }
              />
            </>
          )}
        </StyledItemText>
        <StyledMfaBadgeWrapper>
          <MfaBadge enabled={user.mfaEnabled} />
        </StyledMfaBadgeWrapper>
      </StyledItemUser>
      <StyledItemActions>
        {user.organizationRole ? (
          <UpdateRoleButton user={user} />
        ) : (
          <>
            <Tooltip title={t('organization_users_project_access_hint')}>
              <StyledInfo fontSize="small" />
            </Tooltip>
            <Button size="small" onClick={() => setProjectsOpen(true)}>
              {t('organization_users_project_access')}
            </Button>
          </>
        )}

        {renderMemberAction()}
      </StyledItemActions>
      {projectsOpen && (
        <Dialog open={true} onClose={() => setProjectsOpen(false)} fullWidth>
          <DialogTitle>{t('organization_users_projects_title')}</DialogTitle>
          <DialogContent sx={{ minHeight: 200 }}>
            <p>{t('organization_users_projects_description')}</p>
            <ul>
              {user.projectsWithDirectPermission.map((project) => (
                <li key={project.id}>
                  <Link
                    component={MuiLink}
                    to={LINKS.PROJECT.build({
                      [PARAMS.PROJECT_ID]: project.id,
                    })}
                  >
                    {project.name}
                  </Link>
                </li>
              ))}
            </ul>
          </DialogContent>
        </Dialog>
      )}
    </StyledListItem>
  );
};
