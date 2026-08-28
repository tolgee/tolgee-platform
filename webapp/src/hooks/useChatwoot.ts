import { useEffect } from 'react';
import { useTheme } from '@mui/material';
import {
  useConfig,
  useHasSupportChat,
  usePreferredOrganization,
  useUser,
} from 'tg.globalContext/helpers';
import { components } from 'tg.service/apiSchema.generated';
import { ContextOrganizationModel } from 'tg.globalContext/types';
import { chatwootOrganizationPayload } from 'tg.fixtures/chatwootOrganizationPayload';
import {
  applyChatwootSession,
  chatwootSessionAction,
  openChatwootSession,
} from 'tg.fixtures/chatwootSession';

type User = components['schemas']['PrivateUserAccountModel'];

const BASE_URL = 'https://app.chatwoot.com';
let chatwootLoadPromise: Promise<void> | null = null;
let chatwootIdentifiedUserId: number | undefined;

export function useChatwoot() {
  const user = useUser();
  const { preferredOrganization } = usePreferredOrganization();
  const config = useConfig();
  const token = config?.chatwootToken;

  const hasSupportChat = useHasSupportChat();

  const canOpenSupportChat = !!(token && user && hasSupportChat);

  const sessionAction = () =>
    chatwootSessionAction({
      canOpenSupportChat,
      identifiedUserId: chatwootIdentifiedUserId,
      currentUserId: user?.id,
    });

  const theme = useTheme();
  const darkMode = theme.palette.mode === 'dark';

  useEffect(() => {
    if (canOpenSupportChat) {
      loadChatwootOnce(token, darkMode);
    }
  }, [canOpenSupportChat]);

  useEffect(() => {
    if (!chatwootLoadPromise) {
      return;
    }

    let cancelled = false;
    chatwootLoadPromise.then(() => {
      if (cancelled) {
        return;
      }
      chatwootIdentifiedUserId = applyChatwootSession(
        sessionAction(),
        preferredOrganization,
        chatwootIdentifiedUserId,
        chatwootSdk
      );
    });

    return () => {
      cancelled = true;
    };
  }, [canOpenSupportChat, preferredOrganization, user?.id]);

  const openChatwoot = async () => {
    if (!canOpenSupportChat) {
      return;
    }

    await loadChatwootOnce(token, darkMode);
    chatwootIdentifiedUserId = openChatwootSession(
      sessionAction(),
      { user, userId: user.id, organization: preferredOrganization },
      chatwootSdk
    );
  };

  return {
    chatwootAvailable: canOpenSupportChat,
    openChatwoot,
  };
}

const chatwootSdk = {
  reset: resetChatwootSession,
  setUser: setChatwootUser,
  setAttributes: setChatwootAttributes,
  toggle: toggleChatwoot,
};

function setChatwootUser(user: User) {
  window['$chatwoot']?.setUser(user.id, {
    email: user.username,
    name: user.name,
    url: window.location,
  });
}

function setChatwootAttributes(organization: ContextOrganizationModel) {
  window['$chatwoot']?.setCustomAttributes(
    chatwootOrganizationPayload(organization)
  );
}

function resetChatwootSession() {
  window['$chatwoot']?.reset();
}

function toggleChatwoot() {
  window['$chatwoot']?.toggle();
}

async function loadChatwootOnce(websiteToken: string, darkMode: boolean) {
  if (!chatwootLoadPromise) {
    chatwootLoadPromise = loadChatwoot(websiteToken, darkMode);
  }

  await chatwootLoadPromise;
}

async function loadChatwoot(websiteToken: string, darkMode: boolean) {
  window['chatwootSettings'] = {
    darkMode: darkMode ? 'auto' : 'light',
    hideMessageBubble: true,
  };

  await loadScript(document, BASE_URL + '/packs/js/sdk.js');

  window['chatwootSDK']?.run({
    websiteToken,
    baseUrl: BASE_URL,
  });
}

function loadScript(doc: Document, url: string) {
  return new Promise<void>((resolve) => {
    const element = doc.createElement('script') as HTMLScriptElement;
    const existingElement = doc.getElementsByTagName(
      'script'
    )[0] as HTMLScriptElement;

    element.src = url;
    element.defer = true;
    element.async = true;
    element.onload = () => {
      resolve();
    };

    existingElement?.parentNode?.insertBefore(element, existingElement);
  });
}
