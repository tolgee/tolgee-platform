import { useMemo } from 'react';
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

import { CommunityActivityChart } from 'tg.views/projects/community/contributors/CommunityActivityChart';
import { ContributorCard } from 'tg.component/community/ContributorCard';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import {
  ContributorsToolbar,
  StyledSectionTitle,
} from 'tg.views/projects/community/contributors/ContributorsToolbar';
import { ContributorsEmptyState } from 'tg.views/projects/community/contributors/ContributorsEmptyState';
import { useContributorsFilters } from 'tg.views/projects/community/contributors/useContributorsFilters';

const StyledCompactLanguagesSelect = styled(LanguagesSelect)`
  & .MuiInputBase-root {
    height: 35px;
    width: 200px;
  }
`;

const CONTRIBUTORS_PAGE_SIZE = 30;

export const ContributorsContent = () => {
  const project = useProject();
  const user = useUser();
  const { t } = useTranslate();
  const projectLanguages = useProjectLanguages();
  const theme = useTheme();
  const isSmall = useMediaQuery(theme.breakpoints.down('sm'));

  const {
    targetLanguages,
    period,
    setPeriodParam,
    years,
    isDaily,
    dates,
    statsQuery,
    sort,
    sortParams,
    setSortParam,
    searchInput,
    setSearchInput,
    search,
    selectedLanguages,
    setLanguageTags,
  } = useContributorsFilters(projectLanguages, project.createdAt);

  const contributorsQuery = {
    from: statsQuery.from,
    to: statsQuery.to,
    languageIds: statsQuery.languageIds,
    size: CONTRIBUTORS_PAGE_SIZE,
    sort: sortParams,
    withSuggestionsOnly: false,
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
    query: statsQuery,
    options: { keepPreviousData: true },
  });

  const totalContributors = contributors.data?.pages[0]?.page?.totalElements;
  const showSearch = useLatchedSearchVisibility(totalContributors, search);
  const showControls = showSearch || (totalContributors ?? 0) > 0;
  const people = useMemo(
    () =>
      (
        contributors.data?.pages.flatMap(
          (p) => p._embedded?.contributors ?? []
        ) ?? []
      ).map((c) => {
        const contributed = new Set(
          c.languages
            .filter((cl) => cl.contributions > 0)
            .map((cl) => cl.languageId)
        );
        return {
          ...c,
          contributorLanguages: projectLanguages.filter((l) =>
            contributed.has(l.id)
          ),
        };
      }),
    [contributors.data, projectLanguages]
  );

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
            axisRange={
              dates.to ? { from: dates.from, to: dates.to } : undefined
            }
          />
        ) : activity.isError ? (
          <QueryErrorAlert code={activity.error?.code} />
        ) : (
          <EmptyListMessage loading={activity.isLoading} />
        )}
      </Box>

      <ContributorsToolbar
        isSmall={isSmall}
        search={searchInput}
        onSearch={setSearchInput}
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
