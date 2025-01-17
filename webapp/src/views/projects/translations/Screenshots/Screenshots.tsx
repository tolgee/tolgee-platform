import { useRef, useState } from 'react';
import { Box, styled, SxProps } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';

import { ScreenshotThumbnail } from './ScreenshotThumbnail';
import { ScreenshotDetail } from './ScreenshotDetail';
import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useTranslationsActions } from '../context/TranslationsContext';
import { useScrollStatus } from '../TranslationsTable/useScrollStatus';
import clsx from 'clsx';
import { ChevronLeft, ChevronRight } from '@untitled-ui/icons-react';

export const MAX_FILE_COUNT = 20;
export const ALLOWED_UPLOAD_TYPES = ['image/png', 'image/jpeg', 'image/gif'];

type ScreenshotModel = components['schemas']['ScreenshotModel'];

const MAX_SIZE = 350;
const MIN_SIZE = 100;

const MAX_HEIGHT = 350;

const StyledContainer = styled(Box)`
  display: grid;
  overflow: hidden;
  margin: 0px 6px;
  position: relative;
  --cell-background: ${({ theme }) => theme.palette.background.default};

  &::before,
  &::after {
    content: '';
    height: 100%;
    position: absolute;
    width: 6px;
    z-index: 10;
    pointer-events: none;
    opacity: 0;
    transition: opacity 100ms ease-in-out;
    top: 12px;
  }

  &::before {
    background-image: linear-gradient(
      90deg,
      var(--cell-background),
      transparent
    );
    left: -1px;
  }

  &::after {
    background-image: linear-gradient(
      -90deg,
      var(--cell-background),
      transparent
    );
    right: -1px;
  }

  &.scrollLeft {
    &::before {
      opacity: 1;
    }
  }

  &.scrollRight {
    &::after {
      opacity: 1;
    }
  }

  & .arrowLeft,
  & .arrowRight {
    position: absolute;
    top: calc(50% - 4px);
    width: 20px;
    height: 20px;
    border-radius: 50%;
    display: grid;
    align-content: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.4);
    opacity: 0;
    transition: opacity 0.2s ease-in-out;
  }

  &:hover .arrowLeft,
  &:hover .arrowRight {
    opacity: 1;
  }

  & .arrowLeft {
    left: 2px;
  }

  & .arrowRight {
    right: 2px;
  }
`;

const StyledScrollWrapper = styled(Box)`
  padding: 0px 6px;
  padding-top: 12px;
  overflow-y: auto;
  display: grid;
  grid-auto-flow: column;
  justify-content: start;
  gap: 4px;
  scrollbar-width: none;
  scroll-snap-type: x mandatory;
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

  const [scrollLeft, scrollRight] = useScrollStatus(containerRef, [
    width,
    screenshots,
  ]);

  function handleScrollLeft() {
    const element = containerRef.current;
    if (element) {
      const scrollLeft = element?.scrollLeft;
      element?.scroll({ left: scrollLeft - 100, behavior: 'smooth' });
    }
  }

  function handleScrollRight() {
    const element = containerRef.current;
    if (element) {
      const scrollLeft = element?.scrollLeft;
      element?.scroll({ left: scrollLeft + 100, behavior: 'smooth' });
    }
  }

  return (
    <StyledContainer
      {...{ sx }}
      className={clsx({ scrollLeft, scrollRight })}
      onClick={stopAndPrevent()}
    >
      <StyledScrollWrapper ref={containerRef}>
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
                scrollSnapAlign: 'center',
              }}
            />
          );
        })}
      </StyledScrollWrapper>
      {scrollLeft && (
        <Box className="arrowLeft" onClick={handleScrollLeft} role="button">
          <ChevronLeft width={18} height={18} />
        </Box>
      )}
      {scrollRight && (
        <Box className="arrowRight" onClick={handleScrollRight} role="button">
          <ChevronRight width={18} height={18} />
        </Box>
      )}
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
