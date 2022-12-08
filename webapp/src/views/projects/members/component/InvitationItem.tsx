import { useCallback } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { container } from 'tsyringe';
import { IconButton, styled, Tooltip } from '@mui/material';
import { Link, Clear } from '@mui/icons-material';

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

const StyledPermissions = styled('div')`
  display: flex;
  padding: 3px 8px;
  align-items: center;
  justify-content: center;
  background: ${({ theme }) => theme.palette.emphasis[100]};
  height: 33px;
  border-radius: 3px;
  cursor: default;
`;

type Props = {
  invitation: UserAccountInProjectModel;
};

export const InvitationItem: React.FC<Props> = ({ invitation }) => {
  const { t } = useTranslate();
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
    <StyledListItem>
      <StyledItemText>
        {invitation.invitedUserName || invitation.invitedUserEmail}{' '}
      </StyledItemText>
      <StyledItemActions>
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
          <StyledPermissions>
            <T
              keyName={`permission_type_${
                projectPermissionTypes[invitation.type!]
              }`}
            />
          </StyledPermissions>
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
      </StyledItemActions>
    </StyledListItem>
  );
};
