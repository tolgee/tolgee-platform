import React, { FunctionComponent, LegacyRef, useEffect } from 'react';
import {
  Box,
  BoxProps,
  CircularProgress,
  IconButton,
  makeStyles,
} from '@material-ui/core';
import { green } from '@material-ui/core/colors';
import { KeyboardArrowUp } from '@material-ui/icons';
import CheckIcon from '@material-ui/icons/Check';
import KeyboardArrowDownIcon from '@material-ui/icons/KeyboardArrowDown';
import clsx from 'clsx';

type ImportConflictTranslationProps = {
  text?: string;
  selected?: boolean;
  onSelect?: () => void;
  loading?: boolean;
  loaded?: boolean;
  expanded: boolean;
  onToggle: () => void;
  onDetectedExpandability: (expandable: boolean) => void;
  expandable: boolean;
  'data-cy': string;
};

const useStyles = makeStyles((theme) => ({
  root: {
    borderRadius: theme.shape.borderRadius,
    border: `1px dashed ${theme.palette.grey.A100}`,
    padding: theme.spacing(1),
    cursor: 'pointer',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
  },
  expanded: {
    whiteSpace: 'initial',
    transition: 'max-height 1s',
  },
  selected: {
    borderColor: green['900'],
    backgroundColor: green['50'],
  },
  loading: {
    position: 'absolute',
    right: 0,
    top: 0,
  },
  toggleButton: {
    padding: theme.spacing(0.5),
    marginTop: -theme.spacing(1),
    marginRight: -theme.spacing(0.5),
    marginBottom: -theme.spacing(1),
  },
}));

const BoxWithRef = Box as FunctionComponent<
  BoxProps & { ref: LegacyRef<HTMLDivElement> }
>;

export const ImportConflictTranslation = (
  props: ImportConflictTranslationProps
) => {
  const classes = useStyles();
  const textRef = React.createRef<HTMLDivElement>();

  const detectExpandability = () => {
    const textElement = textRef.current;
    if (textElement != null) {
      const clone = textRef.current?.cloneNode(true) as HTMLDivElement;
      clone.style.position = 'absolute';
      clone.style.visibility = 'hidden';
      textElement.parentElement?.append(clone);
      props.onDetectedExpandability(
        textElement.clientWidth < clone.clientWidth
      );
      textElement.parentElement?.removeChild(clone);
    }
  };

  useEffect(() => {
    detectExpandability();
  }, [props.text, textRef.current]);

  useEffect(() => {
    window.addEventListener('resize', detectExpandability);
    return () => {
      window.removeEventListener('resize', detectExpandability);
    };
  });

  const dataCySelected = { 'data-cy-selected': props.selected || undefined };

  return (
    <Box
      position="relative"
      onClick={props.onSelect}
      className={clsx(
        classes.root,
        { [classes.selected]: props.selected || props.loaded },
        { [classes.expanded]: props.expanded }
      )}
      display="flex"
      {...dataCySelected}
      data-cy={props['data-cy']}
    >
      {props.loading && (
        <Box
          className={classes.loading}
          p={1}
          data-cy="import-resolution-dialog-translation-loading"
        >
          <CircularProgress size={20} />
        </Box>
      )}
      {props.loaded && (
        <Box
          className={classes.loading}
          p={1}
          data-cy="import-resolution-dialog-translation-check"
        >
          <CheckIcon />
        </Box>
      )}
      <BoxWithRef
        flexGrow={1}
        overflow="hidden"
        textOverflow="ellipsis"
        ref={textRef}
      >
        {props.text}
      </BoxWithRef>
      {props.expandable && (
        <Box
          style={{
            opacity: props.loading || props.loaded ? 0 : 1,
          }}
        >
          <IconButton
            data-cy="import-resolution-translation-expand-button"
            className={classes.toggleButton}
            onClick={(e) => {
              e.stopPropagation();
              props.onToggle();
            }}
          >
            {!props.expanded ? <KeyboardArrowDownIcon /> : <KeyboardArrowUp />}
          </IconButton>
        </Box>
      )}
    </Box>
  );
};
