import { useState, useRef } from 'react';
import { useField } from 'formik';
import {
  Popover,
  TextField,
  Tooltip,
  styled,
  FormControl,
  FormHelperText,
  InputLabel,
  useTheme,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';

const ColorPreview = styled('div')<{ color: string }>`
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: ${({ color }) => color};
  border: 1px solid #ccc;
  margin-right: 8px;
  cursor: pointer;
  flex-shrink: 0;
`;

const PaletteGrid = styled('div')`
  display: grid;
  grid-template-columns: repeat(5, 32px);
  gap: 8px;
  padding: 12px;
`;

const PaletteColor = styled('div')<{ color: string; selected: boolean }>`
  width: 32px;
  height: 32px;
  border-radius: 4px;
  background: ${({ color }) => color};
  border: 2px solid ${({ selected }) => (selected ? '#000' : '#fff')};
  cursor: pointer;
  box-shadow: 0 0 2px #aaa;
`;

type Props = {
  name: string;
  label?: string;
  helperText?: string;
  disabled?: boolean;
  minHeight?: boolean;
  randomDefaultColor?: boolean;
  required?: boolean;
};

export const ColorPaletteField = ({
  name,
  label,
  helperText,
  disabled,
  minHeight = true,
  randomDefaultColor = true,
  required = false,
}: Props) => {
  const [field, meta, helpers] = useField(name);
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const theme = useTheme();
  const { t } = useTranslate();

  const setInputValue = (color: string) => {
    helpers.setValue(color.toUpperCase());
  };

  const getRandomColor = () => {
    const colors = Object.values(theme.palette.paletteField);
    return colors[Math.floor(Math.random() * colors.length)];
  };

  const defaultFilled = useRef(false);
  if (
    randomDefaultColor &&
    !field.value &&
    !meta.initialValue &&
    !defaultFilled.current
  ) {
    setInputValue(getRandomColor());
    defaultFilled.current = true;
  }

  const showError = meta.touched && Boolean(meta.error);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setInputValue(e.target.value);
  };

  const handleColorClick = (color: string) => {
    setInputValue(color);
    setAnchorEl(null);
  };

  return (
    <FormControl
      error={showError}
      fullWidth
      style={{ minHeight: minHeight ? 64 : undefined }}
      data-cy={`color-palette-field`}
    >
      {label && <InputLabel shrink>{label}</InputLabel>}
      <div
        style={{
          display: 'flex',
          alignItems: 'flex-start',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center' }}>
          <Tooltip title={t('choose_color')}>
            <ColorPreview
              color={/^#[0-9A-F]{6}$/i.test(field.value) ? field.value : '#fff'}
              onClick={(e) => !disabled && setAnchorEl(e.currentTarget)}
              data-cy="color-preview"
            />
          </Tooltip>
          <TextField
            {...field}
            inputRef={inputRef}
            onChange={handleInputChange}
            placeholder="#RRGGBB"
            size="small"
            variant="outlined"
            disabled={disabled}
            error={showError}
            helperText={helperText}
            style={{ width: 120 }}
            required={required}
          />
        </div>
      </div>
      <Popover
        open={!!anchorEl}
        anchorEl={anchorEl}
        onClose={() => setAnchorEl(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
        transformOrigin={{ vertical: 'top', horizontal: 'left' }}
      >
        <PaletteGrid data-cy="color-palette-popover">
          {Object.values(theme.palette.paletteField).map((color) => (
            <PaletteColor
              key={color}
              color={color}
              selected={field.value?.toLowerCase() === color}
              onClick={() => handleColorClick(color)}
              data-cy={`palette-color`}
            />
          ))}
        </PaletteGrid>
      </Popover>
      {showError && <FormHelperText>{meta.error}</FormHelperText>}
    </FormControl>
  );
};
