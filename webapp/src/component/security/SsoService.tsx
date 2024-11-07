import { v4 as uuidv4 } from 'uuid';
import { useHistory } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';
import { messageService } from 'tg.service/MessageService';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useLocalStorageState } from 'tg.hooks/useLocalStorageState';
import { INVITATION_CODE_STORAGE_KEY } from 'tg.service/InvitationCodeService';

const LOCAL_STORAGE_STATE_KEY = 'oauth2State';
const LOCAL_STORAGE_DOMAIN_KEY = 'oauth2Domain';

export const useSsoService = () => {
  const { handleAfterLogin, setInvitationCode } = useGlobalActions();

  const [invitationCode, _setInvitationCode] = useLocalStorageState<
    string | undefined
  >({
    initial: undefined,
    key: INVITATION_CODE_STORAGE_KEY,
  });

  const history = useHistory();

  const authorizeOAuthLoadable = useApiMutation({
    url: '/api/public/authorize_oauth/{serviceType}',
    method: 'get',
  });

  const openIdAuthUrlLoadable = useApiMutation({
    url: '/v2/public/oauth2/callback/get-authentication-url',
    method: 'post',
  });

  const getSsoAuthLinkByDomain = async (domain: string, state: string) => {
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
  };

  return {
    async login(state: string, code: string) {
      const storedState = localStorage.getItem(LOCAL_STORAGE_STATE_KEY);
      const storedDomain = localStorage.getItem(LOCAL_STORAGE_DOMAIN_KEY);
      if (storedState !== state || storedDomain === null) {
        history.replace(LINKS.LOGIN.build());
        return;
      }

      localStorage.removeItem(LOCAL_STORAGE_STATE_KEY);

      const redirectUri = LINKS.OPENID_RESPONSE.buildWithOrigin({});
      const response = await authorizeOAuthLoadable.mutateAsync(
        {
          path: { serviceType: 'sso' },
          query: {
            code,
            redirect_uri: redirectUri,
            invitationCode: invitationCode,
            domain: storedDomain,
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
      localStorage.removeItem(LOCAL_STORAGE_DOMAIN_KEY);
      await handleAfterLogin(response!);
    },

    async loginRedirect(domain: string) {
      const state = uuidv4();
      localStorage.setItem(LOCAL_STORAGE_STATE_KEY, state);
      const response = await getSsoAuthLinkByDomain(domain, state);
      window.location.href = response.redirectUrl;
    },

    redirectLoadable: openIdAuthUrlLoadable,
  };
};
