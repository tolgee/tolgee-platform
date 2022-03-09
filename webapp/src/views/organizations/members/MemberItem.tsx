import { IconButton, Tooltip, makeStyles } from '@material-ui/core';
import { Clear } from '@material-ui/icons';
import { useUser } from 'tg.hooks/useUser';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { RemoveUserButton } from './RemoveUserButton';
import { UpdateRoleButton } from './UpdateRoleButton';
import { useLeaveOrganization } from '../useLeaveOrganization';

type UserAccountWithOrganizationRoleModel =
  components['schemas']['UserAccountWithOrganizationRoleModel'];

const useStyles = makeStyles((theme) => ({
  listItem: {
    display: 'flex',
    borderBottom: `1px solid ${theme.palette.lightDivider.main}`,
    '&:last-child': {
      borderBottom: 0,
    },
    position: 'relative',
    padding: theme.spacing(1),
    flexWrap: 'wrap',
    alignItems: 'center',
    justifyContent: 'flex-end',
  },
  itemText: {
    flexGrow: 1,
    padding: theme.spacing(1),
  },
  itemActions: {
    display: 'flex',
    gap: theme.spacing(1),
    alignItems: 'center',
    flexWrap: 'wrap',
  },
}));

type Props = {
  user: UserAccountWithOrganizationRoleModel;
  organizationId: number;
};

export const MemberItem: React.FC<Props> = ({ user, organizationId }) => {
  const t = useTranslate();
  const classes = useStyles();
  const currentUser = useUser();
  const leaveOrganization = useLeaveOrganization();

  return (
    <div className={classes.listItem} data-cy="organization-member-item">
      <div className={classes.itemText}>
        {user.name} ({user.username}){' '}
      </div>
      <div className={classes.itemActions}>
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
      </div>
    </div>
  );
};
