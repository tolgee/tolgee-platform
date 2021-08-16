import React from 'react';
import clsx from 'clsx';
import { IconButton, makeStyles } from '@material-ui/core';
import { stopBubble } from 'tg.fixtures/eventHandler';

const useStyles = makeStyles({
  button: {
    display: 'flex',
    cursor: 'pointer',
    width: 36,
    height: 36,
    margin: -8,
  },
});

type Props = React.ComponentProps<typeof IconButton>;

export const ControlsButton: React.FC<Props> = React.forwardRef(
  function ControlsButton({ children, className, onClick, ...props }, ref) {
    const classes = useStyles();
    return (
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
  }
);
