import { useState } from 'react';
import { useTranslate } from '@tolgee/react';
import {
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
import { useUser } from 'tg.globalContext/helpers';
import { Link } from 'react-router-dom';

import { components } from 'tg.service/apiSchema.generated';
import { RemoveUserButton } from './RemoveUserButton';
import { UpdateRoleButton } from './UpdateRoleButton';
import { useLeaveOrganization } from '../useLeaveOrganization';
import { LINKS, PARAMS } from 'tg.constants/links';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { MfaBadge } from '@tginternal/library/components/MfaBadge';

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
  organizationId: number;
};

export const MemberItem: React.FC<Props> = ({ user, organizationId }) => {
  const { t } = useTranslate();
  const currentUser = useUser();
  const leaveOrganization = useLeaveOrganization();

  const [projectsOpen, setProjectsOpen] = useState(false);

  return (
    <StyledListItem data-cy="organization-member-item">
      <StyledItemUser>
        <AvatarImg owner={{ ...user, type: 'USER' }} size={24} />
        <StyledItemText>
          {user.name} ({user.username}){' '}
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

        {currentUser?.id === user.id ? (
          <Tooltip title={t('organization_users_leave')}>
            <IconButton
              size="small"
              onClick={() => leaveOrganization(organizationId)}
              data-cy="organization-member-leave-button"
            >
              <XClose />
            </IconButton>
          </Tooltip>
        ) : (
          <RemoveUserButton userId={user.id} userName={user.username} />
        )}
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
