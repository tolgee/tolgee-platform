import enDateLocale from 'date-fns/locale/en-US';
import csDateLocale from 'date-fns/locale/cs';
import frDateLocale from 'date-fns/locale/fr';
import esDateLocale from 'date-fns/locale/es';
import deDateLocale from 'date-fns/locale/de';
import ptDateLocale from 'date-fns/locale/pt';
import daDateLocale from 'date-fns/locale/da';
import jaDateLocale from 'date-fns/locale/ja';
import zhCNDateLocale from 'date-fns/locale/zh-CN';

export const locales = {
  en: {
    name: 'English',
    flag: '🇬🇧',
    dateFnsLocale: enDateLocale,
    translations: () => import('../i18n/en.json').then((m) => m.default),
  },
  cs: {
    name: 'Čeština',
    flag: '🇨🇿',
    dateFnsLocale: csDateLocale,
    translations: () => import('../i18n/cs.json').then((m) => m.default),
  },
  fr: {
    name: 'Français',
    flag: '🇫🇷',
    dateFnsLocale: frDateLocale,
    translations: () => import('../i18n/fr.json').then((m) => m.default),
  },
  es: {
    name: 'Español',
    flag: '🇪🇸',
    dateFnsLocale: esDateLocale,
    translations: () => import('../i18n/es.json').then((m) => m.default),
  },
  de: {
    name: 'Deutsch',
    flag: '🇩🇪',
    dateFnsLocale: deDateLocale,
    translations: () => import('../i18n/de.json').then((m) => m.default),
  },
  pt: {
    name: 'Português',
    flag: '🇧🇷',
    dateFnsLocale: ptDateLocale,
    translations: () => import('../i18n/pt.json').then((m) => m.default),
  },
  da: {
    name: 'Dansk',
    flag: '🇩🇰',
    dateFnsLocale: daDateLocale,
    translations: () => import('../i18n/da.json').then((m) => m.default),
  },
  ja: {
    name: '日本語',
    flag: '🇯🇵',
    dateFnsLocale: jaDateLocale,
    translations: () => import('../i18n/ja.json').then((m) => m.default),
  },
  zh: {
    name: '简体中文',
    flag: '🇨🇳',
    dateFnsLocale: zhCNDateLocale,
    translations: () => import('../i18n/zh.json').then((m) => m.default),
  },
};
