import { makeStyles } from '@material-ui/core';

export const useCellStyles = makeStyles((theme) => ({
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
    '&:hover': {
      background: theme.palette.grey[50],
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
}));
