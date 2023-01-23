import { FunctionComponent, useCallback } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { useSelector } from 'react-redux';
import { Redirect } from 'react-router-dom';
import { container } from 'tsyringe';
import { useGoogleReCaptcha } from 'react-google-recaptcha-v3';
import { styled, useMediaQuery } from '@mui/material';

import { LINKS } from 'tg.constants/links';
import { useConfig } from 'tg.globalContext/helpers';
import { SignUpActions } from 'tg.store/global/SignUpActions';
import { AppState } from 'tg.store/index';
import { CompactView } from 'tg.component/layout/CompactView';

import { Alert } from '../../common/Alert';
import { DashboardPage } from '../../layout/DashboardPage';
import { SignUpForm } from './SignUpForm';
import { SignUpProviders } from './SignUpProviders';

const actions = container.resolve(SignUpActions);

const BREAK_POINT = '(max-width: 800px)';

const StyledGrid = styled('div')`
  display: grid;
  grid-template-columns: 1fr 1px 1fr;
  gap: 32px 48px;

  @media ${BREAK_POINT} {
    grid-template-columns: 1fr;
    grid-template-rows: auto 1px auto;
  }
`;

const StyledProviders = styled('div')`
  display: grid;
  margin-top: 63px;

  @media ${BREAK_POINT} {
    margin-top: 0px;
  }
`;

const StyledSpacer = styled('div')`
  display: grid;
  background: lightgrey;
  @media ${BREAK_POINT} {
    margin: 0px -8px;
  }
`;

export type SignUpType = {
  name: string;
  email: string;
  password: string;
  passwordRepeat?: string;
  organizationName: string;
  invitationCode?: string;
};

const SignUpView: FunctionComponent = () => {
  const security = useSelector((state: AppState) => state.global.security);
  const state = useSelector((state: AppState) => state.signUp.loadables.signUp);
  const config = useConfig();
  const remoteConfig = useConfig();
  const { t } = useTranslate();

  const isSmall = useMediaQuery(BREAK_POINT);

  const WithRecaptcha = () => {
    const { executeRecaptcha } = useGoogleReCaptcha();

    const handleReCaptchaVerify = useCallback(async () => {
      if (!executeRecaptcha) {
        throw Error('Execute recaptcha not yet available');
      }

      return await executeRecaptcha('sign_up');
    }, [executeRecaptcha]);

    return (
      <View
        onSubmit={async (v: SignUpType) => {
          const recaptchaToken = await handleReCaptchaVerify();
          actions.loadableActions.signUp.dispatch({
            ...v,
            recaptchaToken: recaptchaToken,
          });
        }}
      />
    );
  };

  const WithoutRecaptcha = () => {
    return (
      <View
        onSubmit={async (v: SignUpType) => {
          actions.loadableActions.signUp.dispatch({
            ...v,
          });
        }}
      />
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

  const View = (props: { onSubmit: (v) => void }) => (
    <DashboardPage>
      <CompactView
        maxWidth={isSmall ? 430 : 900}
        windowTitle={t('sign_up_title')}
        title={t('sign_up_title')}
        backLink={LINKS.LOGIN.build()}
        content={
          state.loaded && config.needsEmailVerification ? (
            <Alert severity="success">
              <T>sign_up_success_needs_verification_message</T>
            </Alert>
          ) : (
            <StyledGrid>
              <div>
                <SignUpForm onSubmit={props.onSubmit} />
              </div>

              <StyledSpacer />

              <StyledProviders>
                <SignUpProviders />
              </StyledProviders>
            </StyledGrid>
          )
        }
      />
      {config.capterraTracker && <img src={config.capterraTracker} />}
    </DashboardPage>
  );

  return config.recaptchaSiteKey ? <WithRecaptcha /> : <WithoutRecaptcha />;
};

export default SignUpView;
