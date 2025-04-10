import { useRef, useState } from 'react';
import { Box, styled, SxProps } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import clsx from 'clsx';

import { ScreenshotDetail } from './ScreenshotDetail';
import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import { useScrollStatus } from 'tg.component/common/useScrollStatus';
import { ChevronLeft, ChevronRight } from '@untitled-ui/icons-react';
import { ScreenshotsList } from './ScreenshotsList';
import { useTranslationsActions } from '../context/TranslationsContext';
import { isScreenshotExpired } from './isScreenshotExpired';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';

export const MAX_FILE_COUNT = 20;
export const ALLOWED_UPLOAD_TYPES = ['image/png', 'image/jpeg', 'image/gif'];

type ScreenshotModel = components['schemas']['ScreenshotModel'];

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
    left: 0px;
  }

  &::after {
    background-image: linear-gradient(
      -90deg,
      var(--cell-background),
      transparent
    );
    right: 0px;
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
    top: calc(50% - 6px);
    width: 24px;
    height: 24px;
    border-radius: 50%;
    display: grid;
    align-content: center;
    justify-content: center;
    color: ${({ theme }) => theme.palette.tokens.icon.onDark};
    background-color: ${({ theme }) =>
      theme.palette.tokens.icon.backgroundDark};
    opacity: 0;
    transition: all 0.2s ease-in-out;
    z-index: 1;
  }

  & .arrowLeft {
    left: 4px;
  }
  & .arrowRight {
    right: 4px;
  }

  &:hover .arrowLeft,
  &:hover .arrowRight {
    opacity: 0.6;
  }

  & .arrowLeft:hover,
  & .arrowRight:hover {
    opacity: 0.8;
    color: ${({ theme }) => theme.palette.tokens.icon.onDarkHover};
    background-color: ${({ theme }) =>
      theme.palette.tokens.icon.backgroundDarkHover};
  }
`;

const StyledScrollWrapper = styled(Box)`
  align-self: start;
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
  const { updateScreenshots } = useTranslationsActions();
  const project = useProject();

  const [detailData, setDetailData] = useState<number>();

  const keyScreenshots = useApiQuery({
    url: '/v2/projects/{projectId}/keys/{keyId}/screenshots',
    method: 'get',
    path: { projectId: project.id, keyId },
    options: {
      enabled: false,
      onSuccess(data) {
        updateScreenshots({
          keyId,
          screenshots: data._embedded?.screenshots || [],
        });
      },
    },
  });

  async function handleDetailClick(index: number) {
    setDetailData(index);
    // check if screenshot path is expired when click on detail
    if (isScreenshotExpired(screenshots[index]?.thumbnailUrl)) {
      handleScreenshotsRefetch();
    }
  }

  async function handleScreenshotsRefetch() {
    await keyScreenshots.refetch({ cancelRefetch: false });
  }

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
    <StyledContainer {...{ sx }} className={clsx({ scrollLeft, scrollRight })}>
      <StyledScrollWrapper ref={containerRef}>
        <ScreenshotsList
          width={width}
          keyId={keyId}
          screenshots={screenshots}
          oneBig={oneBig}
          setDetail={handleDetailClick}
          onSrcExpired={handleScreenshotsRefetch}
        />
      </StyledScrollWrapper>
      {scrollLeft && (
        <Box
          className="arrowLeft"
          onClick={stopAndPrevent(handleScrollLeft)}
          role="button"
        >
          <ChevronLeft width={20} height={20} />
        </Box>
      )}
      {scrollRight && (
        <Box
          className="arrowRight"
          onClick={stopAndPrevent(handleScrollRight)}
          role="button"
        >
          <ChevronRight width={20} height={20} />
        </Box>
      )}
      {detailData !== undefined && (
        <ScreenshotDetail
          screenshots={screenshotsMapped}
          initialIndex={detailData}
          onClose={stopAndPrevent(() => setDetailData(undefined))}
          onSrcExpired={handleScreenshotsRefetch}
        />
      )}
    </StyledContainer>
  );
};
