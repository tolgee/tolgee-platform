import { useState } from 'react';
import { Box, styled, SxProps } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';

import { ScreenshotThumbnail } from './ScreenshotThumbnail';
import { ScreenshotDetail } from '../Screenshots/ScreenshotDetail';
import { ScreenshotProps } from 'tg.component/ScreenshotWithLabels';
import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useTranslationsActions } from '../context/TranslationsContext';

type ScreenshotModel = components['schemas']['ScreenshotModel'];

const MAX_SIZE = 450;
const MIN_SIZE = 150;

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

export const Screenshots = ({
  screenshots,
  keyId,
  screenshotMaxWidth,
  sx,
}: Props) => {
  const project = useProject();
  let boundedSize: number | undefined = undefined;
  if (screenshotMaxWidth !== undefined) {
    const size = Math.floor(screenshotMaxWidth / 75) * 75;
    boundedSize = Math.max(Math.min(MAX_SIZE, size), MIN_SIZE);
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

  return (
    <StyledContainer {...{ sx }} onClick={stopAndPrevent()}>
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
                    : 100,
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
