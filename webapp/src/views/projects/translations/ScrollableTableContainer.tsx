import React from 'react';
import { Portal, styled } from '@mui/material';
import { ChevronLeft, ChevronRight } from '@untitled-ui/icons-react';
import clsx from 'clsx';

const ARROW_SIZE = 50;

const StyledContainer = styled('div')`
  position: relative;
  display: grid;
  background: ${({ theme }) => theme.palette.background.default};
  flex-grow: 1;

  &::before {
    content: '';
    height: 100%;
    position: absolute;
    width: 6px;
    background-image: linear-gradient(90deg, #0000002c, transparent);
    top: 0px;
    left: 0px;
    z-index: 10;
    pointer-events: none;
    opacity: 0;
    transition: opacity 100ms ease-in-out;
  }

  &::after {
    content: '';
    height: 100%;
    position: absolute;
    width: 6px;
    background-image: linear-gradient(-90deg, #0000002c, transparent);
    top: 0px;
    right: 0px;
    z-index: 10;
    pointer-events: none;
    opacity: 0;
    transition: opacity 100ms ease-in-out;
  }

  &.scrollLeft {
    &::before {
      opacity: 1;
    }
  }

  &.scrollRight {
    &::after {
      opacity: 1;
    }
  }
`;

const StyledVerticalScroll = styled('div')`
  overflow-x: auto;
  scrollbar-width: none;
  overflow-y: hidden;
  scroll-behavior: smooth;
`;

const StyledScrollArrow = styled('div')`
  position: fixed;
  top: 50vh;
  width: ${ARROW_SIZE / 2}px;
  height: ${ARROW_SIZE}px;
  z-index: 5;
  cursor: pointer;
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  background: ${({ theme }) => theme.palette.background.default};
  opacity: 0;
  transition: opacity 150ms ease-in-out;
  pointer-events: none;

  display: flex;
  align-items: center;
  justify-content: center;

  &.right {
    border-radius: ${ARROW_SIZE}px 0px 0px ${ARROW_SIZE}px;
    padding-left: 4px;
    border-right: none;
  }
  &.left {
    border-radius: 0px ${ARROW_SIZE}px ${ARROW_SIZE}px 0px;
    padding-right: 4px;
    border-left: none;
  }
  &.scrollLeft {
    opacity: 1;
    pointer-events: all;
  }
  &.scrollRight {
    opacity: 1;
    pointer-events: all;
  }
`;

type Props = {
  tableRef: React.RefObject<HTMLDivElement>;
  verticalScrollRef: React.RefObject<HTMLDivElement>;
  scrollLeft: boolean;
  scrollRight: boolean;
  tablePosition: { left: number; right: number };
  hasMinimalHeight: boolean;
  handleHorizontalScroll: (direction: 'left' | 'right') => void;
  'data-cy'?: string;
  className?: string;
  children: React.ReactNode;
};

export const ScrollableTableContainer: React.FC<Props> = ({
  tableRef,
  verticalScrollRef,
  scrollLeft,
  scrollRight,
  tablePosition,
  hasMinimalHeight,
  handleHorizontalScroll,
  'data-cy': dataCy,
  className,
  children,
}) => {
  return (
    <StyledContainer
      data-cy={dataCy}
      className={clsx(className, { scrollLeft, scrollRight })}
      ref={tableRef}
    >
      {hasMinimalHeight && (
        <Portal>
          <StyledScrollArrow
            className={clsx('right', { scrollRight })}
            style={{ right: tablePosition.right }}
            onClick={() => handleHorizontalScroll('right')}
          >
            <ChevronRight width={20} height={20} />
          </StyledScrollArrow>
          <StyledScrollArrow
            className={clsx('left', { scrollLeft })}
            style={{ left: tablePosition.left }}
            onClick={() => handleHorizontalScroll('left')}
          >
            <ChevronLeft width={20} height={20} />
          </StyledScrollArrow>
        </Portal>
      )}
      <StyledVerticalScroll ref={verticalScrollRef}>
        {children}
      </StyledVerticalScroll>
    </StyledContainer>
  );
};
