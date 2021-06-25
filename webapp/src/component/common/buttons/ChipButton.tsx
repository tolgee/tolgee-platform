import { FunctionComponent, ReactNode } from 'react';
import { Button, ButtonProps, makeStyles } from '@material-ui/core';
import Box from '@material-ui/core/Box';
import clsx from 'clsx';

const useStyles = makeStyles((theme) => ({
  root: {
    border: `1px solid ${theme.palette.grey['200']}`,
    borderRadius: 50,
    padding: `${theme.spacing(0.125)}px ${theme.spacing(1.5)}px`,
    backgroundColor: theme.palette.common.white,
    cursor: 'pointer',
    minWidth: '0',
  },
  validIcon: {
    display: 'inline-flex',
    alignItems: 'center',
    '& svg': {
      fontSize: 16,
    },
  },
  beforeIcon: {
    marginRight: theme.spacing(0.5),
  },
}));

export const ChipButton: FunctionComponent<
  {
    beforeIcon?: ReactNode;
    onClick: () => void;
  } & ButtonProps
> = (props) => {
  const { beforeIcon, children, ...buttonProps } = props;

  const classes = useStyles();

  return (
    <Button className={classes.root} {...buttonProps}>
      {beforeIcon && (
        <Box
          display="inline-flex"
          className={clsx(classes.beforeIcon, classes.validIcon)}
        >
          {beforeIcon}
        </Box>
      )}
      {children}
    </Button>
  );
};
