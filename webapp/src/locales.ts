import enDateLocale from 'date-fns/locale/en-US';
import csDateLocale from 'date-fns/locale/cs';
import frDateLocale from 'date-fns/locale/fr';
import esDateLocale from 'date-fns/locale/es';
import deDateLocale from 'date-fns/locale/de';
import ptDateLocale from 'date-fns/locale/pt';
import daDateLocale from 'date-fns/locale/da';
import jaDateLocale from 'date-fns/locale/ja';

export const locales = {
  en: {
    name: 'English',
    flag: '🇬🇧',
    dateFnsLocale: enDateLocale,
  },
  cs: {
    name: 'Čeština',
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
    flag: '🇧🇷',
    dateFnsLocale: ptDateLocale,
  },
  da: {
    name: 'Dansk',
    flag: '🇩🇰',
    dateFnsLocale: daDateLocale,
  },
  ja: {
    name: '日本語',
    flag: '🇯🇵',
    dateFnsLocale: jaDateLocale,
  },
};
