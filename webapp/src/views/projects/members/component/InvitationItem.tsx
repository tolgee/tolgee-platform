import { useCallback } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { container } from 'tsyringe';
import { IconButton, makeStyles, Tooltip } from '@material-ui/core';
import { Link, Clear } from '@material-ui/icons';

import { components } from 'tg.service/apiSchema.generated';
import { LanguagesPermittedList } from 'tg.component/languages/LanguagesPermittedList';
import { projectPermissionTypes } from 'tg.constants/projectPermissionTypes';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { MessageService } from 'tg.service/MessageService';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';

const messaging = container.resolve(MessageService);

type UserAccountInProjectModel =
  components['schemas']['ProjectInvitationModel'];

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
    background: theme.palette.extraLightBackground.main,
    height: 33,
    borderRadius: 3,
    cursor: 'default',
  },
  cancelButton: {
    color: theme.palette.error.dark,
  },
}));

type Props = {
  invitation: UserAccountInProjectModel;
};

export const InvitationItem: React.FC<Props> = ({ invitation }) => {
  const classes = useStyles();
  const t = useTranslate();
  const languages = useProjectLanguages();

  const findLanguage = useCallback(
    (languageId: number) => {
      const result = languages.find((language) => language.id === languageId);
      return result!;
    },
    [languages]
  );

  const deleteInvitation = useApiMutation({
    url: '/v2/invitations/{invitationId}',
    method: 'delete',
    fetchOptions: { disableNotFoundHandling: true },
    invalidatePrefix: '/v2/projects/{projectId}/invitations',
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
  };

  const permission = invitation.type;

  useGlobalLoading(deleteInvitation.isLoading);

  return (
    <div className={classes.listItem}>
      <div className={classes.itemText}>
        {invitation.invitedUserName || invitation.invitedUserEmail}{' '}
      </div>
      <div className={classes.itemActions}>
        {permission === 'TRANSLATE' && (
          <Tooltip
            title={t('permission_languages_hint', {
              subject: invitation.permittedLanguageIds?.length
                ? invitation.permittedLanguageIds
                    .map((l) => findLanguage(l).name)
                    .join(', ')
                : t('languages_permitted_list_all'),
            })}
          >
            <span>
              <LanguagesPermittedList
                languages={invitation.permittedLanguageIds?.map(
                  (permittedLanguageId) => findLanguage(permittedLanguageId)
                )}
              />
            </span>
          </Tooltip>
        )}
        <Tooltip
          title={t(
            `permission_type_${projectPermissionTypes[invitation.type!]}_hint`
          )}
        >
          <div className={classes.permission}>
            <T
              keyName={`permission_type_${
                projectPermissionTypes[invitation.type!]
              }`}
            />
          </div>
        </Tooltip>

        <Tooltip title={t('invite_user_invitation_copy_button')}>
          <IconButton size="small" onClick={handleGetLink}>
            <Link />
          </IconButton>
        </Tooltip>

        <Tooltip title={t('invite_user_invitation_cancel_button')}>
          <IconButton size="small" onClick={handleCancel}>
            <Clear />
          </IconButton>
        </Tooltip>
      </div>
    </div>
  );
};
