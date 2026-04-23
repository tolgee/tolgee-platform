import { useHistory, useRouteMatch } from 'react-router-dom';
import { T, useTranslate } from '@tolgee/react';
import { Box, Link, Paper, styled, Typography } from '@mui/material';

import { LINKS, PARAMS } from 'tg.constants/links';
import { messageService } from 'tg.service/MessageService';
import { tokenService } from 'tg.service/TokenService';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { useWindowTitle } from 'tg.hooks/useWindowTitle';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import { ApiError } from 'tg.service/http/ApiError';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { InvitationActions } from './InvitationActions';

export const FULL_PAGE_BREAK_POINT = '(max-width: 700px)';

const StyledContainer = styled(Box)`
  display: grid;
  justify-content: center;
  align-content: center;
`;

const StyledContent = styled(Box)`
  display: grid;
  width: min(100vw, 660px);
  gap: 32px;
`;

const StyledPaper = styled(Paper)`
  padding: 60px;
  display: grid;
  gap: 32px;
  background: ${({ theme }) => theme.palette.tokens.background['paper-1']};
  @media ${FULL_PAGE_BREAK_POINT} {
    padding: 10px;
    box-shadow: none;
    background: transparent;
  }
`;

const AcceptInvitationView: React.FC = () => {
  const history = useHistory();
  const match = useRouteMatch();
  const { t } = useTranslate();
  const message = useMessage();

  useWindowTitle(t('accept_invitation_title'));

  const code = match.params[PARAMS.INVITATION_CODE];
  const { refetchInitialData, setInvitationCode } = useGlobalActions();

  const acceptCode = useApiMutation({
    url: '/v2/invitations/{code}/accept',
    method: 'put',
    fetchOptions: {
      disableErrorNotification: true,
    },
    options: {
      onError(e: ApiError) {
        if (e.code == 'invitation_email_mismatch') {
          message.error(
            <span data-cy="invitation_email_mismatch_message">
              <T keyName="invitation_email_mismatch" />
            </span>
          );
          return;
        }
        if (e.code == 'plan_seat_limit_exceeded') {
          message.error(
            <span data-cy="plan_seat_limit_exceeded_while_accepting_invitation_message">
              <T keyName="plan_seat_limit_exceeded_while_accepting_invitation_message" />
            </span>
          );
          return;
        }
        if (e.code == 'seats_spending_limit_exceeded') {
          message.error(
            <span data-cy="seat_spending_limit_exceeded_while_accepting_invitation_message">
              <T keyName="seat_spending_limit_exceeded_while_accepting_invitation_message" />
            </span>
          );
        }
      },
    },
  });

  const invitationInfo = useApiQuery({
    url: '/api/public/invitation_info/{code}',
    method: 'get',
    path: {
      code,
    },
    options: {
      onError(e) {
        history.replace(LINKS.ROOT.build());
        if (e.code) {
          messageService.error(<TranslatedError code={e.code} />);
        }
      },
    },
  });

  function handleAccept() {
    if (!tokenService.getToken()) {
      setInvitationCode(code);
      history.push(LINKS.LOGIN.build());
    } else {
      acceptCode.mutate(
        { path: { code } },
        {
          onSuccess() {
            refetchInitialData();
            messageService.success(
              <span data-cy="invitation-accepted-success-message">
                <T keyName="invitation_code_accepted" />
              </span>
            );
          },
          onSettled() {
            history.replace(LINKS.PROJECTS.build());
          },
        }
      );
    }
  }

  function handleDecline() {
    setInvitationCode(undefined);
    history.push(LINKS.LOGIN.build());
  }

  if (!invitationInfo.data) {
    return <FullPageLoading />;
  }

  return (
    <DashboardPage hideQuickStart>
      <StyledContainer>
        <StyledContent>
          <StyledPaper>
            <Typography variant="h3" sx={{ textAlign: 'center' }}>
              {invitationInfo.data.projectName
                ? t('accept_invitation_project_title')
                : t('accept_invitation_organization_title')}
            </Typography>

            {invitationInfo.data.createdBy && (
              <Box display="flex" justifyContent="center">
                <AvatarImg
                  owner={{
                    ...invitationInfo.data.createdBy,
                    type: 'USER',
                  }}
                  size={80}
                />
              </Box>
            )}

            <Box display="grid" gap="24px" justifyItems="center">
              <InvitationActions
                data={invitationInfo.data}
                isLoading={acceptCode.isLoading}
                onAccept={handleAccept}
                onDecline={handleDecline}
              />
            </Box>
          </StyledPaper>
          <Box display="flex" justifyContent="center">
            <Link href="https://tolgee.io">
              {t('accept_invitation_learn_more')}
            </Link>
          </Box>
        </StyledContent>
      </StyledContainer>
    </DashboardPage>
  );
};
export default AcceptInvitationView;
