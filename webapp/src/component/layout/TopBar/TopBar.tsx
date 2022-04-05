import { Link } from 'react-router-dom';
import { Box, Slide, styled } from '@mui/material';
import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';

import { LocaleMenu } from '../../LocaleMenu';
import { UserMenu } from '../../security/UserMenu';
import { useConfig } from 'tg.hooks/useConfig';
import { TolgeeLogo } from 'tg.component/common/icons/TolgeeLogo';
import { useTopBarHidden } from './TopBarContext';

export const TOP_BAR_HEIGHT = 52;

const StyledAppBar = styled(AppBar)(
  ({ theme }) =>
    ({
      zIndex: theme.zIndex.drawer + 1,
      transition: theme.transitions.create(['width', 'margin'], {
        easing: theme.transitions.easing.sharp,
        duration: theme.transitions.duration.leavingScreen,
      }),
      ...theme.mixins.toolbar,
    } as any)
);

const StyledToolbar = styled(Toolbar)`
  padding-right: ${({ theme }) => theme.spacing(2)} !important;
  padding-left: ${({ theme }) => theme.spacing(2)} !important;
`;

const StyledVersion = styled(Typography)`
  margin-left: ${({ theme }) => theme.spacing(2)};
  font-size: 11px;
`;

const StyledLogoTitle = styled(Typography)`
  font-size: 20px;
  font-weight: 500;
  font-family: Righteous, Rubik, Arial;
  transition: filter 0.2s ease-in-out;
`;

const StyledLogoWrapper = styled(Box)`
  transition: filter 0.2s ease-in-out;
`;

const StyledTolgeeLink = styled(Link)`
  color: inherit;
  text-decoration: inherit;
  outline: 0;
  &:focus ${StyledLogoWrapper} {
    filter: brightness(95%);
  }
  ,
  &:focus ${StyledLogoTitle} {
    filter: brightness(95%);
  }
`;

type Props = {
  autoHide?: boolean;
};

export const TopBar: React.FC<Props> = ({ autoHide = false }) => {
  const config = useConfig();

  const trigger = useTopBarHidden() && autoHide;

  return (
    <Slide appear={false} direction="down" in={!trigger}>
      <StyledAppBar>
        <StyledToolbar>
          <Box flexGrow={1} display="flex">
            <Box>
              <StyledTolgeeLink to={'/'}>
                <Box display="flex" alignItems="center">
                  <StyledLogoWrapper
                    pr={1}
                    display="flex"
                    justifyItems="center"
                  >
                    <TolgeeLogo fontSize="large" />
                  </StyledLogoWrapper>
                  <StyledLogoTitle variant="h5" color="inherit">
                    {config.appName}
                  </StyledLogoTitle>
                  {config.showVersion && (
                    <StyledVersion variant="body1">
                      {config.version}
                    </StyledVersion>
                  )}
                </Box>
              </StyledTolgeeLink>
            </Box>
          </Box>
          <LocaleMenu />
          <UserMenu />
        </StyledToolbar>
      </StyledAppBar>
    </Slide>
  );
};
