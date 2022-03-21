import React from 'react';
import clsx from 'clsx';
import { IconButton, Tooltip, Theme } from '@mui/material';
import makeStyles from '@mui/styles/makeStyles';
import { stopBubble } from 'tg.fixtures/eventHandler';

const useStyles = makeStyles<Theme>({
  button: {
    display: 'flex',
    cursor: 'pointer',
    width: 36,
    height: 36,
    margin: -8,
    '& + &': {
      marginLeft: 4,
    },
  },
});

type Props = React.ComponentProps<typeof IconButton> & {
  tooltip?: React.ReactNode;
};

export const ControlsButton: React.FC<Props> = React.forwardRef(
  function ControlsButton(
    { children, className, onClick, tooltip, ...props },
    ref
  ) {
    const classes = useStyles();

    const content = (
      <IconButton
        size="small"
        className={clsx(classes.button, className)}
        onClick={stopBubble(onClick)}
        ref={ref}
        {...props}
      >
        {children}
      </IconButton>
    );

    return tooltip ? <Tooltip title={tooltip}>{content}</Tooltip> : content;
  }
);
