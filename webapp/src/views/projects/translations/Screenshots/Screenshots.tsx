import { useRef, useState } from 'react';
import { Box, styled, SxProps } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';

import { ScreenshotThumbnail } from './ScreenshotThumbnail';
import { ScreenshotDetail } from './ScreenshotDetail';
import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useTranslationsActions } from '../context/TranslationsContext';

export const MAX_FILE_COUNT = 20;
export const ALLOWED_UPLOAD_TYPES = ['image/png', 'image/jpeg', 'image/gif'];

type ScreenshotModel = components['schemas']['ScreenshotModel'];

const MAX_SIZE = 350;
const MIN_SIZE = 100;

const MAX_HEIGHT = 350;

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
  oneBig?: boolean;
  width?: number;
  sx?: SxProps;
};

export const Screenshots = ({
  screenshots,
  keyId,
  oneBig,
  width,
  sx,
}: Props) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const size = width && Math.floor(width / 50) * 50;

  const project = useProject();
  let boundedSize: number | undefined = undefined;
  if (size) {
    boundedSize = Math.max(Math.min(MAX_SIZE, size), MIN_SIZE) - 24;
    if (boundedSize < MIN_SIZE) {
      boundedSize = undefined;
    }
  }
  const { updateScreenshots } = useTranslationsActions();

  const oneOnly = screenshots.length === 1 && boundedSize && oneBig;
  const [detailData, setDetailData] = useState<number>();

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

  const screenshotsMapped = screenshots.map((sc) => {
    return {
      id: sc.id,
      src: sc.fileUrl,
      width: sc.width,
      height: sc.height,
      highlightedKeyId: keyId,
      keyReferences: sc.keyReferences,
    };
  });

  return (
    <StyledContainer {...{ sx }} onClick={stopAndPrevent()} ref={containerRef}>
      <StyledScrollWrapper>
        {screenshots.map((sc, index) => {
          let width =
            oneOnly && boundedSize ? Math.min(boundedSize, sc.width!) : 100;
          let height =
            oneOnly && boundedSize
              ? width / (sc.width! / sc.height!)
              : calculatedHeight;

          if (height > MAX_HEIGHT && oneOnly) {
            height = MAX_HEIGHT;
            width = height * (sc.width! / sc.height!);
          }

          const isLarge = height > 100 || width > 100;

          const screenshot = {
            id: sc.id,
            src: isLarge ? sc.fileUrl : sc.thumbnailUrl,
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
              scaleHighlight={sc.width! / width}
              onClick={() => {
                setDetailData(index);
              }}
              onDelete={() => {
                handleDelete(sc.id);
              }}
              sx={{
                width,
                height,
              }}
            />
          );
        })}
      </StyledScrollWrapper>
      {detailData !== undefined && (
        <ScreenshotDetail
          screenshots={screenshotsMapped}
          initialIndex={detailData}
          onClose={() => setDetailData(undefined)}
        />
      )}
    </StyledContainer>
  );
};
