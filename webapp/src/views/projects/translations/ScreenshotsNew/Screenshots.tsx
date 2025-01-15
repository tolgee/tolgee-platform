import { useRef, useState } from 'react';
import { Box, styled, SxProps } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import { useResizeObserver } from 'usehooks-ts';

import { ScreenshotThumbnail } from './ScreenshotThumbnail';
import { ScreenshotDetail } from '../Screenshots/ScreenshotDetail';
import { ScreenshotProps } from 'tg.component/ScreenshotWithLabels';
import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useTranslationsActions } from '../context/TranslationsContext';

type ScreenshotModel = components['schemas']['ScreenshotModel'];

const MAX_SIZE = 350;
const MIN_SIZE = 100;

const StyledContainer = styled(Box)`
  display: grid;
  overflow: hidden;
`;

const StyledScrollWrapper = styled(Box)`
  padding: 0px 12px;
  padding-top: 12px;
  overflow-y: auto;
  display: grid;
  grid-auto-flow: column;
  justify-content: start;
  gap: 4px;
`;

type Props = {
  screenshots: ScreenshotModel[];
  keyId: number;
  screenshotMaxWidth?: number;
  sx?: SxProps;
};

export const Screenshots = ({ screenshots, keyId, sx }: Props) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [size, setSize] = useState(0);

  useResizeObserver({
    ref: containerRef,
    onResize(size) {
      if (size.width) {
        setSize(Math.floor(size.width / 50) * 50);
      }
    },
  });

  const project = useProject();
  let boundedSize: number | undefined = undefined;
  if (size) {
    boundedSize = Math.max(Math.min(MAX_SIZE, size), MIN_SIZE) - 24;
    if (boundedSize < MIN_SIZE) {
      boundedSize = undefined;
    }
  }
  const { updateScreenshots } = useTranslationsActions();

  const oneOnly = screenshots.length === 1 && boundedSize;
  const [detailData, setDetailData] = useState<ScreenshotProps>();

  const deleteLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/keys/{keyId}/screenshots/{ids}',
    method: 'delete',
  });

  const handleDelete = (id: number) => {
    deleteLoadable.mutate(
      {
        path: { projectId: project.id, ids: [id], keyId },
      },
      {
        onSuccess() {
          updateScreenshots({
            keyId,
            screenshots(data) {
              return data.filter((sc) => sc.id !== id);
            },
          });
        },
      }
    );
  };

  const calculatedHeight = Math.min(
    Math.max(...screenshots.map((sc) => 100 / (sc.width! / sc.height!))),
    100
  );

  return (
    <StyledContainer {...{ sx }} onClick={stopAndPrevent()} ref={containerRef}>
      <StyledScrollWrapper>
        {screenshots.map((sc) => {
          const screenshot = {
            src: sc.fileUrl,
            width: sc.width,
            height: sc.height,
            highlightedKeyId: keyId,
            keyReferences: sc.keyReferences,
          };
          return (
            <ScreenshotThumbnail
              key={sc.id}
              screenshot={screenshot}
              objectFit={oneOnly ? 'cover' : 'contain'}
              onClick={() => {
                setDetailData(screenshot);
              }}
              onDelete={() => {
                handleDelete(sc.id);
              }}
              sx={{
                width: oneOnly ? boundedSize : 100,
                height:
                  oneOnly && boundedSize
                    ? boundedSize / (sc.width! / sc.height!)
                    : calculatedHeight,
              }}
            />
          );
        })}
      </StyledScrollWrapper>
      {Boolean(detailData) && (
        <ScreenshotDetail
          open={Boolean(detailData)}
          screenshot={detailData}
          onClose={() => setDetailData(undefined)}
          highlightedKeyId={keyId}
        />
      )}
    </StyledContainer>
  );
};
