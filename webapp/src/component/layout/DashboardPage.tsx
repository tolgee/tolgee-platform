import { FunctionComponent } from 'react';
import { Box } from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';

import { TopBar } from './TopBar';

const useStyles = makeStyles((theme) => ({
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
      alignItems="stretch"
      flexDirection="column"
      flexGrow={1}
    >
      <TopBar />
      <div className={classes.appBarSpacer} />
      <Box
        component="main"
        position="relative"
        overflow="hidden"
        display="flex"
        flexGrow="1"
        justifyContent="stretch"
      >
        {children}
      </Box>
    </Box>
  );
};
