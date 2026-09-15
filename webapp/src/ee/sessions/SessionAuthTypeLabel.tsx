import { ReactElement } from 'react';
import { T } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';

type UserSessionModel = components['schemas']['UserSessionModel'];
type UserSessionType = UserSessionModel['type'];

const LABELS: Record<UserSessionType, ReactElement> = {
  LOGIN_NATIVE: (
    <T keyName="session-auth-type-login_native" defaultValue="Password login" />
  ),
  LOGIN_GITHUB: (
    <T keyName="session-auth-type-login_github" defaultValue="GitHub login" />
  ),
  LOGIN_GOOGLE: (
    <T keyName="session-auth-type-login_google" defaultValue="Google login" />
  ),
  LOGIN_OAUTH2: (
    <T keyName="session-auth-type-login_oauth2" defaultValue="OAuth2 login" />
  ),
  LOGIN_SSO: (
    <T keyName="session-auth-type-login_sso" defaultValue="SSO login" />
  ),
  SIGN_UP: <T keyName="session-auth-type-sign_up" defaultValue="Sign up" />,
  EMAIL_VERIFICATION: (
    <T
      keyName="session-auth-type-email_verification"
      defaultValue="Email verification"
    />
  ),
  IMPERSONATION: (
    <T keyName="session-auth-type-impersonation" defaultValue="Impersonation" />
  ),
  TEST: <T keyName="session-auth-type-test" defaultValue="Test" />,
  UNKNOWN: (
    <T keyName="session-auth-type-unknown" defaultValue="Unknown origin" />
  ),
};

export function SessionAuthTypeLabel({ type }: { type: UserSessionType }) {
  return LABELS[type] ?? LABELS.UNKNOWN;
}
