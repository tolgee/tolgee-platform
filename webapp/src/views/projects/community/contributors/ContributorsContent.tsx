import { format, subDays, subMonths } from 'date-fns';
import {
  Alert,
  Box,
  Divider,
  Link as MuiLink,
  MenuItem,
  Select,
  Typography,
  styled,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import { LoadingButton } from '@mui/lab';
import { T, useTranslate } from '@tolgee/react';
import { Link } from 'react-router-dom';
import { ArrowUpRight } from '@untitled-ui/icons-react';

import { LINKS, PARAMS, getProjectTranslationsUrl } from 'tg.constants/links';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { LanguagesSelect } from 'tg.component/common/form/LanguagesSelect/LanguagesSelect';
import { useLatchedSearchVisibility } from 'tg.views/projects/useLatchedSearchVisibility';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { useProject } from 'tg.hooks/useProject';
import { useUser } from 'tg.globalContext/helpers';
import { useApiInfiniteQuery, useApiQuery } from 'tg.service/http/useQueryApi';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';

import { CommunityActivityChart } from 'tg.views/projects/community/contributors/CommunityActivityChart';
import { ContributorCard } from 'tg.component/community/ContributorCard';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import {
  ContributorsToolbar,
  SortValue,
  StyledSectionTitle,
} from 'tg.views/projects/community/contributors/ContributorsToolbar';
import { ContributorsEmptyState } from 'tg.views/projects/community/contributors/ContributorsEmptyState';

type RelativePeriod = 'month' | 'quarter' | 'year';
type Period = RelativePeriod | number;

const StyledCompactLanguagesSelect = styled(LanguagesSelect)`
  & .MuiInputBase-root {
    height: 35px;
    width: 200px;
  }
`;

const isoDate = (date: Date) => format(date, 'yyyy-MM-dd');

const periodRange = (period: Period) => {
  if (typeof period === 'number') {
    return { from: `${period}-01-01`, to: `${period}-12-31` };
  }
  const today = new Date();
  const from =
    period === 'month'
      ? subDays(today, 29)
      : subMonths(today, period === 'quarter' ? 3 : 12);
  return { from: isoDate(from), to: isoDate(today) };
};

const CONTRIBUTORS_PAGE_SIZE = 30;

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

export const ContributorsContent = () => {
  const project = useProject();
  const user = useUser();
  const { t } = useTranslate();
  const projectLanguages = useProjectLanguages();
  const theme = useTheme();
  const isSmall = useMediaQuery(theme.breakpoints.down('sm'));

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
  const [contributorSearch, setContributorSearch] = useUrlSearchState(
    'search',
    { defaultVal: '' }
  );
  const search = contributorSearch.trim();

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
    project.createdAt ? new Date(project.createdAt).getFullYear() : currentYear
  );
  const years =
    firstYear === currentYear ? [currentYear] : [currentYear, firstYear];
  const period = parsePeriod(periodParam, years);
  const isDaily = period === 'month' || period === 'quarter';

  const dates = periodRange(period);
  const range = {
    ...dates,
    granularity: isDaily ? ('DAY' as const) : ('WEEK' as const),
    languageIds: languageFilterActive
      ? selectedLanguages.map((l) => l.id)
      : undefined,
  };

  const contributorsQuery = {
    ...range,
    size: CONTRIBUTORS_PAGE_SIZE,
    sort: SORT_PARAMS[sort],
    excludeEmpty: false,
    search: search || undefined,
  };
  const contributors = useApiInfiniteQuery({
    url: '/v2/projects/{projectId}/community/contributors',
    method: 'get',
    path: { projectId: project.id },
    query: contributorsQuery,
    options: {
      keepPreviousData: true,
      getNextPageParam: (lastPage) =>
        lastPage.page && lastPage.page.number! < lastPage.page.totalPages! - 1
          ? {
              path: { projectId: project.id },
              query: { ...contributorsQuery, page: lastPage.page.number! + 1 },
            }
          : null,
    },
  });

  const activity = useApiQuery({
    url: '/v2/projects/{projectId}/community/activity',
    method: 'get',
    path: { projectId: project.id },
    query: range,
    options: { keepPreviousData: true },
  });

  const totalContributors = contributors.data?.pages[0]?.page?.totalElements;
  const showSearch = useLatchedSearchVisibility(totalContributors, search);
  const showControls = showSearch || (totalContributors ?? 0) > 0;
  const people = (
    contributors.data?.pages.flatMap((p) => p._embedded?.contributors ?? []) ??
    []
  ).map((c) => ({
    ...c,
    contributorLanguages: projectLanguages.filter((l) =>
      c.languages.some((cl) => cl.languageId === l.id && cl.contributions > 0)
    ),
  }));

  const languagesSelectProps = {
    languages: targetLanguages,
    value: selectedLanguages.map((l) => l.tag),
    onChange: setLanguageTags,
    context: 'translations',
    allSelectedLabel: t('community_languages_all', 'All languages'),
  };

  return (
    <Box
      display="grid"
      gap={3}
      pt={{ xs: 1, md: 3 }}
      pb={6}
      data-cy="community-contributors-page"
    >
      <Box display="grid" gap={1.5}>
        <Box display="flex" alignItems="center" gap={1} flexWrap="wrap">
          <MuiLink
            component={Link}
            to={LINKS.PROJECT_COMMUNITY.build({
              [PARAMS.PROJECT_ID]: project.id,
            })}
            color="text.primary"
            variant="body2"
            underline="hover"
            display="inline-flex"
            alignItems="center"
            gap={1}
            data-cy="community-contributors-project-link"
          >
            <AvatarImg
              owner={{
                name: project.name,
                avatar: project.avatar,
                type: 'PROJECT',
                id: project.id,
              }}
              size={24}
            />
            {project.name}
          </MuiLink>
        </Box>
        <Box display="flex" alignItems="center" gap={1.5} flexWrap="wrap">
          <Typography variant="h4" component="h1" mr={1}>
            {t('community_contributions_title', 'Project contributions')}
          </Typography>
          <Box
            display="flex"
            alignItems="center"
            gap={1}
            width={{ xs: '100%', sm: 'auto' }}
          >
            <Select
              size="small"
              value={period}
              onChange={(e) => setPeriodParam(String(e.target.value))}
              sx={{
                width: isSmall ? 'auto' : 180,
                flexShrink: 0,
                height: isSmall ? 35 : 40,
              }}
              data-cy="community-contributors-period"
            >
              <MenuItem
                value="month"
                data-cy="community-contributors-period-item"
                data-cy-period="month"
              >
                {t('community_period_month', 'Last 30 days')}
              </MenuItem>
              <MenuItem
                value="quarter"
                data-cy="community-contributors-period-item"
                data-cy-period="quarter"
              >
                {t('community_period_quarter', 'Last 3 months')}
              </MenuItem>
              <MenuItem
                value="year"
                data-cy="community-contributors-period-item"
                data-cy-period="year"
              >
                {t('community_period_year', 'Last 12 months')}
              </MenuItem>
              <Divider />
              {years.map((year) => (
                <MenuItem
                  key={year}
                  value={year}
                  data-cy="community-contributors-period-item"
                  data-cy-period={String(year)}
                >
                  {year}
                </MenuItem>
              ))}
            </Select>
            {targetLanguages.length === 0 ? null : isSmall ? (
              <StyledCompactLanguagesSelect {...languagesSelectProps} />
            ) : (
              <LanguagesSelect {...languagesSelectProps} />
            )}
          </Box>
        </Box>
      </Box>

      <Box display="grid" gap={1.5}>
        <Box
          display="flex"
          alignItems="center"
          justifyContent="space-between"
          gap={1}
        >
          <StyledSectionTitle data-cy="community-activity-title">
            {t('community_activity_title', 'Activity')}
          </StyledSectionTitle>
          <MuiLink
            component={Link}
            to={getProjectTranslationsUrl(project.id, {
              languages: selectedLanguages.map((l) => l.tag),
              filters: { filterHasSuggestions: true },
            })}
            variant="body2"
            color="text.secondary"
            underline="none"
            display="inline-flex"
            alignItems="center"
            gap={0.5}
            sx={{ '&:hover': { color: 'primary.main' } }}
            data-cy="community-chart-suggestions-link"
          >
            {t('community_show_all_suggestions', 'Show all suggestions')}
            <ArrowUpRight width={16} height={16} />
          </MuiLink>
        </Box>
        {activity.data ? (
          <CommunityActivityChart
            buckets={activity.data.buckets}
            granularity={isDaily ? 'day' : 'week'}
            axisRange={typeof period === 'number' ? dates : undefined}
          />
        ) : activity.isError ? (
          <QueryErrorAlert code={activity.error?.code} />
        ) : (
          <EmptyListMessage loading={activity.isLoading} />
        )}
      </Box>

      <ContributorsToolbar
        isSmall={isSmall}
        search={contributorSearch}
        onSearch={setContributorSearch}
        showSearch={showSearch}
        showControls={showControls}
        sort={sort}
        onSortChange={setSortParam}
      />

      {contributors.isLoading ? (
        <EmptyListMessage loading={true} />
      ) : people.length ? (
        <Box
          display="grid"
          gridTemplateColumns={{
            xs: 'minmax(0, 1fr)',
            sm: 'repeat(2, minmax(0, 1fr))',
            lg: 'repeat(3, minmax(0, 1fr))',
          }}
          gap={2}
        >
          {people.map((p) => (
            <ContributorCard
              key={p.id}
              id={p.id}
              name={p.name}
              avatar={p.avatar}
              languages={p.contributorLanguages}
              contributions={p.contributions}
              suggested={p.suggested}
              accepted={p.accepted}
              declined={p.declined}
              firstContributionAt={p.firstContributionAt}
              lastContributionAt={p.lastContributionAt}
              isMe={p.id === user?.id}
              profilePublic={p.profilePublic}
            />
          ))}
          {contributors.hasNextPage && (
            <Box gridColumn="1 / -1" display="flex" justifyContent="center">
              <LoadingButton
                loading={contributors.isFetchingNextPage}
                onClick={() => contributors.fetchNextPage()}
                data-cy="community-contributors-load-more"
              >
                {t('global_load_more')}
              </LoadingButton>
            </Box>
          )}
        </Box>
      ) : contributors.isError ? (
        <QueryErrorAlert code={contributors.error?.code} />
      ) : (
        <ContributorsEmptyState searching={Boolean(search)} />
      )}
    </Box>
  );
};

const QueryErrorAlert = ({ code }: { code?: unknown }) => (
  <Alert severity="error">
    {typeof code === 'string' ? (
      <TranslatedError code={code} />
    ) : (
      <T keyName="simple_paginated_list_error_message" />
    )}
  </Alert>
);
