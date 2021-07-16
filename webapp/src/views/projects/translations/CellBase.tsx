import React, { RefObject } from 'react';
import {
  Box,
  BoxProps,
  makeStyles,
  IconButton,
  Button,
} from '@material-ui/core';
import { Edit, CameraAlt } from '@material-ui/icons';
import { T } from '@tolgee/react';

import { LimitedHeightText } from './LimitedHeightText';

const useStyles = makeStyles((theme) => ({
  cellPlain: {
    '& $showOnHover': {
      display: 'none',
    },
    '&:hover $showOnHover': {
      display: 'flex',
    },
  },
  cellClickable: {
    cursor: 'pointer',
  },
  hover: {
    '&:hover': {
      background: theme.palette.grey[50],
    },
  },
  showOnHover: {},
  controlsAbsolute: {
    position: 'absolute',
    right: 0,
    bottom: 0,
  },
  controlsSpaced: {
    '& > *': {
      marginLeft: 10,
      marginBottom: 5,
    },
  },
}));

type Props = {
  background?: string;
  hover?: boolean;
  onClick?: () => void;
} & BoxProps;

export const CellPlain: React.FC<Props> = ({
  children,
  background,
  hover,
  onClick,
  ...props
}) => {
  const classes = useStyles();

  return (
    <Box
      className={`${classes.cellPlain} ${
        onClick ? classes.cellClickable : ''
      } ${hover ? classes.hover : ''}`}
      onClick={onClick}
      display="flex"
      flexDirection="column"
      width="100%"
      bgcolor={background}
      position="relative"
      data-cy="translations-table-cell"
      {...props}
    >
      {children}
    </Box>
  );
};

type CellContentProps = BoxProps & {
  forwardRef?: RefObject<HTMLElement | undefined | null>;
  maxLines?: number;
};

export const CellContent: React.FC<CellContentProps> = ({
  children,
  forwardRef,
  maxLines,
  ...props
}) => {
  return (
    <Box margin="5px 10px" position="relative" flexGrow={1} {...props}>
      {maxLines !== undefined ? (
        <LimitedHeightText maxLines={maxLines}>{children}</LimitedHeightText>
      ) : (
        children
      )}
    </Box>
  );
};

export const stopBubble = (func?) => (e) => {
  e.stopPropagation();
  func?.(e);
};

type ControlsProps = {
  mode: 'edit' | 'view';
  editEnabled?: boolean;
  onClick?: () => void;
  onEdit?: () => void;
  onSave?: () => void;
  onCancel?: () => void;
  onScreenshots?: () => void;
  screenshotRef?: React.Ref<HTMLButtonElement>;
  screenshotsPresent?: boolean;
  absolute?: boolean;
};

export const CellControls: React.FC<ControlsProps> = ({
  mode,
  editEnabled,
  onEdit,
  onSave,
  onCancel,
  onScreenshots,
  screenshotRef,
  screenshotsPresent,
  absolute,
}) => {
  const classes = useStyles();

  return mode === 'view' ? (
    <Box
      display="flex"
      justifyContent="flex-end"
      width="100%"
      minHeight={26}
      className={absolute ? classes.controlsAbsolute : undefined}
    >
      {editEnabled && (
        <IconButton
          onClick={onEdit}
          size="small"
          data-cy="translations-cell-edit-button"
          className={classes.showOnHover}
        >
          <Edit fontSize="small" />
        </IconButton>
      )}
      {onScreenshots && (
        <IconButton
          size="small"
          ref={screenshotRef}
          onClick={stopBubble(onScreenshots)}
          data-cy="translations-cell-screenshots-button"
        >
          <CameraAlt
            fontSize="small"
            color={screenshotsPresent ? 'secondary' : 'disabled'}
          />
        </IconButton>
      )}
    </Box>
  ) : (
    <Box
      display="flex"
      justifyContent="space-between"
      alignItems="flex-end"
      width="100%"
      minHeight={26}
    >
      <Box className={classes.controlsSpaced}>
        <Button
          onClick={onCancel}
          color="primary"
          variant="outlined"
          size="small"
          data-cy="translations-cell-cancel-button"
        >
          <T>translations_cell_cancel</T>
        </Button>
        <Button
          onClick={onSave}
          color="primary"
          variant="contained"
          size="small"
          data-cy="translations-cell-save-button"
        >
          <T>translations_cell_save</T>
        </Button>
      </Box>
      {onScreenshots && (
        <IconButton
          size="small"
          ref={screenshotRef}
          onClick={onScreenshots}
          data-cy="translations-cell-screenshots-button"
        >
          <CameraAlt
            fontSize="small"
            color={screenshotsPresent ? 'secondary' : 'disabled'}
          />
        </IconButton>
      )}
    </Box>
  );
};
