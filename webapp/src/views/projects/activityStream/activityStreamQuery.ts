import { ACTIVITY_CATEGORIES, ActivityCategory } from './activityCategories';

export type ActivityFilter = {
  user: string;
  category: string;
  language: string;
};

export const DEFAULT_ACTIVITY_FILTER: ActivityFilter = {
  user: 'ALL',
  category: 'ALL',
  language: 'ALL',
};

export const activityFilterQuery = (
  filter: ActivityFilter,
  currentUserId: number | undefined
) => ({
  authorId:
    filter.user !== 'ALL' && filter.user !== 'EXCEPT_ME'
      ? Number(filter.user)
      : undefined,
  excludeAuthorId: filter.user === 'EXCEPT_ME' ? currentUserId : undefined,
  types:
    filter.category === 'ALL'
      ? undefined
      : ([
          ...ACTIVITY_CATEGORIES[filter.category as ActivityCategory],
        ] as string[]),
  languageIds:
    filter.language === 'ALL' ? undefined : [Number(filter.language)],
});
