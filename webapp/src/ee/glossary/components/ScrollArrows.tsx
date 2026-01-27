import { Portal, styled, useMediaQuery } from '@mui/material';
import clsx from 'clsx';
import { ChevronLeft, ChevronRight } from '@untitled-ui/icons-react';
import React, { useState } from 'react';
import { useScrollStatus } from 'tg.component/common/useScrollStatus';
import { useResizeObserver } from 'usehooks-ts';
import { useTranslate } from '@tolgee/react';

const ARROW_SIZE = 50;

const StyledScrollArrow = styled('div')`
  position: fixed;
  top: 50vh;
  width: ${ARROW_SIZE / 2}px;
  height: ${ARROW_SIZE}px;
  z-index: ${({ theme }) => theme.zIndex.fab};
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

  &:focus {
    outline: 0.1px solid ${({ theme }) => theme.palette.primary.main};
  }
`;

type Props = {
  containerRef: React.RefObject<HTMLDivElement>;
  verticalScrollRef: React.RefObject<HTMLDivElement>;
  deps?: React.DependencyList | undefined;
};

export const ScrollArrows = ({
  containerRef,
  verticalScrollRef,
  deps = [],
}: Props) => {
  const { t } = useTranslate();

  const hasMinimalHeight = useMediaQuery('(min-height: 400px)');
  const [tablePosition, setTablePosition] = useState({ left: 0, right: 0 });

  useResizeObserver({
    ref: containerRef,
    onResize: () => {
      const position = containerRef.current?.getBoundingClientRect();
      const windowBounds = window.document.body.getBoundingClientRect();
      if (position) {
        const left = position?.left;
        const right = windowBounds.right - position?.right;
        setTablePosition({ left, right });
      }
    },
  });

  const [scrollLeft, scrollRight] = useScrollStatus(verticalScrollRef, [
    tablePosition,
    ...deps,
  ]);

  const handleScroll = (direction: 'left' | 'right') => {
    const element = verticalScrollRef.current;
    if (element) {
      const position = element.scrollLeft;
      element.scrollTo({
        left: position + (direction === 'left' ? -350 : +350),
      });
    }
  };

  if (!hasMinimalHeight) {
    return null;
  }

  return (
    <Portal>
      <StyledScrollArrow
        aria-label={t('scroll_right')}
        tabIndex={scrollRight ? 0 : -1}
        className={clsx('right', { scrollRight })}
        style={{
          right: tablePosition?.right,
        }}
        onClick={() => handleScroll('right')}
      >
        <ChevronRight width={20} height={20} />
      </StyledScrollArrow>
      <StyledScrollArrow
        aria-label={t('scroll_left')}
        tabIndex={scrollLeft ? 0 : -1}
        className={clsx('left', { scrollLeft })}
        style={{
          left: tablePosition?.left,
        }}
        onClick={() => handleScroll('left')}
      >
        <ChevronLeft width={20} height={20} />
      </StyledScrollArrow>
    </Portal>
  );
};
