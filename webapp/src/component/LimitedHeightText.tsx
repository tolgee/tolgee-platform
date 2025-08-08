import React, { useState, useRef, useEffect, RefObject } from 'react';
import { Popper, keyframes, styled } from '@mui/material';
import { useTimer } from '../fixtures/useTimer';
import { getEffectiveBackgroundColor } from 'tg.fixtures/getEffectiveElementBackground';

const fadeIn = keyframes`
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
`;

const StyledContainer = styled('div')`
  position: relative;
  display: grid;
  animation: ${fadeIn} 0.1s ease-in-out;
  overflow: hidden;

  & .text {
    overflow: hidden;
    // Adds a hyphen where the word breaks
    -ms-hyphens: auto;
    -moz-hyphens: auto;
    -webkit-hyphens: 'auto';
    hyphens: auto;
  }
`;

const StyledOverlay = styled('div')`
  position: relative;
  box-sizing: content-box;
  z-index: ${({ theme }) => theme.zIndex.tooltip};
  box-shadow: '0px 0px 5px 2px rgba(0,0,0,0.1)';
`;

type Props = {
  maxLines?: number | undefined;
  lang?: string;
  wrap?: 'break-word' | 'break-all';
  width?: number | string;
  overlayPadding?: number;
  lineHeight?: string;
  overlay?: boolean;
};

export const LimitedHeightText: React.FC<Props> = ({
  maxLines,
  children,
  lang,
  wrap = 'break-word',
  width,
  overlayPadding = 8,
  lineHeight = '1.2em',
  overlay = true,
}) => {
  const textRef = useRef<HTMLDivElement>();
  const [expandable, setExpandable] = useState<boolean>(false);
  const [overlayOpen, setOverlayOpen] = useState(false);

  const detectExpandability = () => {
    const textElement = textRef.current;
    if (textElement != null) {
      // Use a 2px threshold to account for browser rounding differences
      const heightDifference =
        textElement.scrollHeight - textElement.clientHeight;
      setExpandable(heightDifference > 2);
    }
  };

  const overlayEnabled = expandable && overlay;

  const { clearTimer, reStartTimer } = useTimer({
    callback: () => setOverlayOpen(true),
    delay: 1000,
    enabled: overlayEnabled && !overlayOpen,
  });

  const handleLeave = () => {
    clearTimer();
    setOverlayOpen(false);
  };

  useEffect(() => {
    detectExpandability();
  });

  const gradient = expandable
    ? `linear-gradient(to top, rgba(0,0,0,0) 0%, rgba(0,0,0,0.87) ${lineHeight}, rgba(0,0,0,0.87) ${
        100 / (maxLines || 100)
      }%, black 100%)`
    : undefined;

  return (
    <StyledContainer
      className="text"
      ref={textRef as RefObject<HTMLDivElement>}
      // when moving mouse, reinitialize timer
      // so it only fires when you stop the mouse
      onMouseMove={reStartTimer}
      onMouseLeave={handleLeave}
      style={{
        maxHeight: maxLines ? `calc(${lineHeight} * ${maxLines})` : undefined,
        WebkitMaskImage: gradient,
        maskImage: gradient,
        wordBreak: wrap,
        lineHeight: lineHeight,
      }}
      lang={lang}
    >
      {children}
      {overlayEnabled && overlayOpen && textRef.current ? (
        <Popper
          open={true}
          anchorEl={textRef.current}
          placement="bottom-start"
          style={{
            pointerEvents: 'none',
          }}
          modifiers={[
            {
              name: 'offset',
              options: {
                offset: ({ reference }) => {
                  return [0, -reference.height];
                },
              },
            },
            {
              name: 'computeStyles',
              options: {
                gpuAcceleration: false,
              },
            },
            {
              name: 'preventOverflow',
              enabled: true,
              options: {
                padding: 0,
              },
            },
          ]}
        >
          <StyledOverlay
            lang={lang}
            className="text"
            style={{
              width: textRef.current?.clientWidth + 'px',
              background: getEffectiveBackgroundColor(textRef.current),
              color: window.getComputedStyle(textRef.current).color,
              wordBreak: wrap,
              top: -overlayPadding,
              left: -overlayPadding,
              padding: overlayPadding,
              lineHeight: lineHeight,
              fontSize: window.getComputedStyle(textRef.current).fontSize,
            }}
          >
            {children}
          </StyledOverlay>
        </Popper>
      ) : null}
    </StyledContainer>
  );
};
