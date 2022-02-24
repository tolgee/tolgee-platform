import { makeStyles } from '@material-ui/core';

type Props = React.DetailedHTMLProps<
  React.ButtonHTMLAttributes<HTMLButtonElement>,
  HTMLButtonElement
>;

const useStyles = makeStyles((theme) => ({
  button: {
    marginTop: 2,
    fontSize: 12,
    padding: '2px 6px',
    border: 0,
    color: 'white',
    borderRadius: 8,
    background: theme.palette.primary.main,
    cursor: 'pointer',
    '&:hover, &:active': {
      background: theme.palette.primary.dark,
    },
  },
}));

export const SmallActionButton: React.FC<Props> = ({ children, ...tools }) => {
  const classes = useStyles();
  return (
    <button className={classes.button} {...tools}>
      {children}
    </button>
  );
};
