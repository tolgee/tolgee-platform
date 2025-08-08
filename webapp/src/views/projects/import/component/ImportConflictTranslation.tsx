import React, { FunctionComponent, LegacyRef, useEffect } from 'react';
import {
  Box,
  BoxProps,
  IconButton,
  styled,
  Typography,
  useTheme,
} from '@mui/material';
import { green, grey } from '@mui/material/colors';
import {
  ChevronUp,
  ChevronDown,
  Check,
  AlertTriangle,
  AlertCircle,
} from '@untitled-ui/icons-react';
import clsx from 'clsx';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';
import { TranslationVisual } from 'tg.views/projects/translations/translationVisual/TranslationVisual';
import { T } from '@tolgee/react';

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
  languageTag: string;
  isPlural: boolean;
  disabled?: boolean;
  conflictHint?: string;
  'data-cy': string;
};

const StyledContainer = styled(Box)`
  display: grid;
`;

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

  &.disabled {
    border-color: ${({ theme }) =>
      theme.palette.mode === 'light' ? grey['900'] : grey['100']};
    background-color: ${({ theme }) =>
      theme.palette.mode === 'light' ? grey['50'] : grey['900']};
    opacity: 0.6;
    cursor: default;
  }
`;

const StyledLoading = styled(Box)`
  position: absolute;
  right: 0px;
  top: 0px;
`;

const StyledEmpty = styled(Box)`
  font-style: italic;
  color: ${({ theme }) => theme.palette.text.secondary};
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
  const theme = useTheme();
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
    <StyledContainer>
      <StyledRoot
        position="relative"
        onClick={!props.disabled ? props.onSelect : undefined}
        className={clsx({
          selected: props.selected || props.loaded,
          expanded: props.expanded,
          disabled: props.disabled,
        })}
        display="flex"
        {...dataCySelected}
        data-cy={props['data-cy']}
      >
        {props.loading && (
          <StyledLoading
            p={1}
            data-cy="import-resolution-dialog-translation-loading"
          >
            <SpinnerProgress size={20} />
          </StyledLoading>
        )}
        {props.loaded && (
          <StyledLoading
            p={1}
            data-cy="import-resolution-dialog-translation-check"
          >
            <Check />
          </StyledLoading>
        )}
        <BoxWithRef
          flexGrow={1}
          overflow="hidden"
          textOverflow="ellipsis"
          ref={textRef}
        >
          {props.text ? (
            <TranslationVisual
              maxLines={props.expanded ? 0 : 3}
              text={props.text}
              locale={props.languageTag}
              isPlural={props.isPlural}
            />
          ) : (
            <StyledEmpty>
              <T keyName="import-resolution-translation-empty" />
            </StyledEmpty>
          )}
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
              {!props.expanded ? <ChevronDown /> : <ChevronUp />}
            </StyledToggleButton>
          </Box>
        )}
      </StyledRoot>
      {props.conflictHint && (
        <Box
          color={
            props.disabled
              ? theme.palette.error.main
              : theme.palette.warning.main
          }
          display="flex"
          alignItems="center"
          gap={0.5}
          mt={0.2}
        >
          {props.disabled ? (
            <AlertCircle width={15} />
          ) : (
            <AlertTriangle width={15} />
          )}
          <Typography variant="caption">{props.conflictHint}</Typography>
        </Box>
      )}
    </StyledContainer>
  );
};
