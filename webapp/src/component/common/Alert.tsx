import { default as React, FunctionComponent } from 'react';
import { Theme } from '@mui/material';
import makeStyles from '@mui/styles/makeStyles';
import MuiAlert from '@mui/material/Alert';

const useStyles = makeStyles<Theme>((theme) => ({
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
