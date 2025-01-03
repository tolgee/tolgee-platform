import { T, useTranslate } from '@tolgee/react';
import { Announcement } from './Announcement';
import { User01 } from '@untitled-ui/icons-react';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { Box, styled } from '@mui/material';
import { messageService } from 'tg.service/MessageService';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';

type Props = {
  code: string;
};

const StyledDismiss = styled('div')`
  color: ${({ theme }) => theme.palette.topBanner.linkText};
  text-decoration: underline;
  cursor: pointer;
`;

export const PendingInvitationBanner = ({ code }: Props) => {
  const { t } = useTranslate();
  const { setInvitationCode } = useGlobalActions();

  const invitationInfo = useApiQuery({
    url: '/api/public/invitation_info/{code}',
    method: 'get',
    path: {
      code,
    },
    fetchOptions: {
      disableAuthRedirect: true,
      disable404Redirect: true,
      disableAutoErrorHandle: true,
      disableErrorNotification: true,
    },
    options: {
      onError(e) {
        if (e.code === 'invitation_code_does_not_exist_or_expired') {
          messageService.error(<TranslatedError code={e.code} />);
          setInvitationCode(undefined);
        }
      },
    },
  });

  let infoText: React.ReactNode = null;

  const project = invitationInfo.data?.projectName;
  const organization = invitationInfo.data?.organizationName;

  if (project) {
    infoText = (
      <T
        keyName="pending_invitation_description_project"
        params={{
          project,
          b: <b />,
        }}
      />
    );
  } else if (organization) {
    infoText = (
      <T
        keyName="pending_invitation_description_organization"
        params={{
          organization,
          b: <b />,
        }}
      />
    );
  } else {
    infoText = <T keyName="pending_invitation_description_generic" />;
  }

  function handleDecline() {
    setInvitationCode(undefined);
  }

  return (
    <Announcement
      content={
        <Box sx={{ fontWeight: 'normal' }} data-cy="pending-invitation-banner">
          {infoText}
        </Box>
      }
      icon={<User01 />}
      action={
        <StyledDismiss
          role="button"
          tabIndex={0}
          sx={{ marginLeft: 1 }}
          onClick={handleDecline}
          data-cy="pending-invitation-dismiss"
        >
          {t('pending_invitation_decline')}
        </StyledDismiss>
      }
    />
  );
};
