import { FunctionComponent, ReactElement } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import { TopBar } from './TopBar';

const useStyles = makeStyles((theme) => ({
  root: {
    display: 'flex',
    height: '100vh',
    flexDirection: 'column',
    overflowY: 'hidden',
    alignItems: 'stretch',
  },
  content: {
    flexGrow: 1,
    position: 'relative',
    display: 'flex',
    overflowY: 'auto',
  },
  appBarSpacer: theme.mixins.toolbar,
}));

interface DashboardPageProps {
  sideMenuItems?: ReactElement;
  projectName?: string;
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
      <TopBar />
      <div className={classes.appBarSpacer} />
      <main className={classes.content}>{children}</main>
    </div>
  );
};
