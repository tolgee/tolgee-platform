import { Box } from '@material-ui/core';
import AppBar from '@material-ui/core/AppBar';
import Toolbar from '@material-ui/core/Toolbar';
import Typography from '@material-ui/core/Typography';
import { makeStyles } from '@material-ui/core/styles';
import clsx from 'clsx';
import { Link } from 'react-router-dom';

import { LocaleMenu } from '../LocaleMenu';
import { TolgeeLogo } from '../common/icons/TolgeeLogo';
import { UserMenu } from '../security/UserMenu';
import { useConfig } from 'tg.hooks/useConfig';

export const TOP_BAR_HEIGHT = 52;

const drawerWidth = 240;

const useStyles = makeStyles((theme) => ({
  appBar: {
    zIndex: theme.zIndex.drawer + 1,
    transition: theme.transitions.create(['width', 'margin'], {
      easing: theme.transitions.easing.sharp,
      duration: theme.transitions.duration.leavingScreen,
    }),
    ...theme.mixins.toolbar,
  },
  appBarShift: {
    marginLeft: drawerWidth,
    width: `calc(100% - ${drawerWidth}px)`,
    transition: theme.transitions.create(['width', 'margin'], {
      easing: theme.transitions.easing.sharp,
      duration: theme.transitions.duration.enteringScreen,
    }),
  },
  toolbar: {
    paddingRight: theme.spacing(2), // keep right padding when drawer closed
  },
  tolgeeLink: {
    color: 'inherit',
    textDecoration: 'inherit',
  },
  version: {
    marginLeft: theme.spacing(2),
    fontSize: 11,
  },
  logoTitle: {
    fontSize: 20,
    fontWeight: 500,
    fontFamily: 'Righteous, Rubik, Arial',
  },
}));

interface TopBarProps {}

export function TopBar(props: TopBarProps) {
  const classes = useStyles({});
  const config = useConfig();
  return (
    <AppBar position="fixed" className={clsx(classes.appBar)}>
      <Toolbar className={classes.toolbar}>
        <Box flexGrow={1} display="flex">
          <Box>
            <Link className={classes.tolgeeLink} to={'/'}>
              <Box display="flex" alignItems="center">
                <Box pr={1} display="flex" justifyItems="center">
                  <TolgeeLogo fontSize="large" />
                </Box>
                <Typography
                  variant="h5"
                  color="inherit"
                  className={classes.logoTitle}
                >
                  {config.appName}
                </Typography>
                {config.showVersion && (
                  <Typography variant={'body1'} className={classes.version}>
                    {config.version}
                  </Typography>
                )}
              </Box>
            </Link>
          </Box>
        </Box>
        <LocaleMenu />
        <UserMenu />
      </Toolbar>
    </AppBar>
  );
}
