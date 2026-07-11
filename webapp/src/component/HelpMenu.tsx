import { useState } from 'react';
import {
  styled,
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
  Divider,
  Typography,
} from '@mui/material';
import {
  BookOpen01,
  MessageSquare01,
  Mail01,
  HelpCircle,
  Keyboard02,
} from '@untitled-ui/icons-react';
import { T, useTranslate } from '@tolgee/react';

import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { GitHub, Slack } from './CustomIcons';
import { TranslationsShortcuts } from './shortcuts/TranslationsShortcuts';
import { useChatwoot } from 'tg.hooks/useChatwoot';
import { useIntercom } from 'tg.hooks/useIntercom';

const StyledHelpButton = styled('div')`
  position: fixed;
  z-index: ${({ theme }) => theme.zIndex.fab};
  bottom: 15px;
  left: 10px;
  border-radius: 50%;
`;

export const HelpMenu = () => {
  const { t } = useTranslate();
  const { preferredOrganization } = usePreferredOrganization();
  const { chatwootAvailable, openChatwoot } = useChatwoot();
  const { intercomAvailable, openIntercom } = useIntercom();

  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  const open = Boolean(anchorEl);
  const handleOpen = (event: React.MouseEvent<HTMLDivElement>) => {
    setAnchorEl(event.currentTarget);
  };
  const handleClose = () => {
    setAnchorEl(null);
  };

  const [dialogOpen, setDialogOpen] = useState(false);
  const [shortcutsOpen, setShortcutsOpen] = useState(false);

  function handleOpenDialog() {
    setDialogOpen(true);
    handleClose();
  }

  function handleOpenShortcuts() {
    setShortcutsOpen(true);
    handleClose();
  }

  function buttonLink(url: string) {
    return { href: url, target: 'blank', rel: 'noreferrer noopener' };
  }

  if (!preferredOrganization) {
    return null;
  }

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
        {chatwootAvailable && (
          <MenuItem
            onClick={() => {
              handleClose();
              openChatwoot();
            }}
          >
            <ListItemIcon>
              <MessageSquare01 />
            </ListItemIcon>
            <ListItemText
              primary={t('help_menu_chat_with_us')}
              secondary={t('help_menu_chat_with_us_description')}
            />
          </MenuItem>
        )}
        {intercomAvailable && (
          <MenuItem
            onClick={() => {
              handleClose();
              openIntercom();
            }}
          >
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
        <Divider />
        <MenuItem onClick={handleOpenShortcuts}>
          <ListItemIcon>
            <Keyboard02 />
          </ListItemIcon>
          <ListItemText primary={t('help_menu_shortcuts')} />
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

      <Dialog open={shortcutsOpen} onClose={() => setShortcutsOpen(false)}>
        <DialogTitle>{t('help_menu_shortcuts_dialog_title')}</DialogTitle>
        <DialogContent sx={{ width: '85vw', maxWidth: '400px' }}>
          <Typography
            sx={{ fontWeight: 'bold', paddingBottom: 1, fontSize: 14 }}
          >
            <T keyName="help_menu_shortcuts_dialog_translations_view" />
          </Typography>
          <TranslationsShortcuts />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShortcutsOpen(false)}>
            {t('global_close_button')}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};
