import { useEffect, useState } from 'react';
import {
  styled,
  useTheme,
  Tooltip,
  Fab,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  Link,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
} from '@mui/material';
import {
  BookOpen01,
  MessageSquare01,
  Mail01,
  HelpCircle,
} from '@untitled-ui/icons-react';
import { T, useTranslate } from '@tolgee/react';

import {
  useConfig,
  usePreferredOrganization,
  useUser,
} from 'tg.globalContext/helpers';
import { GitHub, Slack } from './CustomIcons';

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

export const HelpMenu = () => {
  const { t } = useTranslate();
  const user = useUser();
  const config = useConfig();
  const { preferredOrganization } = usePreferredOrganization();
  const token = config?.chatwootToken;
  const {
    palette: { mode },
  } = useTheme();

  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  const open = Boolean(anchorEl);
  const handleOpen = (event: React.MouseEvent<HTMLDivElement>) => {
    setAnchorEl(event.currentTarget);
  };
  const handleClose = () => {
    setAnchorEl(null);
  };

  const [dialogOpen, setDialogOpen] = useState(false);

  function handleOpenDialog() {
    setDialogOpen(true);
    handleClose();
  }

  const darkMode = mode === 'dark';

  useEffect(() => {
    if (token) {
      loadScript(token!, darkMode);
    }
  }, [token]);

  const openChatwoot = () => {
    handleClose();
    loadScript(token!, darkMode).then(() => {
      // @ts-ignore
      window.$chatwoot.setUser(user.id, {
        email: user!.username,
        name: user!.name,
        url: window.location,
      });
      // @ts-ignore
      window.$chatwoot.toggle();
    });
  };

  function buttonLink(url: string) {
    return { href: url, target: 'blank', rel: 'noreferrer noopener' };
  }

  if (!preferredOrganization) {
    return null;
  }

  const enabledFeatures = preferredOrganization.enabledFeatures;

  const hasStandardSupport =
    enabledFeatures.includes('STANDARD_SUPPORT') ||
    enabledFeatures.includes('PREMIUM_SUPPORT');

  const displayChat = token && user && hasStandardSupport;

  return (
    <>
      <Tooltip
        title={t('help_menu_tooltip')}
        PopperProps={{ placement: 'right' }}
      >
        <StyledHelpButton onClick={handleOpen} id="help-button">
          <Fab color="primary" size="small">
            <HelpCircle />
          </Fab>
        </StyledHelpButton>
      </Tooltip>
      <Menu
        id="basic-menu"
        anchorEl={anchorEl}
        open={open}
        onClose={handleClose}
        MenuListProps={{
          'aria-labelledby': 'help-button',
        }}
        anchorOrigin={{ vertical: -10, horizontal: 'left' }}
        transformOrigin={{ vertical: 'bottom', horizontal: 'left' }}
      >
        <MenuItem
          component={Link}
          {...buttonLink('https://tolgee.io/platform')}
        >
          <ListItemIcon>
            <BookOpen01 />
          </ListItemIcon>
          <ListItemText primary={t('help_menu_documentation')} />
        </MenuItem>
        <MenuItem
          component={Link}
          {...buttonLink(
            'https://github.com/tolgee/tolgee-platform/discussions'
          )}
        >
          <ListItemIcon>
            <GitHub />
          </ListItemIcon>
          <ListItemText
            primary={t('help_menu_github_discussions')}
            secondary={t('help_menu_github_discussions_description')}
          />
        </MenuItem>
        <MenuItem
          component={Link}
          {...buttonLink('https://github.com/tolgee/tolgee-platform/issues')}
        >
          <ListItemIcon>
            <GitHub />
          </ListItemIcon>
          <ListItemText
            primary={t('help_menu_github_issues')}
            secondary={t('help_menu_github_issues_description')}
          />
        </MenuItem>
        <MenuItem component={Link} {...buttonLink('https://tolg.ee/slack')}>
          <ListItemIcon>
            <Slack />
          </ListItemIcon>
          <ListItemText
            primary={t('help_menu_slack_community')}
            secondary={t('help_menu_slack_community_description')}
          />
        </MenuItem>
        {displayChat && (
          <MenuItem onClick={openChatwoot}>
            <ListItemIcon>
              <MessageSquare01 />
            </ListItemIcon>
            <ListItemText
              primary={t('help_menu_chat_with_us')}
              secondary={t('help_menu_chat_with_us_description')}
            />
          </MenuItem>
        )}
        <MenuItem onClick={handleOpenDialog}>
          <ListItemIcon>
            <Mail01 />
          </ListItemIcon>
          <ListItemText primary={t('help_menu_email')} />
        </MenuItem>
      </Menu>

      <Dialog open={dialogOpen}>
        <DialogTitle>{t('help_menu_email_dialog_title')}</DialogTitle>
        <DialogContent>
          <T
            keyName="help_menu_email_dialog_message"
            params={{ email: <Link href="mailto:info@tolgee.io" /> }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>
            {t('global_close_button')}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};
