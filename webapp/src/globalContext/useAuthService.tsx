import { useMemo, useState } from 'react';
import { T } from '@tolgee/react';
import { useHistory } from 'react-router-dom';
import { v4 as uuidv4 } from 'uuid';

import { securityService } from 'tg.service/SecurityService';
import {
  ADMIN_JWT_LOCAL_STORAGE_KEY,
  tokenService,
} from 'tg.service/TokenService';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useInitialDataService } from './useInitialDataService';
import { LINKS, PARAMS } from 'tg.constants/links';
import { messageService } from 'tg.service/MessageService';
import { useLocalStorageState } from 'tg.hooks/useLocalStorageState';

type LoginRequest = components['schemas']['LoginRequest'];
type JwtAuthenticationResponse =
  components['schemas']['JwtAuthenticationResponse'];
type SignUpDto = components['schemas']['SignUpDto'];
type SuperTokenAction = { onCancel: () => void; onSuccess: () => void };

export const INVITATION_CODE_STORAGE_KEY = 'invitationCode';

const LOCAL_STORAGE_OAUTH_STATE_KEY = 'oauth2State';
const LOCAL_STORAGE_SSO_STATE_KEY = 'ssoState';
const LOCAL_STORAGE_DOMAIN_KEY = 'ssoDomain';

export function getRedirectUrl(userId?: number) {
  const link = securityService.getAfterLoginLink();
  if (link?.url && (link.userId === undefined || link.userId === userId)) {
    // apply afterLogin redirect (check if the user is same as last time)
    return link.url;
  } else {
    return LINKS.AFTER_LOGIN.build();
  }
}

