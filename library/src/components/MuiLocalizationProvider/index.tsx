import { LocalizationProvider } from '@mui/x-date-pickers';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { FC } from 'react';
import { useCurrentLanguage } from '@tginternal/library/hooks/useCurrentLanguage';
import { locales } from '@tginternal/library/constants/locales';

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
