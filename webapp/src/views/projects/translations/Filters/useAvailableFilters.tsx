import { useTranslate } from '@tolgee/react';
import { useContextSelector } from 'use-context-selector';
import { TranslationsContext } from '../TranslationsContext';

export const useAvailableFilters = (selectedLanguages?: string[]) => {
  const languages = useContextSelector(TranslationsContext, (v) => v.languages);
  const t = useTranslate();

  return [
    {
      name: t('translations_filters_heading_translations'),
      options: [
        {
          label: t('translations_filters_something_translated'),
          value: JSON.stringify({
            filter: 'filterTranslatedAny',
            value: true,
          }),
        },
        {
          label: t('translations_filters_missing_translation'),
          value: JSON.stringify({
            filter: 'filterUntranslatedAny',
            value: true,
          }),
        },
      ],
    },
    {
      name: t('translations_filters_heading_screenshots'),
      options: [
        {
          label: t('translations_filters_no_screenshots'),
          value: JSON.stringify({
            filter: 'filterHasNoScreenshot',
            value: true,
          }),
        },
        {
          label: t('translations_filters_with_screenshots'),
          value: JSON.stringify({
            filter: 'filterHasScreenshot',
            value: true,
          }),
        },
      ],
    },
    {
      name: t('translations_filters_heading_languages'),
      options: selectedLanguages?.flatMap((l) => {
        const language = languages?.find((lang) => lang.tag === l)?.name || l;
        return [
          {
            label: t('translations_filters_translated_in', { language }),
            value: JSON.stringify({
              filter: 'filterTranslatedInLang',
              value: l,
            }),
          },
          {
            label: t('translations_filters_not_translated_in', { language }),
            value: JSON.stringify({
              filter: 'filterUntranslatedInLang',
              value: l,
            }),
          },
        ];
      }),
    },
  ];
};
