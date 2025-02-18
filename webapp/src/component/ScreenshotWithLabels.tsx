/* eslint-disable react/no-unknown-property */
import { Tooltip, useTheme } from '@mui/material';
import { CSSProperties, useState } from 'react';
import { useImagePreload } from 'tg.fixtures/useImagePreload';

import { components } from 'tg.service/apiSchema.generated';
import { isScreenshotExpired } from 'tg.views/projects/translations/Screenshots/useScreenshotLinkCheck';

type KeyInScreenshotModel = components['schemas']['KeyInScreenshotModel'];

const STROKE_WIDTH = 4;

export type ScreenshotProps = {
  id: number;
  src: string;
  width: number | undefined;
  height: number | undefined;
  keyReferences?: KeyInScreenshotModel[];
  highlightedKeyId: number;
};

type Props = {
  screenshot: ScreenshotProps;
  showTooltips?: boolean;
  objectFit?: 'contain' | 'cover';
  scaleHighlight?: number;
  showSecondaryHighlights?: boolean;
  className?: string;
  style?: CSSProperties;
  onSrcExpired: () => void;
};

export const ScreenshotWithLabels: React.FC<Props> = ({
  screenshot,
  showTooltips,
  objectFit = 'contain',
  scaleHighlight = 1,
  showSecondaryHighlights = false,
  className,
  style,
  onSrcExpired,
}) => {
  const strokeWidth = STROKE_WIDTH * scaleHighlight;
  const theme = useTheme();
  const [srcImageExpired, setSrcImageExpired] = useState(false);

  const { size, srcExpired, isLoading } = useImagePreload({
    src: screenshot.src,
    onSrcExpired,
  });

  const ready = !srcExpired && !srcImageExpired && !isLoading;

  const screenshotWidth = screenshot.width || size.width;
  const screenshotHeight = screenshot.height || size.height;

  return (
    <svg
      viewBox={`0 0 ${screenshotWidth} ${screenshotHeight}`}
      className={className}
      style={{
        width: screenshotWidth,
        maxWidth: '100%',
        ...style,
      }}
      preserveAspectRatio={objectFit === 'cover' ? 'xMinYMin slice' : undefined}
      data-cy="screenshot-image"
    >
      {ready && (
        <>
          <image
            href={screenshot.src}
            width={screenshotWidth}
            height={screenshotHeight}
            onLoad={() => {
              setSrcImageExpired(false);
            }}
            onError={() => {
              if (isScreenshotExpired(screenshot.src)) {
                setSrcImageExpired(true);
                onSrcExpired();
              }
            }}
          />
          {screenshot.keyReferences
            ?.filter(
              (key) =>
                showSecondaryHighlights ||
                key.keyId === screenshot.highlightedKeyId
            )
            ?.map((key, i) => {
              if (key.position) {
                const rectangle = (
                  <rect
                    key={i}
                    width={key.position.width + strokeWidth}
                    height={key.position.height + strokeWidth}
                    x={key.position.x - strokeWidth / 2}
                    y={key.position.y - strokeWidth / 2}
                    fill="transparent"
                    stroke={
                      key.keyId === screenshot.highlightedKeyId
                        ? theme.palette.marker.primary
                        : theme.palette.marker.secondary
                    }
                    strokeWidth={strokeWidth}
                    paintOrder="stroke"
                    rx={strokeWidth / 2}
                  />
                );
                if (showTooltips) {
                  return (
                    <Tooltip key={i} title={key.keyName} placement="right">
                      {rectangle}
                    </Tooltip>
                  );
                } else {
                  return rectangle;
                }
              }
              return null;
            })}
        </>
      )}
    </svg>
  );
};
