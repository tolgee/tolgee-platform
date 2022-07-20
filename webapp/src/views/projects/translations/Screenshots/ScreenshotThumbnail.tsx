import { FunctionComponent, useState } from 'react';
import { Box, IconButton, styled, Tooltip } from '@mui/material';
import ClearIcon from '@mui/icons-material/Clear';
import { T } from '@tolgee/react';
import clsx from 'clsx';

import { confirmation } from 'tg.hooks/confirmation';
import { useConfig } from 'tg.globalContext/helpers';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { components } from 'tg.service/apiSchema.generated';
import { ProjectPermissionType } from 'tg.service/response.types';

export interface ScreenshotThumbnailProps {
  onClick: () => void;
  screenshotData: components['schemas']['ScreenshotModel'];
  onDelete: (id: number) => void;
}

const StyledScreenshot = styled('img')`
  width: 100%;
  height: 100%;
  object-fit: cover;
  z-index: 1;
  transition: transform 0.1s, filter 0.5s;
  &:hover {
    transform: scale(1.5);
    filter: blur(2px);
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

export const ScreenshotThumbnail: FunctionComponent<ScreenshotThumbnailProps> =
  (props) => {
    const config = useConfig();
    const [hover, setHover] = useState(false);
    const projectPermissions = useProjectPermissions();

    const onMouseOver = () => {
      setHover(true);
    };

    const onMouseOut = () => {
      setHover(false);
    };

    const onDeleteClick = () => {
      confirmation({
        title: <T>screenshot_delete_title</T>,
        message: <T>screenshot_delete_message</T>,
        onConfirm: () => props.onDelete(props.screenshotData.id),
      });
    };

    return (
      <>
        <StyledScreenshotBox
          onMouseOver={onMouseOver}
          onMouseOut={onMouseOut}
          data-cy="screenshot-box"
        >
          {projectPermissions.satisfiesPermission(
            ProjectPermissionType.TRANSLATE
          ) && (
            <Tooltip
              title={<T noWrap>translations.screenshots.delete_tooltip</T>}
            >
              <StyledDeleteIconButton
                className={clsx({ hover })}
                onClick={onDeleteClick}
                size="large"
              >
                <StyledDeleteIcon />
              </StyledDeleteIconButton>
            </Tooltip>
          )}
          <StyledScreenshotOverflowWrapper
            key={props.screenshotData.id}
            onClick={props.onClick}
          >
            <StyledScreenshot
              onMouseDown={(e) => e.preventDefault()}
              src={`${config.screenshotsUrl}/${props.screenshotData.filename}`}
              alt="Screenshot"
            />
          </StyledScreenshotOverflowWrapper>
        </StyledScreenshotBox>
      </>
    );
  };
