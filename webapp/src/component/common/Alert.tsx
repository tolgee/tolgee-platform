import { default as React, FunctionComponent } from 'react';
import { Theme } from '@material-ui/core';
import makeStyles from '@material-ui/core/styles/makeStyles';
import MuiAlert from '@material-ui/lab/Alert';

const useStyles = makeStyles((theme: Theme) => ({
  alert: {
    marginTop: theme.spacing(2),
    marginBottom: theme.spacing(2),
  },
}));

export const Alert: FunctionComponent<React.ComponentProps<typeof MuiAlert>> = (
  props
) => {
  const classes = useStyles({});

  return <MuiAlert className={classes.alert} {...props} />;
};
