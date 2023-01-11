import enDateLocale from 'date-fns/locale/en-US';
import csDateLocale from 'date-fns/locale/cs';
import frDateLocale from 'date-fns/locale/fr';
import esDateLocale from 'date-fns/locale/es';
import deDateLocale from 'date-fns/locale/de';
import ptDateLocale from 'date-fns/locale/pt';

export const locales = {
  en: {
    name: 'English',
    flag: '🇬🇧',
    dateFnsLocale: enDateLocale,
  },
  cs: {
    name: 'Česky',
    flag: '🇨🇿',
    dateFnsLocale: csDateLocale,
  },
  fr: {
    name: 'Français',
    flag: '🇫🇷',
    dateFnsLocale: frDateLocale,
  },
  es: {
    name: 'Español',
    flag: '🇪🇸',
    dateFnsLocale: esDateLocale,
  },
  de: {
    name: 'Deutsch',
    flag: '🇩🇪',
    dateFnsLocale: deDateLocale,
  },
  pt: {
    name: 'Português',
    flag: '🇵🇹',
    dateFnsLocale: ptDateLocale,
  },
};
