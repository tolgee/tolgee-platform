import React, { useEffect } from 'react';
import { makeStyles, Portal } from '@material-ui/core';
import { useWindowDimensions } from 'tg.hooks/useWindowDimensions';
import clsx from 'clsx';
import { useBottomPanelSetters } from './BottomPanelContext';

const useStyles = makeStyles((theme) => ({
  '@keyframes fadeIn': {
    '0%': {
      opacity: 0,
      transform: 'translateY(100%)',
    },
    '100%': {
      opacity: 1,
      transform: 'translateY(0%)',
    },
  },
  animateIn: {
    opacity: 1,
    animationName: '$fadeIn',
    animationIterationCount: 1,
    animationTimingFunction: 'ease-in-out',
    animationDuration: '0.2s',
  },
  popper: {
    zIndex: theme.zIndex.modal,
    position: 'fixed',
    bottom: 0,
    left: 0,
    right: 0,
  },
  popperContent: {
    display: 'flex',
    background: 'white',
    boxShadow: theme.shadows[10],
  },
}));

type Props = {
  children: (width: number) => React.ReactNode;
  height: number;
};

export const BottomPanel: React.FC<Props> = ({ children, height }) => {
  const classes = useStyles();

  const { width } = useWindowDimensions();

  const { setHeight } = useBottomPanelSetters();

  useEffect(() => {
    setHeight(height);
    return () => setHeight(0);
  }, [height]);

  return (
    <Portal>
      <div className={clsx(classes.popper, classes.animateIn)} role="dialog">
        <div className={classes.popperContent} style={{ height }}>
          {children(width)}
        </div>
      </div>
    </Portal>
  );
};
