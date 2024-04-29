import { FunctionComponent } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Redirect } from 'react-router-dom';
import { styled, useMediaQuery } from '@mui/material';

import { LINKS } from 'tg.constants/links';
import { useConfig } from 'tg.globalContext/helpers';
import { CompactView } from 'tg.component/layout/CompactView';

import { Alert } from '../../common/Alert';
import { DashboardPage } from '../../layout/DashboardPage';
import { SignUpForm } from './SignUpForm';
import { SignUpProviders } from './SignUpProviders';
import { useRecaptcha } from './useRecaptcha';
import { SPLIT_CONTENT_BREAK_POINT, SplitContent } from '../SplitContent';
import { useReportOnce } from 'tg.hooks/useReportEvent';
import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';

export type SignUpType = {
  name: string;
  email: string;
  password: string;
  organizationName: string;
  invitationCode?: string;
};

const StyledRightPart = styled('div')`
  display: grid;
  margin-top: 63px;

  @media ${SPLIT_CONTENT_BREAK_POINT} {
    margin-top: 0px;
  }
`;

export const SignUpView: FunctionComponent = () => {
  const config = useConfig();
  const registrationAllowed = useGlobalContext(
    (c) =>
      c.initialData.serverConfiguration.allowRegistrations ||
      c.auth.allowRegistration
  );
  const { signUp } = useGlobalActions();
  const { t } = useTranslate();

  const isSmall = useMediaQuery(SPLIT_CONTENT_BREAK_POINT);

  const getRecaptchaToken = useRecaptcha();

  const signUpMutation = useGlobalContext((c) => c.auth.signupLoadable);

  useReportOnce('SIGN_UP_PAGE_OPENED');

  const onSubmit = async (data: SignUpType) => {
    signUp({ ...data, recaptchaToken: await getRecaptchaToken() });
  };

  if (!registrationAllowed) {
    return <Redirect to={LINKS.LOGIN.build()} />;
  }

  return (
    <>
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
      </DashboardPage>
      {config.capterraTracker && <img src={config.capterraTracker} />}
    </>
  );
};

export default SignUpView;
