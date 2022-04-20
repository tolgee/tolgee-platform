import React, { FunctionComponent, LegacyRef, useEffect } from 'react';
import {
  Box,
  BoxProps,
  CircularProgress,
  IconButton,
  styled,
} from '@mui/material';
import { green } from '@mui/material/colors';
import { KeyboardArrowUp } from '@mui/icons-material';
import CheckIcon from '@mui/icons-material/Check';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import clsx from 'clsx';

type Props = {
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

const StyledRoot = styled(Box)`
  border-radius: ${({ theme }) => theme.shape.borderRadius};
  border: 1px dashed ${({ theme }) => theme.palette.emphasis.A100};
  padding: ${({ theme }) => theme.spacing(1)};
  cursor: pointer;
  text-overflow: ellipsis;
  white-space: nowrap;
  overflow: hidden;

  &.expanded {
    white-space: initial;
    transition: max-height 1s;
  }

  &.selected {
    border-color: ${({ theme }) =>
      theme.palette.mode === 'light' ? green['900'] : green['100']};
    background-color: ${({ theme }) =>
      theme.palette.mode === 'light' ? green['50'] : green['900']};
  }
`;

const StyledLoading = styled(Box)`
  position: absolute;
  right: 0px;
  top: 0px;
`;

const StyledToggleButton = styled(IconButton)(({ theme }) => ({
  padding: theme.spacing(0.5),
  marginTop: -theme.spacing(1),
  marginRight: -theme.spacing(0.5),
  marginBottom: -theme.spacing(1),
}));

const BoxWithRef = Box as FunctionComponent<
  BoxProps & { ref: LegacyRef<HTMLDivElement> }
>;

export const ImportConflictTranslation: React.FC<Props> = (props) => {
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
    <StyledRoot
      position="relative"
      onClick={props.onSelect}
      className={clsx(
        { selected: props.selected || props.loaded },
        { expanded: props.expanded }
      )}
      display="flex"
      {...dataCySelected}
      data-cy={props['data-cy']}
    >
      {props.loading && (
        <StyledLoading
          p={1}
          data-cy="import-resolution-dialog-translation-loading"
        >
          <CircularProgress size={20} />
        </StyledLoading>
      )}
      {props.loaded && (
        <StyledLoading
          p={1}
          data-cy="import-resolution-dialog-translation-check"
        >
          <CheckIcon />
        </StyledLoading>
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
          <StyledToggleButton
            data-cy="import-resolution-translation-expand-button"
            onClick={(e) => {
              e.stopPropagation();
              props.onToggle();
            }}
            size="large"
          >
            {!props.expanded ? <KeyboardArrowDownIcon /> : <KeyboardArrowUp />}
          </StyledToggleButton>
        </Box>
      )}
    </StyledRoot>
  );
};
