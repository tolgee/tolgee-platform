export const getLanguageDirection = (languageTag?: string): Direction => {
  const rtlLangs = [
    'ar',
    'arc',
    'dv',
    'fa',
    'ha',
    'he',
    'khw',
    'ks',
    'ku',
    'ps',
    'ur',
    'yi',
  ];
  const languageIso = languageTag?.replace(/^([a-zA-Z]+)(.*?)$/, '$1');
  return rtlLangs.indexOf(languageIso || '') > -1 ? 'rtl' : 'ltr';
};

export type Direction = 'ltr' | 'rtl';
