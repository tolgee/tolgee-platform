import { Link } from 'react-router-dom';
import { Box, styled, Toolbar, Typography, useTheme } from '@mui/material';
import AppBar from '@mui/material/AppBar';

import { useConfig, useIsGuest } from 'tg.globalContext/helpers';
import { TolgeeLogo } from 'tg.component/common/icons/TolgeeLogo';
import { LanguageMenu } from 'tg.component/layout/TopBar/LanguageMenu';
import { UserMenu } from 'tg.component/security/UserMenu/UserMenu';
import { LoginSignUpButtons } from 'tg.component/layout/TopBar/LoginSignUpButtons';
import { LINKS } from 'tg.constants/links';

const StyledAppBar = styled(AppBar)(({ theme }) => ({
  zIndex: theme.zIndex.drawer + 1,
  background: theme.palette.navbar.background,
  color: theme.palette.text.primary,
  boxShadow:
    theme.palette.mode === 'light'
      ? '0px 4px 6px 0px rgba(0, 0, 0, 0.02)'
      : 'none',
}));

const StyledToolbar = styled(Toolbar)`
  padding-right: 12.5px !important;
  padding-left: 12.5px !important;
  gap: ${({ theme }) => theme.spacing(1)};
`;

const StyledLogoTitle = styled(Typography)`
  font-size: 20px;
  font-weight: 500;
  font-family: Righteous, Rubik, Arial, sans-serif;
`;

const StyledTolgeeLink = styled(Link)`
  color: ${({ theme }) => theme.palette.navbar.text};
  text-decoration: inherit;
  outline: 0;
`;

export const PublicTopBar = () => {
  const theme = useTheme();
  const config = useConfig();
  const isGuest = useIsGuest();

  return (
    <StyledAppBar position="sticky">
      <StyledToolbar>
        <Box flexGrow={1} display="flex">
          <StyledTolgeeLink to={LINKS.PUBLIC_PROJECTS.build()}>
            <Box display="flex" alignItems="center" gap={1}>
              <TolgeeLogo
                fontSize="large"
                sx={{ color: theme.palette.navbar.logo }}
              />
              <StyledLogoTitle variant="h5" color="inherit">
                {config.appName}
              </StyledLogoTitle>
            </Box>
          </StyledTolgeeLink>
        </Box>
        <LanguageMenu />
        {isGuest ? <LoginSignUpButtons /> : <UserMenu />}
      </StyledToolbar>
    </StyledAppBar>
  );
};
