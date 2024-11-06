import { LINKS } from 'tg.constants/links';
import { messageService } from 'tg.service/MessageService';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useLocalStorageState } from 'tg.hooks/useLocalStorageState';
import { INVITATION_CODE_STORAGE_KEY } from 'tg.service/InvitationCodeService';

const LOCAL_STORAGE_DOMAIN_KEY = 'oauth2Domain';

export const useSsoService = () => {
  const { handleAfterLogin, setInvitationCode } = useGlobalActions();

  const [invitationCode, _setInvitationCode] = useLocalStorageState<
    string | undefined
  >({
    initial: undefined,
    key: INVITATION_CODE_STORAGE_KEY,
  });

  // const authorizeOpenIdLoadable = useApiMutation({
  //   url: '/v2/public/oauth2/callback/{registrationId}',
  //   method: 'get',
  // });
  const authorizeOAuthLoadable = useApiMutation({
    url: '/api/public/authorize_oauth/{serviceType}',
    method: 'get',
  });

  const openIdAuthUrlLoadable = useApiMutation({
    url: '/v2/public/oauth2/callback/get-authentication-url',
    method: 'post',
  });

  return {
    async loginWithOAuthCodeOpenId(registrationId: string, code: string) {
      const redirectUri = LINKS.OPENID_RESPONSE.buildWithOrigin({});
      const response = await authorizeOAuthLoadable.mutateAsync(
        {
          path: { serviceType: 'sso' },
          query: {
            code,
            redirect_uri: redirectUri,
            invitationCode: invitationCode,
            domain: registrationId,
          },
        },
        {
          onError: (error) => {
            if (error.code === 'invitation_code_does_not_exist_or_expired') {
              setInvitationCode(undefined);
            }
            // TODO: this handling should no longer be necessary - code should be fine
            let errorCode = error.code;
            if (errorCode && errorCode.endsWith(': null')) {
              errorCode = errorCode.replace(': null', '');
            }
            messageService.error(<TranslatedError code={errorCode!} />);
          },
        }
      );
      localStorage.removeItem(LOCAL_STORAGE_DOMAIN_KEY);
      await handleAfterLogin(response!);
    },

    async getSsoAuthLinkByDomain(domain: string | null, state: string) {
      return await openIdAuthUrlLoadable.mutateAsync(
        {
          content: { 'application/json': { domain, state } },
        },
        {
          onError: (error) => {
            messageService.error(<TranslatedError code={error.code!} />);
          },
          onSuccess: (response) => {
            if (response.redirectUrl) {
              localStorage.setItem(LOCAL_STORAGE_DOMAIN_KEY, domain || '');
            }
          },
        }
      );
    },
  };
};
