import { FunctionComponent } from 'react';
import { Box } from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';

import { TopBar } from './TopBar';

const useStyles = makeStyles((theme) => ({
  // root: {
  //   display: 'flex',
  //   height: '100vh',
  //   flexDirection: 'column',
  //   overflowY: 'hidden',
  //   alignItems: 'stretch',
  // },
  // content: {
  //   flexGrow: 1,
  //   position: 'relative',
  //   display: 'flex',
  //   overflowY: 'auto',
  // },
  appBarSpacer: theme.mixins.toolbar,
}));

interface DashboardPageProps {
  projectName?: string;
  fullWidth?: boolean;
}

export const DashboardPage: FunctionComponent<DashboardPageProps> = ({
  children,
}) => {
  const classes = useStyles({});

  return (
    <Box
      display="flex"
      height="100vh"
      alignItems="stretch"
      overflow="hidden"
      flexDirection="column"
    >
      <TopBar />
      <div className={classes.appBarSpacer} />
      <Box
        component="main"
        display="flex"
        overflow="auto"
        flexGrow="1"
        position="relative"
      >
        {children}
      </Box>
    </Box>
  );
};
