import { useImperativeHandle, useMemo, useState, forwardRef } from 'react';
import { Box } from '@mui/material';
import { MuiTelInput, MuiTelInputInfo } from 'mui-tel-input';
import { useTranslate } from '@tolgee/react';
import { detectCountry } from 'tg.component/common/form/fields/detectCountry';
import { renderCountryFlag } from 'tg.component/common/form/fields/countryFlag';
import { readPhoneValue } from 'tg.component/common/form/fields/phoneValue';

export type PhoneNumberFieldHandle = {
  /**
   * The number in E.164, or an empty string. Marks the field invalid and
   * returns null when what is typed is not a number.
   */
  validateAndRead: () => string | null;
};

type Props = {
  label?: React.ReactNode;
  helperText?: React.ReactNode;
  initialValue?: string;
  inputProps?: Record<string, unknown>;
};

export const PhoneNumberField = forwardRef<PhoneNumberFieldHandle, Props>(
  function PhoneNumberField(
    { label, helperText, initialValue = '', inputProps },
    ref
  ) {
    const { t } = useTranslate();
    const [value, setValue] = useState(initialValue);
    const [info, setInfo] = useState<MuiTelInputInfo | null>(null);
    const [invalid, setInvalid] = useState(false);
    const defaultCountry = useMemo(detectCountry, []);

    useImperativeHandle(ref, () => ({
      validateAndRead: () => {
        const read = readPhoneValue(value, info);
        if (read === null) {
          setInvalid(true);
        }
        return read;
      },
    }));

    return (
      <Box display="grid" gap="6px">
        {label && (
          <Box
            fontSize={(theme) => theme.typography.body2.fontSize}
            fontWeight={(theme) => theme.typography.fontWeightMedium}
          >
            {label}
          </Box>
        )}
        <MuiTelInput
          value={value}
          onChange={(next, nextInfo) => {
            setValue(next);
            setInfo(nextInfo);
            setInvalid(false);
          }}
          error={invalid}
          helperText={
            invalid
              ? t(
                  'phone_number_field_invalid',
                  "That doesn't look like a valid phone number."
                )
              : helperText
          }
          defaultCountry={defaultCountry}
          forceCallingCode
          focusOnSelectCountry
          getFlagElement={(isoCode) => renderCountryFlag(isoCode)}
          inputProps={inputProps}
        />
      </Box>
    );
  }
);
