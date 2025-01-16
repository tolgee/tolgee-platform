import { Tooltip, useTheme } from '@mui/material';
import { CSSProperties } from 'react';

import { components } from 'tg.service/apiSchema.generated';

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
};

export const ScreenshotWithLabels: React.FC<Props> = ({
  screenshot,
  showTooltips,
  objectFit = 'contain',
  scaleHighlight = 1,
  showSecondaryHighlights = false,
  className,
  style,
}) => {
  const strokeWidth = STROKE_WIDTH * scaleHighlight;
  const theme = useTheme();

  return (
    <svg
      viewBox={`0 0 ${screenshot.width} ${screenshot.height}`}
      className={className}
      style={{
        width: screenshot.width,
        maxWidth: '100%',
        ...style,
      }}
      preserveAspectRatio={objectFit === 'cover' ? 'xMinYMin slice' : ''}
      data-cy="screenshot-image"
    >
      <image
        href={screenshot.src}
        width={screenshot.width}
        height={screenshot.height}
      />
      {screenshot.keyReferences
        ?.filter(
          (key) =>
            showSecondaryHighlights || key.keyId === screenshot.highlightedKeyId
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
    </svg>
  );
};
