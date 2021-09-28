import { makeStyles, Theme, colors } from '@material-ui/core';

export type PositionType = 'left' | 'right';

const getCellGradientBackground = (
  position: PositionType | undefined,
  color: string
) => {
  return position
    ? `linear-gradient(${
        position === 'right' ? '-90deg' : '90deg'
      }, ${color}00 0%, ${color}ff 5px, ${color}ff 100%)`
    : color;
};

export const useCellStyles = makeStyles<Theme, { position?: PositionType }>(
  (theme) => ({
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
      outline: 0,
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
      '&:focus-within': {
        background: ({ position }) =>
          getCellGradientBackground(
            position,
            theme.palette.extraLightBackground.main
          ),
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
        background: ({ position }) =>
          getCellGradientBackground(position, colors.grey[50]),
        transition: 'background 0.1s ease-in',
      },
    },
    showOnHover: {
      '&:focus': {
        opacity: 1,
      },
    },
    cellRaised: {
      background: 'transparent !important',
      '-webkit-box-shadow': '0px 0px 10px rgba(0, 0, 0, 0.2) !important',
      'box-shadow': '0px 0px 10px rgba(0, 0, 0, 0.2) !important',
      zIndex: 1,
    },
    cellSelected: {
      background: '#efefef',
    },
  })
);
