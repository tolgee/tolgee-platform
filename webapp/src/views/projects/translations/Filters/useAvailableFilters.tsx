import { useTranslate } from '@tolgee/react';

import { useProject } from 'tg.hooks/useProject';
import { translationStates } from 'tg.constants/translationStates';
import { useTranslationsSelector } from '../context/TranslationsContext';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { Filters } from '../context/types';

export const NON_EXCLUSIVE_FILTERS = ['filterState', 'filterTag'];

type GroupType = {
  name: string | null;
  options: OptionType[];
  type?: 'states' | 'tags';
};

export type OptionType = {
  label: string;
  value: string | null;
  submenu?: OptionType[];
};

export const useAvailableFilters = (
  selectedLanguages?: string[]
): GroupType[] => {
  const languages = useTranslationsSelector((v) => v.languages);
  const project = useProject();
  const t = useTranslate();

  const tags = useApiQuery({
    url: '/v2/projects/{projectId}/tags',
    method: 'get',
    path: { projectId: project.id },
    query: { size: 1000 },
  });

  return [
    {
      name: null,
      type: 'tags',
      options: [
        {
          label: t('translations_filters_heading_tags', undefined, true),
          value: null,
          submenu:
            tags.data?._embedded?.tags?.map((val) => {
              return {
                label: val.name,
                value: encodeFilter({
                  filter: 'filterTag',
                  value: val.name,
                }),
              };
            }) || [],
        },
      ],
    },
    {
      name: t('translations_filters_heading_translations'),
      options: [
        {
          label: t('translations_filters_something_translated'),
          value: encodeFilter({
            filter: 'filterTranslatedAny',
            value: true,
          }),
        },
        {
          label: t('translations_filters_missing_translation'),
          value: encodeFilter({
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
          value: encodeFilter({
            filter: 'filterHasNoScreenshot',
            value: true,
          }),
        },
        {
          label: t('translations_filters_with_screenshots'),
          value: encodeFilter({
            filter: 'filterHasScreenshot',
            value: true,
          }),
        },
      ],
    },
    {
      name: t('translations_filters_heading_states'),
      type: 'states',
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
                  value: encodeFilter({
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

type FilterType = { filter: string; value: string | boolean | string[] };

export const decodeFilter = (value: string) => JSON.parse(value) as FilterType;
export const encodeFilter = (filter: FilterType) =>
  JSON.stringify({ filter: filter.filter, value: filter.value });

const findGroup = (availableFilters: GroupType[], value: string) =>
  availableFilters.find((g) => g.options?.find((o) => o.value === value));

export const toggleFilter = (
  filtersObj: Filters,
  availableFilters: GroupType[],
  rawValue: string
) => {
  const jsonValue = decodeFilter(rawValue);
  const filterName =
    typeof jsonValue === 'string' ? jsonValue : jsonValue.filter;
  const filterValue = typeof jsonValue === 'string' ? true : jsonValue.value;

  const group = findGroup(availableFilters, rawValue);

  let newFilters = {
    ...filtersObj,
  };

  // remove all filters from new value group
  // so the groups are exclusive
  group?.options?.forEach((o) => {
    if (o.value) newFilters[decodeFilter(o.value).filter] = undefined;
    o.submenu?.forEach((so) => {
      if (so.value) newFilters[decodeFilter(so.value).filter] = undefined;
    });
  });

  let newValue: any;
  if (NON_EXCLUSIVE_FILTERS.includes(filterName)) {
    if (filtersObj[filterName]?.includes(filterValue)) {
      newValue = filtersObj[filterName].filter((v) => v !== filterValue);
      // avoid keeping empty array, as it would stay in url
      newValue = newValue.length ? newValue : undefined;
    } else {
      newValue = [...(filtersObj[filterName] || []), filterValue];
    }
  } else {
    newValue = filtersObj[filterName] !== filterValue ? filterValue : undefined;
  }
  newFilters = {
    ...newFilters,
    [filterName]: newValue,
  };

  return newFilters;
};
