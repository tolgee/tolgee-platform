import { FunctionComponent, useState } from 'react';
import {
  Box,
  createStyles,
  IconButton,
  makeStyles,
  Theme,
  Tooltip,
} from '@material-ui/core';
import ClearIcon from '@material-ui/icons/Clear';
import { T } from '@tolgee/react';
import clsx from 'clsx';

import { confirmation } from 'tg.hooks/confirmation';
import { useConfig } from 'tg.hooks/useConfig';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { components } from 'tg.service/apiSchema.generated';
import { ProjectPermissionType } from 'tg.service/response.types';

export interface ScreenshotThumbnailProps {
  onClick: () => void;
  screenshotData: components['schemas']['ScreenshotModel'];
  onDelete: (id: number) => void;
}

const useStyles = makeStyles((theme: Theme) =>
  createStyles({
    screenshot: {
      width: '100%',
      height: '100%',
      objectFit: 'cover',
      zIndex: 1,
      transition: 'transform .1s, filter 0.5s',
      '&:hover': {
        transform: 'scale(1.5)',
        filter: 'blur(2px)',
      },
    },
    screenshotBox: {
      position: 'relative',
      width: '100px',
      height: '100px',
      alignItems: 'center',
      justifyContent: 'center',
      display: 'flex',
      margin: '1px',
      cursor: 'pointer',
      overflow: 'visible',
    },
    screenShotOverflowWrapper: {
      overflow: 'hidden',
      width: '100%',
      height: '100%',
    },
    deleteIconButton: {
      position: 'absolute',
      zIndex: 2,
      fontSize: 20,
      right: -8,
      top: -8,
      padding: 2,
      backgroundColor: 'rgba(62,62,62,0.9)',
      color: 'rgba(255,255,255,0.8)',
      visibility: 'hidden',
      opacity: 0,
      transition: 'visibility 0.1s linear, opacity 0.1s linear',
      '&:hover': {
        backgroundColor: 'rgba(62,62,62,1)',
        color: 'rgba(255,255,255,0.9)',
      },
      '&.hover': {
        opacity: 1,
        visibility: 'visible',
      },
    },
    deleteIcon: {
      fontSize: 20,
    },
  })
);

export const ScreenshotThumbnail: FunctionComponent<ScreenshotThumbnailProps> =
  (props) => {
    const config = useConfig();
    const classes = useStyles({});
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
        <Box
          className={classes.screenshotBox}
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
              <IconButton
                className={clsx(classes.deleteIconButton, { hover })}
                onClick={onDeleteClick}
              >
                <ClearIcon className={classes.deleteIcon} />
              </IconButton>
            </Tooltip>
          )}
          <Box
            key={props.screenshotData.id}
            className={classes.screenShotOverflowWrapper}
            onClick={props.onClick}
          >
            <img
              className={classes.screenshot}
              onMouseDown={(e) => e.preventDefault()}
              src={`${config.screenshotsUrl}/${props.screenshotData.filename}`}
              alt="Screenshot"
            />
          </Box>
        </Box>
      </>
    );
  };
