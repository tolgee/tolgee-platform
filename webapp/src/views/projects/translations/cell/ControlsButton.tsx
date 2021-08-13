import React from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/core';

const useStyles = makeStyles({
  button: {
    display: 'flex',
    cursor: 'pointer',
    justifyContent: 'center',
    alignItems: 'center',
    background: 'transparent',
    margin: 0,
    padding: 0,
    border: 0,
    outline: 0,
    width: 20,
    height: 20,
    borderRadius: '50%',
    '&:hover': {
      background: '#d3d3d36e',
    },
    '&:focus, &:active': {
      background: 'lightgrey',
    },
    transition: 'background 200ms ease-in-out',
  },
});

type Props = React.ComponentProps<'button'> & {
  passRef?: React.Ref<HTMLButtonElement>;
};

export const ControlsButton: React.FC<Props> = ({
  children,
  className,
  passRef,
  ...props
}) => {
  const classes = useStyles();
  return (
    <button
      className={clsx(classes.button, className)}
      ref={passRef}
      {...props}
    >
      {children}
    </button>
  );
};
