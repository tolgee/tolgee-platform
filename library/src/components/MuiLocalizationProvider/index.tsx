import { LocalizationProvider } from '@mui/x-date-pickers';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { FC } from 'react';
import { useCurrentLanguage } from 'lib.hooks/useCurrentLanguage';
import { locales } from 'lib.constants/locales';

export const MuiLocalizationProvider: FC = (props) => {
  const language =
    useCurrentLanguage() ?? ('en' satisfies keyof typeof locales);

  return (
    <LocalizationProvider
      dateAdapter={AdapterDateFns}
      adapterLocale={locales[language].dateFnsLocale}
    >
      {props.children}
    </LocalizationProvider>
  );
};
