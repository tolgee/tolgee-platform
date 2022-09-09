import { LocalizationProvider } from '@mui/x-date-pickers';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { FC } from 'react';
import { useCurrentLanguage } from '@tolgee/react';
import { locales } from '../locales';

export const MuiLocalizationProvider: FC = (props) => {
  const getLang = useCurrentLanguage();

  return (
    <LocalizationProvider
      dateAdapter={AdapterDateFns}
      adapterLocale={locales[getLang()].dateFnsLocale}
    >
      {props.children}
    </LocalizationProvider>
  );
};
