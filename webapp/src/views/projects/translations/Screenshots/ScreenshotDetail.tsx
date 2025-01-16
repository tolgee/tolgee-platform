import React, { useState } from 'react';
import { Dialog, IconButton, styled } from '@mui/material';

import {
  ScreenshotProps,
  ScreenshotWithLabels,
} from 'tg.component/ScreenshotWithLabels';
import { ChevronLeft, ChevronRight } from '@untitled-ui/icons-react';

const StyledContainer = styled('div')`
  display: grid;
  align-content: center;
  justify-content: center;
  max-height: 80vh;
  max-width: 85vw;
  position: relative;
`;

const StyledArrowButton = styled(IconButton)`
  position: fixed;
  top: calc(50% - 24px);
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
          <StyledArrowButton
            size="large"
            style={{ left: 20 }}
            onClick={moveLeft}
          >
            <ChevronLeft />
          </StyledArrowButton>
        )}
        {screenshot && (
          <ScreenshotWithLabels
            key={screenshot.id}
            showTooltips
            screenshot={screenshot}
            showSecondaryHighlights
            objectFit="contain"
            style={{ maxHeight: '100%' }}
          />
        )}
        {multiple && (
          <StyledArrowButton
            size="large"
            style={{ right: 20 }}
            onClick={moveRight}
          >
            <ChevronRight />
          </StyledArrowButton>
        )}
      </StyledContainer>
    </Dialog>
  );
};
