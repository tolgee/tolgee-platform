import { useEffect } from 'react';
import { useTheme } from '@mui/material';
import {
  useConfig,
  useHasSupportChat,
  usePreferredOrganization,
  useUser,
} from 'tg.globalContext/helpers';
import { Intercom, show, update } from '@intercom/messenger-js-sdk';

export function useIntercom() {
  const user = useUser();
  const hasSupportChat = useHasSupportChat();
  const config = useConfig();
  const appId = config?.intercomAppId;

  const available = !!(appId && user && hasSupportChat);
  const theme = useTheme();

  const darkMode = theme.palette.mode === 'dark';

  const companyInfo = useCompanyInfo();

  useEffect(() => {
    if (available && companyInfo) {
      Intercom({
        app_id: appId,
        hide_default_launcher: true,
        user_id: user?.id.toString(),
        name: user?.name,
        email: user?.username,
        action_color: theme.palette.primary.main,
        company: companyInfo,
      });
    }
  }, [available, user, companyInfo, appId]);

  useEffect(() => {
    update({ theme_mode: darkMode ? 'dark' : 'light' });
  }, [darkMode]);

  const openIntercom = () => {
    if (!available) {
      return;
    }
    show();
  };

  return {
    intercomAvailable: available,
    openIntercom,
  };
}

function useCompanyInfo() {
  const { preferredOrganization } = usePreferredOrganization();
  const subscription = preferredOrganization?.activeCloudSubscription;

  if (!preferredOrganization || !subscription) {
    return null;
  }

  return {
    company_id: preferredOrganization?.id,
    name: preferredOrganization?.name,
    plan: subscription?.plan?.name || 'free',
    subscriptionStatus: subscription?.status || 'inactive',
    enabledFeatures: preferredOrganization?.enabledFeatures.join(', '),
  };
}
