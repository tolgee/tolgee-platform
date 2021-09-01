import { useTranslate } from '@tolgee/react';
import { useContextSelector } from 'use-context-selector';

import { translationStates } from 'tg.constants/translationStates';
import { TranslationsContext } from '../context/TranslationsContext';

export const NON_EXCLUSIVE_FILTERS = ['filterState'];

type GroupType = {
  name: string;
  options: OptionType[];
};

export type OptionType = {
  label: string;
  value: string | null;
  submenu?: OptionType[];
};

export const useAvailableFilters = (
  selectedLanguages?: string[]
): GroupType[] => {
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
      name: t('translations_filters_heading_states'),
      options:
        selectedLanguages?.map((l) => {
          const language = languages?.find((lang) => lang.tag === l)?.name || l;
          return {
            label: language,
            value: null,
            submenu: Object.entries(translationStates)
              // MACHINE_TRANSLATED is not supported yet
              .filter(([key]) => key !== 'MACHINE_TRANSLATED')
              .map(([key, value]) => {
                return {
                  label: t(value.translationKey),
                  value: JSON.stringify({
                    filter: 'filterState',
                    value: `${l},${key}`,
                  }),
                };
              }),
          };
        }) || [],
    },
  ];
};

export const decodeValue = (value: string) =>
  JSON.parse(value) as { filter: string; value: string | boolean | string[] };
