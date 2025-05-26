import { FunctionComponent } from 'react';
import { Box, styled, SxProps } from '@mui/material';
import { T } from '@tolgee/react';

import { confirmation } from 'tg.hooks/confirmation';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import {
  ScreenshotProps,
  ScreenshotWithLabels,
} from 'tg.component/ScreenshotWithLabels';
import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import { CloseButton } from 'tg.component/common/buttons/CloseButton';

const StyledScreenshotWithLabels = styled(ScreenshotWithLabels)`
  width: 100%;
  height: 100%;
  object-fit: contain;
  z-index: 1;
`;

const StyledScreenshotBox = styled(Box)`
  position: relative;
  align-items: center;
  width: 100px;
  height: 100px;
  justify-content: center;
  display: flex;
  cursor: pointer;
`;

const StyledScreenshotOverflowWrapper = styled(Box)`
  overflow: hidden;
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 4px;
  background: ${({ theme }) => theme.palette.tokens.text._states.selected};

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
`;

type Props = {
  onClick: () => void;
  screenshot: ScreenshotProps;
  onDelete: () => void;
  sx?: SxProps;
  objectFit: 'contain' | 'cover';
  scaleHighlight?: number;
  onSrcExpired: () => void;
};

export const ScreenshotThumbnail: FunctionComponent<Props> = (props) => {
  const { satisfiesPermission } = useProjectPermissions();
  const canDeleteScreenshots = satisfiesPermission('screenshots.delete');

  const onDeleteClick = () => {
    confirmation({
      title: <T keyName="screenshot_delete_title" />,
      message: <T keyName="screenshot_delete_message" />,
      onConfirm: () => props.onDelete(),
    });
  };

  return (
    <CloseButton
      onClose={
        canDeleteScreenshots
          ? (e) => {
              stopAndPrevent()(e);
              onDeleteClick();
            }
          : undefined
      }
      data-cy="screenshot-thumbnail-delete"
    >
      <StyledScreenshotBox
        sx={props.sx}
        data-cy="screenshot-thumbnail"
        onMouseDown={stopAndPrevent()}
        onClick={stopAndPrevent()}
      >
        <StyledScreenshotOverflowWrapper
          key={props.screenshot.highlightedKeyId}
          onClick={props.onClick}
        >
          <StyledScreenshotWithLabels
            screenshot={props.screenshot}
            objectFit={props.objectFit}
            scaleHighlight={props.scaleHighlight}
            onSrcExpired={props.onSrcExpired}
          />
        </StyledScreenshotOverflowWrapper>
      </StyledScreenshotBox>
    </CloseButton>
  );
};
