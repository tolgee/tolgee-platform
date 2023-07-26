import { FunctionComponent } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { useSelector } from 'react-redux';
import { Redirect } from 'react-router-dom';
import { styled, useMediaQuery } from '@mui/material';

import { LINKS } from 'tg.constants/links';
import { useConfig } from 'tg.globalContext/helpers';
import { AppState } from 'tg.store/index';
import { CompactView } from 'tg.component/layout/CompactView';

import { Alert } from '../../common/Alert';
import { DashboardPage } from '../../layout/DashboardPage';
import { SignUpForm } from './SignUpForm';
import { SignUpProviders } from './SignUpProviders';
import { InvitationCodeService } from 'tg.service/InvitationCodeService';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { TokenService } from 'tg.service/TokenService';
import { container } from 'tsyringe';
import { GlobalActions } from 'tg.store/global/GlobalActions';
import { useRecaptcha } from './useRecaptcha';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { SPLIT_CONTENT_BREAK_POINT, SplitContent } from '../SplitContent';
import { useReportOnce } from 'tg.hooks/useReportEvent';

export type SignUpType = {
  name: string;
  email: string;
  password: string;
  passwordRepeat?: string;
  organizationName: string;
  invitationCode?: string;
};

const tokenService = container.resolve(TokenService);
const globalActions = container.resolve(GlobalActions);

const StyledRightPart = styled('div')`
  display: grid;
  margin-top: 63px;

  @media ${SPLIT_CONTENT_BREAK_POINT} {
    margin-top: 0px;
  }
`;

export const SignUpView: FunctionComponent = () => {
  const security = useSelector((state: AppState) => state.global.security);
  const config = useConfig();
  const remoteConfig = useConfig();
  const { t } = useTranslate();

  const isSmall = useMediaQuery(SPLIT_CONTENT_BREAK_POINT);

  const message = useMessage();
  const getRecaptchaToken = useRecaptcha();

  const signUpMutation = useApiMutation({
    url: `/api/public/sign_up`,
    method: 'post',
    fetchOptions: {
      disableBadRequestHandling: true,
    },
  });

  useReportOnce('SIGN_UP_PAGE_OPENED');

  const onSubmit = async (data: SignUpType) => {
    const request = {
      ...data,
      invitationCode: InvitationCodeService.getCode(),
      recaptchaToken: await getRecaptchaToken(),
    } as SignUpType;

    delete request.passwordRepeat;

    signUpMutation.mutate(
      { content: { 'application/json': request } },
      {
        onSuccess: (data) => {
          if (data.accessToken) {
            message.success(<T keyName="sign_up_success_message" />);
            InvitationCodeService.disposeCode();
            tokenService.setToken(data.accessToken);
            globalActions.setJWTToken.dispatch(data.accessToken);
          }
        },
        onError: (error) => {
          if (error.code === 'invitation_code_does_not_exist_or_expired') {
            InvitationCodeService.disposeCode();
          }
        },
      }
    );
  };

  const registrationsAllowed =
    remoteConfig.allowRegistrations || security.allowRegistration;

  if (
    !remoteConfig.authentication ||
    !registrationsAllowed ||
    security.allowPrivate
  ) {
    return <Redirect to={LINKS.AFTER_LOGIN.build()} />;
  }

  return (
    <DashboardPage>
      <CompactView
        maxWidth={isSmall ? 430 : 964}
        windowTitle={t('sign_up_title')}
        title={t('sign_up_title')}
        backLink={LINKS.LOGIN.build()}
        content={
          signUpMutation.isSuccess && config.needsEmailVerification ? (
            <Alert severity="success">
              <T keyName="sign_up_success_needs_verification_message" />
            </Alert>
          ) : (
            <SplitContent
              left={
                <SignUpForm onSubmit={onSubmit} loadable={signUpMutation} />
              }
              right={
                <StyledRightPart>
                  <SignUpProviders />
                </StyledRightPart>
              }
            />
          )
        }
      />
      {config.capterraTracker && <img src={config.capterraTracker} />}
    </DashboardPage>
  );
};

export default SignUpView;
