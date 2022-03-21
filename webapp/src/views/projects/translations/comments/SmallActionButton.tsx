import clsx from 'clsx';
import makeStyles from '@mui/styles/makeStyles';
import { Theme } from '@mui/material';

type Props = React.DetailedHTMLProps<
  React.ButtonHTMLAttributes<HTMLButtonElement>,
  HTMLButtonElement
>;

const useStyles = makeStyles<Theme>((theme) => ({
  button: {
    margin: 0,
    outline: 0,
    fontSize: 12,
    padding: 0,
    border: 0,
    opacity: 0.6,
    background: 'transparent',
    cursor: 'pointer',
    '&:hover, &:active': {
      opacity: 1,
    },
  },
}));

export const SmallActionButton: React.FC<Props> = ({
  children,
  className,
  ...tools
}) => {
  const classes = useStyles();
  return (
    <button {...tools} className={clsx(classes.button, className)}>
      {children}
    </button>
  );
};
