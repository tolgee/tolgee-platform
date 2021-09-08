import { makeStyles, Theme, colors } from '@material-ui/core';

type PositionType = 'left' | 'right';

const getCellGradientBackground = (position?: PositionType) => {
  const color = colors.grey[50];
  return position
    ? `linear-gradient(${
        position === 'right' ? '-90deg' : '90deg'
      }, ${color}00 0%, ${color}ff 5px, ${color}ff 100%)`
    : color;
};

export const useCellStyles = makeStyles<Theme, { position?: PositionType }>({
  '@keyframes easeIn': {
    '0%': {
      opacity: 0,
    },
    '100%': {
      opacity: 1,
    },
  },
  '@keyframes easeOut': {
    '0%': {
      opacity: 1,
    },
    '100%': {
      opacity: 0,
    },
  },
  cellPlain: {
    '& $showOnHover': {
      opacity: 0,
      transition: 'opacity 0.1s ease-out',
    },
    '&:hover $showOnHover': {
      opacity: 1,
      animationName: '$easeIn',
      animationDuration: '0.4s',
      animationTimingFunction: 'ease-in',
    },
    '&:focus-within $showOnHover': {
      opacity: 1,
      animationName: 'none',
    },
  },
  cellClickable: {
    cursor: 'pointer',
  },
  state: {
    cursor: 'col-resize',
  },
  hover: {
    background: 'transparent',
    transition: 'background 0.1s ease-out',
    '&:hover': {
      background: ({ position }) => getCellGradientBackground(position),
      transition: 'background 0.1s ease-in',
    },
  },
  showOnHover: {
    '&:focus': {
      opacity: 1,
    },
  },
  cellRaised: {
    '-webkit-box-shadow': '0px 0px 10px rgba(0, 0, 0, 0.2)',
    'box-shadow': '0px 0px 10px rgba(0, 0, 0, 0.2)',
    zIndex: 1,
  },
  cellSelected: {
    background: '#efefef',
  },
});
