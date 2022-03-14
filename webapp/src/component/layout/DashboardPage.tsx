import { FunctionComponent } from 'react';
import { Box } from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';

import { TopBar } from './TopBar/TopBar';

const useStyles = makeStyles((theme) => ({
  appBarSpacer: theme.mixins.toolbar,
}));

type Props = {
  topBarAutoHide?: boolean;
};

export const DashboardPage: FunctionComponent<Props> = ({
  children,
  topBarAutoHide,
}) => {
  const classes = useStyles({});

  return (
    <Box
      display="flex"
      alignItems="stretch"
      flexDirection="column"
      flexGrow={1}
    >
      <TopBar autoHide={topBarAutoHide} />
      <div className={classes.appBarSpacer} />
      <Box
        component="main"
        position="relative"
        display="flex"
        flexGrow="1"
        justifyContent="stretch"
      >
        {children}
      </Box>
    </Box>
  );
};
