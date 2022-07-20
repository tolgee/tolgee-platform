import { useEffect, useState } from 'react';
import { styled, useTheme, Tooltip, Fab } from '@mui/material';
import { Chat } from '@mui/icons-material';
import { useTranslate } from '@tolgee/react';

import { useConfig, useUser } from 'tg.globalContext/helpers';

const BASE_URL = 'https://app.chatwoot.com';
let scriptPromise: Promise<void> | null = null;

const StyledHelpButton = styled('div')`
  position: fixed;
  z-index: ${({ theme }) => theme.zIndex.fab};
  bottom: 15px;
  left: 10px;
  border-radius: 50%;
`;

export const loadScript = (websiteToken: string, darkMode: boolean) => {
  return (function (doc, tag) {
    if (!scriptPromise) {
      const g = doc.createElement(tag) as HTMLScriptElement,
        s = doc.getElementsByTagName(tag)[0] as HTMLScriptElement;
      g.src = BASE_URL + '/packs/js/sdk.js';
      g.defer = true;
      g.async = true;
      s!.parentNode!.insertBefore(g, s);
      // @ts-ignore
      window.chatwootSettings = {
        darkMode: darkMode ? 'auto' : 'light',
        hideMessageBubble: true,
      };
      scriptPromise = new Promise<void>((resolve) => {
        g.onload = function () {
          // @ts-ignore
          window.chatwootSDK.run({
            websiteToken,
            baseUrl: BASE_URL,
          });
          resolve();
        };
      });
    }
    return scriptPromise;
  })(document, 'script');
};

export const Chatwoot = () => {
  const t = useTranslate();
  const user = useUser();
  const config = useConfig();
  const token = config?.chatwootToken;
  const [isOpen, setIsOpen] = useState(false);

  const {
    palette: { mode },
  } = useTheme();

  const darkMode = mode === 'dark';

  useEffect(() => {
    if (token) {
      loadScript(token!, darkMode);
    }
  }, [token]);

  useEffect(() => {
    if (token) {
      const timer = setInterval(() => {
        // @ts-ignore
        setIsOpen(window.$chatwoot?.isOpen);
      }, 500);
      return () => clearInterval(timer);
    }
  }, [token]);

  const openChatwoot = () => {
    loadScript(token!, darkMode).then(() => {
      // @ts-ignore
      window.$chatwoot.setUser(user.id, {
        email: user!.username,
        name: user!.name,
      });
      // @ts-ignore
      window.$chatwoot.toggle();
      setIsOpen(true);
    });
  };

  return user && !isOpen && token ? (
    <Tooltip
      title={t('global_chatwoot_tooltip')}
      PopperProps={{ placement: 'right' }}
    >
      <StyledHelpButton>
        <Fab onClick={openChatwoot} color="primary" size="small">
          <Chat fontSize="small" />
        </Fab>
      </StyledHelpButton>
    </Tooltip>
  ) : null;
};
