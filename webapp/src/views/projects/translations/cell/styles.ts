import { keyframes, styled } from '@mui/material';

import { TOP_BAR_HEIGHT } from 'tg.component/layout/TopBar/TopBar';

export type PositionType = 'left' | 'right';

export const NAMESPACE_BANNER_SPACING = 8;

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

const getOpacityAnimation = (start: number, end: number) => keyframes`
  0% {
    opacity: ${start};
  }
  100% {
    opacity: ${end};
  }
  `;

const easeIn = getOpacityAnimation(0, 1);
const highlightIn = getOpacityAnimation(0.5, 1);

export const CELL_PLAIN = 'cellPlain';
export const CELL_STATE = 'cellState';
export const CELL_HOVER = 'cellHover';
export const CELL_RAISED = 'cellRaised';
export const CELL_SELECTED = 'cellSelected';
export const CELL_SHOW_ON_HOVER = 'cellShowOnHover';
export const CELL_HIGHLIGHT_ON_HOVER = 'cellhighlightOnHover';
export const CELL_CLICKABLE = 'cellClickable';
export const CELL_SPACE_TOP = 'cellSpaceTop';
export const CELL_SPACE_BOTTOM = 'cellSpaceBottom';

const combine = (first: string, second: string) =>
  `${first}.${second}, ${first} .${second}`;

export const StyledCell = styled('div')<{ position?: PositionType }>`
  ${combine('&', CELL_PLAIN)} {
    scroll-margin-top: ${TOP_BAR_HEIGHT}px;
    position: relative;
    outline: 0;

    &:hover .${CELL_SHOW_ON_HOVER} {
      opacity: 1;
      animation: ${easeIn} 0.4s ease-in;
    }

    &:focus-within .${CELL_SHOW_ON_HOVER} {
      opacity: 1;
      animation-name: none;
    }

    &:hover .${CELL_HIGHLIGHT_ON_HOVER} {
      opacity: 1;
      animation: ${highlightIn} 0.4s ease-in;
    }
    &:focus-within .${CELL_HIGHLIGHT_ON_HOVER} {
      opacity: 1;
      animation-name: none;
    }
    &:focus-within {
      background: ${({ position, theme }) =>
        getCellGradientBackground(position, theme.palette.emphasis[100])};
    }
  }

  ${combine('&', CELL_CLICKABLE)} {
    cursor: pointer;
  }

  ${combine('&', CELL_HOVER)} {
    background: transparent;
    transition: background 0.1s ease-out;
    &:hover {
      background: ${({ position, theme }) =>
        getCellGradientBackground(position, theme.palette.emphasis[50])};
      transition: background 0.1s ease-in;
    }
  }

  ${combine('&', CELL_RAISED)} {
    z-index: 1;
    background: transparent !important;
    box-shadow: ${({ theme }) =>
      theme.palette.mode === 'dark'
        ? '0px 0px 7px rgba(0, 0, 0, 1)'
        : '0px 0px 10px rgba(0, 0, 0, 0.2)'} !important;
  }

  ${combine('&', CELL_SELECTED)} {
    background: ${({ theme }) => theme.palette.cellSelected1.main} !important;
  }

  ${combine('&', CELL_STATE)} {
    cursor: 'col-resize';
  }

  ${combine('&', CELL_SHOW_ON_HOVER)} {
    opacity: 0;
    transition: opacity 0.1s ease-out;
    &:focus {
      opacity: 1;
    }
  }

  ${combine('&', CELL_HIGHLIGHT_ON_HOVER)} {
    opacity: 0.5;
    transition: opacity 0.1s ease-out;
    &:focus {
      opacity: 1;
    }
  }

  ${combine('&', CELL_SPACE_TOP)} {
    padding-top: ${NAMESPACE_BANNER_SPACING}px;
  }

  ${combine('&', CELL_SPACE_BOTTOM)} {
    padding-bottom: ${NAMESPACE_BANNER_SPACING}px;
  }
`;
