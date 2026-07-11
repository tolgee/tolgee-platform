import { useField } from 'formik';
import { useEffect, useState } from 'react';
import {
  Box,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  TextField as MuiTextField,
  useTheme,
} from '@mui/material';
import { T } from '@tolgee/react';
import { DatePicker } from '@mui/x-date-pickers';
import { ExpirationDateOptions } from './useExpirationDateOptions';

export const ExpirationDateField = ({
  options,
}: {
  options: ExpirationDateOptions;
}) => {
  const [input, _, helpers] = useField('expiresAt');
  const theme = useTheme();

  const getInitialSelectValue = () =>
    options.find((o) => o.time === input.value)?.value || 'custom';

  const [selectValue, setSelectValue] = useState(getInitialSelectValue());

  useEffect(() => {
    if (selectValue !== 'custom') {
      const newValue = options.find((o) => o.value === selectValue)?.time;
      helpers.setValue(newValue);
    }
  }, [selectValue]);

  return (
    <>
      <Box
        sx={{ display: 'flex', gap: 1, mt: 2, mb: 2 }}
        data-cy="expiration-date-field"
      >
        <Box sx={{ display: 'flex', flexGrow: 1 }}>
          <FormControl fullWidth sx={{ display: 'flex' }}>
            <InputLabel id="expiration-label">
              <T keyName={'expiration-label'} />
            </InputLabel>
            <Select
              data-cy="expiration-select"
              fullWidth
              labelId="expiration-label"
              id="expiration-select"
              value={selectValue}
              label={<T keyName={'expiration-label'} />}
              onChange={(e) => {
                setSelectValue(e.target.value);
              }}
            >
              {options.map((option) => (
                <MenuItem key={option.value} value={option.value}>
                  {option.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Box>
        {selectValue === 'custom' && (
          <Box data-cy="expiration-date-picker">
            <DatePicker
              disablePast
              label={<T keyName={'expiration-date-picker-label'} />}
              value={input.value}
              onChange={(newValue) => {
                if (newValue) {
                  helpers.setValue(new Date(newValue).getTime());
                }
              }}
              slots={{ textField: MuiTextField }}
              desktopModeMediaQuery={theme.breakpoints.up('md')}
            />
          </Box>
        )}
      </Box>
    </>
  );
};
