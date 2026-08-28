import { useEffect, useMemo } from 'react';
import { useTheme } from '@mui/material';
import {
  useConfig,
  useHasSupportChat,
  usePreferredOrganization,
  useUser,
} from 'tg.globalContext/helpers';
import { intercomCompanyPayload } from 'tg.fixtures/intercomCompanyPayload';
import {
  applyIntercomSession,
  intercomSessionAction,
} from 'tg.fixtures/intercomSession';
import {
  boot,
  Intercom,
  show,
  shutdown,
  update,
} from '@intercom/messenger-js-sdk';

let intercomInstalled = false;
let intercomBooted = false;
let intercomBootedFor: number | undefined;

export function useIntercom() {
  const user = useUser();
  const config = useConfig();
  const appId = config?.intercomAppId;

  const hasSupportChat = useHasSupportChat();

  const { preferredOrganization } = usePreferredOrganization();
  const companyInfo = useMemo(
    () => intercomCompanyPayload(preferredOrganization),
    [preferredOrganization]
  );

  // `companyInfo` as well as the entitlement, matching the base gate: without an active cloud
  // subscription there is no company to attribute the session to, and booting would transmit the
  // user's identity to Intercom for an instance that never sent anything before.
  const canOpenSupportChat = !!(appId && user && hasSupportChat && companyInfo);
  const theme = useTheme();
  const darkMode = theme.palette.mode === 'dark';

  useEffect(() => {
    const action = intercomSessionAction({
      canOpenSupportChat,
      installed: intercomInstalled,
      booted: intercomBooted,
      identityChanged: intercomBootedFor !== user?.id,
    });

    const trackers = applyIntercomSession(
      action,
      {
        settings: () => ({
          app_id: appId!,
          hide_default_launcher: true,
          user_id: user!.id.toString(),
          name: user!.name,
          email: user!.username,
          action_color: theme.palette.primary.main,
          theme_mode: darkMode ? 'dark' : 'light',
          company: companyInfo,
        }),
        userId: user?.id,
        companyId: companyInfo?.company_id,
      },
      {
        installed: intercomInstalled,
        booted: intercomBooted,
        bootedFor: intercomBootedFor,
      },
      { install: Intercom, boot, update, shutdown }
    );

    intercomInstalled = trackers.installed;
    intercomBooted = trackers.booted;
    intercomBootedFor = trackers.bootedFor;
  }, [
    canOpenSupportChat,
    appId,
    user?.id,
    user?.name,
    user?.username,
    companyInfo,
  ]);

  useEffect(() => {
    if (canOpenSupportChat) {
      update({
        theme_mode: darkMode ? 'dark' : 'light',
        action_color: theme.palette.primary.main,
      });
    }
  }, [darkMode, canOpenSupportChat, theme.palette.primary.main]);

  const openIntercom = () => {
    if (!canOpenSupportChat) {
      return;
    }
    show();
  };

  return {
    intercomAvailable: canOpenSupportChat,
    openIntercom,
  };
}
