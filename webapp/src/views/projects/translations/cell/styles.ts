import { colors, makeStyles, Theme } from '@material-ui/core';
import { TOP_BAR_HEIGHT } from 'tg.component/layout/TopBar';

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

const opacityAnimation = (start: number, end: number) => ({
  '0%': {
    opacity: start,
  },
  '100%': {
    opacity: end,
  },
});

const showWithAnimation = (name: string) => ({
  opacity: 1,
  animationName: '$' + name,
  animationDuration: '0.4s',
  animationTimingFunction: 'ease-in',
});

const showNoAnimation = () => ({
  opacity: 1,
  animationName: 'none',
});

export const useCellStyles = makeStyles<Theme, { position?: PositionType }>(
  (theme) => ({
    '@keyframes easeIn': opacityAnimation(0, 1),
    '@keyframes highlightIn': opacityAnimation(0.5, 1),

    cellPlain: {
      scrollMarginTop: `${TOP_BAR_HEIGHT}px`,
      position: 'relative',
      outline: 0,
      '&:hover $showOnHover': showWithAnimation('easeIn'),
      '&:focus-within $showOnHover': showNoAnimation(),
      '&:hover $highlightOnHover': showWithAnimation('highlightIn'),
      '&:focus-within $highlightOnHover': showNoAnimation(),
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
      opacity: 0,
      transition: 'opacity 0.1s ease-out',
      '&:focus': {
        opacity: 1,
      },
    },
    highlightOnHover: {
      opacity: 0.5,
      transition: 'opacity 0.1s ease-out',
      '&:focus': {
        opacity: 1,
      },
    },
    cellRaised: {
      background: 'transparent !important',
      '-webkit-box-shadow': '0px 0px 10px rgba(0, 0, 0, 0.2) !important',
      'box-shadow': '0px 0px 10px rgba(0, 0, 0, 0.2) !important',
    },
    cellSelected: {
      background: '#efefef',
    },
  })
);
