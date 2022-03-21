import { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { container } from 'tsyringe';
import {
  makeStyles,
  IconButton,
  Menu,
  MenuItem,
  Tooltip,
} from '@material-ui/core';
import { MoreVert } from '@material-ui/icons';

import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { MessageService } from 'tg.service/MessageService';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useGlobalLoading } from 'tg.component/GlobalLoading';

const messaging = container.resolve(MessageService);

type OrganizationInvitationModel =
  components['schemas']['OrganizationInvitationModel'];

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
  permission: {
    display: 'flex',
    padding: '3px 8px',
    alignItems: 'center',
    justifyContent: 'center',
    background: theme.palette.lightBackground.main,
    height: 33,
    borderRadius: 3,
    cursor: 'default',
  },
  cancelButton: {
    color: theme.palette.error.dark,
  },
}));

type Props = {
  invitation: OrganizationInvitationModel;
};

export const InvitationItem: React.FC<Props> = ({ invitation }) => {
  const classes = useStyles();
  const [menuEl, setMenuEl] = useState<Element | null>(null);
  const t = useTranslate();

  const deleteInvitation = useApiMutation({
    url: '/v2/invitations/{invitationId}',
    method: 'delete',
    fetchOptions: { disableNotFoundHandling: true },
    invalidatePrefix: '/v2/organizations/{organizationId}/invitations',
  });

  const handleCancel = () => {
    deleteInvitation.mutate(
      { path: { invitationId: invitation.id } },
      {
        onError(e) {
          messaging.error(parseErrorResponse(e));
        },
      }
    );
  };

  const handleGetLink = () => {
    navigator.clipboard.writeText(
      LINKS.ACCEPT_INVITATION.buildWithOrigin({
        [PARAMS.INVITATION_CODE]: invitation.code,
      })
    );
    messaging.success(<T keyName="invite_user_invitation_copy_success" />);
    setMenuEl(null);
  };

  useGlobalLoading(deleteInvitation.isLoading);

  return (
    <div className={classes.listItem} data-cy="organization-invitation-item">
      <div className={classes.itemText}>
        {invitation.invitedUserName || invitation.invitedUserEmail}{' '}
      </div>
      <div className={classes.itemActions}>
        <Tooltip title={t(`organization_role_type_${invitation.type}_hint`)}>
          <div className={classes.permission}>
            <T keyName={`organization_role_type_${invitation.type}`} />
          </div>
        </Tooltip>

        <IconButton
          size="small"
          onClick={(e) => setMenuEl(e.currentTarget as Element)}
          aria-haspopup="true"
          aria-expanded={menuEl ? 'true' : undefined}
          data-cy="organization-invitation-item-menu"
        >
          <MoreVert />
        </IconButton>
        <Menu
          anchorEl={menuEl}
          open={Boolean(menuEl)}
          onClose={() => setMenuEl(null)}
          getContentAnchorEl={null}
          anchorOrigin={{
            vertical: 'bottom',
            horizontal: 'right',
          }}
          transformOrigin={{
            vertical: 'top',
            horizontal: 'right',
          }}
        >
          <MenuItem onClick={handleGetLink}>
            <T keyName="invite_user_invitation_copy_button" />
          </MenuItem>
          <MenuItem
            onClick={handleCancel}
            color="inherit"
            className={classes.cancelButton}
            data-cy="organization-invitation-cancel-button"
          >
            <T keyName="invite_user_invitation_cancel_button" />
          </MenuItem>
        </Menu>
      </div>
    </div>
  );
};
