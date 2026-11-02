import { useMemo } from 'react';
import { formatISO, subYears } from 'date-fns';
import {
  Alert,
  Box,
  Link as MuiLink,
  Tooltip,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Link } from 'react-router-dom';
import { ArrowLeft } from '@untitled-ui/icons-react';

import { LINKS } from 'tg.constants/links';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { BaseView } from 'tg.component/layout/BaseView';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { useProject } from 'tg.hooks/useProject';
import { useReportOnce } from 'tg.hooks/useReportEvent';
import { useUser } from 'tg.globalContext/helpers';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { useApiQuery } from 'tg.service/http/useQueryApi';

import { CommunityLanguages } from 'tg.views/projects/community/CommunityLanguages';
import { CommunitySidebar } from 'tg.views/projects/community/CommunitySidebar';
import { ExpandableMarkdown } from 'tg.views/projects/community/ExpandableMarkdown';
import { useCommunityTopContributors } from 'tg.views/projects/community/useCommunityTopContributors';

export const CommunityProjectView = () => {
  const project = useProject();
  const user = useUser();
  const theme = useTheme();
  const { t } = useTranslate();
  const compact = useMediaQuery(theme.breakpoints.down('md'));
  const topBarTop = useGlobalContext(
    (c) => c.layout.topBarHeight + c.layout.topBannerHeight
  );
  const stickyTop = topBarTop + 16;

  useReportOnce('COMMUNITY_PAGE_VIEW', { projectId: project.id });

  const description = project.communityDescription || project.description;

  const stats = useApiQuery({
    url: '/v2/projects/{projectId}/stats',
    method: 'get',
    path: { projectId: project.id },
    query: {},
  });

  const twoYearsAgo = useMemo(
    () => formatISO(subYears(new Date(), 2), { representation: 'date' }),
    []
  );

  const top = useCommunityTopContributors(project.id);

  const contributors = useApiQuery({
    url: '/v2/projects/{projectId}/community/contributors',
    method: 'get',
    path: { projectId: project.id },
    query: {
      size: 1,
      sort: ['lastContributionAt,desc'],
      from: twoYearsAgo,
    },
  });

  const lastActivity =
    contributors.data?._embedded?.contributors?.[0]?.lastContributionAt;

  const me = useApiQuery({
    url: '/v2/projects/{projectId}/community/contributors/{userId}',
    method: 'get',
    path: { projectId: project.id, userId: user?.id ?? 0 },
    query: {},
    fetchOptions: { disable404Redirect: true, disableErrorNotification: true },
    options: { enabled: !!user, retry: false },
  });

  const myLanguageIds = useMemo(
    () =>
      new Set(
        me.data?.languages
          .filter((l) => l.contributions > 0)
          .map((l) => l.languageId)
      ),
    [me.data]
  );

  const header = (
    <Box
      display="grid"
      gridTemplateColumns="50px minmax(0, 1fr)"
      columnGap={2.5}
      rowGap={1}
    >
      <AvatarImg
        owner={{
          name: project.name,
          avatar: project.avatar,
          type: 'PROJECT',
          id: project.id,
        }}
        size={50}
      />
      <Box
        height={50}
        display="flex"
        flexDirection="column"
        justifyContent="space-between"
        minWidth={0}
      >
        {project.organizationOwner && (
          <Tooltip
            title={t(
              'community_owner_tooltip',
              'Organization that owns this project'
            )}
            placement="top-start"
          >
            <Typography
              variant="body2"
              color="text.secondary"
              lineHeight="18px"
              alignSelf="start"
            >
              {project.organizationOwner.name}
            </Typography>
          </Tooltip>
        )}
        <Typography variant="h4" component="h1" lineHeight="28px" noWrap>
          {project.name}
        </Typography>
      </Box>
      {description && (
        <Box gridColumn={2}>
          <ExpandableMarkdown
            text={description}
            data-cy="community-project-description"
          />
        </Box>
      )}
    </Box>
  );

  const sidebar = (
    <CommunitySidebar
      compact={compact}
      guidelines={project.communityGuidelines || undefined}
      showInAppBanner={project.communityInContext}
      appUrl={project.communityAppUrl || undefined}
      top={top}
      details={{
        projectId: project.id,
        lastActivity,
        createdAt: project.createdAt,
        wordsToTranslate: stats.data?.baseWordsCount,
        contributors: contributors.data?.page?.totalElements,
      }}
    />
  );

  const languages = stats.data ? (
    <CommunityLanguages
      projectId={project.id}
      baseLanguageTag={project.baseLanguage?.tag}
      languages={stats.data.languageStats}
      myLanguageIds={myLanguageIds}
    />
  ) : stats.isError ? (
    <Alert severity="error">{t('simple_paginated_list_error_message')}</Alert>
  ) : (
    <EmptyListMessage loading={true} />
  );

  return (
    <DashboardPage>
      <BaseView windowTitle={project.name} maxWidth="wide">
        <Box
          display="grid"
          gap={3}
          pt={{ xs: 1, sm: 3 }}
          pb={6}
          data-cy="community-project-page"
        >
          <MuiLink
            component={Link}
            to={LINKS.PUBLIC_PROJECTS.build()}
            underline="hover"
            color="text.secondary"
            fontSize={14}
            display="inline-flex"
            alignItems="center"
            gap={0.5}
            justifySelf="start"
            data-cy="community-back-link"
          >
            <ArrowLeft width={16} height={16} />
            {t('community_back_to_projects', 'Community projects')}
          </MuiLink>
          {compact ? (
            <Box display="grid" gap={3}>
              {header}
              {sidebar}
              {languages}
            </Box>
          ) : (
            <Box
              display="grid"
              gridTemplateColumns="minmax(0, 1fr) 400px"
              gap={5}
              alignItems="start"
            >
              <Box display="grid" gap={4}>
                {header}
                {languages}
              </Box>
              <Box
                position="sticky"
                top={stickyTop}
                maxHeight={`calc(100vh - ${stickyTop}px)`}
                overflow="auto"
              >
                {sidebar}
              </Box>
            </Box>
          )}
        </Box>
      </BaseView>
    </DashboardPage>
  );
};
