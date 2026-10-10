import { useImperativeHandle, useMemo, useState, forwardRef } from 'react';
import { Box } from '@mui/material';
import { MuiTelInput, MuiTelInputInfo, matchIsValidTel } from 'mui-tel-input';
import { useTranslate } from '@tolgee/react';
import { detectCountry } from './detectCountry';
import { renderCountryFlag } from './countryFlag';

export type PhoneNumberFieldHandle = {
  /** The number in E.164, or an empty string. Null when what is typed is not a number. */
  read: () => string | null;
};

type Props = {
  label?: React.ReactNode;
  helperText?: React.ReactNode;
  initialValue?: string;
  inputProps?: Record<string, unknown>;
};

/**
 * mui-tel-input otherwise renders every flag as an <img> from flagcdn.com, a
 * third-party request on the page it sits on.
 */
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

    // `info` stays null until the field is touched, so emptiness has to come
    // from the value while a prefilled number is still untouched.
    const isEmpty = info ? !info.nationalNumber : !value;

    useImperativeHandle(ref, () => ({
      read: () => {
        if (isEmpty) {
          return '';
        }
        if (!matchIsValidTel(value)) {
          setInvalid(true);
          return null;
        }
        return info?.numberValue ?? value;
      },
    }));

    return (
      <Box display="grid" gap="6px">
        {label && (
          <Box
            fontSize={(theme) => theme.typography.body2.fontSize}
            fontWeight={500}
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
