import { FunctionComponent, useState } from 'react';
import { Box, IconButton, styled, Tooltip } from '@mui/material';
import ClearIcon from '@mui/icons-material/Clear';
import { T } from '@tolgee/react';
import clsx from 'clsx';

import { confirmation } from 'tg.hooks/confirmation';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import {
  ScreenshotProps,
  ScreenshotWithLabels,
} from 'tg.component/ScreenshotWithLabels';

export interface ScreenshotThumbnailProps {
  onClick: () => void;
  screenshot: ScreenshotProps;
  onDelete: () => void;
}

const StyledScreenshotWithLabels = styled(ScreenshotWithLabels)`
  width: 100%;
  height: 100%;
  object-fit: contain;
  z-index: 1;
  transition: transform 0.1s, filter 0.5s;

  &:hover {
    transform: scale(1.1);
  }
`;

const StyledScreenshotBox = styled(Box)`
  position: relative;
  width: 100px;
  height: 100px;
  align-items: center;
  justify-content: center;
  display: flex;
  margin: 1px;
  cursor: pointer;
  overflow: visible;
`;

const StyledScreenshotOverflowWrapper = styled(Box)`
  overflow: hidden;
  width: 100%;
  height: 100%;
`;

const StyledDeleteIconButton = styled(IconButton)`
  position: absolute;
  z-index: 2;
  font-size: 20px;
  right: -8px;
  top: -8px;
  padding: 2px;
  background-color: rgba(62, 62, 62, 0.9);
  color: rgba(255, 255, 255, 0.8);
  visibility: hidden;
  opacity: 0;
  transition: visibility 0.1s linear, opacity 0.1s linear;

  &:hover {
    background-color: rgba(62, 62, 62, 1);
    color: rgba(255, 255, 255, 0.9);
  }

  &.hover {
    opacity: 1;
    visibility: visible;
  }
`;

const StyledDeleteIcon = styled(ClearIcon)`
  font-size: 20px;
`;

export const ScreenshotThumbnail: FunctionComponent<
  ScreenshotThumbnailProps
> = (props) => {
  const [hover, setHover] = useState(false);
  const { satisfiesPermission } = useProjectPermissions();
  const canDeleteScreenshots = satisfiesPermission('screenshots.delete');

  const onMouseOver = () => {
    setHover(true);
  };

  const onMouseOut = () => {
    setHover(false);
  };

  const onDeleteClick = () => {
    confirmation({
      title: <T keyName="screenshot_delete_title" />,
      message: <T keyName="screenshot_delete_message" />,
      onConfirm: () => props.onDelete(),
    });
  };

  return (
    <>
      <StyledScreenshotBox
        onMouseOver={onMouseOver}
        onMouseOut={onMouseOut}
        data-cy="screenshot-thumbnail"
      >
        {canDeleteScreenshots && (
          <Tooltip
            title={<T keyName="translations.screenshots.delete_tooltip" />}
          >
            <StyledDeleteIconButton
              className={clsx({ hover })}
              onClick={onDeleteClick}
              size="large"
              data-cy="screenshot-thumbnail-delete"
            >
              <StyledDeleteIcon />
            </StyledDeleteIconButton>
          </Tooltip>
        )}
        <StyledScreenshotOverflowWrapper
          key={props.screenshot.highlightedKeyId}
          onClick={props.onClick}
        >
          <StyledScreenshotWithLabels screenshot={props.screenshot} />
        </StyledScreenshotOverflowWrapper>
      </StyledScreenshotBox>
    </>
  );
};
