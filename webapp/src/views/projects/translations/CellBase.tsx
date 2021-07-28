import React, { RefObject } from 'react';
import {
  Box,
  BoxProps,
  makeStyles,
  IconButton,
  Button,
  useTheme,
  Tooltip,
} from '@material-ui/core';
import {
  Edit,
  CameraAlt,
  CheckCircleOutlined,
  ErrorOutlined,
} from '@material-ui/icons';
import { T } from '@tolgee/react';

export type StateType =
  | 'UNTRANSLATED'
  | 'MACHINE_TRANSLATED'
  | 'TRANSLATED'
  | 'REVIEWED'
  | 'NEEDS_REVIEW';

const useStyles = makeStyles((theme) => ({
  cellPlain: {
    '&:focus-within $showOnHover': {
      opacity: 0.7,
    },
    '& $showOnHover': {
      opacity: 0,
      transition: 'opacity 0.1s ease-in-out',
    },
    '&:hover $showOnHover': {
      opacity: 0.7,
      transition: 'opacity 0.5s ease-in-out',
    },
  },
  cellClickable: {
    cursor: 'pointer',
  },
  state: {
    cursor: 'col-resize',
  },
  hover: {
    '&:hover': {
      background: theme.palette.grey[50],
    },
  },
  showOnHover: {
    '&:focus': {
      opacity: 1,
    },
  },

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

const getStateColor = (state?: StateType) => {
  switch (state) {
    case 'UNTRANSLATED':
      return '#c4c4c4';
    case 'TRANSLATED':
      return '#ffce00';
    case 'REVIEWED':
      return '#17ad18';
    case 'MACHINE_TRANSLATED':
      return '#39e1fa';
    case 'NEEDS_REVIEW':
      return '#e80000';
    default:
      return undefined;
  }
};

type Props = {
  background?: string;
  hover?: boolean;
  onClick?: () => void;
  state?: StateType | 'NONE';
  onResize?: any;
} & BoxProps;

export const CellPlain: React.FC<Props> = ({
  children,
  background,
  hover,
  onClick,
  state,
  onResize,
  ...props
}) => {
  const classes = useStyles();
  const theme = useTheme();

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
      {state &&
        (state === 'NONE' ? (
          <Box
            onMouseDown={stopBubble(onResize)}
            onClick={stopBubble()}
            onMouseUp={stopBubble()}
            position="absolute"
            height="100%"
            borderLeft={`4px solid ${theme.palette.grey[200]}`}
            className={classes.state}
          />
        ) : (
          <Tooltip title={state}>
            <Box position="absolute" width={12} height="100%">
              <Box
                onMouseDown={stopBubble(onResize)}
                onClick={stopBubble()}
                onMouseUp={stopBubble()}
                position="absolute"
                height="100%"
                borderLeft={`4px solid ${getStateColor(state)}`}
                className={classes.state}
              />
            </Box>
          </Tooltip>
        ))}
    </Box>
  );
};

type CellContentProps = BoxProps & {
  forwardRef?: RefObject<HTMLElement | undefined | null>;
};

export const CellContent: React.FC<CellContentProps> = ({
  children,
  forwardRef,
  ...props
}) => {
  return (
    <Box margin="5px 10px" position="relative" flexGrow={1} {...props}>
      {children}
    </Box>
  );
};

export const stopBubble = (func?) => (e) => {
  e.stopPropagation();
  func?.(e);
};

const getStateTransitions = (state?: StateType): StateType[] => {
  switch (state) {
    case 'TRANSLATED':
      return ['REVIEWED'];
    case 'REVIEWED':
      return ['NEEDS_REVIEW'];
    case 'MACHINE_TRANSLATED':
      return ['REVIEWED', 'NEEDS_REVIEW'];
    case 'NEEDS_REVIEW':
      return ['REVIEWED'];
    case 'UNTRANSLATED':
    default:
      return [];
  }
};

type StateButtonProps = React.ComponentProps<typeof CheckCircleOutlined> & {
  state: StateType;
};

const StateIcon = ({ state, ...props }: StateButtonProps) => {
  switch (state) {
    case 'NEEDS_REVIEW':
      return <ErrorOutlined {...props} />;
    default:
      return <CheckCircleOutlined {...props} />;
  }
};

type ControlsProps = {
  mode: 'edit' | 'view';
  state?: StateType;
  editEnabled?: boolean;
  onClick?: () => void;
  onEdit?: () => void;
  onSave?: () => void;
  onCancel?: () => void;
  onScreenshots?: () => void;
  onStateChange?: (state: StateType) => void;
  screenshotRef?: React.Ref<HTMLButtonElement>;
  screenshotsPresent?: boolean;
  absolute?: boolean;
};

export const CellControls: React.FC<ControlsProps> = ({
  mode,
  state,
  editEnabled,
  onEdit,
  onSave,
  onCancel,
  onScreenshots,
  onStateChange,
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
        <>
          {getStateTransitions(state).map((s) => (
            <Tooltip key={s} title={s}>
              <IconButton
                onClick={stopBubble(() => onStateChange?.(s))}
                size="small"
                className={classes.showOnHover}
              >
                <StateIcon state={s} fontSize="small" />
              </IconButton>
            </Tooltip>
          ))}
          <IconButton
            onClick={onEdit}
            size="small"
            data-cy="translations-cell-edit-button"
            className={classes.showOnHover}
          >
            <Edit fontSize="small" />
          </IconButton>
        </>
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
      <Box
        className={classes.controlsSpaced}
        display="flex"
        alignItems="center"
      >
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
      <Box display="flex">
        {getStateTransitions(state).map((s) => (
          <Tooltip key={s} title={s}>
            <IconButton
              onClick={stopBubble(() => onStateChange?.(s))}
              size="small"
              className={classes.showOnHover}
            >
              <StateIcon state={s} fontSize="small" />
            </IconButton>
          </Tooltip>
        ))}
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
    </Box>
  );
};
