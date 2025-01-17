import React, { useState } from 'react';
import { Dialog, IconButton, styled } from '@mui/material';

import {
  ScreenshotProps,
  ScreenshotWithLabels,
} from 'tg.component/ScreenshotWithLabels';
import { ChevronLeft, ChevronRight } from '@untitled-ui/icons-react';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

const StyledContainer = styled('div')`
  display: grid;
  align-content: center;
  justify-content: center;
  max-height: 80vh;
  max-width: 85vw;
  position: relative;

  &::after {
    content: '';
    position: absolute;
    top: 0px;
    left: 0px;
    right: 0px;
    bottom: 0px;
    border-radius: 4px;
    border: 1px solid ${({ theme }) => theme.palette.tokens.border.primary};
    pointer-events: none;
  }

  & .arrow {
    position: absolute;
    top: calc(50% - 17px);
    backdrop-filter: blur(0.1px);
    color: ${({ theme }) => theme.palette.tokens.icon.onDarkHover};
    &::before {
      content: '';
      position: absolute;
      top: 0px;
      left: 0px;
      bottom: 0px;
      right: 0px;
      background: ${({ theme }) =>
        theme.palette.tokens.icon.backgroundDarkHover};
      z-index: -1;
      border-radius: 50%;
      opacity: 0.6;
      transition: opacity 0.2s ease-in-out;
    }
    opacity: 0;
    transition: opacity 0.2s ease-in-out;
  }

  &:hover .arrow {
    opacity: 1;
  }

  & .arrow:hover::before {
    opacity: 0.7;
  }
`;

interface ScreenshotDetailProps {
  onClose: () => void;
  screenshots: ScreenshotProps[];
  initialIndex: number;
}

export const ScreenshotDetail: React.FC<ScreenshotDetailProps> = ({
  onClose,
  screenshots,
  initialIndex,
}) => {
  const [index, setIndex] = useState(initialIndex);
  const itemsCount = screenshots.length;
  const screenshot = screenshots[index];
  const multiple = screenshots.length > 1;

  const bodyWidth = useGlobalContext((c) => c.layout.bodyWidth);

  let scaleMarkers = 1;
  if (bodyWidth < screenshot.width!) {
    scaleMarkers = screenshot.width! / bodyWidth;
  }

  function moveLeft() {
    setIndex((itemsCount + index - 1) % itemsCount);
  }

  function moveRight() {
    setIndex((index + 1) % itemsCount);
  }

  const handleKeyPress: React.KeyboardEventHandler<HTMLDivElement> = (e) => {
    if (e.key === 'ArrowLeft') {
      moveLeft();
    } else if (e.key === 'ArrowRight') {
      moveRight();
    }
  };

  return (
    <Dialog onClose={onClose} open maxWidth="xl" onKeyDown={handleKeyPress}>
      <StyledContainer>
        {multiple && (
          <IconButton
            className="arrow"
            size="small"
            style={{ left: 8 }}
            onClick={moveLeft}
          >
            <ChevronLeft />
          </IconButton>
        )}
        {screenshot && (
          <ScreenshotWithLabels
            key={screenshot.id}
            showTooltips
            screenshot={screenshot}
            showSecondaryHighlights
            objectFit="contain"
            style={{ maxHeight: '100%' }}
            scaleHighlight={scaleMarkers}
          />
        )}
        {multiple && (
          <IconButton
            className="arrow"
            size="small"
            style={{ right: 8 }}
            onClick={moveRight}
          >
            <ChevronRight />
          </IconButton>
        )}
      </StyledContainer>
    </Dialog>
  );
};
