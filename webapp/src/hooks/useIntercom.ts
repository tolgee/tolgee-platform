import { useEffect, useMemo, useRef } from 'react';
import { useTheme } from '@mui/material';
import {
  useConfig,
  useHasSupportChat,
  usePreferredOrganization,
  useUser,
} from 'tg.globalContext/helpers';
import { organizationCompanyInfo } from 'tg.fixtures/organizationEntitlement';
import { Intercom, show, shutdown, update } from '@intercom/messenger-js-sdk';

export function useIntercom() {
  const user = useUser();
  const config = useConfig();
  const appId = config?.intercomAppId;

  const hasSupportChat = useHasSupportChat();

  const companyInfo = useCompanyInfo();

  const available = !!(appId && user && companyInfo && hasSupportChat);
  const theme = useTheme();
  const darkMode = theme.palette.mode === 'dark';

  const bootedRef = useRef(false);

  useEffect(() => {
    if (available) {
      Intercom({
        app_id: appId,
        hide_default_launcher: true,
        user_id: user.id.toString(),
        name: user.name,
        email: user.username,
        action_color: theme.palette.primary.main,
        company: companyInfo,
      });
      bootedRef.current = true;
    } else if (bootedRef.current) {
      shutdown();
      bootedRef.current = false;
    }
  }, [available, user, companyInfo, appId]);

  useEffect(() => {
    if (available) {
      update({ theme_mode: darkMode ? 'dark' : 'light' });
    }
  }, [darkMode, available]);

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

  return useMemo(
    () => organizationCompanyInfo(preferredOrganization),
    [preferredOrganization]
  );
}
