import { ReactNode } from 'react';
import { useTranslate } from '@tolgee/react';
import { useField } from 'formik';

import { TextField } from '../common/form/fields/TextField';
import { Box, useTheme } from '@mui/material';
import zxcvbn from 'zxcvbn';
import { TextFieldProps } from 'tg.component/common/TextField';

type SetPasswordFieldsProps = {
  label: ReactNode;
};

type Props = SetPasswordFieldsProps & TextFieldProps;

export const PasswordFieldWithValidation: React.FC<Props> = (props) => {
  const { t } = useTranslate();

  function getScoreTranslation(strength: number) {
    switch (strength) {
      case 0:
        return t('password-strength-very-weak');
      case 1:
        return t('password-strength-weak');
      case 2:
        return t('password-strength-medium');
      case 3:
        return t('password-strength-strong');
      default:
        return t('password-strength-very-strong');
    }
  }

  const theme = useTheme();

  function getScoreColor(strength: number) {
    switch (strength) {
      case 0:
      case 1:
        return theme.palette.error.main;
      case 2:
        return theme.palette.text.primary;
      case 3:
      case 4:
        return theme.palette.success.main;
      default:
        return theme.palette.text.primary;
    }
  }

  function getPasswordCheck(value: string) {
    if (value.length >= 8) {
      const passwordCheck = zxcvbn(value);
      return (
        <Box sx={{ color: getScoreColor(passwordCheck.score) }}>
          {getScoreTranslation(passwordCheck.score)}
        </Box>
      );
    }
  }

  const [field] = useField({
    name: 'password',
    validate: (value: string) => {
      const score = zxcvbn(value).score;
      return score <= 1
        ? (getPasswordCheck(value) as unknown as string)
        : undefined;
    },
  });

  return (
    <>
      <TextField
        {...props}
        helperText={getPasswordCheck(field.value)}
        name="password"
        type="password"
      />
    </>
  );
};

export default PasswordFieldWithValidation;
