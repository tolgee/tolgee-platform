import { components, operations } from 'tg.service/apiSchema.generated';

type TranslationsQueryType =
  operations['getTranslations']['parameters']['query'];

export type LanguageModel = components['schemas']['LanguageModel'];

export type FiltersType = Pick<
  TranslationsQueryType,
  | 'filterHasNoScreenshot'
  | 'filterHasScreenshot'
  | 'filterTranslatedAny'
  | 'filterUntranslatedAny'
  | 'filterTranslatedInLang'
  | 'filterUntranslatedInLang'
  | 'filterState'
  | 'filterTag'
>;

// Filters that can have multiple values
export const NON_EXCLUSIVE_FILTERS = [
  'filterState',
  'filterTag',
  'filterNamespace',
];

// filters that are taken as one value
export const UNIT_FILTERS = ['filterOutdatedLanguage'];

export type FilterType = { filter: string; value: string | boolean | string[] };

export type GroupType = {
  name: string | null;
  options: OptionType[];
  type?: 'states' | 'multi';
};

export type OptionType = {
  label: string;
  value: string | null;
  submenu?: OptionType[];
};

export const decodeFilter = (value: string) => JSON.parse(value) as FilterType;
export const encodeFilter = (filter: FilterType) =>
  JSON.stringify({ filter: filter.filter, value: filter.value });

export const findGroup = (availableFilters: GroupType[], value: string) =>
  availableFilters.find((g) => g.options?.find((o) => o.value === value));

export const toggleFilter = (
  filtersObj: FiltersType,
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
    newValue =
      JSON.stringify(filtersObj[filterName]) !== JSON.stringify(filterValue)
        ? filterValue
        : undefined;
  }
  newFilters = {
    ...newFilters,
    [filterName]: newValue,
  };

  return newFilters;
};
