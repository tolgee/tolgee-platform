import { XClose } from '@untitled-ui/icons-react';
import {
  Checkbox,
  ListItemText,
  MenuItem,
  styled,
  SxProps,
  Select,
  Box,
  IconButton,
} from '@mui/material';
import { StateType, TRANSLATION_STATES } from 'tg.constants/translationStates';
import { stopBubble } from 'tg.fixtures/eventHandler';
import { useStateTranslation } from 'tg.translationTools/useStateTranslation';

export type TranslationStateType = StateType | 'OUTDATED';

const StyledDot = styled('div')`
  width: 8px;
  height: 8px;
  border-radius: 4px;
  margin-left: ${({ theme }) => theme.spacing(2)};
`;

const StyledInputButton = styled(IconButton)`
  margin: ${({ theme }) => theme.spacing(-1, -0.5, -1, -0.25)};
`;

const StyledPlaceholder = styled(Box)`
  opacity: 0.5;
`;

type Props = {
  value: TranslationStateType[];
  onChange: (value: TranslationStateType[]) => void;
  placeholder?: string;
  sx?: SxProps;
  className?: string;
};

export const TranslationStateFilter = ({
  value,
  onChange,
  placeholder,
  sx,
  className,
}: Props) => {
  const translateState = useStateTranslation();

  const handleToggle = (item: TranslationStateType) => () => {
    if (value.includes(item)) {
      onChange(value.filter((i) => i !== item));
    } else {
      onChange([...value, item]);
    }
  };

  return (
    <Select
      value={value}
      multiple
      displayEmpty
      renderValue={(value) =>
        value.length ? (
          (value as TranslationStateType[])
            .map((i) => translateState(i))
            .join(', ')
        ) : (
          <StyledPlaceholder>{placeholder}</StyledPlaceholder>
        )
      }
      data-cy="translations-state-filter"
      placeholder={placeholder}
      size="small"
      endAdornment={
        value.length ? (
          <Box
            sx={{
              display: 'flex',
              position: 'relative',
              right: 20,
              marginLeft: -2,
            }}
          >
            <StyledInputButton
              size="small"
              onClick={stopBubble(() => onChange([]))}
              tabIndex={-1}
            >
              <XClose />
            </StyledInputButton>
          </Box>
        ) : null
      }
      {...{ sx, className }}
    >
      <MenuItem
        data-cy="translations-state-filter-option"
        value="OUTDATED"
        onClick={handleToggle('OUTDATED')}
      >
        <Checkbox
          size="small"
          edge="start"
          checked={value.includes('OUTDATED')}
          tabIndex={-1}
          disableRipple
        />
        <ListItemText primary={translateState('OUTDATED')} />
      </MenuItem>
      {Object.entries(TRANSLATION_STATES).map(([key, state]) => (
        <MenuItem
          data-cy="translations-state-filter-option"
          key={key}
          value={key}
          onClick={handleToggle(key as TranslationStateType)}
        >
          <Checkbox
            size="small"
            edge="start"
            checked={value.includes(key as TranslationStateType)}
            tabIndex={-1}
            disableRipple
          />
          <ListItemText primary={state.translation} />
          <StyledDot
            style={{
              background: TRANSLATION_STATES[key as StateType]?.color,
            }}
          />
        </MenuItem>
      ))}
    </Select>
  );
};
