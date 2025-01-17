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

  & .closeButton {
    position: absolute;
    z-index: 2;
    top: -8px;
    right: -8px;
    width: 24px;
    height: 24px;
    background-color: ${({ theme }) =>
      theme.palette.tokens.icon.backgroundDark};
    color: ${({ theme }) => theme.palette.tokens.icon.onDark};
    transition: visibility 0.1s linear, opacity 0.1s linear;
    display: grid;
    align-content: center;
    justify-content: center;
    opacity: 0;
    pointer-events: none;

    &:hover {
      background-color: ${({ theme }) =>
        theme.palette.tokens.icon.backgroundDarkHover};
      color: ${({ theme }) => theme.palette.tokens.icon.onDarkHover};
      visibility: visible;
    }
  }

  &:hover .closeButton {
    pointer-events: all;
    opacity: 1;
  }
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
    <>
      <StyledScreenshotBox sx={props.sx} data-cy="screenshot-thumbnail">
        {canDeleteScreenshots && (
          <Tooltip
            title={<T keyName="translations.screenshots.delete_tooltip" />}
            disableInteractive
          >
            <IconButton
              className={clsx('closeButton')}
              onClick={onDeleteClick}
              size="large"
              data-cy="screenshot-thumbnail-delete"
            >
              <XClose width={20} height={20} />
            </IconButton>
          </Tooltip>
        )}
        <StyledScreenshotOverflowWrapper
          key={props.screenshot.highlightedKeyId}
          onClick={props.onClick}
        >
          <StyledScreenshotWithLabels
            screenshot={props.screenshot}
            objectFit={props.objectFit}
            scaleHighlight={props.scaleHighlight}
          />
        </StyledScreenshotOverflowWrapper>
      </StyledScreenshotBox>
    </>
  );
};
