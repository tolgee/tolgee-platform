import { useHistory, useRouteMatch } from 'react-router-dom';
import { T, useTranslate } from '@tolgee/react';
import { Box, Button, Link, Paper, styled, Typography } from '@mui/material';

import { LINKS, PARAMS } from 'tg.constants/links';
import { messageService } from 'tg.service/MessageService';
import { tokenService } from 'tg.service/TokenService';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { useWindowTitle } from 'tg.hooks/useWindowTitle';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import { ApiError } from 'tg.service/http/ApiError';
import { useMessage } from 'tg.hooks/useSuccessMessage';

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

  let infoText: React.ReactNode = null;

  const username =
    invitationInfo.data.createdBy?.name ??
    invitationInfo.data.createdBy?.username;
  const project = invitationInfo.data.projectName;
  const organization = invitationInfo.data.organizationName;
  const params = {
    username,
    project,
    organization,
    b: <b />,
  };

  if (project) {
    if (username) {
      infoText = (
        <T
          keyName="accept_invitation_description_project_user"
          params={params}
        />
      );
    } else {
      infoText = (
        <T keyName="accept_invitation_description_project" params={params} />
      );
    }
  } else if (invitationInfo.data.createdBy) {
    infoText = (
      <T
        keyName="accept_invitation_description_organization_user"
        params={params}
      />
    );
  } else {
    infoText = (
      <T keyName="accept_invitation_description_organization" params={params} />
    );
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
              <Box textAlign="center" data-cy="accept-invitation-info-text">
                {infoText}
              </Box>
              <Box
                display="flex"
                gap={3}
                flexWrap="wrap"
                justifyContent="center"
              >
                <LoadingButton
                  loading={acceptCode.isLoading}
                  variant="contained"
                  color="primary"
                  onClick={handleAccept}
                  data-cy="accept-invitation-accept"
                >
                  {t('accept_invitation_accept')}
                </LoadingButton>
                <Button
                  variant="outlined"
                  onClick={handleDecline}
                  data-cy="accept-invitation-decline"
                >
                  {t('accept_invitation_decline')}
                </Button>
              </Box>
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
