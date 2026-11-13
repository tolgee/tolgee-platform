import { formatISO, subDays, subMonths } from 'date-fns';
import { useDebounce } from 'use-debounce';

import { components } from 'tg.service/apiSchema.generated';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { SortValue } from 'tg.views/projects/community/contributors/ContributorsToolbar';

type LanguageModel = components['schemas']['LanguageModel'];

type RelativePeriod = 'month' | 'quarter' | 'year';
export type Period = RelativePeriod | number;

export const useContributorsFilters = (
  projectLanguages: LanguageModel[],
  projectCreatedAt: number | undefined
) => {
  const targetLanguages = projectLanguages.filter((l) => !l.base);

  const [periodParam, setPeriodParam] = useUrlSearchState('period', {
    defaultVal: 'month',
  });
  const [languagesParam, setLanguagesParam] = useUrlSearchState('languages', {
    array: true,
  });
  const [sortParam, setSortParam] = useUrlSearchState('sort', {
    defaultVal: 'suggested',
  });
  const sort = (
    Object.keys(SORT_PARAMS).includes(sortParam) ? sortParam : 'suggested'
  ) as SortValue;
  const [searchInput, setSearchInput] = useUrlSearchState('search', {
    defaultVal: '',
  });
  const [search] = useDebounce(searchInput.trim(), 500);

  const languagesFromUrl = targetLanguages.filter((l) =>
    languagesParam?.includes(l.tag)
  );
  const selectedLanguages = languagesFromUrl.length
    ? languagesFromUrl
    : targetLanguages;
  const languageFilterActive =
    selectedLanguages.length < targetLanguages.length;
  const setLanguageTags = (tags: string[]) =>
    setLanguagesParam(
      tags.length === targetLanguages.length ? undefined : tags
    );

  const currentYear = new Date().getFullYear();
  const firstYear = Math.max(
    currentYear - 1,
    projectCreatedAt ? new Date(projectCreatedAt).getFullYear() : currentYear
  );
  const years =
    firstYear === currentYear ? [currentYear] : [currentYear, firstYear];
  const period = parsePeriod(periodParam, years);
  const isDaily = period === 'month' || period === 'quarter';

  const dates = periodRange(period);
  const statsQuery = {
    ...dates,
    granularity: isDaily ? ('DAY' as const) : ('WEEK' as const),
    languageIds: languageFilterActive
      ? selectedLanguages.map((l) => l.id)
      : undefined,
  };

  return {
    targetLanguages,
    period,
    setPeriodParam,
    years,
    isDaily,
    dates,
    statsQuery,
    sort,
    sortParams: SORT_PARAMS[sort],
    setSortParam,
    searchInput,
    setSearchInput,
    search,
    selectedLanguages,
    setLanguageTags,
  };
};

const periodRange = (period: Period) => {
  if (typeof period === 'number') {
    return { from: `${period}-01-01`, to: `${period}-12-31` };
  }
  const today = new Date();
  const from =
    period === 'month'
      ? subDays(today, 29)
      : subMonths(today, period === 'quarter' ? 3 : 12);
  return {
    from: formatISO(from, { representation: 'date' }),
    to: formatISO(today, { representation: 'date' }),
  };
};

const SORT_PARAMS: Record<SortValue, string[]> = {
  suggested: ['suggested,desc', 'accepted,desc'],
  contributions: ['contributions,desc'],
  accepted: ['accepted,desc'],
  recent: ['lastContributionAt,desc'],
};

const parsePeriod = (value: string, years: number[]): Period =>
  value === 'quarter' || value === 'year'
    ? value
    : years.includes(Number(value))
    ? Number(value)
    : 'month';
