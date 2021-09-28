import { IconButton, makeStyles } from '@material-ui/core';
import { ChevronLeft } from '@material-ui/icons';
import clsx from 'clsx';

type Props = {
  open: boolean;
  onClick: () => void;
};

const useStyles = makeStyles((theme) => {
  const enter = {
    easing: theme.transitions.easing.sharp,
    duration: theme.transitions.duration.enteringScreen,
  };
  const leave = {
    easing: theme.transitions.easing.sharp,
    duration: theme.transitions.duration.leavingScreen,
  };
  return {
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
        enter
      ),
    },
    closed: {
      borderColor: 'transparent',
      marginRight: 2,
      transition: theme.transitions.create(
        ['border', 'border-radius', 'margin'],
        leave
      ),
    },
    ripppleOpen: {
      '& > * > *': {
        borderTopRightRadius: 0,
        borderBottomRightRadius: 0,
        transition: theme.transitions.create(['border-radius'], enter),
      },
    },
    rippleClosed: {
      '& > * > *': {
        transition: theme.transitions.create(['border-radius'], leave),
      },
    },
    iconOpen: {
      transform: 'none',
      transition: theme.transitions.create(['transform'], enter),
    },
    iconClosed: {
      transform: 'rotate(180deg)',
      transition: theme.transitions.create(['transform'], leave),
    },
  };
});

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
      <ChevronLeft className={open ? classes.iconOpen : classes.iconClosed} />
    </IconButton>
  );
};
