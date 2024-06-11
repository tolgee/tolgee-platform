import { useState } from 'react';
import { T } from '@tolgee/react';
import { useHistory } from 'react-router-dom';

import { securityService } from 'tg.service/SecurityService';
import {
  ADMIN_JWT_LOCAL_STORAGE_KEY,
  tokenService,
} from 'tg.service/TokenService';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useInitialDataService } from './useInitialDataService';
import { LINKS, PARAMS } from 'tg.constants/links';
import {
  INVITATION_CODE_STORAGE_KEY,
  InvitationCodeService,
} from 'tg.service/InvitationCodeService';
import { messageService } from 'tg.service/MessageService';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import { useLocalStorageState } from 'tg.hooks/useLocalStorageState';

type LoginRequest = components['schemas']['LoginRequest'];
type JwtAuthenticationResponse =
  components['schemas']['JwtAuthenticationResponse'];
type SignUpDto = components['schemas']['SignUpDto'];
type SuperTokenAction = { onCancel: () => void; onSuccess: () => void };

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
  });

  const acceptInvitationLoadable = useApiMutation({
    url: '/v2/invitations/{code}/accept',
    method: 'get',
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

  const [adminToken, setAdminToken] = useLocalStorageState<string | undefined>({
    initial: undefined,
    key: ADMIN_JWT_LOCAL_STORAGE_KEY,
  });

  const [superTokenAfter, setSuperTokenAfter] = useState<SuperTokenAction[]>(
    []
  );
  const [userId, setUserId] = useState<number>();
  const [invitationCode, _setInvitationCode] = useLocalStorageState<
    string | undefined
  >({
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
    const code = InvitationCodeService.getCode();
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
    allowRegistration,
  };

  const actions = {
    async login(credentials: LoginRequest) {
      const response = await loginLoadable.mutateAsync({
        content: { 'application/json': credentials },
      });
      response.accessToken;
      await handleAfterLogin(response!);
    },
    async loginWithOAuthCode(type: string, code: string) {
      const redirectUri = LINKS.OAUTH_RESPONSE.buildWithOrigin({
        [PARAMS.SERVICE_TYPE]: type,
      });
      const response = await authorizeOAuthLoadable.mutateAsync(
        {
          path: { serviceType: type },
          query: {
            code,
            redirect_uri: redirectUri,
            invitationCode: invitationCode,
          },
        },
        {
          onError: (error) => {
            if (error.code === 'invitation_code_does_not_exist_or_expired') {
              setInvitationCode(undefined);
            }
            messageService.error(<TranslatedError code={error.code!} />);
          },
        }
      );
      setInvitationCode(undefined);
      await handleAfterLogin(response!);
    },
    async signUp(data: Omit<SignUpDto, 'invitationCode'>) {
      signupLoadable.mutate(
        {
          content: { 'application/json': { ...data, invitationCode } },
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
      setJwtToken(adminToken);
      setAdminToken(undefined);
    },
    setInvitationCode,
    redirectTo(url: string) {
      history.replace(LINKS.AFTER_LOGIN.build());
    },
  };

  return { state, actions };
};
