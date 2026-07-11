import { useEffect } from 'react';
import { useTheme } from '@mui/material';
import {
  useConfig,
  usePreferredOrganization,
  useUser,
} from 'tg.globalContext/helpers';
import { components } from 'tg.service/apiSchema.generated';

type User = components['schemas']['PrivateUserAccountModel'];
type Organization = components['schemas']['PrivateOrganizationModel'];

const BASE_URL = 'https://app.chatwoot.com';
let chatwootLoadPromise: Promise<void> | null = null;

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

async function loadChatwootOnce(websiteToken: string, darkMode: boolean) {
  if (!chatwootLoadPromise) {
    chatwootLoadPromise = loadChatwoot(websiteToken, darkMode);
  }

  await chatwootLoadPromise;
}

function setChatwootUser(user: User) {
  window['$chatwoot']?.setUser(user.id, {
    email: user!.username,
    name: user!.name,
    url: window.location,
  });
}

function setChatwootAttributes(organization: Organization) {
  const subscription = organization.activeCloudSubscription;
  window['$chatwoot']?.setCustomAttributes({
    plan: subscription?.plan?.name || 'free',
    subscriptionStatus: subscription?.status || 'inactive',
    organizationId: organization.id,
    organizationName: organization.name,
    enabledFeatures: organization.enabledFeatures.join(', '),
    currentUserRole: organization.currentUserRole,
  });
}

function toggleChatwoot() {
  window['$chatwoot']?.toggle();
}

export function useChatwoot() {
  const user = useUser();
  const { preferredOrganization } = usePreferredOrganization();
  const config = useConfig();
  const token = config?.chatwootToken;

  const enabledFeatures = preferredOrganization?.enabledFeatures;

  const hasStandardSupport =
    enabledFeatures?.includes('STANDARD_SUPPORT') ||
    enabledFeatures?.includes('PREMIUM_SUPPORT');

  const available = !!(token && user && hasStandardSupport);

  const {
    palette: { mode },
  } = useTheme();

  const darkMode = mode === 'dark';

  useEffect(() => {
    if (token) {
      loadChatwootOnce(token, darkMode);
    }
  }, [token]);

  const openChatwoot = async () => {
    if (!available) {
      return;
    }

    await loadChatwootOnce(token, darkMode);
    setChatwootUser(user);
    if (preferredOrganization) {
      setChatwootAttributes(preferredOrganization);
    }

    toggleChatwoot();
  };

  return {
    chatwootAvailable: available,
    openChatwoot,
  };
}
