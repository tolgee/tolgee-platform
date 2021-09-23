import React, { useState, useRef, useEffect, RefObject } from 'react';
import { makeStyles, Popper } from '@material-ui/core';
import clsx from 'clsx';
import { useTimer } from './useTimer';

function getInheritedBackgroundColor(el) {
  // get default style for current browser
  const defaultStyle = getDefaultBackground(); // typically "rgba(0, 0, 0, 0)"

  // get computed color for el
  const backgroundColor = window.getComputedStyle(el).backgroundColor;

  // if we got a real value, return it
  if (backgroundColor != defaultStyle) return backgroundColor;

  // if we've reached the top parent el without getting an explicit color, return default
  if (!el.parentElement) return defaultStyle;

  // otherwise, recurse and try again on parent element
  return getInheritedBackgroundColor(el.parentElement);
}

function getDefaultBackground() {
  // have to add to the document in order to use getComputedStyle
  const div = document.createElement('div');
  document.head.appendChild(div);
  const bg = window.getComputedStyle(div).backgroundColor;
  document.head.removeChild(div);
  return bg;
}

const useStyles = makeStyles((theme) => ({
  '@keyframes fadeIn': {
    from: { opacity: 0 },
    to: { opacity: 1 },
  },
  container: {
    position: 'relative',
    display: 'flex',
    animationName: '$fadeIn',
    animationDuration: '0.1s',
    animationTimingFunction: 'ease-in-out',
  },
  overlay: {
    position: 'relative',
    boxSizing: 'content-box',
    zIndex: theme.zIndex.tooltip,
    '-webkit-box-shadow': '0px 0px 5px 2px rgba(0,0,0,0.1)',
    'box-shadow': '0px 0px 5px 2px rgba(0,0,0,0.1)',
  },
  text: {
    overflow: 'hidden',
    // Adds a hyphen where the word breaks
    '-ms-hyphens': 'auto',
    '-moz-hyphens': 'auto',
    '-webkit-hyphens': 'auto',
    hyphens: 'auto',
  },
}));

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
  lineHeight = '1.2rem',
  overlay = true,
}) => {
  const classes = useStyles();
  const textRef = useRef<HTMLDivElement>();
  const [expandable, setExpandable] = useState<boolean>(false);
  const [overlayOpen, setOverlayOpen] = useState(false);

  const detectExpandability = () => {
    const textElement = textRef.current;
    if (textElement != null) {
      setExpandable(textElement.clientHeight < textElement.scrollHeight);
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
    <div
      className={clsx(classes.container, classes.text)}
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
          modifiers={{
            offset: { offset: '0, -100%p' },
            computeStyle: {
              gpuAcceleration: false,
            },
            preventOverflow: {
              enabled: true,
              padding: 0,
            },
          }}
        >
          <div
            lang={lang}
            className={clsx(classes.text, classes.overlay)}
            style={{
              width: textRef.current?.clientWidth + 'px',
              background: getInheritedBackgroundColor(textRef.current),
              color: window.getComputedStyle(textRef.current).color,
              wordBreak: wrap,
              top: -overlayPadding,
              left: -overlayPadding,
              padding: overlayPadding,
              lineHeight: lineHeight,
            }}
          >
            {children}
          </div>
        </Popper>
      ) : null}
    </div>
  );
};
