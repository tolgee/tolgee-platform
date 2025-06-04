import { useState, useRef, useEffect } from 'react';
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
  colors: Map<string, string> | object;
  darkColors?: Map<string, string> | object;
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
  colors,
  darkColors,
  minHeight = true,
  randomDefaultColor = true,
  required = false,
}: Props) => {
  const [field, meta, helpers] = useField(name);
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);
  const [pickedColorKey, setPickedColorKey] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const defaultFilled = useRef(false);
  const theme = useTheme();
  const { t } = useTranslate();
  const isDarkMode = theme.palette.mode === 'dark';

  useEffect(() => {
    if (
      randomDefaultColor &&
      !field.value &&
      !meta.initialValue &&
      !defaultFilled.current
    ) {
      const key = getRandomColorKey();
      setInputValue(getReferenceColor(key));
      setPickedColorKey(key);
      defaultFilled.current = true;
    }
  }, [randomDefaultColor, field.value, meta.initialValue]);

  const palette = (
    typeof colors === 'object' ? new Map(Object.entries(colors)) : colors
  ) as Map<string, string>;

  const darkPalette = (
    typeof darkColors === 'object'
      ? new Map(Object.entries(darkColors))
      : darkColors
  ) as Map<string, string>;

  const activePalette = isDarkMode && darkPalette ? darkPalette : palette;

  const setInputValue = (color: string) => {
    helpers.setValue(color.toUpperCase());
  };

  function getReferenceColor(key: string) {
    const color = palette.get(key);
    if (!color) {
      throw new Error(`Color key "${key}" not found in palette.`);
    }
    return color;
  }

  function getSelectedColor() {
    return pickedColorKey ? activePalette.get(pickedColorKey) : field.value;
  }

  const getRandomColorKey = () => {
    return [...activePalette.keys()][
      Math.floor(Math.random() * activePalette.size)
    ];
  };

  const showError = meta.touched && Boolean(meta.error);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setPickedColorKey(findColorKey(e.target.value) || null);
    setInputValue(e.target.value);
  };

  const findColorKey = (color: string) => {
    const colorLower = color.toLowerCase();
    return [...palette].find(
      ([, value]) => value.toLowerCase() === colorLower
    )?.[0];
  };

  const handleColorClick = (key: string) => {
    setInputValue(getReferenceColor(key));
    setPickedColorKey(key);
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
              color={getSelectedColor()}
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
          {[...activePalette].map(([key, color]) => (
            <PaletteColor
              key={key}
              color={color}
              selected={pickedColorKey === key}
              onClick={() => handleColorClick(key)}
              data-cy={`palette-color`}
            />
          ))}
        </PaletteGrid>
      </Popover>
      {showError && <FormHelperText>{meta.error}</FormHelperText>}
    </FormControl>
  );
};
