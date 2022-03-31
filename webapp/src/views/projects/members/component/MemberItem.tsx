import { T, useTranslate } from '@tolgee/react';
import { container } from 'tsyringe';
import { Chip, makeStyles } from '@material-ui/core';

import { PermissionsMenu } from 'tg.component/security/PermissionsMenu';
import { LanguagePermissionsMenu } from 'tg.component/security/LanguagePermissionsMenu';
import { confirmation } from 'tg.hooks/confirmation';
import { useProject } from 'tg.hooks/useProject';
import { useUser } from 'tg.hooks/useUser';
import { MessageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import RevokePermissionsButton from './RevokePermissionsButton';

type UserAccountInProjectModel =
  components['schemas']['UserAccountInProjectModel'];

const messageService = container.resolve(MessageService);

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
  user: UserAccountInProjectModel;
};

export const MemberItem: React.FC<Props> = ({ user }) => {
  const project = useProject();
  const currentUser = useUser();
  const classes = useStyles();
  const t = useTranslate();

  const editPermission = useApiMutation({
    url: '/v2/projects/{projectId}/users/{userId}/set-permissions/{permissionType}',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/users',
  });

  const changePermission = (permissionType, languages, showMessage) => {
    editPermission.mutate(
      {
        path: {
          userId: user?.id,
          permissionType,
          projectId: project.id,
        },
        query: {
          languages: permissionType === 'TRANSLATE' ? languages : undefined,
        },
      },
      {
        onSuccess() {
          if (showMessage) {
            messageService.success(<T>permissions_set_message</T>);
          }
        },
        onError(e) {
          parseErrorResponse(e).forEach((err) =>
            messageService.error(<T>{err}</T>)
          );
        },
      }
    );
  };

  const changePermissionConfirm = (permissionType, languages) => {
    confirmation({
      message: <T>change_permissions_confirmation</T>,
      onConfirm: () => changePermission(permissionType, languages, true),
    });
  };

  const allLanguages = useProjectLanguages();
  const allLangIds = allLanguages.map((l) => l.id);
  const projectPermissionType = user.computedPermissions.type;
  const isCurrentUser = currentUser?.id === user.id;
  const isOwner = user.organizationRole === 'OWNER';

  return (
    <div className={classes.listItem} data-cy="project-member-item">
      <div className={classes.itemText}>
        {user.name} ({user.username}){' '}
        {user.organizationRole && (
          <Chip size="small" label={project.organizationOwnerName} />
        )}
      </div>
      <div className={classes.itemActions}>
        {projectPermissionType === 'TRANSLATE' && (
          <LanguagePermissionsMenu
            selected={user.computedPermissions.permittedLanguageIds || []}
            onSelect={(langs) =>
              changePermission(projectPermissionType, langs, false)
            }
          />
        )}
        <PermissionsMenu
          title={
            isOwner && !isCurrentUser
              ? t('user_is_owner_of_organization_tooltip')
              : isOwner
              ? t('cannot_change_your_own_access_tooltip')
              : undefined
          }
          selected={user.computedPermissions.type!}
          onSelect={(permission) =>
            changePermissionConfirm(permission, allLangIds)
          }
          buttonProps={{
            size: 'small',
            disabled: isCurrentUser || isOwner,
          }}
          minPermissions={user.organizationBasePermissions}
        />
        <RevokePermissionsButton user={user} />
      </div>
    </div>
  );
};