export const useAuthService = (
  initialData: ReturnType<typeof useInitialDataService>
) => {
  const loginLoadable = useApiMutation({
    url: '/api/public/generatetoken',
    method: 'post',
    fetchOptions: {
      disableAuthRedirect: true,
      disableErrorNotification: true,
      disable404Redirect: true,
      disableAutoErrorHandle: true,
    },
  });

  const authorizeOAuthLoadable = useApiMutation({
    url: '/api/public/authorize_oauth/{serviceType}',
    method: 'get',
    fetchOptions: {
      disableAuthRedirect: true,
      disableErrorNotification: true,
      disable404Redirect: true,
      disableAutoErrorHandle: true,
    },
  });

  const redirectSsoUrlLoadable = useApiMutation({
    url: '/api/public/authorize_oauth/sso/authentication-url',
    method: 'post',
    fetchOptions: {
      disableAuthRedirect: true,
      disableErrorNotification: true,
      disable404Redirect: true,
      disableAutoErrorHandle: true,
    },
  });

  const acceptInvitationLoadable = useApiMutation({
    url: '/v2/invitations/{code}/accept',
    method: 'put',
  });

  const signupLoadable = useApiMutation({
    url: `/api/public/sign_up`,
    method: 'post',
    fetchOptions: {
      disableAutoErrorHandle: true,
      disableErrorNotification: true,
      disable404Redirect: true,
    },
  });

  const [jwtToken, _setJwtToken] = useState<string | undefined>(
    tokenService.getToken()
  );

  const [adminToken, setAdminToken] = useLocalStorageState({
    initial: undefined,
    key: ADMIN_JWT_LOCAL_STORAGE_KEY,
  });

  const [superTokenAfter, setSuperTokenAfter] = useState<SuperTokenAction[]>(
    []
  );
  const [userId, setUserId] = useState<number>();
  const [invitationCode, _setInvitationCode, getInvitationCode] =
    useLocalStorageState({
      initial: undefined,
      key: INVITATION_CODE_STORAGE_KEY,
    });

  const [allowRegistration, setAllowRegistration] = useState(
    Boolean(invitationCode)
  );

  function setInvitationCode(code: string | undefined) {
    _setInvitationCode(code);
    if (code) {
      setAllowRegistration(true);
    }
  }

  const history = useHistory();

  async function getSsoAuthLinkByDomain(domain: string) {
    localStorage.setItem(LOCAL_STORAGE_DOMAIN_KEY, domain);
    const state = generateSsoStateKey();
    return await redirectSsoUrlLoadable.mutateAsync({
      content: { 'application/json': { domain, state } },
    });
  }

  function useSsoAuthLinkByDomain(domain: string) {
    const state = useMemo(() => generateSsoStateKey(), []);
    localStorage.setItem(LOCAL_STORAGE_DOMAIN_KEY, domain);
    return useApiQuery({
      url: '/api/public/authorize_oauth/sso/authentication-url',
      method: 'post',
      content: { 'application/json': { domain, state } },
      fetchOptions: {
        disableAutoErrorHandle: true,
        disableErrorNotification: true,
        disable404Redirect: true,
      },
    });
  }

  function getLastSsoDomain() {
    return localStorage.getItem(LOCAL_STORAGE_DOMAIN_KEY) ?? undefined;
  }

  function generateStateKey(key: string): string {
    const state = uuidv4();
    localStorage.setItem(key, state);
    return state;
  }

  function generateOAuthStateKey(): string {
    return generateStateKey(LOCAL_STORAGE_OAUTH_STATE_KEY);
  }

  function getOAuthStateKey(): string | undefined {
    return localStorage.getItem(LOCAL_STORAGE_OAUTH_STATE_KEY) ?? undefined;
  }

  function clearOAuthStateKey() {
    localStorage.removeItem(LOCAL_STORAGE_OAUTH_STATE_KEY);
  }

  function generateSsoStateKey(): string {
    return generateStateKey(LOCAL_STORAGE_SSO_STATE_KEY);
  }

  function getSsoStateKey(): string | undefined {
    return localStorage.getItem(LOCAL_STORAGE_SSO_STATE_KEY) ?? undefined;
  }

  function clearSsoStateKey() {
    localStorage.removeItem(LOCAL_STORAGE_SSO_STATE_KEY);
  }

  async function setJwtToken(token: string | undefined) {
    _setJwtToken(token);
    if (token) {
      tokenService.setToken(token);
    } else {
      tokenService.disposeToken();
    }
    if (Boolean(token) !== Boolean(jwtToken)) {
      return initialData.actions.invalidateInitialData();
    } else {
      return initialData.actions.refetchInitialData();
    }
  }

  async function handleAcceptInvitation() {
    // use code directly from localstorage
    // react state might be outdated, but we don't want to wait for next render
    const code = getInvitationCode();
    if (code) {
      try {
        await acceptInvitationLoadable.mutateAsync({
          path: { code },
        });
      } catch (error: any) {
        // we want to continue regardless, error will be logged
      }

      setInvitationCode(undefined);
    }
  }

  async function handleAfterLogin({ accessToken }: JwtAuthenticationResponse) {
    // don't re-render the page yet
    tokenService.setToken(accessToken!);
    await handleAcceptInvitation();
    // now set the JWT, when we are already redirecting
    setUserId(tokenService.getUserId(accessToken));
    setJwtToken(accessToken!);
  }

  const state = {
    allowPrivate: Boolean(
      jwtToken || !initialData.state?.serverConfiguration.authentication
    ),
    jwtToken,
    adminToken,
    superTokenNeeded: superTokenAfter.length > 0,
    loginLoadable,
    signupLoadable,
    authorizeOAuthLoadable,
    redirectSsoUrlLoadable,
    allowRegistration,
    invitationCode,
  };

  async function loginRedirectSso(domain: string) {
    const response = await getSsoAuthLinkByDomain(domain);
    window.location.href = response.redirectUrl;
  }

  const actions = {
    async login(credentials: LoginRequest) {
      const response = await loginLoadable.mutateAsync(
        {
          content: { 'application/json': credentials },
        },
        {
          onError: (error) => {
            if (error.code === 'third_party_switch_initiated') {
              history.replace(LINKS.ACCEPT_AUTH_PROVIDER_CHANGE.build());
            }
            if (error.code === 'use_sso_for_authentication_instead') {
              loginRedirectSso(error.params?.[0]);
            }
          },
        }
      );
      response.accessToken;
      await handleAfterLogin(response!);
    },
    async loginWithOAuthCode(type: string, code: string, domain?: string) {
      const redirectUri = LINKS.OAUTH_RESPONSE.buildWithOrigin({
        [PARAMS.SERVICE_TYPE]: type,
      });
      const response = await authorizeOAuthLoadable.mutateAsync(
        {
          path: { serviceType: type },
          query: {
            code,
            redirect_uri: redirectUri,
            invitationCode: getInvitationCode(),
            domain,
          },
        },
        {
          onSuccess: (response) => {
            history.replace(LINKS.AFTER_LOGIN.build());
          },
          onError: (error) => {
            if (error.code === 'third_party_switch_initiated') {
              history.replace(LINKS.ACCEPT_AUTH_PROVIDER_CHANGE.build());
            }
            if (error.code === 'invitation_code_does_not_exist_or_expired') {
              setInvitationCode(undefined);
            }
            if (error.code === 'use_sso_for_authentication_instead') {
              loginRedirectSso(error.params?.[0]);
            }
          },
        }
      );
      setInvitationCode(undefined);
      await handleAfterLogin(response!);
    },
    loginRedirectSso,
    getSsoAuthLinkByDomain,
    useSsoAuthLinkByDomain,
    generateSsoStateKey,
    getSsoStateKey,
    clearSsoStateKey,
    generateOAuthStateKey,
    getOAuthStateKey,
    clearOAuthStateKey,
    getLastSsoDomain,
    async signUp(data: Omit<SignUpDto, 'invitationCode'>) {
      signupLoadable.mutate(
        {
          content: {
            'application/json': {
              ...data,
              invitationCode: getInvitationCode(),
            },
          },
        },
        {
          onError: (error) => {
            if (error.code === 'invitation_code_does_not_exist_or_expired') {
              setInvitationCode(undefined);
            } else {
              error.handleError?.();
            }
          },
          onSuccess(data) {
            setInvitationCode(undefined);
            handleAfterLogin(data);
            messageService.success(<T keyName="sign_up_success_message" />);
          },
        }
      );
    },
    handleAfterLogin,
    redirectAfterLogin() {
      const link = getRedirectUrl(userId);
      history.replace(link);
      securityService.removeAfterLoginLink();
    },
    saveAfterLoginLink(url: string) {
      securityService.saveAfterLoginLink({ url, userId });
    },
    logout() {
      return setJwtToken(undefined);
    },
    waitForSuperToken(afterAction: SuperTokenAction) {
      setSuperTokenAfter((value) => {
        const existing = value || [];
        return [...existing, afterAction];
      });
    },
    superTokenRequestCancel() {
      superTokenAfter?.forEach(({ onCancel }) => onCancel());
      setSuperTokenAfter([]);
    },
    async superTokenRequestSuccess(token: string) {
      setJwtToken(token);
      superTokenAfter?.forEach(({ onSuccess }) => onSuccess());
      setSuperTokenAfter([]);
    },
    debugCustomerAccount(customerJwt: string) {
      setJwtToken(customerJwt);
      setAdminToken(jwtToken);
    },
    exitDebugCustomerAccount() {
      if (adminToken === undefined) {
        return;
      }
      setJwtToken(adminToken);
      setAdminToken(undefined);
    },
    exitDebugCustomerAccountOrLogout() {
      setJwtToken(adminToken);
      setAdminToken(undefined);
    },
    setInvitationCode,
    currentLocation() {
      return history.location.pathname + history.location.search;
    },
    redirectTo(url: string) {
      history.replace(url);
    },
  };

  return { state, actions };
};
