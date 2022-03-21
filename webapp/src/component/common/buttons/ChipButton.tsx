import { FunctionComponent, ReactNode } from 'react';
import { Button, ButtonProps } from '@mui/material';
import makeStyles from '@mui/styles/makeStyles';
import Box from '@mui/material/Box';
import clsx from 'clsx';

const useStyles = makeStyles<Theme>((theme) => ({
  root: {
    border: `1px solid ${theme.palette.grey['200']}`,
    borderRadius: 50,
    padding: `${theme.spacing(0.125)} ${theme.spacing(1.5)}`,
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
