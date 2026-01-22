import enDateLocale from 'date-fns/locale/en-US';
import csDateLocale from 'date-fns/locale/cs';
import frDateLocale from 'date-fns/locale/fr';
import esDateLocale from 'date-fns/locale/es';
import deDateLocale from 'date-fns/locale/de';
import ptDateLocale from 'date-fns/locale/pt';
import daDateLocale from 'date-fns/locale/da';
import jaDateLocale from 'date-fns/locale/ja';
import zhCNDateLocale from 'date-fns/locale/zh-CN';
import ukrDateLocale from 'date-fns/locale/uk';
import huDateLocale from 'date-fns/locale/hu';
import itDateLocale from 'date-fns/locale/it';
import nlDateLocale from 'date-fns/locale/nl';
import noDateLocale from 'date-fns/locale/nb';
import plDateLocale from 'date-fns/locale/pl';
import roDateLocale from 'date-fns/locale/ro';
import ruDateLocale from 'date-fns/locale/ru';

export const locales = {
  en: {
    name: 'English',
    flag: '🇬🇧',
    dateFnsLocale: enDateLocale,
    translations: () =>
      import('../../../webapp/src/i18n/en.json').then((m) => m.default),
  },
  cs: {
    name: 'Čeština',
    flag: '🇨🇿',
    dateFnsLocale: csDateLocale,
    translations: () =>
      import('../../../webapp/src/i18n/cs.json').then((m) => m.default),
  },
  fr: {
    name: 'Français',
    flag: '🇫🇷',
    dateFnsLocale: frDateLocale,
    translations: () =>
      import('../../../webapp/src/i18n/fr.json').then((m) => m.default),
  },
  es: {
    name: 'Español',
    flag: '🇪🇸',
    dateFnsLocale: esDateLocale,
    translations: () =>
      import('../../../webapp/src/i18n/es.json').then((m) => m.default),
  },
  de: {
    name: 'Deutsch',
    flag: '🇩🇪',
    dateFnsLocale: deDateLocale,
    translations: () =>
      import('../../../webapp/src/i18n/de.json').then((m) => m.default),
  },
  pt: {
    name: 'Português',
    flag: '🇧🇷',
    dateFnsLocale: ptDateLocale,
    translations: () =>
      import('../../../webapp/src/i18n/pt.json').then((m) => m.default),
  },
  da: {
    name: 'Dansk',
    flag: '🇩🇰',
    dateFnsLocale: daDateLocale,
    translations: () =>
      import('../../../webapp/src/i18n/da.json').then((m) => m.default),
  },
  ja: {
    name: '日本語',
    flag: '🇯🇵',
    dateFnsLocale: jaDateLocale,
    translations: () =>
      import('../../../webapp/src/i18n/ja.json').then((m) => m.default),
  },
  zh: {
    name: '简体中文',
    flag: '🇨🇳',
    dateFnsLocale: zhCNDateLocale,
    translations: () =>
      import('../../../webapp/src/i18n/zh.json').then((m) => m.default),
  },
  uk: {
    name: 'Українська',
    flag: '🇺🇦',
    dateFnsLocale: ukrDateLocale,
    translations: () =>
      import('../../../webapp/src/i18n/uk-UA.json').then((m) => m.default),
  },
  hu: {
    name: 'Magyar',
    flag: '🇭🇺',
    dateFnsLocale: huDateLocale,
    translations: () =>
      import('../../../webapp/src/i18n/hu.json').then((m) => m.default),
  },
  it: {
    name: 'Italiano',
    flag: '🇮🇹',
    dateFnsLocale: itDateLocale,
    translations: () =>
      import('../../../webapp/src/i18n/it-IT.json').then((m) => m.default),
  },
  nl: {
    name: 'Nederlands',
    flag: '🇳🇱',
    dateFnsLocale: nlDateLocale,
    translations: () =>
      import('../../../webapp/src/i18n/nl.json').then((m) => m.default),
  },
  no: {
    name: 'Norsk',
    flag: '🇳🇴',
    dateFnsLocale: noDateLocale,
    translations: () =>
      import('../../../webapp/src/i18n/no.json').then((m) => m.default),
  },
  pl: {
    name: 'Polski',
    flag: '🇵🇱',
    dateFnsLocale: plDateLocale,
    translations: () =>
      import('../../../webapp/src/i18n/pl.json').then((m) => m.default),
  },
  ro: {
    name: 'Română',
    flag: '🇷🇴',
    dateFnsLocale: roDateLocale,
    translations: () =>
      import('../../../webapp/src/i18n/ro.json').then((m) => m.default),
  },
  ru: {
    name: 'Русский',
    flag: '🇷🇺',
    dateFnsLocale: ruDateLocale,
    translations: () =>
      import('../../../webapp/src/i18n/ru.json').then((m) => m.default),
  },
};
