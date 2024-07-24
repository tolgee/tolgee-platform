import { FunctionComponent } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Link, Redirect } from 'react-router-dom';
import { Link as MuiLink, styled, useMediaQuery } from '@mui/material';

import { LINKS } from 'tg.constants/links';
import { useConfig } from 'tg.globalContext/helpers';
import {
  CompactView,
  SPLIT_CONTENT_BREAK_POINT,
} from 'tg.component/layout/CompactView';
import { DashboardPage } from '../../layout/DashboardPage';
import { SignUpForm } from './SignUpForm';
import { SignUpProviders } from './SignUpProviders';
import { useRecaptcha } from './useRecaptcha';
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
  align-content: end;
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
          maxWidth={isSmall ? 550 : 964}
          windowTitle={t('sign_up_title')}
          title={t('sign_up_title')}
          subtitle={
            <T
              keyName="sign_up_subtitle"
              params={{
                link: <MuiLink to={LINKS.LOGIN.build()} component={Link} />,
              }}
            />
          }
          primaryContent={
            <SignUpForm onSubmit={onSubmit} loadable={signUpMutation} />
          }
          secondaryContent={
            <StyledRightPart>
              <SignUpProviders />
            </StyledRightPart>
          }
        />
      </DashboardPage>
      {config.capterraTracker && (
        <img style={{ height: 0 }} src={config.capterraTracker} />
      )}
    </>
  );
};

export default SignUpView;
