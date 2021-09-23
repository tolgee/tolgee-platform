import { IconButton, makeStyles } from '@material-ui/core';
import { ChevronRight, ChevronLeft } from '@material-ui/icons';
import clsx from 'clsx';

type Props = {
  open: boolean;
  onClick: () => void;
};

const useStyles = makeStyles((theme) => ({
  button: {
    borderWidth: 1,
    borderStyle: 'solid',
    borderColor: theme.palette.lightDivider.main,
  },
  open: {
    borderTopRightRadius: 0,
    borderBottomRightRadius: 0,
    borderRightColor: 'transparent',
    borderRightWidth: 0,
    marginRight: -theme.spacing(0.5),
    transition: theme.transitions.create(
      ['border', 'border-radius', 'margin'],
      {
        easing: theme.transitions.easing.sharp,
        duration: theme.transitions.duration.enteringScreen,
      }
    ),
  },
  closed: {
    borderColor: 'transparent',
    marginRight: 2,
    transition: theme.transitions.create(
      ['border', 'border-radius', 'margin'],
      {
        easing: theme.transitions.easing.sharp,
        duration: theme.transitions.duration.leavingScreen,
      }
    ),
  },
  ripppleOpen: {
    '& > * > *': {
      borderTopRightRadius: 0,
      borderBottomRightRadius: 0,
      transition: theme.transitions.create(['border-radius'], {
        easing: theme.transitions.easing.sharp,
        duration: theme.transitions.duration.enteringScreen,
      }),
    },
  },
  rippleClosed: {
    '& > * > *': {
      transition: theme.transitions.create(['border-radius'], {
        easing: theme.transitions.easing.sharp,
        duration: theme.transitions.duration.leavingScreen,
      }),
    },
  },
}));

export const ToggleButton: React.FC<Props> = ({ open, onClick }) => {
  const classes = useStyles();
  return (
    <IconButton
      className={clsx(classes.button, open ? classes.open : classes.closed)}
      onClick={onClick}
      TouchRippleProps={{
        className: open ? classes.ripppleOpen : classes.rippleClosed,
      }}
    >
      {open ? <ChevronLeft /> : <ChevronRight />}
    </IconButton>
  );
};
