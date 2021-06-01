import * as React from 'react';
import { FunctionComponent, ReactElement } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import CssBaseline from '@material-ui/core/CssBaseline';
import Box from '@material-ui/core/Box';
import { MainMenu } from './MainMenu';
import Grid from '@material-ui/core/Grid';
import Container from '@material-ui/core/Container';
import { Link } from 'react-router-dom';
import { LINKS } from '../../constants/links';
import { TolgeeLogo } from '../common/icons/TolgeeLogo';

const FOOTER_IMAGE_HEIGHT = 30;
const FOOTER_PADDING = 1;

function Copyright() {
  return (
    <>
      <Box
        display="flex"
        p={1}
        alignItems="center"
        justifyContent="center"
        fontWeight="500"
      >
        <Link to={LINKS.ROOT.build()} style={{ color: 'inherit', height: 30 }}>
          <TolgeeLogo
            opacity={0.2}
            style={{
              width: 30,
              height: 30,
              filter: 'drop-shadow(0px 1px 1px rgba(0, 0, 0, 0.2))',
            }}
          />
        </Link>
      </Box>
    </>
  );
}

const useStyles = makeStyles((theme) => ({
  root: {
    display: 'flex',
  },
  appBarSpacer: theme.mixins.toolbar,
  content: {
    flexGrow: 1,
    height: `100vh`,
    overflow: 'auto',
  },
  container: {
    flexGrow: 1,
    display: 'flex',
  },
}));

interface DashboardPageProps {
  sideMenuItems?: ReactElement;
  repositoryName?: string;
  fullWidth?: boolean;
}

export const DashboardPage: FunctionComponent<DashboardPageProps> = ({
  children,
  sideMenuItems,
  ...props
}) => {
  const classes = useStyles({});

  return (
    <div className={classes.root}>
      <CssBaseline />
      <MainMenu
        repositoryName={props.repositoryName}
        sideMenuItems={sideMenuItems}
      />
      <main className={classes.content}>
        <Box
          display="flex"
          flexDirection="column"
          flexGrow={1}
          minHeight="100%"
        >
          {!props.repositoryName && <div className={classes.appBarSpacer} />}
          <Box flexGrow={1} display={'flex'} flexDirection="column">
            <Box className={classes.container}>{children}</Box>
          </Box>
          <Box>
            <Copyright />
          </Box>
        </Box>
      </main>
    </div>
  );
};
