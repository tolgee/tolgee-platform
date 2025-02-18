import clsx from 'clsx';
import React, { useState } from 'react';
import { Dialog, IconButton, styled } from '@mui/material';

import {
  ScreenshotProps,
  ScreenshotWithLabels,
} from 'tg.component/ScreenshotWithLabels';
import { ChevronLeft, ChevronRight } from '@untitled-ui/icons-react';
import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import { useWindowSize } from 'usehooks-ts';
import { scaleImage, useImagePreload } from 'tg.fixtures/useImagePreload';
import { BoxLoading } from 'tg.component/common/BoxLoading';

const SCREENSHOT_DETAIL_SIZE = 0.8;

const StyledContainer = styled('div')`
  display: grid;
  align-content: center;
  justify-content: center;
  overflow: hidden;
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
  onSrcExpired: () => void;
}

export const ScreenshotDetail: React.FC<ScreenshotDetailProps> = ({
  onClose,
  screenshots,
  initialIndex,
  onSrcExpired,
}) => {
  const [index, setIndex] = useState(initialIndex);
  const itemsCount = screenshots.length;
  const screenshot = screenshots[index];
  const multiple = screenshots.length > 1;
  const viewPort = useWindowSize();
  const maxDialogSize = {
    width: viewPort.width * SCREENSHOT_DETAIL_SIZE,
    height: viewPort.height * SCREENSHOT_DETAIL_SIZE,
  };

  const {
    size: loadedSize,
    srcExpired,
    isLoading,
  } = useImagePreload({
    src: screenshot.src,
    onSrcExpired,
  });

  const screenshotSize = {
    width: screenshot.width || loadedSize.width || 0,
    height: screenshot.height || loadedSize.height || 0,
  };

  const scaledSize = scaleImage(screenshotSize, maxDialogSize);

  const width = scaledSize.width || maxDialogSize.width;
  const height = scaledSize.height || maxDialogSize.height;

  let scaleMarkers = 1;
  if (screenshot.width && width < screenshot.width) {
    scaleMarkers = screenshot.width / width;
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
    <Dialog
      onClose={onClose}
      open
      maxWidth={false}
      onKeyDown={handleKeyPress}
      onClick={stopAndPrevent()}
      PaperProps={{ style: { margin: 0 } }}
    >
      <StyledContainer
        style={{
          width,
          height,
        }}
        className={clsx({ loading: isLoading || srcExpired })}
      >
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
        {isLoading || srcExpired ? (
          <BoxLoading />
        ) : (
          <>
            {screenshot && (
              <ScreenshotWithLabels
                key={screenshot.id}
                showTooltips
                screenshot={{
                  ...screenshot,
                  width: screenshotSize.width,
                  height: screenshotSize.height,
                }}
                showSecondaryHighlights
                style={{
                  width,
                  height,
                  maxWidth: 'unset',
                  maxHeight: 'unset',
                }}
                scaleHighlight={scaleMarkers}
                onSrcExpired={onSrcExpired}
              />
            )}
          </>
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
