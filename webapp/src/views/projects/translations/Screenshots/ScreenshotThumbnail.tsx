import { FunctionComponent, useState } from 'react';
import { Box, IconButton, styled, SxProps, Tooltip } from '@mui/material';
import { XClose } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';
import clsx from 'clsx';

import { confirmation } from 'tg.hooks/confirmation';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import {
  ScreenshotProps,
  ScreenshotWithLabels,
} from 'tg.component/ScreenshotWithLabels';

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
  width: 100%;
  height: 100%;
  background: ${({ theme }) => theme.palette.tokens.text._states.selected};
  border-radius: 3px;
  border-radius: 4px;
  border: 1px solid ${({ theme }) => theme.palette.tokens.border.secondary};
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

const StyledDeleteIcon = styled(XClose)`
  width: 20px;
  height: 20px;
`;

type Props = {
  onClick: () => void;
  screenshot: ScreenshotProps;
  onDelete: () => void;
  sx?: SxProps;
  objectFit: 'contain' | 'cover';
  highlightFilled?: boolean;
};

export const ScreenshotThumbnail: FunctionComponent<Props> = (props) => {
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
        sx={props.sx}
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
          <StyledScreenshotWithLabels
            screenshot={props.screenshot}
            objectFit={props.objectFit}
            highlightFilled={props.highlightFilled}
          />
        </StyledScreenshotOverflowWrapper>
      </StyledScreenshotBox>
    </>
  );
};
