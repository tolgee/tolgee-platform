import clsx from 'clsx';
import { makeStyles } from '@material-ui/core';

const useStyles = makeStyles((theme) => ({
  wrapper: {
    padding: theme.spacing(1, 1.25),
  },
  placeholder: {
    color: theme.palette.text.disabled,
  },
  error: {
    color: theme.palette.error.dark,
  },
}));

type Props = {
  type: 'placeholder' | 'error';
  message: string;
};

export const TabMessage: React.FC<Props> = ({ type, message }) => {
  const classes = useStyles();

  return <div className={clsx(classes.wrapper, classes[type])}>{message}</div>;
};
